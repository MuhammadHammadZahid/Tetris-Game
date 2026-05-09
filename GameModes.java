// package com.mycompany.tetrisgame;

/**
 * GameModes.java — Defines all game modes and difficulties for Tetris.
 *
 * ═══════════════════════════════════════════════════════════════════
 * DESIGN PATTERNS & PRINCIPLES APPLIED
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. TEMPLATE METHOD PATTERN (Behavioral)
 * ──────────────────────────────────────
 * WHERE : GameMode abstract class — getResultStats(), onStart()
 * WHY : The result stats structure (score → lines → time) is shared
 * by StandardMode and TimeTrial. ZenMode and FourtyLines only
 * change the ORDER of fields, not the formatting logic.
 * Template Method defines the skeleton in the base class;
 * subclasses override only what differs (buildResultStats).
 * formatTime() is a protected utility — a classic "hook" that
 * all subclasses inherit without duplication.
 * CATEGORY: Behavioral — defines how objects carry out their work,
 * with the base class controlling the algorithm's structure.
 *
 * 2. FACTORY METHOD PATTERN (Creational)
 * ──────────────────────────────────────
 * WHERE : createFresh() — abstract method on GameMode, implemented
 * by every concrete subclass.
 * WHY : UserInterface.GameOverScreen previously had a private
 * freshMode() method containing a brittle instanceof chain:
 *
 * if (mode instanceof StandardMode) return new StandardMode();
 * if (mode instanceof FourtyLines) return new FourtyLines();
 * ...
 *
 * This forced GameOverScreen to know every concrete GameMode
 * type — a clear OCP violation. Now each GameMode subclass
 * is responsible for producing a fresh copy of itself.
 * GameOverScreen just calls mode.createFresh() — it never
 * needs to be touched when a new mode is added.
 * CATEGORY: Creational — each subclass decides which object to create.
 *
 * 3. OPEN / CLOSED PRINCIPLE (SOLID — O)
 * ──────────────────────────────────────
 * WHERE : GameMode abstract class + all subclasses
 * WHY : The original code required editing GameOverScreen every time
 * a new mode was added (instanceof chain). Now GameMode defines
 * the contract; new modes are added by extension only — no
 * existing file needs modification. The abstract method
 * createFresh() enforces this at compile time.
 *
 * 4. SINGLE RESPONSIBILITY PRINCIPLE (SOLID — S)
 * ───────────────────────────────────────────────
 * WHERE : Each GameMode subclass
 * WHY : Each mode has exactly one reason to change — its own rules.
 * StandardMode only changes if standard rules change.
 * TimeTrial only changes if the time limit changes. Etc.
 * The base class owns shared state (lines, score, level) and
 * shared utilities (formatTime). Subclasses own their policies.
 *
 * 5. LISKOV SUBSTITUTION PRINCIPLE (SOLID — L)
 * ─────────────────────────────────────────────
 * WHERE : All four GameMode subclasses
 * WHY : Every subclass can be used wherever GameMode is expected
 * without breaking the game. GameBoard only holds a GameMode
 * reference and calls isGameOver(), recordsHighScore(), etc. —
 * it works correctly with any subclass passed to it.
 * ZenMode overrides showScore() and fixedLevel() but does NOT
 * break the contract — it returns valid booleans as expected.
 *
 * 6. INTERFACE SEGREGATION PRINCIPLE (SOLID — I)
 * ─────────────────────────────────────────────
 * WHERE : GameMode abstract class method grouping
 * WHY : Methods are grouped by who calls them:
 * - GameBoard calls: isGameOver(), onStart(), fixedLevel(),
 * shouldClearOnTopOut(), recordsHighScore(), showScore()
 * - UI calls: getModeName(), getResultStats(), createFresh()
 * No subclass is forced to implement methods it doesn't need —
 * defaults are provided for optional hooks (showScore, fixedLevel,
 * shouldClearOnTopOut) so subclasses only override what matters.
 *
 * 7. DEPENDENCY INVERSION PRINCIPLE (SOLID — D)
 * ──────────────────────────────────────────────
 * WHERE : GameBoard (caller) depends on GameMode (abstraction)
 * WHY : GameBoard never references StandardMode, ZenMode, etc.
 * It depends only on the GameMode abstraction. This file
 * reinforces that contract by keeping all concrete details
 * inside subclasses, invisible to callers.
 *
 * ═══════════════════════════════════════════════════════════════════
 */
