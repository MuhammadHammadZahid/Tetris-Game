/**
 * SoundEffects.java — Sound effect hooks for Tetris.
 *
 * ═══════════════════════════════════════════════════════════════════
 * DESIGN PATTERNS & PRINCIPLES APPLIED
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. SINGLETON PATTERN (Creational)
 * ──────────────────────────────────
 * WHERE : SoundEffects class itself
 * WHY : The original class was a static utility — every method was
 * static, and the class could never be instantiated (private
 * constructor). This is a common anti-pattern because:
 * - Static classes cannot implement interfaces
 * - Static classes cannot be swapped for a mock/silent version
 * - Static state is shared globally and hard to test
 * Singleton gives the same "one global instance" guarantee
 * while allowing the class to implement an interface, be
 * subclassed (e.g. SilentSoundEffects for testing), and be
 * injected as a dependency.
 * HOW : Private constructor + static getInstance() method.
 * The single instance is created lazily on first access.
 * CATEGORY: Creational — controls how and when the one object is made.
 *
 * 2. FACADE PATTERN (Structural)
 * ──────────────────────────────
 * WHERE : SoundEffects as a whole, wrapping SoundManager
 * WHY : SoundManager has a low-level API:
 * playMusic(path), stopMusic(), pauseMusic(), resumeMusic(),
 * playSound(path), playComboSound(path, count)
 * Callers (GameBoard, TetrisFrame, UserInterface) should not
 * need to know file paths, combo counts, or whether to call
 * pauseMusic vs stopMusic. SoundEffects is the Facade —
 * it presents a clean, game-event-oriented API:
 * onMove(), onHardDrop(), onTetris(), onGameOver() …
 * Changing an audio file path or switching audio libraries
 * only requires editing SoundEffects, not every call site.
 * CATEGORY: Structural — describes how objects are composed/connected.
 *
 * 3. SINGLE RESPONSIBILITY PRINCIPLE (SOLID — S)
 * ───────────────────────────────────────────────
 * WHERE : SoundEffects vs SoundManager
 * WHY : SoundEffects is only responsible for MAPPING game events
 * to sound calls. It does not load files, manage clips, or
 * control audio hardware — that is SoundManager's job.
 * The two classes have separate, well-defined responsibilities.
 *
 * 4. OPEN / CLOSED PRINCIPLE (SOLID — O)
 * ──────────────────────────────────────
 * WHERE : ISoundEffects interface
 * WHY : By extracting ISoundEffects, new sound behaviour (e.g. a
 * "SilentMode" for testing, or "SurroundSoundEffects") can be
 * added by implementing the interface — without modifying
 * SoundEffects or any call site.
 *
 * 5. DEPENDENCY INVERSION PRINCIPLE (SOLID — D)
 * ──────────────────────────────────────────────
 * WHERE : ISoundEffects interface
 * WHY : Callers depend on the ISoundEffects abstraction, not the
 * concrete SoundEffects class. High-level modules (GameBoard,
 * TetrisFrame) should not depend on low-level audio details.
 *
 * 6. INTERFACE SEGREGATION PRINCIPLE (SOLID — I)
 * ─────────────────────────────────────────────
 * WHERE : ISoundEffects split into logical method groups
 * WHY : All methods are grouped by concern (music, movement, lines,
 * milestones, menu, game state). A caller that only needs menu
 * sounds is not forced to depend on gameplay sound methods.
 * The interface is kept minimal and cohesive.
 *
 * ═══════════════════════════════════════════════════════════════════
 * BACKWARD COMPATIBILITY
 * ═══════════════════════════════════════════════════════════════════
 * All existing call sites use SoundEffects.onMove(), SoundEffects.onDrop()
 * etc. (static calls). To avoid editing every call site while still
 * applying the Singleton pattern, static forwarding methods are kept
 * below. They delegate to getInstance() internally.
 * This preserves the existing API while introducing proper OO structure.
 * ═══════════════════════════════════════════════════════════════════
 */
public class SoundEffects implements ISoundEffects {

