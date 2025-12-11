package edu.lukina.dotty

/*
Created by Yelyzaveta Lukina on 11.14.2025.
 */

// Imports the Context class, providing access to application-specific resources and classes.
import android.content.Context
// Imports the SoundPool class, used for playing multiple short audio clips.
import android.media.SoundPool
// Imports the AudioAttributes class, which defines how an audio stream should be handled by the system.
import android.media.AudioAttributes

// Declares the SoundEffects class with a private constructor to enforce the Singleton pattern.
class SoundEffects private constructor(context: Context) {

    // Declares a private, nullable property to hold the SoundPool instance.
    private var soundPool: SoundPool? = null
    // Creates a private mutable list to store the loaded IDs for the dot selection sounds.
    private val selectSoundIds = mutableListOf<Int>()
    // Declares a private property to track the current index in the 'selectSoundIds' list.
    private var soundIndex = 0
    // Declares a private property to store the loaded ID for the game over sound.
    private var endGameSoundId = 0

    // Defines a companion object, which allows for static-like members in Kotlin.
    companion object {
        // Declares a private, nullable, static-like variable to hold the single instance of SoundEffects.
        private var instance: SoundEffects? = null

        // Defines a public, static-like function to get the single instance of the class.
        fun getInstance(context: Context): SoundEffects {
            // Checks if the 'instance' variable has been created yet.
            if (instance == null) {
                // If not, creates a new SoundEffects instance, passing the application context.
                instance = SoundEffects(context)
            }
            // Returns the non-null instance. The '!!' asserts that 'instance' is not null here.
            return instance!!
        }
    }

    // The 'init' block is run when the SoundEffects instance is first created.
    init {
        // Creates an AudioAttributes object to define the sound's behavior as a game sound.
        val attributes = AudioAttributes.Builder()
            // Sets the usage type to USAGE_GAME, indicating it's for game audio.
            .setUsage(AudioAttributes.USAGE_GAME)
            // Sets the content type to SONIFICATION, for sounds accompanying user actions.
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            // Builds the final AudioAttributes object.
            .build()

        // Creates a new SoundPool instance using a builder.
        soundPool = SoundPool.Builder()
            // Sets the maximum number of simultaneous audio streams to 5.
            .setMaxStreams(5)
            // Applies the previously defined audio attributes.
            .setAudioAttributes(attributes)
            // Builds the final SoundPool object.
            .build()

        // Safely unwraps the nullable 'soundPool' with a 'let' block to load sounds.
        soundPool?.let {
            // Loads the 'note_e' sound file and adds its ID to the list of selection sounds.
            selectSoundIds.add(it.load(context, R.raw.note_e, 1))
            // Loads the 'note_f' sound file and adds its ID to the list.
            selectSoundIds.add(it.load(context, R.raw.note_f, 1))
            // Loads the 'note_f_sharp' sound file and adds its ID to the list.
            selectSoundIds.add(it.load(context, R.raw.note_f_sharp, 1))
            // Loads the 'note_g' sound file and adds its ID to the list.
            selectSoundIds.add(it.load(context, R.raw.note_g, 1))

            // Loads the 'game_over' sound file and stores its ID.
            endGameSoundId = it.load(context, R.raw.game_over, 1)
        }

        // Calls the function to reset the tone index to its initial state.
        resetTones()
    }

    // Defines a public function to reset the tone sequence for a new selection chain.
    fun resetTones() {
        // Resets the sound index to -1, so the first call to playTone will increment it to 0.
        soundIndex = -1
    }

    // Defines a public function to play the next or previous tone in the sequence.
    fun playTone(advance: Boolean) {
        // Checks if the tone sequence should advance forward.
        if (advance) {
            // If so, increments the sound index.
            soundIndex++
        } else {
            // Otherwise (for backtracking), decrements the sound index.
            soundIndex--
        }

        // Checks if the index has gone below the lower bound.
        if (soundIndex < 0) {
            // If so, resets it to 0 to prevent a crash and play the first tone.
            soundIndex = 0
            // Checks if the index has gone above the upper bound of the list.
        } else if (soundIndex >= selectSoundIds.size) {
            // If so, wraps it around back to 0 to loop the tones.
            soundIndex = 0
        }

        // Safely plays the sound at the current index from the sound pool.
        soundPool?.play(selectSoundIds[soundIndex], 1f, 1f, 1, 0, 1f)
    }

    // Defines a public function to play the game over sound effect.
    fun playGameOver() {
        // Safely plays the game over sound with a reduced volume (0.5).
        soundPool?.play(endGameSoundId, 0.5f, 0.5f, 1, 0, 1f)
    }

    // Defines a public function to release the SoundPool resources when they are no longer needed.
    fun release() {
        // Safely releases all memory and system resources used by the SoundPool.
        soundPool?.release()
        // Sets the soundPool property to null to prevent further use.
        soundPool = null
    }
}
