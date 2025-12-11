package edu.lukina.dotty

/*
Created by Yelyzaveta Lukina on 11.14.2025.
 */

// Imports an annotation to suppress lint warnings, in this case for touch event handling.
import android.annotation.SuppressLint
// Imports the Context class, providing access to application-specific resources and classes.
import android.content.Context
// Imports the Canvas class, which is used for drawing 2D graphics.
import android.graphics.Canvas
// Imports the Paint class, which holds style and color information for drawing.
import android.graphics.Paint
// Imports the Path class, used to create and draw complex geometric shapes.
import android.graphics.Path
// Imports the AttributeSet class, which contains attributes from an XML layout file.
import android.util.AttributeSet
// Imports the View class, the basic building block for user interface components.
import android.view.View
// Imports the MotionEvent class, which represents user touch screen interactions.
import android.view.MotionEvent
// Imports the Animator class, used to animate properties of views.
import android.animation.Animator
// Imports the AnimatorListenerAdapter class, used to listen for animation events.
import android.animation.AnimatorListenerAdapter
// Imports the AnimatorSet class, used to group multiple animations together.
import android.animation.AnimatorSet
// Imports the ValueAnimator class, used to animate values over a duration.
import android.animation.ValueAnimator
// Imports the AccelerateInterpolator class, used to control the speed of an animation.
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator

// Defines an enum class to represent the different stages of a dot selection gesture.
enum class DotSelectionStatus {
    // Represents the selection of the very first dot in a chain.
    First,
    // Represents the selection of any subsequent dot in a chain.
    Additional,
    // Represents the end of the selection gesture (when the user lifts their finger).
    Last
}

// Defines a constant for the radius of each dot to be drawn on the screen, in pixels.
const val DOT_RADIUS = 40f

