# LIFEPOINT — Personal Growth & Wealth Operating System

A production-grade, full-stack Spring Boot web application uniting behavioral habit psychology with modern fintech cashflow, category budgeting, target savings vaults, multi-currency support, gamification, Chart.js analytics, and real-time WebSockets.

---

## 🚀 Key Features

### 1. User Authentication & Multi-Tenancy
- **Google OAuth 2.0**: Full sign-in flow (`openid`, `profile`, `email`) creating isolated user profiles.
- **Fixed Google OAuth 401 `invalid_client` Protection**: Previously, when environment variables were not configured, Spring Boot silently sent placeholder values (`your-client-id`) to Google, triggering an external Google 401 error page. With the `GoogleOAuthValidationFilter` and `GoogleOAuthConfig`, missing or placeholder credentials are automatically detected:
  - Startup logs a clear, structured configuration guide with exact setup steps.
  - Any attempt to initiate Google OAuth without credentials is intercepted before contacting Google and redirects to a clear, actionable configuration message.
  - Zero hardcoded client secrets.
- **Faculty / Instant Access Login**: Operates completely independently of Google OAuth. Evaluators and faculty can log in as `Prof. Davis (Faculty Reviewer)`, `Demo Explorer`, or enter any custom email with 100% strict data isolation.
- **Strict Cross-User Isolation**: Every database entity (`Habit`, `Expense`, `Income`, `Budget`, `SavingsGoal`, `Achievement`, `HabitLog`) is strictly bound to `ownerEmail` with repository-level query isolation. Users can never view, update, or delete other users' records.
- **Secure Session Management & CSRF**: Complete session invalidation on logout, cookie cleanup, and Spring Security CSRF protection for all REST endpoints and forms.

### 2. Gamified Habit Tracker
- **Full CRUD**: Create, edit, and delete habits across categories (`Health`, `Finance`, `Productivity`, `Fitness`, `Mindset`).
- **Streak Calculation**: Tracks `currentStreak`, `bestStreak`, and `totalCompletions` with daily flame badges (`🔥`).
- **"Saved by Choice" Linkage**: Connects money-saving habits (e.g. skip takeout, brew coffee at home, avoid impulse rides) directly to **Savings Vaults** to auto-deposit on daily check-in.
- **XP & Level Rewards**: Earn +10 XP on habit creation, +20 XP + streak bonus on check-in, and +50 XP perfection bonus when all habits are completed for the day.

### 3. Expense & Income Cashflow Tracker
- **Full CRUD**: Add, edit, and delete categorized expenses and income streams.
- **Multi-filter Toolbar**: Filter transactions by search text, category, and transaction type (Expense/Income) with live search and sorting.
- **Monthly Spending Summaries**: Live calculation of total income, total expense, and net cashflow.

### 4. Category Budget Management
- **Category Spending Thresholds**: Configure monthly budget limits for Food, Bills, Shopping, Travel, Education, Health, Entertainment, Other.
- **Automatic Budget Health Status**:
  - `SAFE` (< 80% used) — Emerald status.
  - `WARNING` (80% – 100% used) — Amber alert badge.
  - `DANGER` (> 100% exceeded) — Over-limit alert badge with pulsing neon indicator.

### 5. Target Savings Vaults
- **Target Wealth Milestones**: Create, edit, and delete savings goals with target amounts, target dates, categories, and custom badges.
- **Quick Deposits**: Add funds directly to any savings vault with instant progress percentage updates and XP rewards.
- **Milestone Unlocking**: Auto-completes goal and awards +100 XP bonus and "Vault Unlocked" trophy upon hitting 100% target.

### 6. Gamification & Community Leaderboard
- **XP & Levels Engine**: Mathematical curve threshold `floor(sqrt(xp / 25)) + 1` with live progress bars.
- **12+ Unlockable Achievement Badges**:
  - 🌱 *First Step*, ⚡ *Spark of Focus (3d)*, 🔥 *Habit Titan (7d)*, 👑 *Iron Will (30d)*, 🎯 *Momentum Builder*, 📊 *Mindful Spender*, 💰 *Cashflow Ignition*, 🏦 *Future Architect*, 💎 *Saved by Choice*, 🚀 *Compound Genius*, ⭐ *Discipline Master (Lvl 5)*, 🌟 *Ascended Discipline (Lvl 10)*, 🏆 *Vault Unlocked*, 🛡️ *Budget Guardian*.