public class GameModes {

    // ─────────────────────────────────────────────────────────────────────────
    // Difficulty enum — controls drop speed via starting level
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Difficulty levels map to starting level values that drive drop speed.
     * Adding a new difficulty requires only adding a new enum constant here
     * — no other file needs editing. (OCP)
     */
    public enum Difficulty {
        SLOW("Slow", 1),
        MEDIUM("Medium", 5),
        FAST("Fast", 10);

        private final String displayName;
        private final int startingLevel;

        Difficulty(String displayName, int startingLevel) {
            this.displayName = displayName;
            this.startingLevel = startingLevel;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getStartingLevel() {
            return startingLevel;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Abstract GameMode — Template Method base class
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Abstract superclass for all game modes.
     *
     * TEMPLATE METHOD: onStart() and getResultStats() define algorithm
     * skeletons. Subclasses override only the parts that differ.
     *
     * FACTORY METHOD: createFresh() is an abstract factory method — each
     * subclass returns a brand-new instance of itself, allowing
     * GameOverScreen to reset the mode without any instanceof checks.
     *
     * GameBoard holds a reference to a GameMode and calls its methods
     * each tick — it depends on this abstraction, never on subclasses. (DIP)
     */
    public static abstract class GameMode {

        // ── Shared mutable state (owned by base class — SRP) ─────────────────
        protected int lines = 0;
        protected int score = 0;
        protected long startTimeMs = 0;
        protected int level = 1;
        protected boolean started = false;

        // ── Abstract methods — every subclass MUST define these ──────────────

        /** Human-readable name shown in the UI (mode select + side panel). */
        public abstract String getModeName();

        /**
         * Called by GameBoard every tick to check whether the game should end.
         *
         * @param linesCleared total lines cleared so far
         * @param elapsedMs    milliseconds since the game started
         * @return true if the game should end
         */
        public abstract boolean isGameOver(int linesCleared, long elapsedMs);

        /** Whether this mode persists a high score to disk. */
        public abstract boolean recordsHighScore();

        // ── FACTORY METHOD (Creational) ───────────────────────────────────────
        /**
         * Returns a brand-new, reset instance of this exact GameMode subclass.
         *
         * PATTERN — Factory Method:
         * Each concrete subclass overrides this to return new XxxMode().
         * Callers (GameOverScreen via ScreenFactory) never use instanceof —
         * they simply call mode.createFresh() and get the right type back.
         *
         * WHY ABSTRACT:
         * Making it abstract forces every new subclass to implement it at
         * compile time. A missing implementation is a compile error, not a
         * silent runtime bug (the old default "return new StandardMode()" was
         * a hidden fallback that masked missing cases).
         */
        public abstract GameMode createFresh();

        // ── Optional hook methods — subclasses override only what they need ───

        /**
         * Whether the score counter is visible in the side panel.
         * DEFAULT: true. ZenMode overrides to false.
         * (ISP: most modes don't need to think about this)
         */
        public boolean showScore() {
            return true;
        }

        /**
         * Whether this mode keeps the level fixed (ignores line-based levelling).
         * DEFAULT: false. ZenMode overrides to true.
         */
        public boolean fixedLevel() {
            return false;
        }

        /**
         * Whether the board should be wiped instead of ending when topped out.
         * DEFAULT: false. ZenMode overrides to true (no game over in Zen).
         */
        public boolean shouldClearOnTopOut() {
            return false;
        }

        // ── TEMPLATE METHOD (Behavioral) — onStart() ─────────────────────────
        /**
         * Called when the game starts. Captures start time and initial level.
         *
         * TEMPLATE METHOD: base class owns the initialisation skeleton.
         * Subclasses may override to add mode-specific startup logic, but
         * should call super.onStart() to preserve shared state setup.
         */
        public void onStart(int startingLevel) {
            this.startTimeMs = System.currentTimeMillis();
            this.level = startingLevel;
            this.started = true;
        }

        // ── TEMPLATE METHOD (Behavioral) — getResultStats() ──────────────────
        /**
         * Builds the result screen text shown after game over.
         *
         * TEMPLATE METHOD: the base class provides the default layout
         * (score → lines → time). Subclasses override to reorder or
         * customise fields, but all share the same formatTime() utility.
         *
         * @param finalScore total score at game end
         * @param finalLines total lines cleared
         * @param elapsedMs  total elapsed time in ms
         * @return label/value pairs: { {"SCORE","1200"}, {"LINES","14"}, ... }
         */
        public String[][] getResultStats(int finalScore, int finalLines, long elapsedMs) {
            // Default layout: score first, then lines, then time.
            // Subclasses reorder this to suit their primary metric.
            return new String[][] {
                    { "SCORE", String.valueOf(finalScore) },
                    { "LINES", String.valueOf(finalLines) },
                    { "TIME", formatTime(elapsedMs) }
            };
        }

        // ── Protected utility (shared by all subclasses — DRY) ───────────────
        /**
         * Formats a millisecond duration as "M:SS".
         * Protected so all subclasses can use it in getResultStats() overrides
         * without duplicating the formatting logic.
         */
        protected static String formatTime(long ms) {
            long totalSec = ms / 1000;
            long min = totalSec / 60;
            long sec = totalSec % 60;
            return String.format("%d:%02d", min, sec);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // StandardMode — classic endless Tetris
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Classic endless mode. The game ends when a new piece cannot spawn
     * (stack reaches the top). isGameOver() always returns false here —
     * GameBoard handles top-out termination via spawnPiece().
     *
     * LSP: can replace any GameMode reference without breaking GameBoard.
     * SRP: only responsible for "endless until top-out" rules.
     */
    public static class StandardMode extends GameMode {

        @Override
        public String getModeName() {
            return "Standard";
        }

        @Override
        public boolean isGameOver(int linesCleared, long elapsedMs) {
            // Top-out termination is handled by GameBoard.spawnPiece().
            // This method is only for time/line-based conditions.
            return false;
        }

        @Override
        public boolean recordsHighScore() {
            return true;
        }

        // ── FACTORY METHOD implementation ─────────────────────────────────────
        /**
         * Returns a fresh StandardMode instance.
         * Called by ScreenFactory.createFreshMode() when "Play Again" is pressed.
         * No instanceof check needed anywhere — this class knows its own type.
         */
        @Override
        public GameMode createFresh() {
            return new StandardMode();
        }

        // Inherits default getResultStats() from GameMode (score→lines→time).
        // Overridden here to make the ordering explicit and visible in code review.
        @Override
        public String[][] getResultStats(int finalScore, int finalLines, long elapsedMs) {
            return new String[][] {
                    { "SCORE", String.valueOf(finalScore) },
                    { "LINES", String.valueOf(finalLines) },
                    { "TIME", formatTime(elapsedMs) } // formatTime() inherited from base
            };
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FourtyLines — clear 40 lines as fast as possible
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sprint mode: race to clear 40 lines. Time is the primary metric.
     * isGameOver() triggers when linesCleared >= 40.
     *
     * SRP: only responsible for the "clear 40 lines" termination rule.
     * OCP: adding a different target (e.g. 20 lines) = new subclass, no edits here.
     */
    public static class FourtyLines extends GameMode {

        // Named constant — no magic numbers. Easy to spot and change. (DRY)
        private static final int TARGET_LINES = 40;

        @Override
        public String getModeName() {
            return "40 Lines";
        }

        @Override
        public boolean isGameOver(int linesCleared, long elapsedMs) {
            return linesCleared >= TARGET_LINES;
        }

        @Override
        public boolean recordsHighScore() {
            return true;
        }

        // ── FACTORY METHOD implementation ─────────────────────────────────────
        @Override
        public GameMode createFresh() {
            return new FourtyLines();
        }

        // Time is the primary metric for this mode — result shows TIME first.
        // TEMPLATE METHOD: overrides base getResultStats() to reorder fields.
        @Override
        public String[][] getResultStats(int finalScore, int finalLines, long elapsedMs) {
            return new String[][] {
                    { "TIME", formatTime(elapsedMs) }, // primary metric first
                    { "LINES", String.valueOf(finalLines) },
                    { "SCORE", String.valueOf(finalScore) }
            };
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TimeTrial — score as many points as possible in 2 minutes
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Timed mode: 2-minute countdown. Score is the primary metric.
     * isGameOver() triggers when elapsed time >= 120 000 ms.
     *
     * SRP: only responsible for the "2-minute countdown" termination rule.
     * Extra public methods getRemainingMs() / getDurationMs() serve the UI
     * countdown display — they are part of TimeTrial's specific contract.
     */
    public static class TimeTrial extends GameMode {

        private static final long DURATION_MS = 120_000L; // 2 minutes

        @Override
        public String getModeName() {
            return "Time Trial";
        }

        @Override
        public boolean isGameOver(int linesCleared, long elapsedMs) {
            return elapsedMs >= DURATION_MS;
        }

        @Override
        public boolean recordsHighScore() {
            return true;
        }

        // ── FACTORY METHOD implementation ─────────────────────────────────────
        @Override
        public GameMode createFresh() {
            return new TimeTrial();
        }

        /**
         * Returns remaining countdown time in ms.
         * Used by GameBoard.drawSidePanel() for the live countdown display.
         */
        public long getRemainingMs(long elapsedMs) {
            return Math.max(0, DURATION_MS - elapsedMs);
        }

        /** Exposes total duration for any UI progress bar or display. */
        public long getDurationMs() {
            return DURATION_MS;
        }

        // Score is the primary metric — result shows SCORE first.
        // TEMPLATE METHOD: overrides base getResultStats() to reorder fields.
        @Override
        public String[][] getResultStats(int finalScore, int finalLines, long elapsedMs) {
            return new String[][] {
                    { "SCORE", String.valueOf(finalScore) }, // primary metric first
                    { "LINES", String.valueOf(finalLines) },
                    { "TIME", formatTime(elapsedMs) }
            };
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ZenMode — no game over, no pressure
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Relaxation / practice mode.
     * - isGameOver() always returns false — the game never ends.
     * - Score counter is hidden (showScore() = false).
     * - Level is fixed at the chosen starting level (fixedLevel() = true).
     * - On top-out the board is wiped instead of ending (shouldClearOnTopOut()).
     * - No high score is recorded.
     *
     * SRP: only responsible for "infinite, no-pressure" behaviour.
     * LSP: still fully substitutable for GameMode — GameBoard works correctly
     * with a ZenMode reference despite all the overrides.
     */
    public static class ZenMode extends GameMode {

        @Override
        public String getModeName() {
            return "Zen";
        }

        @Override
        public boolean isGameOver(int linesCleared, long elapsedMs) {
            return false; // Never ends — ZenMode's defining rule
        }

        @Override
        public boolean recordsHighScore() {
            return false;
        }

        // ── FACTORY METHOD implementation ─────────────────────────────────────
        @Override
        public GameMode createFresh() {
            return new ZenMode();
        }

        // ── ZenMode-specific hook overrides ───────────────────────────────────

        /** Hides the score counter in the side panel — Zen is stress-free. */
        @Override
        public boolean showScore() {
            return false;
        }

        /** Keeps level fixed — speed never increases in Zen mode. */
        @Override
        public boolean fixedLevel() {
            return true;
        }

        /**
         * Wipes the board on top-out instead of ending the game.
         * This is ZenMode's unique behaviour — overrides the default false.
         */
        @Override
        public boolean shouldClearOnTopOut() {
            return true;
        }

        // TEMPLATE METHOD: ZenMode never shows a game-over screen, but
        // getResultStats() is implemented for safety (LSP — must honour contract).
        // Shows only lines, since score is hidden during play.
        @Override
        public String[][] getResultStats(int finalScore, int finalLines, long elapsedMs) {
            return new String[][] {
                    { "LINES", String.valueOf(finalLines) }
            };
        }
    }
}