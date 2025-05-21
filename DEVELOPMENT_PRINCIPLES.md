# Development Principles

## Code Organization

### 1. Architecture
- MVVM architecture
- Single Activity
- Compose for UI
- ViewModel for state management
- Foreground Service for audio playback

### 2. Package Structure
```
com.fuseforge.chromatone/
├── audio/           # Audio generation and playback
├── data/            # Data models and state
├── service/         # Background playback service
├── ui/              # UI components and theme
└── utils/           # Utility functions
```

### 3. Code Quality Standards
- Kotlin coding conventions
- Clear documentation for complex logic
- No unit tests (project intentionally avoids test complexity)
- No UI tests (manual device checks only for critical flows)
- No warnings policy
- Clear naming conventions

### 4. Performance Guidelines
- Minimize object allocations
- Efficient audio buffer management
- No background processes except audio
- Optimize battery usage
- Small APK size target (<5MB)

### 5. Testing Strategy
- No automated unit or UI tests
- Manual device checks for:
  - Audio generation
  - Timer logic
  - State management
  - Noise selection
  - Play/pause functionality
  - Timer functionality
- Battery consumption and APK size checked via profiling tools as needed
- Always run a full Gradle build (`./gradlew clean build`) after making changes to ensure the project builds successfully before testing on a device.

### 6. Git Workflow
- Feature branches from main
- Clear commit messages
- PR template with checklist
- No direct pushes to main
- Conventional commits format

### 7. Development Phases
1. Basic UI implementation
2. Audio engine integration
3. Timer functionality
4. Background playback
5. Final polish

## Build Process
- Regular gradle clean builds
- ProGuard optimization
- APK size monitoring
- Performance profiling
- Requires Java 17

## Debugging & Error Handling
- Function independently and ask questions only if you are not able to make a decision. Use the user as a resource for better decisions, not for step-by-step approval.
- If the same error occurs multiple times, take a step back and reason about the root cause.
- Use the internet or external resources to get context if needed, rather than just reacting to the last error message.
- Avoid repeatedly attempting the same fix without new information.

## Definition of Done
1. Code compiles without warnings
2. Manual device checks pass for all flows
3. No memory leaks
4. Meets performance targets
5. Follows design principles
6. Documentation updated
7. Gradle build succeeds (`./gradlew build`)
8. Changes verified on physical device with user feedback
9. Requirements checklist verified with stakeholder
