package edu.lukina.dotty

/*
Created by Yelyzaveta Lukina on 11.14.2025.
 */

// Imports the 'abs' function from the Kotlin math library to calculate the absolute value of a number.
import kotlin.math.abs
// Imports the 'Random' object from the Kotlin random library to generate random numbers.
import kotlin.random.Random

// Declares the Dot class with a primary constructor that initializes its row and column.
class Dot(var row: Int = 0, var col: Int = 0) {
    // Declares a 'color' property and initializes it with a random integer between 0 (inclusive)
    // and NUM_COLORS (exclusive).
    var color = Random.nextInt(NUM_COLORS)
    // Declares a 'centerX' property to store the horizontal center coordinate of the dot on the screen.
    var centerX = 0f
    // Declares a 'centerY' property to store the vertical center coordinate of the dot on the screen.
    var centerY = 0f
    // Declares a 'radius' property to store the radius of the dot when drawn.
    var radius = 1f
    // Declares a boolean property 'isSelected' to track if the dot is part of the current selection chain.
    var isSelected = false

    // Defines a function to assign a new random color to this dot.
    fun setRandomColor() {
        // Assigns a new random integer to the 'color' property.
        color = Random.nextInt(NUM_COLORS)
    }

    // Defines a function to check if another dot is directly adjacent (not diagonally).
    fun isAdjacent(dot: Dot): Boolean {
        // Calculates the absolute difference in columns between this dot and the other dot.
        val colDiff = abs(col - dot.col)
        // Calculates the absolute difference in rows between this dot and the other dot.
        val rowDiff = abs(row - dot.row)
        // Returns true if the sum of the differences is exactly 1, meaning they are adjacent.
        return colDiff + rowDiff == 1
    }
}


