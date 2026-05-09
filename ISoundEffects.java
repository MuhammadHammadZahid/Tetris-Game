/**
 * ISoundEffects.java — Contract for all sound-effect implementations.
 *
 * ═══════════════════════════════════════════════════════════════════
 * WHY THIS INTERFACE EXISTS
 * ═══════════════════════════════════════════════════════════════════
 *
 * OPEN / CLOSED PRINCIPLE (SOLID — O):
 * New sound behaviour (e.g. SilentSoundEffects for unit testing,
 * SurroundSoundEffects for a premium build) can be added by
 * implementing this interface — without modifying SoundEffects
 * or any game class that calls it.
 *
 * DEPENDENCY INVERSION PRINCIPLE (SOLID — D):
 * High-level modules (GameBoard, TetrisFrame, UserInterface) should
 * depend on abstractions, not concretions. If those classes ever
 * accept an ISoundEffects via constructor injection instead of
 * calling SoundEffects.getInstance() directly, they become fully
 * decoupled from the audio implementation.
 *
 * INTERFACE SEGREGATION PRINCIPLE (SOLID — I):
 * The interface groups only the methods that external callers need.
 * Internal helpers (getInstance, path constants) stay in the
 * concrete SoundEffects class and are invisible through this interface.
 *
 * ═══════════════════════════════════════════════════════════════════
 */
public interface ISoundEffects {

    // ── Background Music ─────────────────────────────────────────────────────
    void startMusic();

    void stopMusic();

    void setMusicPaused(boolean paused);

    // ── Piece Movement & Placement ───────────────────────────────────────────
    void onSoftDrop();

    void onHardDrop();

    void onPieceLock();

    void onRotate();

    void onMove();

    // ── Line Clears ──────────────────────────────────────────────────────────
    void onLineClear(int lineCount);

    void onTetris();

    // ── Level & Score Milestones ─────────────────────────────────────────────
    void onLevelUp();

    // ── Menu & UI Interactions ───────────────────────────────────────────────
    void onMenuClick();

    void onMenuBack();

    // ── Game State Events ────────────────────────────────────────────────────
    void onGameOver();

    void onHighScore();
}