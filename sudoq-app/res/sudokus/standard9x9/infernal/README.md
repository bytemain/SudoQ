# Infernal Difficulty Sudokus

## Overview
This directory contains 10 pre-generated sudokus for the **Infernal** difficulty level.

## Generation Details
- **Generated:** November 2025
- **Algorithm:** GenerationAlgo (Original algorithm)
- **Validation:** All puzzles validated as `MUCH_TOO_DIFFICULT` or `TOO_DIFFICULT`
- **Filled cells:** 24-31 cells (50-57 empty cells)
- **Average filled per row:** 2.7-3.4 cells

## Difficulty Characteristics
These sudokus are significantly harder than typical infernal puzzles:
- Very sparse initial configuration (only ~30% filled)
- Requires advanced solving techniques
- Minimal given information per row/column/block
- Algorithmically validated difficulty rating

## File Format
Each XML file follows the standard sudoku.dtd format:
```xml
<sudoku transformCount="0" type="0" complexity="3" id="N">
  <fieldmap id="X" editable="true" solution="Y">
    <position x="X" y="Y"></position>
  </fieldmap>
  ...
</sudoku>
```

## Usage in Game
The game will:
1. Load these pre-generated sudokus when user selects Infernal difficulty
2. Apply transformations (rotate, mirror) for variety
3. Only generate NEW sudokus after 10 transformations

## Generation Tools
To regenerate or add more sudokus:
```bash
# Generate new infernal sudokus
./gradlew :sudoqmodel:test --tests GenerateInfernalXmlFiles.generateAndSaveInfernalSudokus

# Visualize existing sudokus
python3 utilities/visualize_infernal_sudokus.py
```

## Statistics
| File | Filled Cells | Difficulty |
|------|--------------|------------|
| sudoku_1.xml | 26 | MUCH_TOO_DIFFICULT |
| sudoku_2.xml | 29 | MUCH_TOO_DIFFICULT |
| sudoku_3.xml | 28 | MUCH_TOO_DIFFICULT |
| sudoku_4.xml | 28 | MUCH_TOO_DIFFICULT |
| sudoku_5.xml | 27 | TOO_DIFFICULT |
| sudoku_6.xml | 30 | MUCH_TOO_DIFFICULT |
| sudoku_7.xml | 30 | MUCH_TOO_DIFFICULT |
| sudoku_8.xml | 29 | MUCH_TOO_DIFFICULT |
| sudoku_9.xml | 28 | TOO_DIFFICULT |
| sudoku_10.xml | 31 | TOO_DIFFICULT |

**Note:** These are genuinely challenging puzzles validated by the solver algorithm!
