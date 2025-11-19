# Playground - Sudoku Generation Tools

This module contains **expensive** sudoku generation and analysis tools that should NOT run during normal testing.

## Purpose

These tools are used for:
- Generating new sudoku templates for different difficulty levels
- Analyzing and validating sudoku difficulty
- Benchmarking generation performance
- Visualizing generated puzzles

## Why Separate?

These tests were moved from `sudoqmodel` because:
1. ⏱️ **Slow**: Generation can take 4-5 seconds per puzzle
2. 🔄 **Repetitive**: Running on every test suite is wasteful
3. 🎯 **Special purpose**: Only needed when updating templates

## Available Tools

### 1. Generate Infernal Sudokus
Generates 10 new infernal difficulty sudokus and saves them as XML files.

```bash
./gradlew :Playground:test --tests GenerateInfernalXmlFiles.generateAndSaveInfernalSudokus
```

**Output:** `res/sudokus/standard9x9/infernal/sudoku_1.xml` through `sudoku_10.xml`

### 2. Show Hard Sudokus
Displays 3 pre-generated hard sudokus with statistics.

```bash
./gradlew :Playground:test --tests ShowHardSudokusTest.showExtremelyHardPuzzles
```

### 3. Sudoku Generator Tool
Comprehensive generation and benchmarking tool.

```bash
# Generate extremely hard 9x9 puzzles
./gradlew :Playground:test --tests SudokuGeneratorTool.testGenerateExtremelyHard9x9

# Generate hard 16x16 puzzles
./gradlew :Playground:test --tests SudokuGeneratorTool.testGenerateHard16x16

# Benchmark generation speed
./gradlew :Playground:test --tests SudokuGeneratorTool.benchmarkGenerationSpeed
```

## Project Structure

```
Playground/
├── build.gradle.kts            # Depends on sudoqmodel
├── src/
│   └── test/
│       └── kotlin/
│           └── de/
│               └── sudoq/
│                   └── tools/
│                       ├── GenerateInfernalXmlFiles.kt    # Generate infernal sudoku XML files
│                       ├── ShowHardSudokusTest.kt         # Display hard puzzles
│                       ├── ShowGeneratedInfernalSudokuTest.kt  # Show infernal puzzles
│                       ├── SudokuGeneratorTool.kt         # Comprehensive generation tool
│                       └── TestSudokuTypeRepo.java        # Test utility for sudoku types
└── README.md (this file)
```

## Usage Guidelines

### ✅ When to use these tools:

- Creating new sudoku templates for a new difficulty level
- Updating existing templates because users report difficulty issues
- Benchmarking to optimize generation algorithms
- Researching and developing new generation strategies

### ❌ When NOT to use:

- During normal development testing
- In CI/CD pipelines (unless specifically testing generation)
- When making changes unrelated to sudoku generation

## Technical Notes

### Generation Performance

| Type | Difficulty | Average Time |
|------|-----------|--------------|
| 4x4 | easy | ~7ms |
| 9x9 | easy | ~30ms |
| 9x9 | difficult | ~4.7s |
| 9x9 | infernal | ~5s+ |

### Algorithms

- **GenerationAlgo** (Original): Stable, used for production templates
- **ImprovedGenerationAlgo**: Experimental, faster but still under testing

### Path Configuration

All tools use relative paths from the project root:
```kotlin
val projectRoot = File(System.getProperty("user.dir")).parentFile
val outputDir = File(projectRoot, "res/sudokus/standard9x9/infernal")
```

## Maintenance

When updating this module:
1. Keep tools focused and isolated
2. Add clear documentation for new tools
3. Ensure relative paths work across different environments
4. Test generation output before committing templates

---

**Last Updated:** November 20, 2025  
**Maintainer:** SudoQ Development Team