- **Live Leaderboard**: Top 10 disciplined members ranked by XP, level, total habit ROI savings, and streak.

### 7. Multi-Currency Support (Default: INR ₹)
- Default currency: Indian Rupee (`INR` — `₹`).
- Seamless switching across 9 global currencies: `INR` (₹), `USD` ($), `EUR` (€), `GBP` (£), `AED` (AED), `CAD` (C$), `AUD` (A$), `JPY` (¥), `SGD` (S$).
- Updates user profile preference in the database and updates all symbols instantly.

### 8. Analytics & Working Chart.js Charts
- **Cashflow Trajectory**: 6-month monthly Income vs. Expense bar chart.
- **Monthly Spending Distribution**: Category expense doughnut chart with percentage breakdown.
- **7-Day Habit Activity**: Check-ins completed over the last 7 days.
- **"Saved by Choice" Trajectory**: 30-day cumulative compound savings area line chart.

### 9. Real-Time WebSockets & Interactive UI
- **WebSocket / STOMP Integration**: Subscribes to `/topic/dashboard/{email}` and `/topic/dashboard` via SockJS.
- **Dynamic In-Place Updates**: Instant optimistic and confirmed DOM updates via REST APIs and WebSocket synchronization without full page reloads.
- **Slide-in Toast Notifications**: Instant feedback for XP earned, habit check-ins, and trophy unlocks.

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Backend** | Java 17, Spring Boot 3.4.2, Spring Security 6, Spring Data JPA, Spring WebSocket (STOMP / SockJS), Spring Validation |
| **Frontend** | HTML5, CSS3 (Modern Glassmorphic Dark Fintech Design), Vanilla JavaScript (Fetch API), Chart.js |
| **Database** | Persistent H2 file database (Local dev: `./data/lifepoint`), PostgreSQL (Production profile: `prod`) |
| **Security** | Google OAuth 2.0 Client, Session management, CSRF Token Repository, User Isolation |
| **Build Tool** | Apache Maven |

---

## 🏛️ Database Architecture

The relational schema is configured in `src/main/resources/schema.sql` and managed via JPA Hibernate `update` mode:

```
+--------------------+        +---------------------+
|      PROFILE       |        |        HABIT        |
+--------------------+        +---------------------+
| id (PK)            |        | id (PK)             |
| email (Unique)     | <----+ | owner_email (FK-idx)|
| name               |      | | title               |
| currency (INR,...) |      | | category            |
| xp                 |      | | completed_on        |
| level              |      | | current_streak      |
| total_saved_by_... |      | | best_streak         |
+--------------------+      | | total_completions   |
                            | | money_saver         |
+--------------------+      | | saved_amount        |
|      EXPENSE       |      | | linked_goal_id      |
+--------------------+      | +---------------------+
| id (PK)            |      |
| owner_email (FK)   | -----+ +---------------------+
| title, category    |      | |     SAVINGS_GOAL    |
| amount, spent_on   |      | +---------------------+
+--------------------+      | | id (PK)             |
                            | | owner_email (FK-idx)|
+--------------------+      | | title, target_amount|
|       INCOME       |      | | current_amount      |
+--------------------+      | | target_date, color  |
| id (PK)            |      | | completed           |
| owner_email (FK)   | -----+ +---------------------+
| title, category    |      |
| amount, received_on|      | +---------------------+
+--------------------+      | |        BUDGET       |
                            | +---------------------+
+--------------------+      | | id (PK)             |
|    ACHIEVEMENT     |      | | owner_email (FK-idx)|
+--------------------+      | | category            |
| id (PK)            |      | | monthly_limit       |
| owner_email (FK)   | -----+ +---------------------+
| badge_code, title  |      |
| description, icon  |      | +---------------------+
| unlocked_on        |      | |      HABIT_LOG      |
+--------------------+      | +---------------------+
                            | | id (PK)             |
                            +-| owner_email (FK-idx)|
                              | habit_id, date, saved|
                              +---------------------+
```

