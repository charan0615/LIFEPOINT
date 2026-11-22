package com.lifepoint.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifepoint.dto.BudgetStatus;
import com.lifepoint.dto.LeaderboardEntry;
import com.lifepoint.model.*;
import com.lifepoint.repo.*;
import com.lifepoint.service.LifePointService;
import com.lifepoint.util.CurrencyUtils;
import com.lifepoint.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Controller
public class AppController {
    private final LifePointService service;
    private final HabitRepository habits;
    private final ExpenseRepository expenses;
    private final IncomeRepository incomes;
    private final SavingsGoalRepository goals;
    private final AchievementRepository achievements;
    private final ObjectMapper objectMapper;
    private final com.lifepoint.config.GoogleOAuthConfig googleOAuthConfig;

    private static final Set<String> CURRENCIES = Set.of("INR", "USD", "EUR", "GBP", "AED", "CAD", "AUD", "JPY", "SGD");

    public AppController(
            LifePointService service,
            HabitRepository habits,
            ExpenseRepository expenses,
            IncomeRepository incomes,
            SavingsGoalRepository goals,
            AchievementRepository achievements,
            ObjectMapper objectMapper,
            com.lifepoint.config.GoogleOAuthConfig googleOAuthConfig) {
        this.service = service;
        this.habits = habits;
        this.expenses = expenses;
        this.incomes = incomes;
        this.goals = goals;
        this.achievements = achievements;
        this.objectMapper = objectMapper;
        this.googleOAuthConfig = googleOAuthConfig;
    }

    private Profile resolveProfile(Principal principal) {
        if (principal == null) return null;
        String email = SecurityUtils.resolveEmail(principal);
        String name = SecurityUtils.resolveName(principal);

        if (email == null || email.isBlank()) {
            email = "demo@lifepoint.local";
        }
        if (name == null || name.isBlank()) {
            name = "Lifepoint Member";
        }

        return service.getOrCreateProfile(email, name);
    }

    @GetMapping({"/", "/index"})
    public String home(
            Principal principal,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String message,
            Model model) {
        if (principal != null) {
            return "redirect:/dashboard";
        }

        boolean isConfigured = googleOAuthConfig.isConfigured();
        model.addAttribute("googleOAuthConfigured", isConfigured);

        if ("oauth_missing_credentials".equalsIgnoreCase(error)) {
            model.addAttribute("oauthError",
                "Google OAuth 2.0 is not configured: GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET environment variables are missing or set to placeholder values. " +
                "To enable Google Sign-In, set valid Google Cloud Console credentials with Authorized Redirect URI: http://localhost:8080/login/oauth2/code/google. " +
                "Meanwhile, use the Faculty / Instant Access login below to evaluate all features independently.");
        } else if ("oauth_failure".equalsIgnoreCase(error)) {
            String detail = (message != null && !message.isBlank()) ? " (" + message + ")" : "";
            model.addAttribute("oauthError",
                "Google Sign-In failed" + detail + ". Please verify your credentials or use the Faculty / Instant Access login below.");
        } else if ("oauth".equalsIgnoreCase(error)) {
            model.addAttribute("oauthError",
                "Google Sign-In could not authenticate. You can use Faculty / Instant Access login below to test with any profile!");
        }
        return "home";
    }

