# GitHub Copilot Instructions for SudoQ Project

## Code Style Guidelines

### Comments and Documentation

- **Use English for all comments**: All code comments, including inline comments, documentation comments, and block comments, should be written in English.
- **Javadoc/KDoc**: Documentation comments for classes, methods, and functions should be in English.
- **TODO/FIXME comments**: Should be in English.
- **Explanatory comments**: Any explanatory comments in the code should be in English.

### Examples

✅ Good:
```kotlin
/**
 * Updates the statistics for the current profile
 */
private fun updateStatistics() {
    // Save the updated statistics to disk immediately
    p.saveChanges()
}
```

❌ Avoid:
```kotlin
/**
 * Updatet die Spielerstatistik des aktuellen Profils
 */
private fun updateStatistics() {
    // Speichert die aktualisierte Statistik sofort auf die Festplatte
    p.saveChanges()
}
```

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material3
- **Build System**: Gradle with Kotlin DSL
- **Architecture**: MVVM-like with Activities and custom Views

## Migration Guidelines

When migrating from XML to Compose:
1. Use Material3 components
2. Implement proper state management
3. Add TopAppBar with navigation
4. Ensure proper string resource usage
5. Test all functionality after migration
