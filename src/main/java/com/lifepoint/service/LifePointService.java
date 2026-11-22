package com.lifepoint.service;

import com.lifepoint.dto.BudgetStatus;
import com.lifepoint.dto.DashboardSummary;
import com.lifepoint.dto.LeaderboardEntry;
import com.lifepoint.model.*;
import com.lifepoint.repo.*;
import com.lifepoint.util.CurrencyUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class LifePointService {
    private final ProfileRepository profileRepo;
    private final HabitRepository habitRepo;
    private final HabitLogRepository habitLogRepo;
    private final ExpenseRepository expenseRepo;
    private final IncomeRepository incomeRepo;
    private final BudgetRepository budgetRepo;
    private final SavingsGoalRepository goalRepo;
    private final AchievementRepository achievementRepo;
    private final RealtimeNotificationService realtime;

    public LifePointService(
            ProfileRepository profileRepo,
            HabitRepository habitRepo,
            HabitLogRepository habitLogRepo,
            ExpenseRepository expenseRepo,
            IncomeRepository incomeRepo,
            BudgetRepository budgetRepo,
            SavingsGoalRepository goalRepo,
            AchievementRepository achievementRepo,
            RealtimeNotificationService realtime) {
        this.profileRepo = profileRepo;
        this.habitRepo = habitRepo;
        this.habitLogRepo = habitLogRepo;
        this.expenseRepo = expenseRepo;
        this.incomeRepo = incomeRepo;
        this.budgetRepo = budgetRepo;
        this.goalRepo = goalRepo;
        this.achievementRepo = achievementRepo;
        this.realtime = realtime;
    }

    public Profile getOrCreateProfile(String email, String name) {
        return profileRepo.findByEmail(email).orElseGet(() -> {
            Profile newProfile = new Profile(email, name);
            return profileRepo.save(newProfile);
        });
    }

    public void updateCurrency(String email, String currency) {
        profileRepo.findByEmail(email).ifPresent(p -> {
            p.setCurrency(currency);
            profileRepo.save(p);
            realtime.broadcastDashboardUpdate(email, "CURRENCY_UPDATED", Map.of("currency", currency));
        });
    }

    // --- Habits & Gamification ---

    public Habit addHabit(String email, String title, String category, boolean moneySaver, double savedAmount, Long linkedGoalId) {
        Habit habit = new Habit(email, title, category, moneySaver, savedAmount, linkedGoalId);
        Habit saved = habitRepo.save(habit);

        // Award XP for creating habit (+10 XP)
        profileRepo.findByEmail(email).ifPresent(p -> {
            p.addXp(10);
            checkAchievements(p);
            profileRepo.save(p);
        });

        realtime.broadcastDashboardUpdate(email, "HABIT_ADDED", saved);
        return saved;
    }

    public Habit updateHabit(String email, Long habitId, String title, String category, boolean moneySaver, double savedAmount, Long linkedGoalId) {
        return habitRepo.findByIdAndOwnerEmail(habitId, email).map(h -> {
            h.update(title, category, moneySaver, savedAmount, linkedGoalId);
            Habit saved = habitRepo.save(h);
            realtime.broadcastDashboardUpdate(email, "HABIT_UPDATED", saved);
            return saved;
        }).orElse(null);
    }

    public boolean completeHabit(String email, Long habitId) {
        Habit habit = habitRepo.findByIdAndOwnerEmail(habitId, email).orElse(null);
        if (habit == null || habit.isDoneToday()) {
            return false;
        }

        boolean done = habit.completeToday();
        if (!done) return false;

        habitRepo.save(habit);

        // Record Habit Log for analytics
        double saved = habit.isMoneySaver() ? habit.getSavedAmount() : 0.0;
        HabitLog log = new HabitLog(email, habit.getId(), habit.getTitle(), LocalDate.now(), saved);
        habitLogRepo.save(log);

        // Handle Financial Link ("Saved by choice")
        if (habit.isMoneySaver() && habit.getSavedAmount() > 0) {
            profileRepo.findByEmail(email).ifPresent(p -> {
                p.addSavedByHabits(habit.getSavedAmount());
            });

            if (habit.getLinkedGoalId() != null) {
                goalRepo.findByIdAndOwnerEmail(habit.getLinkedGoalId(), email).ifPresent(goal -> {
                    boolean wasCompleted = goal.isCompleted();
                    goal.addFunds(habit.getSavedAmount());
                    goalRepo.save(goal);
                    if (!wasCompleted && goal.isCompleted()) {
                        unlockAchievement(email, "GOAL_REACHED", "Vault Unlocked", "Fully funded a savings goal!", "🏆");
                    }
                });
            }
        }

        // Award XP: 20 base + (streak * 2)
        profileRepo.findByEmail(email).ifPresent(p -> {
            int earnedXp = 20 + Math.min(50, habit.getCurrentStreak() * 2);
            p.addXp(earnedXp);

            // Check if all habits are completed today
            List<Habit> allHabits = habitRepo.findByOwnerEmail(email);
            boolean allDone = !allHabits.isEmpty() && allHabits.stream().allMatch(Habit::isDoneToday);
            if (allDone) {
                p.addXp(50); // Daily perfection bonus
            }

            checkAchievements(p);
            profileRepo.save(p);
        });

        realtime.broadcastDashboardUpdate(email, "HABIT_COMPLETED", habit);
        return true;
    }

    public void deleteHabit(String email, Long habitId) {
        habitRepo.findByIdAndOwnerEmail(habitId, email).ifPresent(h -> {
            habitRepo.delete(h);
            realtime.broadcastDashboardUpdate(email, "HABIT_DELETED", Map.of("id", habitId));
        });
    }

    // --- Expenses & Income ---

    public Expense addExpense(String email, String title, String category, double amount, LocalDate spentOn) {
        Expense expense = new Expense(email, title, category, amount, spentOn);
        Expense saved = expenseRepo.save(expense);

        profileRepo.findByEmail(email).ifPresent(p -> {
            p.addXp(5);
            checkAchievements(p);
            profileRepo.save(p);
        });

        realtime.broadcastDashboardUpdate(email, "EXPENSE_ADDED", saved);
        return saved;
    }

    public Expense updateExpense(String email, Long id, String title, String category, double amount, LocalDate spentOn) {
        return expenseRepo.findByIdAndOwnerEmail(id, email).map(e -> {
            e.update(title, category, amount, spentOn);
            Expense saved = expenseRepo.save(e);
            realtime.broadcastDashboardUpdate(email, "EXPENSE_UPDATED", saved);
            return saved;
        }).orElse(null);
    }

    public void deleteExpense(String email, Long id) {
        expenseRepo.findByIdAndOwnerEmail(id, email).ifPresent(e -> {
            expenseRepo.delete(e);
            realtime.broadcastDashboardUpdate(email, "EXPENSE_DELETED", Map.of("id", id));
        });
    }

    public Income addIncome(String email, String title, String category, double amount, LocalDate receivedOn) {
        Income income = new Income(email, title, category, amount, receivedOn);
        Income saved = incomeRepo.save(income);

        profileRepo.findByEmail(email).ifPresent(p -> {
            p.addXp(10);
            checkAchievements(p);
            profileRepo.save(p);
        });

        realtime.broadcastDashboardUpdate(email, "INCOME_ADDED", saved);
        return saved;
    }

    public Income updateIncome(String email, Long id, String title, String category, double amount, LocalDate receivedOn) {
        return incomeRepo.findByIdAndOwnerEmail(id, email).map(i -> {
            i.update(title, category, amount, receivedOn);
            Income saved = incomeRepo.save(i);
            realtime.broadcastDashboardUpdate(email, "INCOME_UPDATED", saved);
            return saved;
        }).orElse(null);
    }

    public void deleteIncome(String email, Long id) {
        incomeRepo.findByIdAndOwnerEmail(id, email).ifPresent(i -> {
            incomeRepo.delete(i);
            realtime.broadcastDashboardUpdate(email, "INCOME_DELETED", Map.of("id", id));
        });
    }

    // --- Budgets ---

    public Budget setBudget(String email, String category, double monthlyLimit) {
        Optional<Budget> existing = budgetRepo.findByOwnerEmailAndCategory(email, category);
        Budget b;
        if (existing.isPresent()) {
            b = existing.get();
            b.setMonthlyLimit(monthlyLimit);
        } else {
            b = new Budget(email, category, monthlyLimit);
        }
        Budget saved = budgetRepo.save(b);

        profileRepo.findByEmail(email).ifPresent(this::checkAchievements);
        realtime.broadcastDashboardUpdate(email, "BUDGET_SAVED", saved);
        return saved;
    }

    public Budget updateBudget(String email, Long id, double monthlyLimit) {
        return budgetRepo.findByIdAndOwnerEmail(id, email).map(b -> {
            b.setMonthlyLimit(monthlyLimit);
            Budget saved = budgetRepo.save(b);
            realtime.broadcastDashboardUpdate(email, "BUDGET_UPDATED", saved);
            return saved;
        }).orElse(null);
    }

    public void deleteBudget(String email, Long id) {
        budgetRepo.findByIdAndOwnerEmail(id, email).ifPresent(b -> {
            budgetRepo.delete(b);
            realtime.broadcastDashboardUpdate(email, "BUDGET_DELETED", Map.of("id", id));
        });
    }

    public List<BudgetStatus> getBudgetStatuses(String email) {
        List<Budget> budgets = budgetRepo.findByOwnerEmail(email);
        if (budgets.isEmpty()) return Collections.emptyList();

        LocalDate startOfMonth = YearMonth.now().atDay(1);
        LocalDate endOfMonth = YearMonth.now().atEndOfMonth();
        List<Expense> monthlyExpenses = expenseRepo.findByOwnerEmailAndSpentOnBetween(email, startOfMonth, endOfMonth);

        Map<String, Double> spendingByCategory = monthlyExpenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory, Collectors.summingDouble(Expense::getAmount)));

        List<BudgetStatus> statuses = new ArrayList<>();
        for (Budget b : budgets) {
            double spent = spendingByCategory.getOrDefault(b.getCategory(), 0.0);
            statuses.add(new BudgetStatus(b.getCategory(), b.getMonthlyLimit(), spent));
        }
        return statuses;
    }

    // --- Savings Goals ---

    public SavingsGoal addSavingsGoal(String email, String title, double targetAmount, LocalDate targetDate, String category, String color) {
        SavingsGoal goal = new SavingsGoal(email, title, targetAmount, targetDate, category, color);
        SavingsGoal saved = goalRepo.save(goal);

        profileRepo.findByEmail(email).ifPresent(p -> {
            p.addXp(15);
            checkAchievements(p);
            profileRepo.save(p);
        });

        realtime.broadcastDashboardUpdate(email, "GOAL_ADDED", saved);
        return saved;
    }

    public SavingsGoal updateSavingsGoal(String email, Long id, String title, double targetAmount, LocalDate targetDate, String category, String color) {
        return goalRepo.findByIdAndOwnerEmail(id, email).map(g -> {
            g.update(title, targetAmount, targetDate, category, color);
            SavingsGoal saved = goalRepo.save(g);
            realtime.broadcastDashboardUpdate(email, "GOAL_UPDATED", saved);
            return saved;
        }).orElse(null);
    }

    public void depositToSavingsGoal(String email, Long goalId, double amount) {
        if (amount <= 0) return;
        goalRepo.findByIdAndOwnerEmail(goalId, email).ifPresent(goal -> {
            boolean wasCompleted = goal.isCompleted();
            goal.addFunds(amount);
            goalRepo.save(goal);

            profileRepo.findByEmail(email).ifPresent(p -> {
                p.addXp(20);
                if (!wasCompleted && goal.isCompleted()) {
                    p.addXp(100);
                    unlockAchievement(email, "GOAL_REACHED", "Vault Unlocked", "Fully funded a savings goal!", "🏆");
                }
                checkAchievements(p);
                profileRepo.save(p);
            });

            realtime.broadcastDashboardUpdate(email, "GOAL_DEPOSITED", goal);
        });
    }

    public void deleteSavingsGoal(String email, Long id) {
        goalRepo.findByIdAndOwnerEmail(id, email).ifPresent(g -> {
            goalRepo.delete(g);
            realtime.broadcastDashboardUpdate(email, "GOAL_DELETED", Map.of("id", id));
        });
    }

    // --- Achievements Engine ---

    public void unlockAchievement(String email, String badgeCode, String title, String description, String icon) {
        if (!achievementRepo.existsByOwnerEmailAndBadgeCode(email, badgeCode)) {
            Achievement ach = new Achievement(email, badgeCode, title, description, icon);
            achievementRepo.save(ach);
            profileRepo.findByEmail(email).ifPresent(p -> {
                p.addXp(30); // Bonus XP for achievement
                profileRepo.save(p);
            });
            realtime.broadcastDashboardUpdate(email, "ACHIEVEMENT_UNLOCKED", ach);
        }
    }

    public void checkAchievements(Profile profile) {
        String email = profile.getEmail();
        List<Habit> habits = habitRepo.findByOwnerEmail(email);
        List<Expense> expenses = expenseRepo.findByOwnerEmailOrderBySpentOnDesc(email);
        List<Income> incomes = incomeRepo.findByOwnerEmailOrderByReceivedOnDesc(email);
        List<SavingsGoal> goals = goalRepo.findByOwnerEmailOrderByTargetDateAsc(email);
        List<Budget> budgets = budgetRepo.findByOwnerEmail(email);

        int totalHabitCompletions = habits.stream().mapToInt(Habit::getTotalCompletions).sum();
        int maxStreak = habits.stream().mapToInt(Habit::getBestStreak).max().orElse(0);

        if (totalHabitCompletions >= 1) {
            unlockAchievement(email, "FIRST_HABIT", "First Step", "Completed your first habit check-in!", "🌱");
        }
        if (maxStreak >= 3) {
            unlockAchievement(email, "STREAK_3", "Spark of Focus", "Maintained a 3-day habit streak!", "⚡");
        }
        if (maxStreak >= 7) {
            unlockAchievement(email, "STREAK_7", "Habit Titan", "Crushed a 7-day habit streak!", "🔥");
        }
        if (maxStreak >= 30) {
            unlockAchievement(email, "STREAK_30", "Iron Will", "Maintained a 30-day habit streak!", "👑");
        }
        if (totalHabitCompletions >= 10) {
            unlockAchievement(email, "HABIT_10", "Momentum Builder", "Completed 10 total habit check-ins!", "🎯");
        }
        if (totalHabitCompletions >= 50) {
            unlockAchievement(email, "HABIT_50", "Unstoppable Force", "Completed 50 habit check-ins!", "⚡");
        }
        if (!expenses.isEmpty()) {
            unlockAchievement(email, "FIRST_EXPENSE", "Mindful Spender", "Logged your first expense!", "📊");
        }
        if (!incomes.isEmpty()) {
            unlockAchievement(email, "FIRST_INCOME", "Cashflow Ignition", "Logged your first income stream!", "💰");
        }
        if (!goals.isEmpty()) {
            unlockAchievement(email, "FIRST_GOAL", "Future Architect", "Set your first savings goal!", "🏦");
        }
        if (profile.getTotalSavedByHabits() >= 500) {
            unlockAchievement(email, "CHOICE_SAVER", "Saved by Choice", "Saved 500+ through positive habits!", "💎");
        }
        if (profile.getTotalSavedByHabits() >= 2000) {
            unlockAchievement(email, "CHOICE_SAVER_2K", "Compound Genius", "Saved 2,000+ through disciplined choices!", "🚀");
        }
        if (profile.getLevel() >= 5) {
            unlockAchievement(email, "LEVEL_5", "Discipline Master", "Reached Level 5 in LIFEPOINT!", "⭐");
        }
        if (profile.getLevel() >= 10) {
            unlockAchievement(email, "LEVEL_10", "Ascended Discipline", "Reached Level 10 in LIFEPOINT!", "🌟");
        }
        if (budgets.size() >= 3) {
            unlockAchievement(email, "BUDGET_GUARDIAN", "Budget Guardian", "Configured 3 or more category budgets!", "🛡️");
        }
    }

    // --- Leaderboard ---

    public List<LeaderboardEntry> getLeaderboard() {
        List<Profile> topProfiles = profileRepo.findTop10ByOrderByXpDesc();
        List<LeaderboardEntry> entries = new ArrayList<>();
        int rank = 1;
        for (Profile p : topProfiles) {
            List<Habit> userHabits = habitRepo.findByOwnerEmail(p.getEmail());
            int completions = userHabits.stream().mapToInt(Habit::getTotalCompletions).sum();
            int maxStreak = userHabits.stream().mapToInt(Habit::getBestStreak).max().orElse(0);

            // Anonymize email: johndoe@gmail.com -> j***e@gmail.com
            String maskedEmail = maskEmail(p.getEmail());
            entries.add(new LeaderboardEntry(
                rank++,
                p.getName() != null && !p.getName().isBlank() ? p.getName() : "Disciplined Member",
                maskedEmail,
                p.getLevel(),
                p.getXp(),
                p.getTotalSavedByHabits(),
                maxStreak
            ));
        }
        return entries;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "user@lifepoint";
        int atIdx = email.indexOf('@');
        String namePart = email.substring(0, atIdx);
        String domainPart = email.substring(atIdx);
        if (namePart.length() <= 2) {
            return namePart.charAt(0) + "***" + domainPart;
        }
        return namePart.charAt(0) + "***" + namePart.charAt(namePart.length() - 1) + domainPart;
    }

    // --- Dashboard Summary ---

    public DashboardSummary getDashboardSummary(String email) {
        Profile p = getOrCreateProfile(email, "User");
        List<Habit> userHabits = habitRepo.findByOwnerEmail(email);
        List<Expense> userExpenses = expenseRepo.findByOwnerEmailOrderBySpentOnDesc(email);
        List<Income> userIncomes = incomeRepo.findByOwnerEmailOrderByReceivedOnDesc(email);
        List<BudgetStatus> budgetStatuses = getBudgetStatuses(email);
        List<SavingsGoal> userGoals = goalRepo.findByOwnerEmailOrderByTargetDateAsc(email);
        List<Achievement> userAchievements = achievementRepo.findByOwnerEmailOrderByUnlockedOnDesc(email);

        double totalExpense = userExpenses.stream().mapToDouble(Expense::getAmount).sum();
        double totalIncome = userIncomes.stream().mapToDouble(Income::getAmount).sum();
        double netCashflow = totalIncome - totalExpense;

        LocalDate startOfMonth = YearMonth.now().atDay(1);
        LocalDate endOfMonth = YearMonth.now().atEndOfMonth();
        double monthlyExpense = userExpenses.stream()
                .filter(e -> e.getSpentOn() != null && !e.getSpentOn().isBefore(startOfMonth) && !e.getSpentOn().isAfter(endOfMonth))
                .mapToDouble(Expense::getAmount).sum();
        double monthlyIncome = userIncomes.stream()
                .filter(i -> i.getReceivedOn() != null && !i.getReceivedOn().isBefore(startOfMonth) && !i.getReceivedOn().isAfter(endOfMonth))
                .mapToDouble(Income::getAmount).sum();

        long doneHabitsCount = userHabits.stream().filter(Habit::isDoneToday).count();
        int habitCount = userHabits.size();
        int habitCompletionRate = habitCount > 0 ? (int) Math.round(((double) doneHabitsCount / habitCount) * 100) : 0;
        int maxStreak = userHabits.stream().mapToInt(Habit::getCurrentStreak).max().orElse(0);

        double totalSavedInGoals = userGoals.stream().mapToDouble(SavingsGoal::getCurrentAmount).sum();
        double totalTargetInGoals = userGoals.stream().mapToDouble(SavingsGoal::getTargetAmount).sum();
        int goalsOverallProgress = totalTargetInGoals > 0 ? (int) Math.round((totalSavedInGoals / totalTargetInGoals) * 100) : 0;

        DashboardSummary summary = new DashboardSummary();
        summary.setProfile(p);
        summary.setCurrencySymbol(CurrencyUtils.getSymbol(p.getCurrency()));
        summary.setTotalIncome(totalIncome);
        summary.setTotalExpense(totalExpense);
        summary.setNetCashflow(netCashflow);
        summary.setMonthlyIncome(monthlyIncome);
        summary.setMonthlyExpense(monthlyExpense);
        summary.setTotalSavedByHabits(p.getTotalSavedByHabits());
        summary.setDoneHabitsCount(doneHabitsCount);
        summary.setHabitCount(habitCount);
        summary.setHabitCompletionRate(habitCompletionRate);
        summary.setMaxStreak(maxStreak);
        summary.setTotalSavedInGoals(totalSavedInGoals);
        summary.setTotalTargetInGoals(totalTargetInGoals);
        summary.setGoalsOverallProgress(Math.min(100, goalsOverallProgress));
        summary.setBudgetStatuses(budgetStatuses);
        summary.setHabits(userHabits);
        summary.setExpenses(userExpenses);
        summary.setIncomes(userIncomes);
        summary.setGoals(userGoals);
        summary.setAchievements(userAchievements);

        return summary;
    }

    // --- Analytics & Chart Data ---

    public Map<String, Object> getAnalytics(String email) {
        Map<String, Object> data = new HashMap<>();

        // 1. Category Expense Breakdown (Current Month)
        LocalDate startOfMonth = YearMonth.now().atDay(1);
        LocalDate endOfMonth = YearMonth.now().atEndOfMonth();
        List<Expense> monthlyExpenses = expenseRepo.findByOwnerEmailAndSpentOnBetween(email, startOfMonth, endOfMonth);
        Map<String, Double> categoryTotals = monthlyExpenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory, Collectors.summingDouble(Expense::getAmount)));

        data.put("categoryLabels", new ArrayList<>(categoryTotals.keySet()));
        data.put("categoryValues", new ArrayList<>(categoryTotals.values()));

        // 2. 7-Day Habit Activity
        LocalDate today = LocalDate.now();
        List<String> last7DaysLabels = new ArrayList<>();
        List<Integer> last7DaysCompletions = new ArrayList<>();
        List<HabitLog> logs7Days = habitLogRepo.findByOwnerEmailAndCompletedDateBetween(email, today.minusDays(6), today);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE (dd)");
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            last7DaysLabels.add(d.format(dtf));
            long count = logs7Days.stream().filter(l -> d.equals(l.getCompletedDate())).count();
            last7DaysCompletions.add((int) count);
        }
        data.put("habitLabels", last7DaysLabels);
        data.put("habitCounts", last7DaysCompletions);

        // 3. 6-Month Cashflow (Income vs Expense)
        List<String> monthLabels = new ArrayList<>();
        List<Double> monthlyIncomeList = new ArrayList<>();
        List<Double> monthlyExpenseList = new ArrayList<>();

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM yyyy");
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            monthLabels.add(ym.format(monthFmt));

            LocalDate mStart = ym.atDay(1);
            LocalDate mEnd = ym.atEndOfMonth();

            double incSum = incomeRepo.findByOwnerEmailAndReceivedOnBetween(email, mStart, mEnd).stream()
                    .mapToDouble(Income::getAmount).sum();
            double expSum = expenseRepo.findByOwnerEmailAndSpentOnBetween(email, mStart, mEnd).stream()
                    .mapToDouble(Expense::getAmount).sum();

            monthlyIncomeList.add(incSum);
            monthlyExpenseList.add(expSum);
        }

        data.put("cashflowMonths", monthLabels);
        data.put("cashflowIncome", monthlyIncomeList);
        data.put("cashflowExpense", monthlyExpenseList);

        // 4. Savings Vaults Growth Distribution
        List<SavingsGoal> goals = goalRepo.findByOwnerEmailOrderByTargetDateAsc(email);
        List<String> goalLabels = new ArrayList<>();
        List<Double> goalSavedList = new ArrayList<>();
        List<Double> goalTargetList = new ArrayList<>();
        for (SavingsGoal g : goals) {
            goalLabels.add(g.getTitle());
            goalSavedList.add(g.getCurrentAmount());
            goalTargetList.add(g.getTargetAmount());
        }
        data.put("goalLabels", goalLabels);
        data.put("goalSaved", goalSavedList);
        data.put("goalTarget", goalTargetList);

        // 5. 30-Day Cumulative Habit Savings Trend ("Saved by Choice")
        List<String> day30Labels = new ArrayList<>();
        List<Double> cumulativeSavings = new ArrayList<>();
        List<HabitLog> logs30Days = habitLogRepo.findByOwnerEmailAndCompletedDateBetween(email, today.minusDays(29), today);
        double runningSaved = 0.0;
        DateTimeFormatter shortDayFmt = DateTimeFormatter.ofPattern("dd MMM");

        for (int i = 29; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            day30Labels.add(d.format(shortDayFmt));
            double daySaved = logs30Days.stream()
                    .filter(l -> d.equals(l.getCompletedDate()))
                    .mapToDouble(HabitLog::getSavedAmount).sum();
            runningSaved += daySaved;
            cumulativeSavings.add(runningSaved);
        }
        data.put("trendDays", day30Labels);
        data.put("trendSavings", cumulativeSavings);

        return data;
    }
}
