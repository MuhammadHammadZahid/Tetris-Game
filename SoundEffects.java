/**
 * SoundEffects.java — Stub/dummy sound effect hooks for Tetris.
 *
 * HOW TO IMPLEMENT:
 *   All methods in this class are intentionally empty stubs.
 *   A programmer wishing to add real audio should:
 *     1. Add audio libraries (e.g. javax.sound.sampled, or a third-party lib).
 *     2. Load audio resources in the static initialiser or constructor.
 *     3. Replace each method body with the appropriate playback call.
 *   No other file needs to be modified — TetrisGame, GameBoard and
 *   UserInterface already call these methods at the correct moments.
 *
 * All methods are static for easy call-site ergonomics (SoundEffects.onDrop()).
 */
public class SoundEffects {

    // ── Private constructor — static utility class, not instantiated ──────────
    private SoundEffects() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Background Music
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Start the background music loop.
     * Called once when the gameplay screen is shown.
     * Should loop continuously until stopBackgroundMusic() is called.
     */
    public static void startBackgroundMusic() {
        SoundManager.playMusic("OOP-project/Sounds/tetrismusic.wav");
    }

    /**
     * Stop the background music.
     * Called when the game ends, is paused, or the player returns to a menu.
     */
    public static void stopBackgroundMusic() {
        SoundManager.stopMusic();
    }

    /**
     * Pause/resume the background music (e.g. on P key).
     * @param paused true to pause, false to resume
     */
    public static void setBackgroundMusicPaused(boolean paused) {
        if (paused) {
            SoundManager.pauseMusic();
        } else {
            SoundManager.resumeMusic();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Piece Movement & Placement
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Played when a piece is soft-dropped (player holds the down key).
     */
    public static void onSoftDrop() {
        SoundManager.playSound("Sounds/soundsdrop.wav");
    }

    /**
     * Played when a piece is hard-dropped (player presses Space).
     * Typically a louder, sharper impact sound than a soft drop.
     */
    public static void onHardDrop() {
        SoundManager.playComboSound("OOP-project/Sounds/soundsdrop.wav", 4);
    }

    /**
     * Played when a piece naturally locks into position (touches the stack).
     */
    public static void onPieceLock() {
        SoundManager.playSound("Sounds/soundspiecelock.wav");
    }

    /**
     * Played when a piece is rotated.
     */
    public static void onRotate() {
        SoundManager.playSound("Sounds/soundsrotate.wav");
    }

    /**
     * Played when a piece is moved left or right.
     */
    public static void onMove() {
        SoundManager.playSound("Sounds/soundslateralmove.wav");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Line Clears
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Played when one, two, or three lines are cleared at once.
     * @param lineCount the number of lines cleared (1, 2, or 3)
     */
    public static void onLineClear(int lineCount) {
        SoundManager.playComboSound("OOP-project/Sounds/soundsclear.wav", lineCount);
    }

    /**
     * Played when exactly four lines are cleared simultaneously (Tetris!).
     * Should be distinct and more dramatic than onLineClear().
     */
    public static void onTetris() {
        SoundManager.playComboSound("OOP-project/Sounds/soundstetris.wav", 4);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Level & Score Milestones
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Played when the player advances to a new level.
     */
    public static void onLevelUp() {
        SoundManager.playSound("Sounds/soundslevelup.wav");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Menu & UI Interactions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Played when any menu button is clicked.
     */
    public static void onMenuClick() {
        SoundManager.playSound("Sounds/soundsselect.wav");
    }

    /**
     * Played when the player navigates back from a screen.
     */
    public static void onMenuBack() {
        SoundManager.playSound("Sounds/soundsselect.wav");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Game State Events
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Played when the game ends (stack reaches the top).
     */
    public static void onGameOver() {
        SoundManager.playSound("Sounds/soundsgameover.wav");
    }

    /**
     * Played when the player achieves or beats a high score.
     */
    public static void onHighScore() {
        SoundManager.playSound("Sounds/soundshighscore.wav");
    }
}