// Declares the DotsView class, which is a custom View for displaying the game grid.
class DotsView(context: Context, attrs: AttributeSet) :
// It inherits from the base View class and takes Context and AttributeSet as parameters.
    View(context, attrs) {

    // Defines an interface that the hosting Activity must implement to listen for dot selection events.
    interface DotsGridListener {
        // Declares a function signature for the callback method when a dot is selected.
        fun onDotSelected(dot: Dot, status: DotSelectionStatus)
        // Declares a function signature for the callback method when the animation finishes.
        fun onAnimationFinished()
    }

    // Gets the singleton instance of the DotsGame logic controller.
    private val dotsGame = DotsGame.getInstance()
    // Creates a Path object to draw the connecting lines between selected dots.
    private val dotPath = Path()
    // Declares a nullable property to hold a reference to the listener (the hosting Activity).
    private var gridListener: DotsGridListener? = null
    // Initializes an array of color integers from the 'dotColors' resource array.
    private val dotColors = resources.getIntArray(R.array.dotColors)
    // Declares a property to store the calculated width of a single grid cell.
    private var cellWidth = 0
    // Declares a property to store the calculated height of a single grid cell.
    private var cellHeight = 0
    // Creates a Paint object for drawing the dots, with anti-aliasing for smooth circles.
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Creates a Paint object for drawing the connector path, with anti-aliasing.
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Creates an AnimatorSet object to group multiple animations together.
    private var animatorSet = AnimatorSet()

    // Defines a public function to animate the dots on the screen.
    fun animateDots() {

        // For storing multiple animations
        val animationList = mutableListOf<Animator>()

        // Get an animation to make selected dots disappear
        animationList.add(getDisappearingAnimator())

        // Iterates through each of the lowest selected dots in every column that had a selected dot.
        for (dot in dotsGame.lowestSelectedDots) {
            // Initializes a counter for how many rows a dot in this column needs to move down.
            // It starts at 1 for the dot that was just cleared.
            var rowsToMove = 1
            // Iterates through all the rows *above* the current lowest selected dot,
            // starting from the one directly above and moving to the top of the grid.
            for (row in dot.row - 1 downTo 0) {
                // Gets the dot object at the current row being checked, in the same column.
                val dotToMove = dotsGame.getDot(row, dot.col)
                // Safely unwraps the potentially null 'dotToMove' with a 'let' block.
                dotToMove?.let {
                    // Checks if this dot was also part of the selected chain.
                    if (it.isSelected) {
                        // If it was selected, it will also be removed,
                        // so increment the number of rows that dots above it must fall.
                        rowsToMove++
                    } else {
                        // If this dot was not selected, it needs to fall down to fill the empty spaces below.
                        // Calculates the target Y coordinate for the dot by adding its current position
                        // to the total vertical distance it needs to move.
                        val targetY = it.centerY + rowsToMove * cellHeight
                        // Creates a falling animation for this dot and adds it to the list of animations to be played.
                        animationList.add(getFallingAnimator(it, targetY))
                    }
                }
            }
        }

        // Play animations (just one right now) together, then reset radius to full size
        animatorSet = AnimatorSet()
        animatorSet.playTogether(animationList)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                resetDots()
                gridListener?.onAnimationFinished()
            }
        })
        // Start the animation
        animatorSet.start()
    }

    // Defines a private function that creates and returns a ValueAnimator for the disappearing dots effect.
    private fun getDisappearingAnimator(): ValueAnimator {
        // Creates a ValueAnimator that animates a float value from 1.0 down to 0.0.
        val animator = ValueAnimator.ofFloat(1f, 0f)
        // Sets the duration of the animation to 100 milliseconds.
        animator.duration = 100
        // Sets an interpolator that makes the animation start slowly and then accelerate.
        animator.interpolator = AccelerateInterpolator()
        // Adds a listener that is called on every frame of the animation.
        animator.addUpdateListener { animation: ValueAnimator ->
            // Iterates through each of the currently selected dots in the game logic.
            for (dot in dotsGame.selectedDots) {
                // Updates the radius of each selected dot by multiplying its original radius
                // by the current animated value (which goes from 1.0 to 0.0).
                dot.radius = DOT_RADIUS * animation.animatedValue as Float
            }
            // Triggers a redraw of the DotsView to show the updated (shrinking) dot radii.
            invalidate()
        }
        // Returns the configured ValueAnimator object.
        return animator
    }


    // The 'init' block is run when the DotsView instance is first created.
    init {
        // Sets the width of the line drawn for the connector path.
        pathPaint.strokeWidth = 10f
        // Sets the paint style to STROKE, so it only draws the outline of the path, not a fill.
        pathPaint.style = Paint.Style.STROKE
    }

    // Overrides the onSizeChanged method, called when the view's size changes (including initial layout).
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        // Calculates the usable width of the board, excluding padding.
        val boardWidth = width - paddingLeft - paddingRight
        // Calculates the usable height of the board, excluding padding.
        val boardHeight = height - paddingTop - paddingBottom
        // Calculates the width of a single cell by dividing the board width by the grid size.
        cellWidth = boardWidth / GRID_SIZE
        // Calculates the height of a single cell by dividing the board height by the grid size.
        cellHeight = boardHeight / GRID_SIZE
        // Calls the function to calculate and set the positions of all dots based on the new cell sizes.
        resetDots()
    }

    // Overrides the onDraw method, which is where all custom drawing for the view happens.
    override fun onDraw(canvas: Canvas) {
        // Calls the superclass's implementation of onDraw.
        super.onDraw(canvas)

        // Iterates through each row of the game grid to draw the dots.
        for (row in 0 until GRID_SIZE) {
            // Iterates through each column in the current row.
            for (col in 0 until GRID_SIZE) {
                // Safely gets the Dot object at the current row and column (using 'let' to handle nulls).
                dotsGame.getDot(row, col)?.let {
                    // Sets the color of the dot paint based on the dot's color property.
                    dotPaint.color = dotColors[it.color]
                    // Draws the dot as a circle on the canvas at its calculated center coordinates.
                    canvas.drawCircle(it.centerX, it.centerY, it.radius, dotPaint)
                }
            }
        }
        if (!animatorSet.isRunning) {
            // Retrieves the list of currently selected dots from the game logic.
            val selectedDots = dotsGame.selectedDots
            // Checks if at least one dot has been selected.
            if (selectedDots.isNotEmpty()) {
                // Resets the connector path to clear any previous lines.
                dotPath.reset()
                // Gets the first dot in the selection list.
                var dot = selectedDots[0]
                // Moves the starting point of the path to the center of the first dot.
                dotPath.moveTo(dot.centerX, dot.centerY)
                // Iterates through the rest of the selected dots to draw connecting lines.
                for (i in 1 until selectedDots.size) {
                    // Gets the next dot in the selection list.
                    dot = selectedDots[i]
                    // Draws a line from the previous dot's position to the current dot's center.
                    dotPath.lineTo(dot.centerX, dot.centerY)
                }
                // Sets the color of the connector path to match the color of the selected dots.
                pathPaint.color = dotColors[dot.color]
                // Draws the complete connector path onto the canvas.
                canvas.drawPath(dotPath, pathPaint)
            }
        }
    }

    // Suppresses a lint warning because this view's touch handling is managed manually.
    @SuppressLint("ClickableViewAccessibility")
    // Overrides the onTouchEvent method to handle user touch input.
    override fun onTouchEvent(event: MotionEvent): Boolean {

        // Only execute when a listener exists and the animations aren't running
        if (gridListener == null || animatorSet.isRunning) return true

        // Calculates the grid column corresponding to the touch event's X coordinate.
        val col = event.x.toInt() / cellWidth
        // Calculates the grid row corresponding to the touch event's Y coordinate.
        val row = event.y.toInt() / cellHeight
        // Gets the Dot object at the calculated row and column.
        var selectedDot = dotsGame.getDot(row, col)

        // If the touch moves outside the grid, default to using the last selected dot.
        if (selectedDot == null) {
            // This allows the ACTION_UP event to be handled correctly even
            // if the finger is released off-grid.
            selectedDot = dotsGame.lastSelectedDot
        }

        // Checks if a valid dot was identified (either by direct touch or by the off-grid logic above).
        if (selectedDot != null) {
            // A 'when' statement handles the different types of touch actions.
            when (event.action) {
                // Corresponds to the user first touching the screen.
                MotionEvent.ACTION_DOWN -> {
                    // Notifies the listener that the first dot in a chain has been selected.
                    gridListener!!.onDotSelected(selectedDot, DotSelectionStatus.First)
                }
                // Corresponds to the user dragging their finger across the screen.
                MotionEvent.ACTION_MOVE -> {
                    // Notifies the listener that an additional dot in the chain has been selected.
                    gridListener!!.onDotSelected(selectedDot, DotSelectionStatus.Additional)
                }
                // Corresponds to the user lifting their finger from the screen.
                MotionEvent.ACTION_UP -> {
                    // Notifies the listener that the selection gesture has ended.
                    gridListener!!.onDotSelected(selectedDot, DotSelectionStatus.Last)
                }
            }
        }

        // Returns true to indicate that this view has handled the touch event.
        return true
    }

    // Defines a public function to allow the hosting Activity to register itself as a listener.
    fun setGridListener(gridListener: DotsGridListener) {
        // Assigns the provided listener object to the internal 'gridListener' property.
        this.gridListener = gridListener
    }

    // Defines a private function to calculate the screen coordinates for each dot.
    private fun resetDots() {
        // Iterates through each row of the game grid.
        for (row in 0 until GRID_SIZE) {
            // Iterates through each column in the current row.
            for (col in 0 until GRID_SIZE) {
                // Safely gets the Dot object at the current position.
                dotsGame.getDot(row, col)?.let {
                    // Sets the radius of the dot to the predefined constant value.
                    it.radius = DOT_RADIUS
                    // Calculates and sets the horizontal center coordinate of the dot.
                    it.centerX = col * cellWidth + cellWidth / 2f
                    // Calculates and sets the vertical center coordinate of the dot.
                    it.centerY = row * cellHeight + cellHeight / 2f
                }
            }
        }
    }

    // Defines a private function that creates and returns a ValueAnimator for the falling dots effect.
    private fun getFallingAnimator(dot: Dot, destinationY: Float): ValueAnimator {
        // Creates a ValueAnimator that animates a float value from the dot's current vertical position
        // to its new destination.
        val animator = ValueAnimator.ofFloat(dot.centerY, destinationY)
        // Sets the duration of the animation to 300 milliseconds.
        animator.duration = 300
        // Sets an interpolator that creates a "bounce" effect at the end of the animation.
        animator.interpolator = BounceInterpolator()
        // Adds a listener that is called on every frame of the animation.
        animator.addUpdateListener { animation: ValueAnimator ->
            // Updates the dot's vertical position (centerY) to the current animated value for this frame.
            dot.centerY = animation.animatedValue as Float
            // Triggers a redraw of the DotsView to show the dot in its new position.
            invalidate()
        }
        // Returns the configured ValueAnimator object.
        return animator
    }
}
