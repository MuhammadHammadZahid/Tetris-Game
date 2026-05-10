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
public class SoundEffects {

    // ── Audio file path constants ─────────────────────────────────────────────
    private static final String MUSIC_PATH    = "OOP-project/Sounds/tetrismusic.wav";
    private static final String SND_DROP      = "OOP-project/Sounds/soundsdrop.wav";
    private static final String SND_PIECE_LOCK = "Sounds/soundspiecelock.wav";
    private static final String SND_ROTATE    = "Sounds/soundsrotate.wav";
    private static final String SND_MOVE      = "Sounds/soundslateralmove.wav";
    private static final String SND_CLEAR     = "OOP-project/Sounds/soundsclear.wav";
    private static final String SND_TETRIS    = "OOP-project/Sounds/soundstetris.wav";
    private static final String SND_LEVEL_UP  = "Sounds/soundslevelup.wav";
    private static final String SND_SELECT    = "Sounds/soundsselect.wav";
    private static final String SND_GAME_OVER = "Sounds/soundsgameover.wav";
    private static final String SND_HIGH_SCORE = "Sounds/soundshighscore.wav";

    // ═════════════════════════════════════════════════════════════════════════
    // SINGLETON
    // ═════════════════════════════════════════════════════════════════════════

    private static SoundEffects instance = null;

    private SoundEffects() {}

    public static SoundEffects getInstance() {
        if (instance == null) {
            instance = new SoundEffects();
        }
        return instance;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACADE — instance (impl) methods
    // These are the real implementations. Static forwarders below delegate
    // here via getInstance(). Named with "impl" prefix to make the
    // distinction clear and prevent accidental self-recursion.
    // ═════════════════════════════════════════════════════════════════════════

    // ── Background Music ──────────────────────────────────────────────────────

    private void implStartMusic() {
        SoundManager.playMusic(MUSIC_PATH);
    }

    private void implStopMusic() {
        SoundManager.stopMusic();
    }

    private void implSetMusicPaused(boolean paused) {
        if (paused) SoundManager.pauseMusic();
        else        SoundManager.resumeMusic();
    }

    // ── Piece Movement & Placement ────────────────────────────────────────────

    private void implOnSoftDrop() {
        SoundManager.playSound(SND_DROP);
    }

    private void implOnHardDrop() {
        SoundManager.playComboSound(SND_DROP, 4);
    }

    private void implOnPieceLock() {
        SoundManager.playSound(SND_PIECE_LOCK);
    }

    private void implOnRotate() {
        SoundManager.playSound(SND_ROTATE);
    }

    private void implOnMove() {
        SoundManager.playSound(SND_MOVE);
    }

    // ── Line Clears ───────────────────────────────────────────────────────────

    private void implOnLineClear(int lineCount) {
        SoundManager.playComboSound(SND_CLEAR, lineCount);
    }

    private void implOnTetris() {
        SoundManager.playComboSound(SND_TETRIS, 4);
    }

    // ── Level & Score Milestones ──────────────────────────────────────────────

    private void implOnLevelUp() {
        SoundManager.playSound(SND_LEVEL_UP);
    }

    // ── Menu & UI Interactions ────────────────────────────────────────────────

    private void implOnMenuClick() {
        SoundManager.playSound(SND_SELECT);
    }

    private void implOnMenuBack() {
        SoundManager.playSound(SND_SELECT);
    }

    // ── Game State Events ─────────────────────────────────────────────────────

    private void implOnGameOver() {
        SoundManager.playSound(SND_GAME_OVER);
    }

    private void implOnHighScore() {
        SoundManager.playSound(SND_HIGH_SCORE);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STATIC FORWARDING METHODS — backward-compatible call-site API
    // ──────────────────────────────────────────────────────────────
    // Each static method delegates to the corresponding impl method on the
    // Singleton instance. They must NOT call a same-named instance method
    // or they will recurse infinitely (stack overflow).
    // ═════════════════════════════════════════════════════════════════════════

    // ── Background Music forwarders ───────────────────────────────────────────
    public static void startBackgroundMusic()            { getInstance().implStartMusic();            }
    public static void stopBackgroundMusic()             { getInstance().implStopMusic();             }
    public static void setBackgroundMusicPaused(boolean p) { getInstance().implSetMusicPaused(p);    }

    // ── Movement forwarders ───────────────────────────────────────────────────
    public static void onSoftDrop()                      { getInstance().implOnSoftDrop();            }
    public static void onHardDrop()                      { getInstance().implOnHardDrop();            }
    public static void onPieceLock()                     { getInstance().implOnPieceLock();           }
    public static void onRotate()                        { getInstance().implOnRotate();              }
    public static void onMove()                          { getInstance().implOnMove();                }

    // ── Line clear forwarders ─────────────────────────────────────────────────
    public static void onLineClear(int lineCount)        { getInstance().implOnLineClear(lineCount);  }
    public static void onTetris()                        { getInstance().implOnTetris();              }

    // ── Milestone forwarders ──────────────────────────────────────────────────
    public static void onLevelUp()                       { getInstance().implOnLevelUp();             }

    // ── Menu forwarders ───────────────────────────────────────────────────────
    public static void onMenuClick()                     { getInstance().implOnMenuClick();           }
    public static void onMenuBack()                      { getInstance().implOnMenuBack();            }

    // ── Game state forwarders ─────────────────────────────────────────────────
    public static void onGameOver()                      { getInstance().implOnGameOver();            }
    public static void onHighScore()                     { getInstance().implOnHighScore();           }
}