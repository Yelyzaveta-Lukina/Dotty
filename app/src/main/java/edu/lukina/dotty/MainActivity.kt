package edu.lukina.dotty

/*
Created by Yelyzaveta Lukina on 11.14.2025.
 */

// Imports the Bundle class, used for passing data between Android components and saving instance state.
import android.os.Bundle
// Imports the Button widget class.
import android.widget.Button
// Imports the TextView widget class.
import android.widget.TextView
// Imports AppCompatActivity, a base class for activities that use the support library action bar features.
import androidx.appcompat.app.AppCompatActivity
// Imports the DotsGridListener interface from the DotsView class to handle callbacks.
import edu.lukina.dotty.DotsView.DotsGridListener
// Imports the Locale class, used for formatting strings according to language and regional conventions.
import java.util.Locale
// Imports the DotSelectionStatus enum from the DotsView class to represent the status of a dot selection.
import android.animation.Animator
// Imports the AnimatorListenerAdapter class, used to listen for animation events.
import android.animation.AnimatorListenerAdapter
// Imports the ObjectAnimator class, used to animate properties of views.
import android.animation.ObjectAnimator

// Declares the MainActivity class, inheriting from AppCompatActivity to gain Activity functionality.
class MainActivity : AppCompatActivity() {

    // Gets the singleton instance of the DotsGame logic controller.
    private val dotsGame = DotsGame.getInstance()
    // Declares a private, late-initialized property to hold the custom DotsView.
    private lateinit var dotsView: DotsView
    // Declares a private, late-initialized property to hold the TextView for displaying moves left.
    private lateinit var movesRemainingTextView: TextView
    // Declares a private, late-initialized property to hold the TextView for displaying the score.
    private lateinit var scoreTextView: TextView
    // Declares a private, late-initialized property to hold the SoundEffects controller.
    private lateinit var soundEffects: SoundEffects

    // Overrides the onCreate method, which is called when the activity is first created.
    override fun onCreate(savedInstanceState: Bundle?) {
        // Calls the superclass's implementation of onCreate.
        super.onCreate(savedInstanceState)
        // Sets the user interface layout for this activity from the specified XML resource file.
        setContentView(R.layout.activity_main)

        // Finds and assigns the TextView for displaying the remaining moves from the layout.
        movesRemainingTextView = findViewById(R.id.moves_remaining_text_view)
        // Finds and assigns the TextView for displaying the score from the layout.
        scoreTextView = findViewById(R.id.score_text_view)
        // Finds and assigns the custom DotsView from the layout.
        dotsView = findViewById(R.id.dots_view)

        // Finds the 'New Game' button and sets a click listener to call the 'newGameClick' method.
        findViewById<Button>(R.id.new_game_button).setOnClickListener { newGameClick() }

        // Registers the 'gridListener' object with the DotsView to receive callbacks for touch events.
        dotsView.setGridListener(gridListener)

        // Initializes the SoundEffects controller with the application context.
        soundEffects = SoundEffects.getInstance(applicationContext)

        // Calls the function to initialize and start the first game when the app launches.
        startNewGame()
    }

    // Declares the onDestroy method, which is called when the activity is about to be destroyed.
    override fun onDestroy() {
        // Calls the superclass's implementation of onDestroy.
        super.onDestroy()
        // Releases the resources associated with the SoundEffects controller.
        soundEffects.release()
    }