    // ── Audio file path constants ─────────────────────────────────────────────
    // SOLID/DRY: all paths in one place. Changing a file name = one edit here.
    // Previously these were scattered as inline string literals across methods.
    private static final String MUSIC_PATH = "OOP-project/Sounds/tetrismusic.wav";
    private static final String SND_DROP = "OOP-project/Sounds/soundsdrop.wav";
    private static final String SND_PIECE_LOCK = "Sounds/soundspiecelock.wav";
    private static final String SND_ROTATE = "Sounds/soundsrotate.wav";
    private static final String SND_MOVE = "Sounds/soundslateralmove.wav";
    private static final String SND_CLEAR = "OOP-project/Sounds/soundsclear.wav";
    private static final String SND_TETRIS = "OOP-project/Sounds/soundstetris.wav";
    private static final String SND_LEVEL_UP = "Sounds/soundslevelup.wav";
    private static final String SND_SELECT = "Sounds/soundsselect.wav";
    private static final String SND_GAME_OVER = "Sounds/soundsgameover.wav";
    private static final String SND_HIGH_SCORE = "Sounds/soundshighscore.wav";

    // ═════════════════════════════════════════════════════════════════════════
    // SINGLETON — single shared instance
    // ─────────────────────────────────
    // Only one SoundEffects object exists for the lifetime of the program.
    // Lazy initialisation: the instance is created on first call to getInstance().
    //
    // WHY NOT STATIC METHODS ONLY (the original design):
    // Static classes cannot implement ISoundEffects, cannot be mocked in
    // tests, and cannot be swapped for a different implementation (e.g.
    // SilentSoundEffects). Singleton gives the same guarantee with more
    // flexibility.
    // ═════════════════════════════════════════════════════════════════════════

    /** The single instance — null until first access (lazy initialisation). */
    private static SoundEffects instance = null;

    /**
     * Private constructor — enforces Singleton.
     * No code outside this class can call new SoundEffects().
     */
    private SoundEffects() {
    }

