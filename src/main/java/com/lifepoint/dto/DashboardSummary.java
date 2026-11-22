package com.lifepoint.dto;

import com.lifepoint.model.*;
import java.util.List;

public class DashboardSummary {
    private Profile profile;
    private String currencySymbol;
    private double totalIncome;
    private double totalExpense;
    private double netCashflow;
    private double monthlyIncome;
    private double monthlyExpense;
    private double totalSavedByHabits;
    private long doneHabitsCount;
    private int habitCount;
    private int habitCompletionRate;
    private int maxStreak;
    private double totalSavedInGoals;
    private double totalTargetInGoals;
    private int goalsOverallProgress;
    private List<BudgetStatus> budgetStatuses;
    private List<Habit> habits;
    private List<Expense> expenses;
    private List<Income> incomes;
    private List<SavingsGoal> goals;
    private List<Achievement> achievements;

    public DashboardSummary() {}

    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }
    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }
    public double getTotalIncome() { return totalIncome; }
    public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }
    public double getTotalExpense() { return totalExpense; }
    public void setTotalExpense(double totalExpense) { this.totalExpense = totalExpense; }
    public double getNetCashflow() { return netCashflow; }
    public void setNetCashflow(double netCashflow) { this.netCashflow = netCashflow; }
    public double getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(double monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public double getMonthlyExpense() { return monthlyExpense; }
    public void setMonthlyExpense(double monthlyExpense) { this.monthlyExpense = monthlyExpense; }
    public double getTotalSavedByHabits() { return totalSavedByHabits; }
    public void setTotalSavedByHabits(double totalSavedByHabits) { this.totalSavedByHabits = totalSavedByHabits; }
    public long getDoneHabitsCount() { return doneHabitsCount; }
    public void setDoneHabitsCount(long doneHabitsCount) { this.doneHabitsCount = doneHabitsCount; }
    public int getHabitCount() { return habitCount; }
    public void setHabitCount(int habitCount) { this.habitCount = habitCount; }
    public int getHabitCompletionRate() { return habitCompletionRate; }
    public void setHabitCompletionRate(int habitCompletionRate) { this.habitCompletionRate = habitCompletionRate; }
    public int getMaxStreak() { return maxStreak; }
    public void setMaxStreak(int maxStreak) { this.maxStreak = maxStreak; }
    public double getTotalSavedInGoals() { return totalSavedInGoals; }
    public void setTotalSavedInGoals(double totalSavedInGoals) { this.totalSavedInGoals = totalSavedInGoals; }
    public double getTotalTargetInGoals() { return totalTargetInGoals; }
    public void setTotalTargetInGoals(double totalTargetInGoals) { this.totalTargetInGoals = totalTargetInGoals; }
    public int getGoalsOverallProgress() { return goalsOverallProgress; }
    public void setGoalsOverallProgress(int goalsOverallProgress) { this.goalsOverallProgress = goalsOverallProgress; }
    public List<BudgetStatus> getBudgetStatuses() { return budgetStatuses; }
    public void setBudgetStatuses(List<BudgetStatus> budgetStatuses) { this.budgetStatuses = budgetStatuses; }
    public List<Habit> getHabits() { return habits; }
    public void setHabits(List<Habit> habits) { this.habits = habits; }
    public List<Expense> getExpenses() { return expenses; }
    public void setExpenses(List<Expense> expenses) { this.expenses = expenses; }
    public List<Income> getIncomes() { return incomes; }
    public void setIncomes(List<Income> incomes) { this.incomes = incomes; }
    public List<SavingsGoal> getGoals() { return goals; }
    public void setGoals(List<SavingsGoal> goals) { this.goals = goals; }
    public List<Achievement> getAchievements() { return achievements; }
    public void setAchievements(List<Achievement> achievements) { this.achievements = achievements; }
}
