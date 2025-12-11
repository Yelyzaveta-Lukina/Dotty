package edu.lukina.dotty

/*
Created by Yelyzaveta Lukina on 11.14.2025.
 */

// Imports the 'java.util' package, mainly for using 'Collections.unmodifiableList'.
import java.util.*

// Defines a constant for the number of different colors a dot can have.
const val NUM_COLORS = 5
// Defines a constant for the size of the game grid (e.g., 6x6).
const val GRID_SIZE = 6
// Defines a constant for the initial number of moves a player starts with.
const val INIT_MOVES = 10

// Defines an enum class to represent the outcome of trying to select a dot.
enum class DotStatus {
    // Represents that a dot was successfully added to the selection chain.
    Added,
    // Represents that a dot was not added to the selection chain
    // (e.g., wrong color or not adjacent).
    Rejected,
    // Represents that a dot was removed from the selection chain (backtracking).
    Removed
}

// Declares the DotsGame class with a private constructor to enforce the Singleton pattern.
class DotsGame private constructor() {

    // Declares a public property to track the number of moves the player has left.
    var movesLeft = 0
    // Declares a public property to store the player's current score.
    var score = 0

    // Creates a 2D list (a list of lists) to represent the game grid, initializing it with new Dot objects.
    private val dotGrid = MutableList(GRID_SIZE) { MutableList(GRID_SIZE) { Dot() } }
    // Creates a private mutable list to keep track of the dots currently selected by the player in a single move.
    private val selectedDotList = mutableListOf<Dot>()

    // Declares a public read-only computed property to check if the game is over.
    val isGameOver: Boolean
        // The getter returns true if the player has no moves left.
        get() = movesLeft == 0

    // Declares a public read-only property to provide a safe, unmodifiable view of the selected dots list.
    val selectedDots: List<Dot>
        // The getter returns a read-only wrapper around the internal 'selectedDotList'.
        get() = Collections.unmodifiableList(selectedDotList)

    // Declares a public read-only computed property to get the last dot that was selected.
    val lastSelectedDot: Dot?
        // The getter checks if the list of selected dots is empty.
        get() {
            return if (selectedDotList.isEmpty()) {
                // If it's empty, returns null.
                null
            }
            // If it's not empty,
            else {
                // returns the last element in the list.
                selectedDotList[selectedDotList.size - 1]
            }
        }

    // Declares a public read-only computed property to find the lowest selected dot in each column.
    val lowestSelectedDots: List<Dot>
        get() {
            // Creates a temporary mutable list to store the found dots.
            val dotList = mutableListOf<Dot>()
            // Iterates through each column of the grid.
            for (col in 0 until GRID_SIZE) {
                // Iterates through each row in that column, starting from the bottom and going up.
                for (row in GRID_SIZE - 1 downTo 0) {
                    // Checks if the dot at the current row and column is selected.
                    if (dotGrid[row][col].isSelected) {
                        // If it is, adds the dot to the list.
                        dotList.add(dotGrid[row][col])
                        // Breaks the inner loop to move to the next column,
                        // as we've found the lowest one in this column.
                        break
                    }
                }
            }
            // Returns the list of the lowest selected dots.
            return dotList
        }

    // Defines a companion object, which allows for static-like members in Kotlin.
    companion object {
        // Declares a private, nullable, static-like variable to hold the single instance of the game.
        private var instance: DotsGame? = null

        // Defines a public, static-like function to get the single instance of the game.
        fun getInstance(): DotsGame {
            // Checks if the 'instance' variable has been created yet.
            if (instance == null) {
                // If not, creates a new DotsGame instance.
                instance = DotsGame()
            }
            // Returns the non-null instance. The '!!' asserts that 'instance' is not null here.
            return instance!!
        }
    }

    // The 'init' block is run when the DotsGame instance is first created.
    init {
        // Iterates through each row of the grid.
        for (row in 0 until GRID_SIZE) {
            // Iterates through each column of the grid.
            for (col in 0 until GRID_SIZE) {
                // Assigns the correct row index to the Dot object at that position.
                dotGrid[row][col].row = row
                // Assigns the correct column index to the Dot object at that position.
                dotGrid[row][col].col = col
            }
        }
    }