    // Defines a private listener object that implements the DotsGridListener interface.
    private val gridListener = object : DotsGridListener {
        // Overrides the method to be called when a dot is selected on the DotsView.
        override fun onDotSelected(dot: Dot, status: DotSelectionStatus) {
            // If the game is over (no moves left), ignore any further dot selections.
            if (dotsGame.isGameOver) return

            // Play first tone when first dot is selected
            if (status == DotSelectionStatus.First) {
                soundEffects.resetTones()
            }

            // Tells the game logic to process the selected dot (add it, remove it, or reject it).
            val addStatus = dotsGame.processDot(dot)

            // If the dot was successfully added to the selection chain, play a tone.
            if (addStatus == DotStatus.Added) {
                soundEffects.playTone(true)
            }
            // If the dot was rejected, play a different tone.
            else if (addStatus == DotStatus.Removed) {
                soundEffects.playTone(false)
            }

            // Checks if the user has finished their selection gesture (lifted their finger).
            if (status === DotSelectionStatus.Last) {
                // If a valid chain of more than one dot has been selected...
                if (dotsGame.selectedDots.size > 1) {
                    dotsView.animateDots()
                    // ...tells the game logic to finalize the move (remove dots, update score).
                    //dotsGame.finishMove()
                    // Updates the on-screen TextViews with the new moves and score.
                    //updateMovesAndScore()
                } else {
                    // If only one dot was selected, it's not a valid move, so just clear the selection.
                    dotsGame.clearSelectedDots()
                }
            }

            // Triggers a redraw of the DotsView to show the changes (like new selected dots or new grid colors).
            dotsView.invalidate()
        }

        // Overrides the method to be called when the animation finishes.
        override fun onAnimationFinished() {
            // Tells the game logic to finalize the move (remove dots, update score).
            dotsGame.finishMove()
            // Updates the on-screen TextViews with the new moves and score.
            dotsView.invalidate()
            // Checks if the game is over (no moves left).
            updateMovesAndScore()

            // If the game is over, play the game over sound.
            if (dotsGame.isGameOver) {
                soundEffects.playGameOver()
            }
        }
    }

    // Defines a private function to handle the click of the 'New Game' button.
    private fun newGameClick() {
        // Gets the total height of the screen's main window view.
        val screenHeight = this.window.decorView.height.toFloat()
        // Creates an ObjectAnimator to animate the 'translationY' property of the dotsView.
        val moveBoardOff = ObjectAnimator.ofFloat(
            // The view to be animated.
            dotsView,
            // The property to animate ('translationY' moves it vertically).
            "translationY",
            // The value to animate to, moving the view completely off the bottom of the screen.
            screenHeight
        )
        // Sets the duration of the animation to 700 milliseconds.
        moveBoardOff.duration = 700
        // Starts the animation.
        moveBoardOff.start()

        // Adds a listener to be notified when the animation state changes.
        moveBoardOff.addListener(object : AnimatorListenerAdapter() {
            // Overrides the onAnimationEnd method, which is called when the animation finishes.
            override fun onAnimationEnd(animation: Animator) {
                // Resets the game board and state after the old board has animated off-screen.
                startNewGame()

                // Creates a new ObjectAnimator to bring the new game board onto the screen.
                val moveBoardOn = ObjectAnimator.ofFloat(
                    // The view to be animated.
                    dotsView,
                    // The property to animate.
                    "translationY",
                    // The starting and ending Y positions for the animation.
                    -screenHeight, // Start from above the top of the screen.
                    0f             // End at its original, default position.
                )
                // Sets the duration of the animation to 700 milliseconds.
                moveBoardOn.duration = 700
                // Starts the animation to slide the new board into place.
                moveBoardOn.start()
            }
        })
    }

    // Defines a private function to set up and start a new game session.
    private fun startNewGame() {
        // Tells the game logic to reset the board, score, and moves.
        dotsGame.newGame()
        // Triggers a full redraw of the DotsView to display the new board.
        dotsView.invalidate()
        // Updates the on-screen TextViews to show the starting moves and score.
        updateMovesAndScore()
    }

    // Defines a private function to update the moves and score TextViews on the screen.
    private fun updateMovesAndScore() {
        // Formats and sets the text for the moves remaining TextView.
        movesRemainingTextView.text = String.format(Locale.getDefault(), "%d", dotsGame.movesLeft)
        // Formats and sets the text for the score TextView.
        scoreTextView.text = String.format(Locale.getDefault(), "%d", dotsGame.score)
    }
}
