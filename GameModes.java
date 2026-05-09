// package com.mycompany.tetrisgame;
/**
 * GameModes.java — Defines all game modes and difficulties for Tetris.
 *
 * To add a new mode: extend GameMode, implement isGameOver(), getModeName(),
 * recordsHighScore(), and override any hooks you need.
 *
 * No other file needs to be edited to add new modes — GameBoard calls the
 * GameMode interface exclusively.
 */
public class GameModes {
    public enum Difficulty { //changes drop speed
        SLOW   ("Slow",   1),
        MEDIUM ("Medium", 5),
        FAST   ("Fast",  10);

        private final String displayName;
        private final int startingLevel;

        Difficulty(String displayName, int startingLevel) {
            this.displayName  = displayName;
            this.startingLevel = startingLevel;
        }

        public String getDisplayName()  { return displayName;  }
        public int    getStartingLevel() { return startingLevel; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Abstract GameMode base class
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Abstract superclass for all game modes.
     * GameBoard holds a reference to a GameMode and calls its methods
     * each tick to decide game-over conditions and scoring behaviour.
     */
    public static abstract class GameMode {

        protected int    lines      = 0;
        protected int    score      = 0;
        protected long   startTimeMs = 0;
        protected int    level      = 1;
        protected boolean started   = false;
        public boolean shouldClearOnTopOut() { return false; } //for zen mode

        /** Human-readable name shown in the UI. */
        public abstract String getModeName();

        /**
         * Called by GameBoard every game tick to check whether the game
         * should end.
         * @param linesCleared total lines cleared so far
         * @param elapsedMs    milliseconds since the game started
         * @return true if the game should end
         */
        public abstract boolean isGameOver(int linesCleared, long elapsedMs);

        /**
         * Whether this mode saves a high score.
         * ZenMode returns false; all others return true.
         */
        public abstract boolean recordsHighScore();

        /**
         * Whether this mode should show the score counter.
         * ZenMode hides the score; all other modes show it.
         */
        public boolean showScore() { return true; }

        /**
         * Whether this mode uses a fixed level (ignores line-based levelling).
         * ZenMode keeps the level fixed.
         */
        public boolean fixedLevel() { return false; }

        /**
         * Called when the mode is first started so it can capture the start time.
         */
        public void onStart(int startingLevel) {
            this.startTimeMs = System.currentTimeMillis();
            this.level       = startingLevel;
            this.started     = true;
        }

        /**
         * Constructs the result screen text shown after game over.
         * Subclasses may override for custom messaging.
         * @param finalScore total score
         * @param finalLines total lines cleared
         * @param elapsedMs  total time elapsed in ms
         * @return array of label/value pairs: { {"Label", "Value"}, ... }
         */
        public String[][] getResultStats(int finalScore, int finalLines, long elapsedMs) {
            return new String[][] {
                {"SCORE", String.valueOf(finalScore)},
                {"LINES", String.valueOf(finalLines)},
                {"TIME",  formatTime(elapsedMs)}
            };
        }

        /** Utility: format ms as M:SS */
        protected static String formatTime(long ms) {
            long totalSec = ms / 1000;
            long min      = totalSec / 60;
            long sec      = totalSec % 60;
            return String.format("%d:%02d", min, sec);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // StandardMode — classic endless mode
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * StandardMode: the classic endless Tetris experience.
     * The game ends when a new piece cannot be placed (stack reaches the top).
     * GameBoard triggers this via spawnPiece() detecting an invalid position —
     * isGameOver() here always returns false; the board handles the termination.
     */
    public static class StandardMode extends GameMode {

        @Override
        public String getModeName() { return "Standard"; }

        @Override
        public boolean isGameOver(int linesCleared, long elapsedMs) {
            // Termination is handled by GameBoard when a spawn fails.
            return false;
        }

        @Override
        public boolean recordsHighScore() { return true; }

        @Override
        public String[][] getResultStats(int finalScore, int finalLines, long elapsedMs) {
            return new String[][] {
                {"SCORE", String.valueOf(finalScore)},
                {"LINES", String.valueOf(finalLines)},
                {"TIME",  formatTime(elapsedMs)}
            };
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FourtyLines — clear 40 lines as fast as possible
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * FourtyLines (40-Lines Sprint): race to clear exactly 40 lines.
     * isGameOver() returns true once the player has cleared >= 40 lines.
     * The result screen shows the time taken as the primary metric.
     */
    public static class FourtyLines extends GameMode {

        private static final int TARGET_LINES = 40;

        @Override
        public String getModeName() { return "40 Lines"; }

        @Override
        public boolean isGameOver(int linesCleared, long elapsedMs) {
            return linesCleared >= TARGET_LINES;
        }

        @Override
        public boolean recordsHighScore() { return true; }

        @Override
        public String[][] getResultStats(int finalScore, int finalLines, long elapsedMs) {
            return new String[][] {
                {"TIME",  formatTime(elapsedMs)},
                {"LINES", String.valueOf(finalLines)},
                {"SCORE", String.valueOf(finalScore)}
            };
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TimeTrial — score as many points as possible in 2 minutes
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * TimeTrial: 2-minute countdown.
     * isGameOver() returns true when elapsed time exceeds 120 000 ms.
     * The result screen focuses on score.
     */
    public static class TimeTrial extends GameMode {

        private static final long DURATION_MS = 120_000L; // 2 minutes

        @Override
        public String getModeName() { return "Time Trial"; }

        @Override
        public boolean isGameOver(int linesCleared, long elapsedMs) {
            return elapsedMs >= DURATION_MS;
        }

        @Override
        public boolean recordsHighScore() { return true; }

        /**
         * Returns the remaining time in ms (useful for the UI countdown).
         */
        public long getRemainingMs(long elapsedMs) {
            return Math.max(0, DURATION_MS - elapsedMs);
        }

        /** Total duration of this mode. */
        public long getDurationMs() { return DURATION_MS; }

        @Override
        public String[][] getResultStats(int finalScore, int finalLines, long elapsedMs) {
            return new String[][] {
                {"SCORE", String.valueOf(finalScore)},
                {"LINES", String.valueOf(finalLines)},
                {"TIME",  formatTime(elapsedMs)}
            };
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ZenMode — no game over, no pressure
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ZenMode: practice/relaxation mode.
     * isGameOver() always returns false — the game never ends.
     * Score counter is hidden. Level stays fixed at the chosen starting level.
     * No high score is recorded.
     */
    public static class ZenMode extends GameMode {
        
        @Override
        public boolean shouldClearOnTopOut() { return true; }

        @Override
        public String getModeName() { return "Zen"; }

        @Override
        public boolean isGameOver(int linesCleared, long elapsedMs) {
            return false; // Never game over
        }

        @Override
        public boolean recordsHighScore() { return false; }

        @Override
        public boolean showScore() { return false; }

        @Override
        public boolean fixedLevel() { return true; }

        @Override
        public String[][] getResultStats(int finalScore, int finalLines, long elapsedMs) {
            // Zen mode never produces a game-over screen, but implement for safety
            return new String[][] {
                {"LINES", String.valueOf(finalLines)}
            };
        }
    }
}