    // Defines a function to retrieve the Dot object at a specific row and column.
    fun getDot(row: Int, col: Int): Dot? {
        // Checks if the given row or column is outside the grid boundaries.
        return if (row >= GRID_SIZE || row < 0 || col >= GRID_SIZE || col < 0) {
            // If it's out of bounds, returns null.
            null
        } else {
            // Otherwise, returns the Dot object from the grid.
            dotGrid[row][col]
        }
    }

    // Defines a function to start a new game.
    fun newGame() {
        // Resets the score to 0.
        score = 0
        // Resets the number of moves to the initial value.
        movesLeft = INIT_MOVES
        // Iterates through each row of the grid.
        for (row in 0 until GRID_SIZE) {
            // Iterates through each column of the grid.
            for (col in 0 until GRID_SIZE) {
                // Assigns a new random color to each dot.
                dotGrid[row][col].setRandomColor()
            }
        }
    }

    // Defines a function to handle the logic when a player interacts with a dot.
    fun processDot(dot: Dot): DotStatus {
        // Initializes the status to 'Rejected' by default.
        var status = DotStatus.Rejected

        // Checks if this is the very first dot being selected in a chain.
        if (selectedDotList.isEmpty()) {
            // Adds the dot to the list of selected dots.
            selectedDotList.add(dot)
            // Marks the dot as selected.
            dot.isSelected = true
            // Updates the status to 'Added'.
            status = DotStatus.Added
        }
        // Checks if the dot is not already selected.
        else if (!dot.isSelected) {
            // If it's a new dot, ensures it has the same color and is adjacent to the previously selected dot.
            val lastDot: Dot? = this.lastSelectedDot
            // Checks if the last dot's color matches the new dot's color and if they are adjacent.
            if (lastDot?.color == dot.color && lastDot.isAdjacent(dot)) {
                // If the conditions are met, adds the dot to the selection list.
                selectedDotList.add(dot)
                // Marks the dot as selected.
                dot.isSelected = true
                // Updates the status to 'Added'.
                status = DotStatus.Added
            }
        }
        // This block handles the case where the player touches a dot that is already selected.
        else if (selectedDotList.size > 1) {
            // If the selected dot is the second-to-last dot in the chain, it means the player is backtracking.
            val secondLast = selectedDotList[selectedDotList.size - 2]
            // Checks if the touched dot is indeed the second-to-last one.
            if (secondLast == dot) {
                // Removes the last dot from the selection list.
                val removedDot = selectedDotList.removeAt(selectedDotList.size - 1)
                // Marks the removed dot as no longer selected.
                removedDot.isSelected = false
                // Updates the status to 'Removed'.
                status = DotStatus.Removed
            }
        }
        // Returns the final status of the operation.
        return status
    }

    // Defines a function to clear the list of selected dots.
    fun clearSelectedDots() {

        // Iterates through each dot in the selection list and resets its 'isSelected' flag to false.
        selectedDotList.forEach { it.isSelected = false }

        // Removes all elements from the selection list.
        selectedDotList.clear()
    }

    // Defines a function to be called after a player completes a valid path of dots.
    fun finishMove() {
        // Checks if more than one dot was selected, as a single dot does not constitute a valid move.
        if (selectedDotList.size > 1) {

            // Sorts the selected dots by their row number, so they are processed from top to bottom.
            val sortedDotList = selectedDotList.sortedWith(compareBy { it.row })

            // Iterates through each of the sorted, selected dots.
            for (dot in sortedDotList) {
                // For each selected dot, moves all the dots above it in the same column down by one position.
                for (row in dot.row downTo 1) {
                    // Gets the dot at the current position.
                    val dotCurrent = dotGrid[row][dot.col]
                    // Gets the dot directly above the current one.
                    val dotAbove = dotGrid[row - 1][dot.col]
                    // Sets the color of the current dot to the color of the dot above it.
                    dotCurrent.color = dotAbove.color
                }

                // After moving the column down, places a new dot with a random color at the top of that column.
                val topDot = dotGrid[0][dot.col]
                // Assigns a new random color to the top dot.
                topDot.setRandomColor()
            }

            // Increases the score by the number of dots that were cleared.
            score += selectedDotList.size
            // Decrements the number of moves left.
            movesLeft--
            // Clears the selection list to prepare for the next move.
            clearSelectedDots()
        }
    }
}
