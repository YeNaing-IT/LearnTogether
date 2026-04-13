# LearnTogether

An interactive Android educational app built with **Kotlin** and **Jetpack Compose**, designed to support meaningful learning through engaging, accessible, and ethically responsible experiences.

Developed as part of **CP3406 – Mobile Application Development**.

---

## About

LearnTogether helps students learn through interactive activities such as quizzes, puzzles, or simulations. The app promotes analytical thinking, problem-solving, and memory retention while adhering to ethical design principles — including data privacy, accessibility, inclusiveness, and age-appropriate content.

---

## Features

- **Landing Page** – Entry point with key information, shortcuts, and navigation to all screens.
- **Activity Screen** – Main learning interaction (quiz, puzzle, simulation, or flashcards).
- **Settings Screen** – Adjust user preferences such as sound, difficulty, and themes.
- **User Statistics Screen** – View progress, scores, and learning metrics over time.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material Design 3 |
| Architecture | MVVM (ViewModel + Repository Pattern) |
| Dependency Injection | Hilt / Koin |
| Navigation | Navigation Component (Compose) |
| Local Database | Room (SQLite) |
| Networking | Retrofit / Ktor (external API integration) |
| Testing | JUnit, Espresso, Compose UI Testing |
| Version Control | Git + GitHub |

---

## Architecture

The app follows **clean architecture** principles:

```
app/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # API service interfaces
│   └── repository/     # Repository implementations
├── di/                 # Dependency injection modules
├── domain/
│   ├── model/          # Domain models
│   └── usecase/        # Business logic use cases
├── ui/
│   ├── landing/        # Landing page screen
│   ├── activity/       # Learning activity screen
│   ├── settings/       # Settings screen
│   ├── statistics/     # User statistics screen
│   ├── components/     # Shared composable components
│   ├── navigation/     # Navigation graph and routes
│   └── theme/          # Material 3 theming
└── util/               # Utility classes and extensions
```

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Android SDK (minimum API 26, target API 34)

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/LearnTogether.git
   ```
2. Open the project in Android Studio.
3. Sync Gradle dependencies.
4. Run on an emulator or physical device (API 26+).

---

## API Integration

The app fetches content from external APIs to provide dynamic and up-to-date learning material. Network calls are handled using Retrofit/Ktor and follow best practices for error handling and caching.

---

## Database

User progress, scores, and preferences are persisted locally using **Room**. The database schema supports efficient querying for the statistics screen and offline-first functionality.

---

## Testing

The project includes both unit and UI tests:

```bash
# Run unit tests
./gradlew test

# Run instrumented (UI) tests
./gradlew connectedAndroidTest
```

- **Unit Tests** – Validate model logic, repository methods, and data handling.
- **UI Tests** – Verify composable rendering and user interaction flows.

---

## Ethical Design Considerations

- **Privacy** – Minimal data collection; all user data stored locally on-device.
- **Accessibility** – Content descriptions, sufficient contrast ratios, and scalable text.
- **Inclusiveness** – Designed for diverse learners across different backgrounds.
- **Transparency** – Clear communication of what the app does with user data.
- **User Autonomy** – No dark patterns; users control their own settings and progress.

---



---

## License

This project is developed for academic purposes as part of CP3406 at James Cook University.

---

## Author

**Your Name**
James Cook University – CP3406 Mobile Application Development