    /**
     * Returns the single shared SoundEffects instance.
     * Creates it on the first call (lazy initialisation).
     *
     * PATTERN — Singleton (Creational):
     * Guarantees exactly one instance exists.
     * All static forwarding methods below delegate here.
     */
    public static SoundEffects getInstance() {
        if (instance == null) {
            instance = new SoundEffects(); // created once, reused forever
        }
        return instance;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACADE — game-event API (implements ISoundEffects)
    // ───────────────────────────────────────────────────
    // Each method maps a high-level game event to one or more low-level
    // SoundManager calls. Call sites never see file paths or combo counts.
    // ═════════════════════════════════════════════════════════════════════════

    // ── Background Music ─────────────────────────────────────────────────────

    /**
     * Starts the looping background music.
     * FACADE: hides the file path from the caller.
     */
    @Override
    public void startMusic() {
        SoundManager.playMusic(MUSIC_PATH);
    }

    /**
     * Stops the background music entirely.
     * Called on game over or returning to a menu.
     */
    @Override
    public void stopMusic() {
        SoundManager.stopMusic();
    }

    /**
     * Pauses or resumes background music.
     * FACADE: hides the pauseMusic/resumeMusic split from the caller.
     *
     * @param paused true to pause, false to resume
     */
    @Override
    public void setMusicPaused(boolean paused) {
        if (paused)
            SoundManager.pauseMusic();
        else
            SoundManager.resumeMusic();
    }

    // ── Piece Movement & Placement ───────────────────────────────────────────

    /** Soft-drop sound (player holds down key). */
    @Override
    public void onSoftDrop() {
        SoundManager.playSound(SND_DROP);
    }

    /**
     * Hard-drop sound (player presses Space).
     * FACADE: playComboSound detail hidden — caller just says "hard drop".
     */
    @Override
    public void onHardDrop() {
        SoundManager.playComboSound(SND_DROP, 4);
    }

    /** Piece lock sound — played when a piece settles onto the stack. */
    @Override
    public void onPieceLock() {
        SoundManager.playSound(SND_PIECE_LOCK);
    }

    /** Rotation sound. */
    @Override
    public void onRotate() {
        SoundManager.playSound(SND_ROTATE);
    }

    /** Lateral movement sound (left / right). */
    @Override
    public void onMove() {
        SoundManager.playSound(SND_MOVE);
    }

    // ── Line Clears ──────────────────────────────────────────────────────────

    /**
     * Line-clear sound for 1, 2, or 3 lines.
     * FACADE: combo count is an internal audio detail — caller just passes
     * how many lines were cleared; SoundEffects decides what that means.
     *
     * @param lineCount number of lines cleared simultaneously (1–3)
     */
    @Override
    public void onLineClear(int lineCount) {
        SoundManager.playComboSound(SND_CLEAR, lineCount);
    }

    /**
     * Tetris sound — four lines cleared simultaneously.
     * Kept separate from onLineClear() so the caller expresses intent clearly.
     */
    @Override
    public void onTetris() {
        SoundManager.playComboSound(SND_TETRIS, 4);
    }

    // ── Level & Score Milestones ─────────────────────────────────────────────

    /** Level-up sound — played when the player advances a level. */
    @Override
    public void onLevelUp() {
        SoundManager.playSound(SND_LEVEL_UP);
    }

    // ── Menu & UI Interactions ───────────────────────────────────────────────

    /**
     * Menu button click sound.
     * onMenuClick and onMenuBack use the same audio file —
     * keeping them as separate methods preserves the semantic distinction
     * (they could diverge in future without changing call sites).
     */
    @Override
    public void onMenuClick() {
        SoundManager.playSound(SND_SELECT);
    }

    /** Back-navigation sound. */
    @Override
    public void onMenuBack() {
        SoundManager.playSound(SND_SELECT);
    }

    // ── Game State Events ────────────────────────────────────────────────────

    /** Game-over sound — stack reached the top. */
    @Override
    public void onGameOver() {
        SoundManager.playSound(SND_GAME_OVER);
    }

    /** High-score sound — player beat or matched their best score. */
    @Override
    public void onHighScore() {
        SoundManager.playSound(SND_HIGH_SCORE);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STATIC FORWARDING METHODS — backward-compatible call-site API
    // ──────────────────────────────────────────────────────────────
    // All existing callers use SoundEffects.onMove(), SoundEffects.onDrop()
    // etc. as static calls. These forwarders preserve that API exactly,
    // while internally routing through the Singleton instance.
    //
    // BENEFIT: zero changes required in GameBoard, TetrisFrame, or
    // UserInterface — they continue calling SoundEffects.onXxx() as before.
    // The Singleton pattern is fully applied under the hood.
    // ═════════════════════════════════════════════════════════════════════════

    // ── Background Music forwarders ───────────────────────────────────────────
    public static void startBackgroundMusic() {
        getInstance().startMusic();
    }

    public static void stopBackgroundMusic() {
        getInstance().stopMusic();
    }

    public static void setBackgroundMusicPaused(boolean p) {
        getInstance().setMusicPaused(p);
    }

    // ── Movement forwarders ───────────────────────────────────────────────────
    public static void onSoftDrop() {
        getInstance().onSoftDrop();
    }

    public static void onHardDrop() {
        getInstance().onHardDrop();
    }

    public static void onPieceLock() {
        getInstance().onPieceLock();
    }

    public static void onRotate() {
        getInstance().onRotate();
    }

    public static void onMove() {
        getInstance().onMove();
    }

    // ── Line clear forwarders ─────────────────────────────────────────────────
    public static void onLineClear(int lineCount) {
        getInstance().onLineClear(lineCount);
    }

    public static void onTetris() {
        getInstance().onTetris();
    }

    // ── Milestone forwarders ──────────────────────────────────────────────────
    public static void onLevelUp() {
        getInstance().onLevelUp();
    }

    // ── Menu forwarders ───────────────────────────────────────────────────────
    public static void onMenuClick() {
        getInstance().onMenuClick();
    }

    public static void onMenuBack() {
        getInstance().onMenuBack();
    }

    // ── Game state forwarders ─────────────────────────────────────────────────
    public static void onGameOver() {
        getInstance().onGameOver();
    }

    public static void onHighScore() {
        getInstance().onHighScore();
    }
}