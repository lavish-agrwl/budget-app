# Budget — Offline Personal Expense & Borrow/Lend Tracker

A **calm, offline-first Android app** for tracking personal expenses, income, and borrow/lend balances.

Built as a daily‑use tool — not a demo — with a focus on **clarity, correctness, and UX discipline**.

---

## ✨ Key Features

### 💸 Expense & Income Tracking

* Add expenses and income with descriptions
* Predefined + user-created expense categories
* Automatic date & time logging
* Search, filter, and sort history
* Soft delete with undo

### 🤝 Borrow / Lend Tracking

* Person-based borrow & lend records
* Directional transactions (Borrowed / Lent)
* Partial settlements per transaction
* One‑tap **full settlement (swipe gesture)** per person
* Clear audit trail of transactions & settlements
* Automatic balance calculation (never stored)

### 🏠 Home Dashboard

* Monthly expense & income summary
* Net balance overview
* Total borrowed vs total lent
* Quick actions for fast entry

### 🎨 Thoughtful UI / UX

* Custom **pastel light & dark themes** (no default Material colors)
* Offline-only, no accounts, no cloud
* Modal add‑transaction flows (no back‑stack pollution)
* Keyboard-aware forms with smooth dismissal
* Designed for one‑handed, daily use

### 📦 Data Ownership

* CSV export (expenses & borrow/lend)
* Optional CSV import
* All data stored locally on device

---

## 🧠 Design Philosophy

* **Offline-first**: Your data stays on your device
* **Derived, not stored**: Balances & summaries are computed live
* **Audit-safe**: No silent mutations; settlements are events
* **Navigation vs Modal**: Screens are places, adds are actions
* **Boring reliability beats cleverness**

---

## 🏗️ Architecture

* **Language**: Kotlin
* **UI**: Jetpack Compose (Material 3, custom theme)
* **Architecture**: MVVM + Repository pattern
* **Local Storage**: Room (SQLite)
* **State**: StateFlow + Flow
* **Navigation**: Navigation Compose with nested graphs

### Layered Flow

```
UI (Compose)
 → ViewModel (State & orchestration)
   → Repository (single source of truth)
     → Room DB (entities & DAOs)
```

---

## 📁 Project Structure

```
app/
 ├─ ui/
 │   ├─ home/
 │   ├─ expenses/
 │   ├─ borrowlend/
 │   ├─ navigation/
 │   └─ theme/
 ├─ viewmodel/
 ├─ data/
 │   ├─ db/
 │   │   ├─ entity/
 │   │   └─ dao/
 │   └─ repository/
 ├─ domain/
 └─ util/
```

---

## 🚀 Getting Started

### Prerequisites

* Android Studio (latest stable)
* Android SDK 26+

### Run Locally

1. Clone the repository

   ```bash
   git clone https://github.com/your-username/budget.git
   ```
2. Open in Android Studio
3. Let Gradle sync
4. Run on emulator or device

---

## 📦 Download APK

A **debug APK is already included in GitHub Releases**, so you can install and test the app **without building it locally**.

### Install from Releases

1. Go to the **Releases** section of this repository
2. Download the latest `app-debug.apk`
3. Enable **Install unknown apps** on your device
4. Install the APK

> ℹ️ This APK is meant for testing and personal use. For production or Play Store distribution, build a signed release APK or AAB.

---

## 🧪 Testing Philosophy

* Manual testing with a structured checklist
* No flaky UI tests
* Logic kept in ViewModels for easy validation

---

## ❌ Explicit Non‑Goals

* No authentication
* No cloud sync
* No ads
* No social features
* No AI predictions

This app is intentionally **personal, private, and calm**.

---

## 🛣️ Future Improvements (Optional)

* Recurring transactions
* Monthly insights & trends
* Local encrypted backup & restore
* Widgets for quick add
* App lock (biometric / PIN)

---

## 📜 License

This project is open for personal and educational use.

Add a license if you plan to distribute or accept contributions.

---

## 🙌 Acknowledgements

Built with:

* Jetpack Compose
* AndroidX
* Kotlin Coroutines
* Room

Designed with long‑term usability in mind.

---

> *"Software that handles money should feel calm, predictable, and honest."*