---

## 📡 REST API Documentation

### Habits (`/api/habits`)
- `GET /api/habits` — Retrieve habits for authenticated user.
- `POST /api/habits` — Create habit (`title`, `category`, `moneySaver`, `savedAmount`, `linkedGoalId`).
- `PUT /api/habits/{id}` — Update habit details.
- `DELETE /api/habits/{id}` — Delete habit.
- `POST /api/habits/{id}/complete` — Complete habit today, extend streak, calculate XP, and fund linked goal.

### Cashflow & Expenses (`/api/expenses` & `/api/income`)
- `GET /api/expenses?category=&search=&startDate=&endDate=` — Filtered expense list.
- `POST /api/expenses` — Log expense (`title`, `category`, `amount`, `spentOn`).
- `PUT /api/expenses/{id}` — Update expense.
- `DELETE /api/expenses/{id}` — Delete expense.
- `GET /api/income` — Filtered income list.
- `POST /api/income` — Log income (`title`, `category`, `amount`, `receivedOn`).
- `PUT /api/income/{id}` — Update income.
- `DELETE /api/income/{id}` — Delete income.

### Budgets (`/api/budgets`)
- `GET /api/budgets` — List budget statuses with spent amount, remaining amount, and health status (`SAFE`, `WARNING`, `DANGER`).
- `POST /api/budgets` — Set/create category budget limit.
- `PUT /api/budgets/{id}` — Update category monthly limit.
- `DELETE /api/budgets/{id}` — Delete category budget.

### Savings Vaults (`/api/goals`)
- `GET /api/goals` — List user savings vaults.
- `POST /api/goals` — Create savings goal (`title`, `targetAmount`, `targetDate`, `category`, `color`).
- `PUT /api/goals/{id}` — Update goal target or date.
- `DELETE /api/goals/{id}` — Delete goal.
- `POST /api/goals/{id}/deposit` — Deposit amount into goal.

### Real-Time Dashboard & Analytics (`/api/dashboard`)
- `GET /api/dashboard/summary` — Full real-time dashboard snapshot.
- `GET /api/dashboard/leaderboard` — Top 10 disciplined members leaderboard.
- `GET /api/dashboard/analytics` — Dynamic Chart.js dataset (cashflow trajectory, category spending, habit activity, cumulative savings).

### Profile & Currency (`/api/profile`)
- `GET /api/profile` — Current user profile and gamification stats.
- `POST /api/profile/currency` — Change currency preference (`INR`, `USD`, `EUR`, `GBP`, `AED`, `CAD`, `AUD`, `JPY`, `SGD`).

---

## 🏃 Instructions to Run the Application

### 1. Prerequisites
- Java 17 or higher
- Maven 3.8+

### 2. Run Locally (Default H2 Database)
```powershell
mvn spring-boot:run
```
Open **[http://localhost:8080](http://localhost:8080)** in your browser.

- Click **"Explore Live Demo / Faculty Login"** to log in instantly as `Prof. Davis (Faculty Reviewer)`, `Demo Explorer`, or enter any custom test email.
- To test with Google OAuth 2.0, provide Google Cloud Console credentials:
```powershell
$env:GOOGLE_CLIENT_ID="your-client-id"
$env:GOOGLE_CLIENT_SECRET="your-client-secret"
mvn spring-boot:run
```
Authorized Redirect URI in Google Cloud Console:
`http://localhost:8080/login/oauth2/code/google`

### 3. Run with PostgreSQL (Production Profile)
```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/lifepoint"
$env:DATABASE_USERNAME="postgres"
$env:DATABASE_PASSWORD="your-password"
mvn spring-boot:run
```

### 4. Run Automated Test Suite
```powershell
mvn test
```
**Results**: 19 tests, 0 failures, 0 errors. Covers authentication, CRUD operations, budget calculations, "Saved by Choice", gamification XP progression, and cross-user data isolation.
