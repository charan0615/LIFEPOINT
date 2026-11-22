package com.lifepoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifepoint.dto.*;
import com.lifepoint.model.*;
import com.lifepoint.repo.*;
import com.lifepoint.service.LifePointService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LifePointApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LifePointService service;

    @Autowired
    private ProfileRepository profileRepo;

    @Autowired
    private HabitRepository habitRepo;

    @Autowired
    private SavingsGoalRepository goalRepo;

    @Autowired
    private ExpenseRepository expenseRepo;

    @Autowired
    private IncomeRepository incomeRepo;

    @Autowired
    private BudgetRepository budgetRepo;

    @Autowired
    private AchievementRepository achievementRepo;

    @Autowired
    private ObjectMapper objectMapper;

    private final String testEmail = "testuser@lifepoint.local";
    private final String userBEmail = "userb@lifepoint.local";

    @BeforeEach
    void setUp() {
        service.getOrCreateProfile(testEmail, "Test User");
        service.getOrCreateProfile(userBEmail, "User B");
    }

    // --- Core Logic & Gamification Tests ---

    @Test
    void testProfileLevelAndXpCalculation() {
        Profile profile = new Profile("xp@test.local", "XP Tester");
        assertEquals(1, profile.getLevel());
        assertEquals(0, profile.getXp());

        profile.addXp(100);
        assertEquals(3, profile.getLevel()); // floor(sqrt(100/25)) + 1 = 2 + 1 = 3
        assertTrue(profile.getLevelProgressPercent() >= 0 && profile.getLevelProgressPercent() <= 100);

        profile.addXp(150); // total 250
        assertEquals(4, profile.getLevel()); // floor(sqrt(250/25)) + 1 = 3 + 1 = 4
    }

    @Test
    void testHabitStreakProgression() {
        Habit habit = new Habit(testEmail, "Morning Yoga");
        assertEquals(0, habit.getCurrentStreak());
        assertEquals(0, habit.getBestStreak());

        // Day 1 completion
        habit.completeToday();
        assertEquals(1, habit.getCurrentStreak());
        assertEquals(1, habit.getBestStreak());
        assertTrue(habit.isDoneToday());

        // Same day completion should not re-increment
        boolean secondComplete = habit.completeToday();
        assertFalse(secondComplete);
        assertEquals(1, habit.getCurrentStreak());

        // Simulate completed yesterday and completing today
        habit.setCompletedOn(LocalDate.now().minusDays(1));
        assertFalse(habit.isDoneToday());

        habit.completeToday();
        assertEquals(2, habit.getCurrentStreak());
        assertEquals(2, habit.getBestStreak());
    }

    @Test
    void testSavedByChoiceFeature() {
        // Create savings goal
        SavingsGoal goal = service.addSavingsGoal(testEmail, "Emergency Fund", 5000, LocalDate.now().plusMonths(6), "Emergency", "#10b981");
        assertEquals(0.0, goal.getCurrentAmount());

        // Create money saving habit: "Skip Takeout Coffee" saves 200 per day into Emergency Fund
        Habit habit = service.addHabit(testEmail, "Skip Takeout Coffee", "Finance", true, 200.0, goal.getId());
        assertTrue(habit.isMoneySaver());
        assertEquals(200.0, habit.getSavedAmount());
        assertEquals(goal.getId(), habit.getLinkedGoalId());

        // Complete habit today
        boolean completed = service.completeHabit(testEmail, habit.getId());
        assertTrue(completed);

        // Check Profile total saved
        Profile profile = profileRepo.findByEmail(testEmail).orElseThrow();
        assertEquals(200.0, profile.getTotalSavedByHabits());

        // Check Savings Goal balance updated automatically
        SavingsGoal updatedGoal = goalRepo.findById(goal.getId()).orElseThrow();
        assertEquals(200.0, updatedGoal.getCurrentAmount());
        assertEquals(4, updatedGoal.getProgressPercentage()); // 200 / 5000 = 4%
    }

    @Test
    void testBudgetStatusAndAlerts() {
        // Safe budget (0 spent)
        BudgetStatus safe = new BudgetStatus("Food", 10000, 2000);
        assertEquals(20, safe.getPercentage());
        assertEquals(8000, safe.getRemaining());
        assertEquals("SAFE", safe.getStatus());

        // Warning budget (85% spent)
        BudgetStatus warning = new BudgetStatus("Dining", 5000, 4250);
        assertEquals(85, warning.getPercentage());
        assertEquals("WARNING", warning.getStatus());

        // Danger / Over budget (110% spent)
        BudgetStatus danger = new BudgetStatus("Shopping", 4000, 4500);
        assertEquals(113, danger.getPercentage());
        assertEquals(0, danger.getRemaining());
        assertEquals("DANGER", danger.getStatus());
    }

    @Test
    void testCashflowAndAnalytics() {
        service.addIncome(testEmail, "Tech Salary", "Salary", 80000, LocalDate.now());
        service.addExpense(testEmail, "Groceries", "Food", 3500, LocalDate.now());
        service.addExpense(testEmail, "Electricity Bill", "Bills", 1500, LocalDate.now());

        Map<String, Object> analytics = service.getAnalytics(testEmail);
        assertNotNull(analytics.get("categoryLabels"));
        assertNotNull(analytics.get("categoryValues"));
        assertNotNull(analytics.get("cashflowMonths"));
        assertNotNull(analytics.get("habitLabels"));
        assertNotNull(analytics.get("trendDays"));
        assertNotNull(analytics.get("trendSavings"));

        @SuppressWarnings("unchecked")
        List<String> catLabels = (List<String>) analytics.get("categoryLabels");
        assertTrue(catLabels.contains("Food"));
        assertTrue(catLabels.contains("Bills"));
    }

    @Test
    void testAchievementUnlocking() {
        // Add and complete habit
        Habit habit = service.addHabit(testEmail, "Daily Walk", "Health", false, 0, null);
        service.completeHabit(testEmail, habit.getId());

        // Check achievement unlocked
        boolean hasFirstHabit = achievementRepo.existsByOwnerEmailAndBadgeCode(testEmail, "FIRST_HABIT");
        assertTrue(hasFirstHabit);

        // Add expense
        service.addExpense(testEmail, "Coffee", "Food", 150, LocalDate.now());
        boolean hasFirstExpense = achievementRepo.existsByOwnerEmailAndBadgeCode(testEmail, "FIRST_EXPENSE");
        assertTrue(hasFirstExpense);

        // Add income
        service.addIncome(testEmail, "Freelance", "Freelance", 5000, LocalDate.now());
        boolean hasFirstIncome = achievementRepo.existsByOwnerEmailAndBadgeCode(testEmail, "FIRST_INCOME");
        assertTrue(hasFirstIncome);
    }

    // --- CRUD Operations Tests ---

    @Test
    void testHabitUpdateAndDelete() {
        Habit h = service.addHabit(testEmail, "Old Title", "Health", false, 0, null);
        assertNotNull(h.getId());

        Habit updated = service.updateHabit(testEmail, h.getId(), "New Title", "Fitness", true, 100.0, null);
        assertNotNull(updated);
        assertEquals("New Title", updated.getTitle());
        assertEquals("Fitness", updated.getCategory());
        assertTrue(updated.isMoneySaver());
        assertEquals(100.0, updated.getSavedAmount());

        service.deleteHabit(testEmail, h.getId());
        assertTrue(habitRepo.findByIdAndOwnerEmail(h.getId(), testEmail).isEmpty());
    }

    @Test
    void testExpenseUpdateAndDelete() {
        Expense e = service.addExpense(testEmail, "Lunch", "Food", 250, LocalDate.now());
        assertNotNull(e.getId());

        Expense updated = service.updateExpense(testEmail, e.getId(), "Team Lunch", "Food", 500, LocalDate.now());
        assertNotNull(updated);
        assertEquals("Team Lunch", updated.getTitle());
        assertEquals(500, updated.getAmount());

        service.deleteExpense(testEmail, e.getId());
        assertTrue(expenseRepo.findByIdAndOwnerEmail(e.getId(), testEmail).isEmpty());
    }

    @Test
    void testIncomeUpdateAndDelete() {
        Income inc = service.addIncome(testEmail, "Side Hustle", "Freelance", 12000, LocalDate.now());
        assertNotNull(inc.getId());

        Income updated = service.updateIncome(testEmail, inc.getId(), "Side Hustle Bonus", "Freelance", 15000, LocalDate.now());
        assertNotNull(updated);
        assertEquals("Side Hustle Bonus", updated.getTitle());
        assertEquals(15000, updated.getAmount());

        service.deleteIncome(testEmail, inc.getId());
        assertTrue(incomeRepo.findByIdAndOwnerEmail(inc.getId(), testEmail).isEmpty());
    }

    @Test
    void testSavingsGoalUpdateAndDelete() {
        SavingsGoal g = service.addSavingsGoal(testEmail, "Goa Trip", 25000, LocalDate.now().plusMonths(3), "Travel", "#10b981");
        assertNotNull(g.getId());

        SavingsGoal updated = service.updateSavingsGoal(testEmail, g.getId(), "Bali Trip", 45000, LocalDate.now().plusMonths(5), "Travel", "#6366f1");
        assertNotNull(updated);
        assertEquals("Bali Trip", updated.getTitle());
        assertEquals(45000, updated.getTargetAmount());

        service.deleteSavingsGoal(testEmail, g.getId());
        assertTrue(goalRepo.findByIdAndOwnerEmail(g.getId(), testEmail).isEmpty());
    }

    @Test
    void testBudgetUpdateAndDelete() {
        Budget b = service.setBudget(testEmail, "Dining", 8000);
        assertNotNull(b.getId());
        assertEquals(8000, b.getMonthlyLimit());

        Budget updated = service.updateBudget(testEmail, b.getId(), 10000);
        assertNotNull(updated);
        assertEquals(10000, updated.getMonthlyLimit());

        service.deleteBudget(testEmail, b.getId());
        assertTrue(budgetRepo.findByIdAndOwnerEmail(b.getId(), testEmail).isEmpty());
    }

    // --- Strict Cross-User Data Isolation Tests ---

    @Test
    void testCrossUserDataIsolationStrictEnforcement() {
        // User A creates resources
        Habit habitA = service.addHabit(testEmail, "User A Secret Habit", "Personal", false, 0, null);
        Expense expenseA = service.addExpense(testEmail, "User A Secret Expense", "Shopping", 999, LocalDate.now());
        Income incomeA = service.addIncome(testEmail, "User A Secret Income", "Salary", 50000, LocalDate.now());
        Budget budgetA = service.setBudget(testEmail, "Travel", 20000);
        SavingsGoal goalA = service.addSavingsGoal(testEmail, "User A Secret Goal", 100000, LocalDate.now().plusMonths(6), "General", "#6366f1");

        // Verify User B cannot access User A's habit
        assertTrue(habitRepo.findByIdAndOwnerEmail(habitA.getId(), userBEmail).isEmpty());
        Habit updateAttempt = service.updateHabit(userBEmail, habitA.getId(), "Hacked Title", "Personal", false, 0, null);
        assertNull(updateAttempt);
        service.deleteHabit(userBEmail, habitA.getId());
        assertTrue(habitRepo.findByIdAndOwnerEmail(habitA.getId(), testEmail).isPresent()); // Still intact!

        // Verify User B cannot access User A's expense
        assertTrue(expenseRepo.findByIdAndOwnerEmail(expenseA.getId(), userBEmail).isEmpty());
        Expense expenseUpdateAttempt = service.updateExpense(userBEmail, expenseA.getId(), "Hacked Expense", "Food", 1, LocalDate.now());
        assertNull(expenseUpdateAttempt);
        service.deleteExpense(userBEmail, expenseA.getId());
        assertTrue(expenseRepo.findByIdAndOwnerEmail(expenseA.getId(), testEmail).isPresent()); // Still intact!

        // Verify User B cannot access User A's income
        assertTrue(incomeRepo.findByIdAndOwnerEmail(incomeA.getId(), userBEmail).isEmpty());
        Income incomeUpdateAttempt = service.updateIncome(userBEmail, incomeA.getId(), "Hacked Income", "Other", 1, LocalDate.now());
        assertNull(incomeUpdateAttempt);
        service.deleteIncome(userBEmail, incomeA.getId());
        assertTrue(incomeRepo.findByIdAndOwnerEmail(incomeA.getId(), testEmail).isPresent()); // Still intact!

        // Verify User B cannot access User A's budget
        assertTrue(budgetRepo.findByIdAndOwnerEmail(budgetA.getId(), userBEmail).isEmpty());
        Budget budgetUpdateAttempt = service.updateBudget(userBEmail, budgetA.getId(), 50);
        assertNull(budgetUpdateAttempt);
        service.deleteBudget(userBEmail, budgetA.getId());
        assertTrue(budgetRepo.findByIdAndOwnerEmail(budgetA.getId(), testEmail).isPresent()); // Still intact!

        // Verify User B cannot access User A's savings goal or deposit into it
        assertTrue(goalRepo.findByIdAndOwnerEmail(goalA.getId(), userBEmail).isEmpty());
        SavingsGoal goalUpdateAttempt = service.updateSavingsGoal(userBEmail, goalA.getId(), "Hacked Goal", 1, LocalDate.now(), "Other", "#fff");
        assertNull(goalUpdateAttempt);
        service.depositToSavingsGoal(userBEmail, goalA.getId(), 5000);
        SavingsGoal freshGoalA = goalRepo.findByIdAndOwnerEmail(goalA.getId(), testEmail).orElseThrow();
        assertEquals(0.0, freshGoalA.getCurrentAmount()); // Deposit did not apply!
        service.deleteSavingsGoal(userBEmail, goalA.getId());
        assertTrue(goalRepo.findByIdAndOwnerEmail(goalA.getId(), testEmail).isPresent()); // Still intact!
    }

    // --- Authentication, Logout & CSRF Security Tests ---

    @Test
    void testUnauthenticatedAccessToDashboardIsProtected() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testLogoutWithoutCsrfReturns403Forbidden() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testLoginLogoutAndReloginCycle() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // 1. Initial login via Google OAuth2
        mockMvc.perform(get("/dashboard")
                .session(session)
                .with(oauth2Login().attributes(attrs -> {
                    attrs.put("email", "authflow@lifepoint.local");
                    attrs.put("name", "Auth Flow User");
                })))
                .andExpect(status().isOk());

        // 2. Perform Logout using POST /logout WITH CSRF token
        MvcResult logoutResult = mockMvc.perform(post("/logout")
                .session(session)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(cookie().maxAge("JSESSIONID", 0))
                .andReturn();

        // 3. Verify session is invalidated
        HttpSession postLogoutSession = logoutResult.getRequest().getSession(false);
        assertTrue(postLogoutSession == null || session.isInvalid(), "HTTP session must be invalidated after logout");

        // 4. Ensure protected dashboard cannot be accessed after logout
        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().is3xxRedirection());

        // 5. Login again with a fresh session
        MockHttpSession newSession = new MockHttpSession();
        mockMvc.perform(get("/dashboard")
                .session(newSession)
                .with(oauth2Login().attributes(attrs -> {
                    attrs.put("email", "authflow@lifepoint.local");
                    attrs.put("name", "Auth Flow User");
                })))
                .andExpect(status().isOk());
    }

    @Test
    void testQuickLoginEndpoint() throws Exception {
        mockMvc.perform(post("/auth/quick-login")
                .with(csrf())
                .param("email", "quickuser@lifepoint.local")
                .param("name", "Quick User"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        assertTrue(profileRepo.findByEmail("quickuser@lifepoint.local").isPresent());
    }

    @Test
    void testGoogleOAuthUnconfiguredInterceptsAndProducesClearConfigurationError() throws Exception {
        // When Google OAuth credentials are not configured or placeholder,
        // /oauth2/authorization/google must NOT silently redirect to Google with invalid_client.
        // It must redirect to /?error=oauth_missing_credentials.
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?error=oauth_missing_credentials"));

        // Verify the landing page displays the clear configuration message
        mockMvc.perform(get("/").param("error", "oauth_missing_credentials"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Google OAuth 2.0 is not configured")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("GOOGLE_CLIENT_ID")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("http://localhost:8080/login/oauth2/code/google")));
    }

    @Test
    void testGoogleOAuthLoginFlowWithMockedProvider() throws Exception {
        // Verifies Google OAuth flow creates/retrieves profile and provides authenticated session
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/dashboard")
                .session(session)
                .with(oauth2Login().attributes(attrs -> {
                    attrs.put("email", "prof.researcher@gmail.com");
                    attrs.put("name", "Prof. Researcher");
                })))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Prof. Researcher")));

        assertTrue(profileRepo.findByEmail("prof.researcher@gmail.com").isPresent());
    }

    @Test
    void testFacultyDemoLoginWorksIndependentlyOfOAuth() throws Exception {
        // 1. Direct /demo endpoint
        MockHttpSession demoSession = new MockHttpSession();
        mockMvc.perform(get("/demo").session(demoSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mockMvc.perform(get("/dashboard").session(demoSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Demo Explorer")));

        // 2. Custom Faculty review login
        MockHttpSession facultySession = new MockHttpSession();
        mockMvc.perform(post("/auth/quick-login")
                .session(facultySession)
                .with(csrf())
                .param("email", "faculty.evaluator@university.edu")
                .param("name", "Prof. Evaluator"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mockMvc.perform(get("/dashboard").session(facultySession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Prof. Evaluator")));

        assertTrue(profileRepo.findByEmail("faculty.evaluator@university.edu").isPresent());
    }

    // --- REST API Tests ---

    @Test
    @WithMockUser(username = "apiuser@lifepoint.local")
    void testRestApiHabitsCrud() throws Exception {
        // Create Habit
        HabitRequest req = new HabitRequest("Read 30 mins", "Mindset", false, 0, null);
        MvcResult createResult = mockMvc.perform(post("/api/habits")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Read 30 mins"))
                .andReturn();

        ApiResponse<?> resp = objectMapper.readValue(createResult.getResponse().getContentAsString(), ApiResponse.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.getData();
        Long habitId = ((Number) data.get("id")).longValue();

        // Complete Habit
        mockMvc.perform(post("/api/habits/" + habitId + "/complete").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Get Habits
        mockMvc.perform(get("/api/habits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Read 30 mins"));

        // Delete Habit
        mockMvc.perform(delete("/api/habits/" + habitId).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "apiuser@lifepoint.local")
    void testRestApiExpensesCrudAndFiltering() throws Exception {
        ExpenseRequest req = new ExpenseRequest("Dinner with friends", "Food", 1250, LocalDate.now());
        mockMvc.perform(post("/api/expenses")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Dinner with friends"));

        // Filter by category
        mockMvc.perform(get("/api/expenses").param("category", "Food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Food"));

        // Search query
        mockMvc.perform(get("/api/expenses").param("search", "friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Dinner with friends"));
    }

    @Test
    @WithMockUser(username = "apiuser@lifepoint.local")
    void testRestApiDashboardSummaryAndLeaderboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile.email").value("apiuser@lifepoint.local"));

        mockMvc.perform(get("/api/dashboard/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