    @PostMapping("/auth/quick-login")
    public String quickLogin(
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "Disciplined Member") String name,
            HttpServletRequest request) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            email = "demo@lifepoint.local";
        }
        String cleanEmail = email.trim().toLowerCase();
        String cleanName = (name != null && !name.isBlank()) ? name.trim() : "Disciplined Member";

        service.getOrCreateProfile(cleanEmail, cleanName);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                cleanEmail, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return "redirect:/dashboard";
    }

    @GetMapping("/demo")
    public String demoLogin(HttpServletRequest request) {
        String email = "demo@lifepoint.local";
        String name = "Demo Explorer";

        service.getOrCreateProfile(email, name);

        // Preload rich sample data if profile is brand new so demo shines immediately
        List<Habit> existingHabits = habits.findByOwnerEmail(email);
        if (existingHabits.isEmpty()) {
            SavingsGoal emergencyGoal = service.addSavingsGoal(email, "Emergency Fund", 50000, LocalDate.now().plusMonths(6), "Emergency Fund", "#10b981");
            service.depositToSavingsGoal(email, emergencyGoal.getId(), 12500);

            SavingsGoal techGoal = service.addSavingsGoal(email, "New MacBook / Workspace", 80000, LocalDate.now().plusMonths(4), "Gadgets", "#6366f1");
            service.depositToSavingsGoal(email, techGoal.getId(), 30000);

            service.addHabit(email, "Morning Deep Work (60m)", "Productivity", false, 0.0, null);
            service.addHabit(email, "Cook healthy dinner at home", "Health", true, 350.0, emergencyGoal.getId());
            service.addHabit(email, "Skip food delivery & impulse snacks", "Finance", true, 250.0, emergencyGoal.getId());
            service.addHabit(email, "30-Min Gym / Run", "Fitness", false, 0.0, null);

            service.addIncome(email, "Primary Salary", "Salary", 75000, LocalDate.now().minusDays(5));
            service.addIncome(email, "Freelance UI Consulting", "Freelance", 18000, LocalDate.now().minusDays(12));

            service.addExpense(email, "Apartment Rent", "Bills", 18000, LocalDate.now().minusDays(14));
            service.addExpense(email, "Organic Grocery Haul", "Food", 3200, LocalDate.now().minusDays(3));
            service.addExpense(email, "High-Speed Fiber Internet", "Bills", 1199, LocalDate.now().minusDays(8));
            service.addExpense(email, "Book: Atomic Habits", "Education", 499, LocalDate.now().minusDays(2));

            service.setBudget(email, "Food", 12000);
            service.setBudget(email, "Bills", 25000);
            service.setBudget(email, "Shopping", 8000);
            service.setBudget(email, "Travel", 5000);

            // Complete first habit to trigger XP, streak and badge
            List<Habit> createdHabits = habits.findByOwnerEmail(email);
            if (!createdHabits.isEmpty()) {
                service.completeHabit(email, createdHabits.get(0).getId());
            }
        }

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) throws JsonProcessingException {
        Profile p = resolveProfile(principal);
        if (p == null) {
            return "redirect:/";
        }

        String email = p.getEmail();

        List<Habit> userHabits = habits.findByOwnerEmail(email);
        List<Expense> userExpenses = expenses.findByOwnerEmailOrderBySpentOnDesc(email);
        List<Income> userIncomes = incomes.findByOwnerEmailOrderByReceivedOnDesc(email);
        List<BudgetStatus> budgetStatuses = service.getBudgetStatuses(email);
        List<SavingsGoal> userGoals = goals.findByOwnerEmailOrderByTargetDateAsc(email);
        List<Achievement> userAchievements = achievements.findByOwnerEmailOrderByUnlockedOnDesc(email);
        List<LeaderboardEntry> leaderboard = service.getLeaderboard();

        // Core Financial Metrics
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

        // Habit Metrics
        long doneHabitsCount = userHabits.stream().filter(Habit::isDoneToday).count();
        int habitCount = userHabits.size();
        int habitCompletionRate = habitCount > 0 ? (int) Math.round(((double) doneHabitsCount / habitCount) * 100) : 0;
        int maxStreak = userHabits.stream().mapToInt(Habit::getCurrentStreak).max().orElse(0);

        // Savings & Goals Metrics
        double totalSavedInGoals = userGoals.stream().mapToDouble(SavingsGoal::getCurrentAmount).sum();
        double totalTargetInGoals = userGoals.stream().mapToDouble(SavingsGoal::getTargetAmount).sum();
        int goalsOverallProgress = totalTargetInGoals > 0 ? (int) Math.round((totalSavedInGoals / totalTargetInGoals) * 100) : 0;

        // Analytics Data for Chart.js
        Map<String, Object> analytics = service.getAnalytics(email);

        model.addAttribute("profile", p);
        model.addAttribute("currencySymbol", CurrencyUtils.getSymbol(p.getCurrency()));
        model.addAttribute("currencies", CURRENCIES);

        model.addAttribute("habits", userHabits);
        model.addAttribute("doneHabitsCount", doneHabitsCount);
        model.addAttribute("habitCount", habitCount);
        model.addAttribute("habitCompletionRate", habitCompletionRate);
        model.addAttribute("maxStreak", maxStreak);

        model.addAttribute("expenses", userExpenses);
        model.addAttribute("incomes", userIncomes);
        model.addAttribute("totalExpense", totalExpense);
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("netCashflow", netCashflow);
        model.addAttribute("monthlyExpense", monthlyExpense);
        model.addAttribute("monthlyIncome", monthlyIncome);
        model.addAttribute("totalSavedByHabits", p.getTotalSavedByHabits());

        model.addAttribute("budgetStatuses", budgetStatuses);
        model.addAttribute("goals", userGoals);
        model.addAttribute("totalSavedInGoals", totalSavedInGoals);
        model.addAttribute("goalsOverallProgress", Math.min(100, goalsOverallProgress));
        model.addAttribute("achievements", userAchievements);
        model.addAttribute("leaderboard", leaderboard);

        // Chart JSON attributes
        model.addAttribute("categoryLabelsJson", objectMapper.writeValueAsString(analytics.get("categoryLabels")));
        model.addAttribute("categoryValuesJson", objectMapper.writeValueAsString(analytics.get("categoryValues")));
        model.addAttribute("habitLabelsJson", objectMapper.writeValueAsString(analytics.get("habitLabels")));
        model.addAttribute("habitCountsJson", objectMapper.writeValueAsString(analytics.get("habitCounts")));
        model.addAttribute("cashflowMonthsJson", objectMapper.writeValueAsString(analytics.get("cashflowMonths")));
        model.addAttribute("cashflowIncomeJson", objectMapper.writeValueAsString(analytics.get("cashflowIncome")));
        model.addAttribute("cashflowExpenseJson", objectMapper.writeValueAsString(analytics.get("cashflowExpense")));
        model.addAttribute("goalLabelsJson", objectMapper.writeValueAsString(analytics.get("goalLabels")));
        model.addAttribute("goalSavedJson", objectMapper.writeValueAsString(analytics.get("goalSaved")));
        model.addAttribute("goalTargetJson", objectMapper.writeValueAsString(analytics.get("goalTarget")));
        model.addAttribute("trendDaysJson", objectMapper.writeValueAsString(analytics.get("trendDays")));
        model.addAttribute("trendSavingsJson", objectMapper.writeValueAsString(analytics.get("trendSavings")));

        return "dashboard";
    }

    // --- Habits ---

    @PostMapping("/habits")
    public String addHabit(
            Principal principal,
            @RequestParam String title,
            @RequestParam(required = false, defaultValue = "Personal") String category,
            @RequestParam(required = false, defaultValue = "false") boolean moneySaver,
            @RequestParam(required = false, defaultValue = "0") double savedAmount,
            @RequestParam(required = false) Long linkedGoalId,
            RedirectAttributes ra) {
        Profile p = resolveProfile(principal);
        if (p == null) return "redirect:/";

        if (title == null || title.isBlank() || title.length() > 90) {
            ra.addFlashAttribute("habitError", "Please provide a valid habit title.");
            return "redirect:/dashboard";
        }

        service.addHabit(p.getEmail(), title.trim(), category, moneySaver, Math.max(0, savedAmount), linkedGoalId);
        ra.addFlashAttribute("successMessage", "Habit added! +10 XP");
        return "redirect:/dashboard";
    }

    @PostMapping("/habits/{id}/edit")
    public String editHabit(
            Principal principal,
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false, defaultValue = "Personal") String category,
            @RequestParam(required = false, defaultValue = "false") boolean moneySaver,
            @RequestParam(required = false, defaultValue = "0") double savedAmount,
            @RequestParam(required = false) Long linkedGoalId,
            RedirectAttributes ra) {
        Profile p = resolveProfile(principal);
        if (p == null) return "redirect:/";

        if (title == null || title.isBlank() || title.length() > 90) {
            ra.addFlashAttribute("habitError", "Please provide a valid habit title.");
            return "redirect:/dashboard";
        }

        service.updateHabit(p.getEmail(), id, title.trim(), category, moneySaver, Math.max(0, savedAmount), linkedGoalId);
        ra.addFlashAttribute("successMessage", "Habit updated successfully!");
        return "redirect:/dashboard";
    }

    @PostMapping("/habits/{id}/complete")
    public String completeHabit(Principal principal, @PathVariable Long id, RedirectAttributes ra) {
        Profile p = resolveProfile(principal);
        if (p == null) return "redirect:/";

        boolean done = service.completeHabit(p.getEmail(), id);
        if (done) {
            ra.addFlashAttribute("successMessage", "Habit completed! Streak extended and XP earned!");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/habits/{id}/delete")
    public String deleteHabit(Principal principal, @PathVariable Long id) {
        Profile p = resolveProfile(principal);
        if (p != null) {
            service.deleteHabit(p.getEmail(), id);
        }
        return "redirect:/dashboard";
    }

    // --- Expenses ---

    @PostMapping("/expenses")
    public String addExpense(
            Principal principal,
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam double amount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate spentOn,
            RedirectAttributes ra) {
        Profile p = resolveProfile(principal);
        if (p == null) return "redirect:/";

        if (title == null || title.isBlank() || amount <= 0 || amount > 100000000) {
            ra.addFlashAttribute("expenseError", "Enter a valid description and positive amount.");
            return "redirect:/dashboard";
        }

        LocalDate date = (spentOn != null) ? spentOn : LocalDate.now();
        service.addExpense(p.getEmail(), title.trim(), category, amount, date);
        ra.addFlashAttribute("successMessage", "Expense recorded. +5 XP");
        return "redirect:/dashboard";
    }

    @PostMapping("/expenses/{id}/edit")
    public String editExpense(
            Principal principal,
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam double amount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate spentOn,
            RedirectAttributes ra) {
        Profile p = resolveProfile(principal);
        if (p == null) return "redirect:/";

        if (title == null || title.isBlank() || amount <= 0 || amount > 100000000) {
            ra.addFlashAttribute("expenseError", "Enter a valid description and positive amount.");
            return "redirect:/dashboard";
        }

        LocalDate date = (spentOn != null) ? spentOn : LocalDate.now();
        service.updateExpense(p.getEmail(), id, title.trim(), category, amount, date);
        ra.addFlashAttribute("successMessage", "Expense updated successfully.");
        return "redirect:/dashboard";
    }

    @PostMapping("/expenses/{id}/delete")
    public String deleteExpense(Principal principal, @PathVariable Long id) {
        Profile p = resolveProfile(principal);
        if (p != null) {
            service.deleteExpense(p.getEmail(), id);
        }
        return "redirect:/dashboard";
    }

    // --- Income ---

    @PostMapping("/income")
    public String addIncome(
            Principal principal,
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam double amount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receivedOn,
            RedirectAttributes ra) {
        Profile p = resolveProfile(principal);
        if (p == null) return "redirect:/";

        if (title == null || title.isBlank() || amount <= 0 || amount > 100000000) {
            ra.addFlashAttribute("incomeError", "Enter a valid description and positive amount.");
            return "redirect:/dashboard";
        }

        LocalDate date = (receivedOn != null) ? receivedOn : LocalDate.now();
        service.addIncome(p.getEmail(), title.trim(), category, amount, date);
        ra.addFlashAttribute("successMessage", "Income logged! +10 XP");
        return "redirect:/dashboard";
    }

    @PostMapping("/income/{id}/edit")
    public String editIncome(
            Principal principal,
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam double amount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receivedOn,
            RedirectAttributes ra) {
        Profile p = resolveProfile(principal);
        if (p == null) return "redirect:/";

        if (title == null || title.isBlank() || amount <= 0 || amount > 100000000) {
            ra.addFlashAttribute("incomeError", "Enter a valid description and positive amount.");
            return "redirect:/dashboard";
        }

        LocalDate date = (receivedOn != null) ? receivedOn : LocalDate.now();
        service.updateIncome(p.getEmail(), id, title.trim(), category, amount, date);
        ra.addFlashAttribute("successMessage", "Income updated successfully.");
        return "redirect:/dashboard";
    }

    @PostMapping("/income/{id}/delete")
    public String deleteIncome(Principal principal, @PathVariable Long id) {
        Profile p = resolveProfile(principal);
        if (p != null) {
            service.deleteIncome(p.getEmail(), id);
        }
        return "redirect:/dashboard";
    }

    // --- Budgets ---

    @PostMapping("/budgets")
    public String setBudget(
            Principal principal,
            @RequestParam String category,
            @RequestParam double monthlyLimit,
            RedirectAttributes ra) {
        Profile p = resolveProfile(principal);
        if (p == null) return "redirect:/";

        if (category == null || category.isBlank() || monthlyLimit <= 0) {
            ra.addFlashAttribute("budgetError", "Provide a category and a positive monthly budget.");
            return "redirect:/dashboard";
        }

        service.setBudget(p.getEmail(), category.trim(), monthlyLimit);
        ra.addFlashAttribute("successMessage", "Monthly budget updated!");
        return "redirect:/dashboard";
    }

    @PostMapping("/budgets/{id}/delete")
    public String deleteBudget(Principal principal, @PathVariable Long id) {
        Profile p = resolveProfile(principal);
        if (p != null) {
            service.deleteBudget(p.getEmail(), id);
        }
        return "redirect:/dashboard";
    }

    // --- Savings Goals ---

    @PostMapping("/goals")
    public String addSavingsGoal(
            Principal principal,
            @RequestParam String title,
            @RequestParam double targetAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
            @RequestParam(required = false, defaultValue = "General") String category,
            @RequestParam(required = false, defaultValue = "#6366f1") String color,
            RedirectAttributes ra) {
        Profile p = resolveProfile(principal);
        if (p == null) return "redirect:/";

        if (title == null || title.isBlank() || targetAmount <= 0) {
            ra.addFlashAttribute("goalError", "Provide a goal name and target amount.");
            return "redirect:/dashboard";
        }

        LocalDate date = (targetDate != null) ? targetDate : LocalDate.now().plusMonths(6);
        service.addSavingsGoal(p.getEmail(), title.trim(), targetAmount, date, category, color);
        ra.addFlashAttribute("successMessage", "Savings goal created! +15 XP");
        return "redirect:/dashboard";
    }

    @PostMapping("/goals/{id}/edit")
    public String editSavingsGoal(
            Principal principal,
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam double targetAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
            @RequestParam(required = false, defaultValue = "General") String category,
            @RequestParam(required = false, defaultValue = "#6366f1") String color,
            RedirectAttributes ra) {
        Profile p = resolveProfile(principal);
        if (p == null) return "redirect:/";

        if (title == null || title.isBlank() || targetAmount <= 0) {
            ra.addFlashAttribute("goalError", "Provide a goal name and target amount.");
            return "redirect:/dashboard";
        }

        LocalDate date = (targetDate != null) ? targetDate : LocalDate.now().plusMonths(6);
        service.updateSavingsGoal(p.getEmail(), id, title.trim(), targetAmount, date, category, color);
        ra.addFlashAttribute("successMessage", "Savings goal updated!");
        return "redirect:/dashboard";
    }

    @PostMapping("/goals/{id}/deposit")
    public String depositToGoal(
            Principal principal,
            @PathVariable Long id,
            @RequestParam double amount,
            RedirectAttributes ra) {
        Profile p = resolveProfile(principal);
        if (p == null) return "redirect:/";

        if (amount <= 0) {
            ra.addFlashAttribute("goalError", "Deposit amount must be positive.");
            return "redirect:/dashboard";
        }

        service.depositToSavingsGoal(p.getEmail(), id, amount);
        ra.addFlashAttribute("successMessage", "Deposited into savings vault! +20 XP");
        return "redirect:/dashboard";
    }

    @PostMapping("/goals/{id}/delete")
    public String deleteSavingsGoal(Principal principal, @PathVariable Long id) {
        Profile p = resolveProfile(principal);
        if (p != null) {
            service.deleteSavingsGoal(p.getEmail(), id);
        }
        return "redirect:/dashboard";
    }

    // --- Currency Preference ---

    @PostMapping("/currency")
    public String currency(Principal principal, @RequestParam String currency) {
        Profile p = resolveProfile(principal);
        if (p != null && CURRENCIES.contains(currency)) {
            service.updateCurrency(p.getEmail(), currency);
        }
        return "redirect:/dashboard";
    }
}
