package de.sudoq.model.sudoku.sudokuTypes;

import de.sudoq.model.persistence.IRepo;

public class TypeBuilder {

	// Test repository that creates basic SudokuType instances
	private static IRepo<SudokuType> sudokuTypeRepo = new TestSudokuTypeRepo();

	public static SudokuType getType(SudokuTypes st){
		return SudokuTypeProvider.getSudokuType(st, sudokuTypeRepo);
	}
	
	public static SudokuType get99(){
		return SudokuTypeProvider.getSudokuType(SudokuTypes.standard9x9, sudokuTypeRepo);
	}

}
