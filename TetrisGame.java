// package com.mycompany.tetrisgame;

import java.awt.*; //.awt numerous GUI elements
import java.awt.event.*;
import java.io.*; //.io and .nio needed to read-write files (highscores)
import java.nio.file.*;
import java.util.ArrayList; //.util tracks line clear animations
import java.util.List;
import javax.swing.*; //platform independant GUI elements, buttons etc.

/**
 * TetrisGame — Main class containing:
 * - TetrisFrame      : window/screen manager (delegates UI to UserInterface.java)
 * - GameBoard        : core game logic + rendering
 * - HighScoreManager : reads/writes one high score per mode to CSV
 * - main()           : entry point
 *
 * UI screens live in UserInterface.java.
 * Game modes and difficulties live in GameModes.java.
 * Sound effect stubs live in SoundEffects.java.
 *
 * ═══════════════════════════════════════════════════════════════════
 * DESIGN PATTERNS & PRINCIPLES APPLIED
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. TEMPLATE METHOD PATTERN (Behavioral)
 * ─────────────────────────────────────────
 * WHERE : HighScoreManager — private readCsvLines() helper
 * WHY   : getHighScore() and saveIfHighScore() both duplicated the
 *         same file-existence check, Files.readAllLines() call, line-
 *         split logic, and exception swallowing — roughly 10 lines
 *         each. readCsvLines() defines the shared skeleton once (open
 *         → read → catch → return); each public method only supplies
 *         what is unique to its operation. Eliminates ~10 lines of
 *         duplicated boilerplate.
 * CATEGORY: Behavioral — defines how objects carry out their work.
 *
 * 2. OPEN / CLOSED PRINCIPLE (SOLID — O)
 * ─────────────────────────────────────────
 * WHERE : GameBoard.drawSidePanel() → GameModes.GameMode.getSidePanelStats()
 * WHY   : The original drawSidePanel() contained two instanceof checks:
 *
 *             if (mode instanceof GameModes.TimeTrial tt) { ... }
 *             if (mode instanceof GameModes.FourtyLines)  { ... }
 *
 *         This is the exact same OCP violation as the old freshMode()
 *         instanceof chain in UserInterface — every new GameMode
 *         required editing GameBoard. The fix mirrors the Factory Method
 *         solution used there: each GameMode now implements
 *         getSidePanelStats(lines, elapsed) to return its own extra
 *         label/value pairs. GameBoard iterates them generically and
 *         never needs modification when a new mode is added.
 *
 *         BEFORE (OCP violation — open for modification):
 *             if (mode instanceof GameModes.TimeTrial tt) {
 *                 long remaining = tt.getRemainingMs(elapsed);
 *                 ... draw "TIME" label manually ...
 *             }
 *             if (mode instanceof GameModes.FourtyLines) {
 *                 int remaining = Math.max(0, 40 - lines);
 *                 ... draw "LEFT" label manually ...
 *             }
 *
 *         AFTER (OCP satisfied — closed for modification):
 *             for (String[] stat : mode.getSidePanelStats(lines, elapsed))
 *                 cy = drawStat(g2, stat[0], stat[1], ox, cy);
 *
 * 3. DRY — EXTRACT METHOD (Refactoring)
 * ─────────────────────────────────────────
 * WHERE : GameBoard.tryRotate(Runnable rotate, Runnable undo)
 * WHY   : rotatePieceClock() and rotatePieceCounter() were near-
 *         identical: both applied a rotate op, looped over the same
 *         five wall-kick offsets {0,-1,1,-2,2}, and reversed the
 *         rotation on failure — ~15 lines of logic duplicated verbatim.
 *         tryRotate() extracts the shared wall-kick scaffold; each
 *         caller passes only the rotation direction as a Runnable.
 *
 *         BEFORE (duplicated in both rotate methods):
 *             currentPiece.rotateClockwise();
 *             int[] offsets = { 0, -1, 1, -2, 2 };
 *             boolean placed = false;
 *             for (int dx : offsets) {
 *                 if (isValidPosition(..., pieceX + dx, pieceY)) {
 *                     pieceX += dx; placed = true; break;
 *                 }
 *             }
 *             if (!placed) currentPiece.rotateClockwise(); // undo
 *
 *         AFTER (one method, two callers in InputHandler):
 *             tryRotate(() -> currentPiece.rotateClockwise(),
 *                       () -> currentPiece.rotateClockwise());
 *
 * CATEGORY: Refactoring — Don't Repeat Yourself.
 *
 * 4. SINGLE RESPONSIBILITY PRINCIPLE (SOLID — S)
 * ─────────────────────────────────────────────────
 * WHERE : GameBoard.InputHandler (new inner class)
 * WHY   : GameBoard originally held a setupKeyBindings() method inline,
 *         mixing key-event handling directly into a class that also
 *         manages board state, timers, scoring, and rendering — four
 *         distinct responsibilities in one class.
 *         InputHandler owns exactly one responsibility: translate raw
 *         KeyEvents into GameBoard method calls. GameBoard delegates to
 *         it via addKeyListener(new InputHandler()), keeping concerns
 *         separate and making input logic independently readable.
 *
 *         OLD CODE (inline anonymous class inside GameBoard):
 *             private void setupKeyBindings() {
 *                 addKeyListener(new KeyAdapter() {
 *                     public void keyPressed(KeyEvent e) { ... }
 *                 });
 *             }
 *
 *         NEW CODE: named inner class, registered in constructor.
 *         Input logic lives in InputHandler; game logic stays in GameBoard.
 * CATEGORY: Structural — how responsibilities are distributed.
 *
 * 5. DEPENDENCY INVERSION PRINCIPLE (SOLID — D)
 * ─────────────────────────────────────────────────
 * WHERE : GameBoard — mode field typed as GameModes.GameMode (abstraction)
 * WHY   : All mode-specific behaviour (isGameOver, fixedLevel, showScore,
 *         shouldClearOnTopOut, getSidePanelStats, recordsHighScore) is
 *         accessed through the GameMode abstraction. No concrete subclass
 *         (StandardMode, TimeTrial, etc.) is ever referenced inside
 *         GameBoard. High-level policy (the game loop) does not depend on
 *         low-level detail (individual mode rules).
 * CATEGORY: Structural — direction of dependencies.
 *
 * ═══════════════════════════════════════════════════════════════════
 */
public class TetrisGame {

    // ── Board / layout constants ───────────────────────────────────────────────

    static final int BOARD_COLS   = 10;
    static final int BOARD_ROWS   = 20;
    static final int CELL_SIZE    = 24;
    static final int BOARD_WIDTH  = BOARD_COLS * CELL_SIZE;
    static final int BOARD_HEIGHT = BOARD_ROWS * CELL_SIZE;
    static final int SIDE_PANEL   = 180;
    static final int PADDING      = 16;

    /** Drop interval in ms at level 1; speeds up each level. */
    static final int BASE_DROP_MS = 800;

    // ── Entry Point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TetrisFrame frame = new TetrisFrame();
            frame.showStartScreen();
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TEMPLATE METHOD — HighScoreManager
    // ────────────────────────────────────
    // readCsvLines() defines the shared file-access skeleton:
    //   1. Check the file exists.
    //   2. Read and return all lines.
    //   3. Swallow I/O exceptions (graceful degradation — a score read
    //      failure should never crash the game).
    //
    // getHighScore() and saveIfHighScore() call readCsvLines() for the
    // skeleton and only supply the logic unique to their operation:
    // the search predicate and the write step respectively.
    //
    // Pattern  : TEMPLATE METHOD (Behavioral)
    // Principle: DRY — one file-access skeleton, zero duplication.
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Stores and retrieves high scores from "highscores.csv".
     * Format: one line per mode — MODE_NAME,SCORE
     * Example:
     *   Standard,14200
     *   40 Lines,30500
     *   Time Trial,8100
     * Zen mode is excluded (recordsHighScore() returns false).
     */
    public static class HighScoreManager {

        private static final String FILE_NAME = "highscores.csv";

        // ── TEMPLATE METHOD skeleton ─────────────────────────────────────────
        // Encapsulates all file I/O boilerplate shared by both public methods.
        // Returns an empty list on any error so callers never need to catch.
        private static List<String> readCsvLines() {
            try {
                File f = new File(FILE_NAME);
                if (!f.exists()) return List.of();
                return Files.readAllLines(f.toPath());
            } catch (Exception ignored) {
                return List.of();
            }
        }

        /**
         * Returns the stored high score for the given mode, or 0 if none.
         *
         * Uses readCsvLines() for the skeleton; only supplies the
         * line-search predicate unique to this operation.
         */
        public static int getHighScore(GameModes.GameMode mode) {
            for (String line : readCsvLines()) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2
                        && parts[0].trim().equalsIgnoreCase(mode.getModeName())) {
                    try { return Integer.parseInt(parts[1].trim()); }
                    catch (NumberFormatException ignored) { }
                }
            }
            return 0;
        }

        /**
         * Saves the score for the given mode if it is a new high score.
         * Rewrites the entire CSV preserving all other entries.
         * Only call this when mode.recordsHighScore() is true.
         *
         * Uses readCsvLines() for the skeleton; only supplies the
         * line-transform and write step unique to this operation.
         */
        public static void saveIfHighScore(GameModes.GameMode mode, int score) {
            if (score <= getHighScore(mode)) return;

            List<String> updated = new ArrayList<>();
            boolean found = false;

            for (String line : readCsvLines()) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2
                        && parts[0].trim().equalsIgnoreCase(mode.getModeName())) {
                    updated.add(mode.getModeName() + "," + score);
                    found = true;
                } else {
                    updated.add(line);
                }
            }

            if (!found) updated.add(mode.getModeName() + "," + score);

            try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
                for (String line : updated) pw.println(line);
            } catch (IOException ignored) { }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TetrisFrame — Window manager; delegates all UI to UserInterface.java
    // ─────────────────────────────────────────────────────────────────────────

    public static class TetrisFrame extends JFrame {

        TetrisFrame() {
            setTitle("TETRIS");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setResizable(false);
            setBackground(UserInterface.BG);
        }

        /** Shows the plain main menu (Start / High Scores / Exit). */
        public void showStartScreen() {
            switchTo(new UserInterface.MainMenuScreen(this));
        }

        /** Shows the mode selection screen (called by main menu "Start"). */
        public void showModeSelect() {
            switchTo(new UserInterface.ModeSelectScreen(this));
        }

        /** Shows the difficulty selection screen for the chosen mode. */
        public void showDifficultySelect(GameModes.GameMode mode) {
            switchTo(new UserInterface.DifficultyScreen(this, mode));
        }

        /** Shows the plain high score screen. */
        public void showHighScores() {
            switchTo(new UserInterface.HighScoreScreen(this));
        }

        /**
         * Starts the game with the selected mode and difficulty.
         * Called by DifficultyScreen when the player picks a difficulty.
         */
        public void startGame(GameModes.GameMode mode, GameModes.Difficulty difficulty) {
            GameBoard board = new GameBoard(this, mode, difficulty);
            switchTo(board);
            board.requestFocusInWindow();
            board.startGame();
            SoundEffects.startBackgroundMusic();
        }

        /**
         * Shows the game-over / result screen.
         * Called by GameBoard when isGameOver() is satisfied or the stack tops out.
         */
        public void showGameOver(String[][] stats,
                                 GameModes.GameMode mode,
                                 GameModes.Difficulty difficulty) {
            SoundEffects.stopBackgroundMusic();
            SoundEffects.onGameOver();
            switchTo(new UserInterface.GameOverScreen(this, stats, mode, difficulty));
        }

        private void switchTo(JPanel panel) {
            getContentPane().removeAll();
            setContentPane(panel);
            pack();
            setLocationRelativeTo(null);
            setVisible(true);
            revalidate();
            repaint();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GameBoard — Core game logic + rendering
    // ═════════════════════════════════════════════════════════════════════════

    static class GameBoard extends JPanel {

        // ── Board state ──────────────────────────────────────────────────────
        private final Color[][] board = new Color[BOARD_ROWS][BOARD_COLS];

        // ── Pieces ───────────────────────────────────────────────────────────
        private Tetromino currentPiece;
        private Tetromino nextPiece;
        private int pieceX, pieceY;

        // ── Game state ───────────────────────────────────────────────────────
        private int score    = 0;
        private int level    = 1;
        private int lines    = 0;
        private boolean paused   = false;
        private boolean gameOver = false;
        private long startTimeMs;

        // ── Mode & difficulty ─────────────────────────────────────────────────
        // DIP: both fields use the abstraction (GameMode / Difficulty), never
        // a concrete subclass. High-level game-loop logic never touches
        // StandardMode, TimeTrial, etc. directly.
        private final GameModes.GameMode   mode;
        private final GameModes.Difficulty difficulty;

        // ── Timers ───────────────────────────────────────────────────────────
        private Timer dropTimer;
        private Timer renderTimer;

        // ── Line-clear flash ─────────────────────────────────────────────────
        private final List<Integer> flashRows = new ArrayList<>();
        private int flashFrames = 0;

        private final TetrisFrame frame;

        // ── Colours (plain theme from UserInterface) ─────────────────────────
        private static final Color BOARD_BG   = UserInterface.BOARD_BG;
        private static final Color BORDER_COL = UserInterface.BORDER;
        private static final Color GRID_COLOR = new Color(180, 180, 180);

        GameBoard(TetrisFrame frame, GameModes.GameMode mode, GameModes.Difficulty difficulty) {
            this.frame      = frame;
            this.mode       = mode;
            this.difficulty = difficulty;

            setPreferredSize(new Dimension(
                    BOARD_WIDTH + SIDE_PANEL + PADDING * 3,
                    BOARD_HEIGHT + PADDING * 2));
            setBackground(Color.WHITE);
            setFocusable(true);

            // SRP: all key-binding logic is delegated to the InputHandler inner
            // class. GameBoard's constructor no longer mixes input concerns.
            addKeyListener(new InputHandler());
        }

        // ═════════════════════════════════════════════════════════════════════
        // SRP — InputHandler (inner class)
        // ──────────────────────────────────
        // GameBoard originally wired key events through an inline anonymous
        // KeyAdapter inside setupKeyBindings(), blending input-handling
        // directly into a class responsible for board state, timers, scoring,
        // and rendering — four distinct responsibilities in one place.
        //
        // InputHandler owns exactly one responsibility: translate raw
        // KeyEvents into GameBoard method calls. Moving it to a named inner
        // class makes the input contract explicit and independently readable.
        //
        // Pattern  : (none — pure SRP extraction)
        // Principle: SRP — one class, one reason to change.
        //
        // OLD CODE (anonymous class buried inside GameBoard):
        //     private void setupKeyBindings() {
        //         addKeyListener(new KeyAdapter() {
        //             public void keyPressed(KeyEvent e) {
        //                 if (gameOver) return;
        //                 switch (e.getKeyCode()) {
        //                     case KeyEvent.VK_UP -> { SoundEffects.onRotate(); rotatePieceClock(); }
        //                     ...
        //                 }
        //             }
        //         });
        //     }
        //
        // NEW CODE: named inner class; registered with one line in the
        // constructor. Input logic lives here; game logic stays in GameBoard.
        // ═════════════════════════════════════════════════════════════════════
        private class InputHandler extends KeyAdapter {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameOver) return;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT   -> { SoundEffects.onMove();     moveLeft();  }
                    case KeyEvent.VK_RIGHT  -> { SoundEffects.onMove();     moveRight(); }
                    case KeyEvent.VK_DOWN   -> { SoundEffects.onSoftDrop(); softDrop();  }
                    case KeyEvent.VK_SPACE  -> { SoundEffects.onHardDrop(); hardDrop();  }
                    case KeyEvent.VK_P      -> togglePause();
                    case KeyEvent.VK_ESCAPE -> frame.showStartScreen();

                    // DRY: both rotation keys delegate to tryRotate(), passing
                    // only the direction-specific operation as a Runnable.
                    // See tryRotate() for the full explanation.
                    case KeyEvent.VK_UP -> {
                        SoundEffects.onRotate();
                        tryRotate(
                            () -> currentPiece.rotateClockwise(),
                            () -> currentPiece.rotateCounterClockwise()
                        );
                    }
                    case KeyEvent.VK_C -> {
                        SoundEffects.onRotate();
                        tryRotate(
                            () -> currentPiece.rotateCounterClockwise(),
                            () -> currentPiece.rotateClockwise()
                        );
                    }
                }
            }
        }

        // ─── Game Lifecycle ──────────────────────────────────────────────────

        void startGame() {
            for (Color[] row : board)
                java.util.Arrays.fill(row, null);
            level    = difficulty.getStartingLevel();
            score    = 0;
            lines    = 0;
            paused   = false;
            gameOver = false;

            mode.onStart(level);
            startTimeMs = System.currentTimeMillis();

            nextPiece = TetrominoFactory.createRandom();
            spawnPiece();

            dropTimer = new Timer(getDropInterval(), e -> {
                if (!paused && !gameOver) step();
            });
            dropTimer.start();

            renderTimer = new Timer(16, e -> { // ~60 fps
                if (flashFrames > 0) {
                    flashFrames--;
                    if (flashFrames == 0) {
                        removeFlashRows();
                        flashRows.clear();
                    }
                }
                repaint();
            });
            renderTimer.start();

            requestFocusInWindow();
        }

        private int getDropInterval() {
            return Math.max(80, BASE_DROP_MS - (level - 1) * 70);
        }

        private void spawnPiece() {
            currentPiece = nextPiece;
            nextPiece    = TetrominoFactory.createRandom();
            pieceX       = BOARD_COLS / 2 - 2;
            pieceY       = 0;

            if (!isValidPosition(currentPiece.getShape(), pieceX, pieceY)) {
                // DIP: ask the mode how to handle a top-out, never inspect
                // the concrete subclass here.
                if (mode.shouldClearOnTopOut()) {
                    clearBoard(); // Zen: wipe and continue
                } else {
                    endGame();   // all other modes: end normally
                }
            }
        }

        private void clearBoard() {
            for (Color[] row : board)
                java.util.Arrays.fill(row, null);
        }

        private void endGame() {
            if (gameOver) return;
            gameOver = true;
            dropTimer.stop();
            renderTimer.stop();

            long elapsed = System.currentTimeMillis() - startTimeMs;

            if (mode.recordsHighScore()) {
                HighScoreManager.saveIfHighScore(mode, score);
                if (score >= HighScoreManager.getHighScore(mode))
                    SoundEffects.onHighScore();
            }

            String[][] stats = mode.getResultStats(score, lines, elapsed);
            SwingUtilities.invokeLater(() -> frame.showGameOver(stats, mode, difficulty));
        }

        // ─── Movement ────────────────────────────────────────────────────────

        private void moveLeft() {
            if (isValidPosition(currentPiece.getShape(), pieceX - 1, pieceY))
                pieceX--;
        }

        private void moveRight() {
            if (isValidPosition(currentPiece.getShape(), pieceX + 1, pieceY))
                pieceX++;
        }

        private void softDrop() {
            if (isValidPosition(currentPiece.getShape(), pieceX, pieceY + 1)) {
                pieceY++;
                score++;
            }
        }

        // ═════════════════════════════════════════════════════════════════════
        // DRY — tryRotate (Extract Method)
        // ──────────────────────────────────
        // The original code had two nearly-identical rotation methods,
        // rotatePieceClock() and rotatePieceCounter(), each containing the
        // full wall-kick loop duplicated verbatim (~15 lines each):
        //
        //   currentPiece.rotateClockwise();       // or Counter
        //   int[] offsets = { 0, -1, 1, -2, 2 };
        //   boolean placed = false;
        //   for (int dx : offsets) {
        //       if (isValidPosition(..., pieceX + dx, pieceY)) {
        //           pieceX += dx; placed = true; break;
        //       }
        //   }
        //   if (!placed) currentPiece.rotateClockwise(); // undo — or Counter
        //
        // tryRotate() extracts the shared wall-kick scaffold once.
        // Two Runnables are accepted: the forward rotation and its exact
        // inverse (used to undo on failure). InputHandler passes method
        // references so each key binding remains a clear one-liner.
        //
        // Pattern  : Extract Method (refactoring technique)
        // Principle: DRY — one wall-kick implementation, zero duplication.
        // ═════════════════════════════════════════════════════════════════════
        private void tryRotate(Runnable rotate, Runnable undo) {
            rotate.run();
            int[] offsets = { 0, -1, 1, -2, 2 };
            for (int dx : offsets) {
                if (isValidPosition(currentPiece.getShape(), pieceX + dx, pieceY)) {
                    pieceX += dx;
                    return; // wall-kick succeeded — keep the rotation
                }
            }
            undo.run(); // no valid position found — reverse the rotation
        }

        private void hardDrop() {
            int dropped = 0;
            while (isValidPosition(currentPiece.getShape(), pieceX, pieceY + 1)) {
                pieceY++;
                dropped++;
            }
            score += dropped * 2;
            lockPiece();
        }

        private void togglePause() {
            paused = !paused;
            SoundEffects.setBackgroundMusicPaused(paused);
            if (paused) {
                dropTimer.stop();
            } else {
                dropTimer.setDelay(getDropInterval());
                dropTimer.start();
            }
        }

        // ─── Game Step ───────────────────────────────────────────────────────

        private void step() {
            long elapsed = System.currentTimeMillis() - startTimeMs;
            // DIP: mode decides its own termination condition; GameBoard
            // never inspects concrete mode fields directly.
            if (mode.isGameOver(lines, elapsed)) {
                endGame();
                return;
            }

            if (flashFrames > 0) return; // waiting for flash animation to finish

            if (isValidPosition(currentPiece.getShape(), pieceX, pieceY + 1)) {
                pieceY++;
            } else {
                lockPiece();
            }
        }

        private void lockPiece() {
            SoundEffects.onPieceLock();
            int[][] shape = currentPiece.getShape();
            Color   color = currentPiece.getColor();
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    if (shape[r][c] == 1) {
                        int boardRow = pieceY + r;
                        int boardCol = pieceX + c;
                        if (boardRow >= 0 && boardRow < BOARD_ROWS)
                            board[boardRow][boardCol] = color;
                    }
                }
            }
            checkLines();
            if (flashRows.isEmpty()) spawnPiece();
        }

        private void checkLines() {
            flashRows.clear();
            for (int r = 0; r < BOARD_ROWS; r++) {
                boolean full = true;
                for (int c = 0; c < BOARD_COLS; c++) {
                    if (board[r][c] == null) { full = false; break; }
                }
                if (full) flashRows.add(r);
            }
            if (!flashRows.isEmpty()) {
                flashFrames = 12; // ~200 ms at 60 fps
                int cleared = flashRows.size();
                addScore(cleared);
                lines += cleared;

                // DIP: ask the mode whether the level is fixed; no subclass cast.
                if (!mode.fixedLevel()) {
                    level = difficulty.getStartingLevel() + lines / 10;
                    dropTimer.setDelay(getDropInterval());
                }

                if (cleared == 4) SoundEffects.onTetris();
                else              SoundEffects.onLineClear(cleared);
            }
        }

        private void addScore(int clearedLines) {
            int[] pts = { 0, 100, 300, 500, 800 };
            score += pts[Math.min(clearedLines, 4)] * level;
        }

        private void removeFlashRows() {
            for (int row : flashRows) {
                for (int r = row; r > 0; r--)
                    board[r] = board[r - 1].clone();
                board[0] = new Color[BOARD_COLS];
            }
            spawnPiece();
        }

        // ─── Collision Detection ─────────────────────────────────────────────

        private boolean isValidPosition(int[][] shape, int offX, int offY) {
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    if (shape[r][c] == 1) {
                        int bx = offX + c;
                        int by = offY + r;
                        if (bx < 0 || bx >= BOARD_COLS || by >= BOARD_ROWS) return false;
                        if (by >= 0 && board[by][bx] != null)               return false;
                    }
                }
            }
            return true;
        }

        // ─── Rendering ───────────────────────────────────────────────────────

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            int boardOffX = PADDING;
            int boardOffY = PADDING;

            drawBoard(g2, boardOffX, boardOffY);
            // Ghost piece intentionally removed
            drawCurrentPiece(g2, boardOffX, boardOffY);
            drawFlash(g2, boardOffX, boardOffY);
            drawBoardBorder(g2, boardOffX, boardOffY);
            drawSidePanel(g2, boardOffX + BOARD_WIDTH + PADDING, boardOffY);

            if (paused) drawPauseOverlay(g2);

            g2.dispose();
        }

        private void drawBoard(Graphics2D g2, int ox, int oy) {
            g2.setColor(BOARD_BG);
            g2.fillRect(ox, oy, BOARD_WIDTH, BOARD_HEIGHT);

            g2.setColor(GRID_COLOR);
            g2.setStroke(new BasicStroke(0.5f));
            for (int c = 0; c <= BOARD_COLS; c++)
                g2.drawLine(ox + c * CELL_SIZE, oy, ox + c * CELL_SIZE, oy + BOARD_HEIGHT);
            for (int r = 0; r <= BOARD_ROWS; r++)
                g2.drawLine(ox, oy + r * CELL_SIZE, ox + BOARD_WIDTH, oy + r * CELL_SIZE);

            for (int r = 0; r < BOARD_ROWS; r++)
                for (int c = 0; c < BOARD_COLS; c++)
                    if (board[r][c] != null && !flashRows.contains(r))
                        drawCell(g2, ox + c * CELL_SIZE, oy + r * CELL_SIZE, board[r][c]);
        }

        private void drawCurrentPiece(Graphics2D g2, int ox, int oy) {
            if (currentPiece == null) return;
            int[][] shape = currentPiece.getShape();
            for (int r = 0; r < 4; r++)
                for (int c = 0; c < 4; c++)
                    if (shape[r][c] == 1)
                        drawCell(g2,
                                ox + (pieceX + c) * CELL_SIZE,
                                oy + (pieceY + r) * CELL_SIZE,
                                currentPiece.getColor());
        }

        private void drawFlash(Graphics2D g2, int ox, int oy) {
            if (flashRows.isEmpty()) return;
            float alpha = (float) flashFrames / 8f;
            g2.setColor(new Color(1f, 1f, 1f, alpha * 0.85f));
            for (int row : flashRows)
                g2.fillRect(ox, oy + row * CELL_SIZE, BOARD_WIDTH, CELL_SIZE);
        }

        private void drawBoardBorder(Graphics2D g2, int ox, int oy) {
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(BORDER_COL);
            g2.drawRect(ox, oy, BOARD_WIDTH, BOARD_HEIGHT);
        }

        private void drawCell(Graphics2D g2, int x, int y, Color color) {
            g2.setColor(color);
            g2.fillRect(x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2);
            g2.setColor(color.darker());
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2);
        }

        // ═════════════════════════════════════════════════════════════════════
        // OCP — drawSidePanel → mode.getSidePanelStats()
        // ──────────────────────────────────────────────
        // The original method hard-coded mode-specific rendering with two
        // instanceof checks. Adding any new mode required editing this method.
        //
        // Common stats (score, level, lines) are drawn directly as before.
        // Mode-specific extras are obtained by calling
        // mode.getSidePanelStats(lines, elapsed), which each GameMode
        // implements to return its own String[][] of { label, value } pairs.
        // This method iterates them generically via the drawStat() helper.
        //
        // Pattern  : (mirrors Factory Method delegation used in UserInterface)
        // Principle: OCP — closed for modification, open for extension.
        //            DIP — depends on the GameMode abstraction, not subclasses.
        // ═════════════════════════════════════════════════════════════════════
        private void drawSidePanel(Graphics2D g2, int ox, int oy) {
            // Grey sidebar background with grey border
            g2.setColor(BOARD_BG);
            g2.fillRect(ox, oy, SIDE_PANEL, BOARD_HEIGHT);
            g2.setColor(BORDER_COL);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(ox, oy, SIDE_PANEL, BOARD_HEIGHT);

            int cy = oy + 20;

            // Mode name
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
            g2.drawString(mode.getModeName().toUpperCase(), ox + 10, cy);
            cy += 20;

            // Next piece preview
            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("NEXT", ox + 10, cy);
            cy += 5;
            cy = drawNextPiece(g2, ox, cy);
            cy += 10;

            // Score — hidden for ZenMode via mode.showScore() (DIP: no cast needed)
            if (mode.showScore())
                cy = drawStat(g2, "SCORE", String.valueOf(score), ox, cy);

            // Level — always shown; fixed or progressing depending on mode
            cy = drawStat(g2, "LEVEL", String.valueOf(level), ox, cy);

            // Lines — always shown
            cy = drawStat(g2, "LINES", String.valueOf(lines), ox, cy);

            // ── OCP: mode-specific extra stats ────────────────────────────────
            // Each GameMode returns its own label/value pairs via
            // getSidePanelStats(). No instanceof checks. Adding a new mode
            // with unique stats (e.g. a countdown or a target) requires
            // zero changes to this method — only a getSidePanelStats()
            // implementation in the new GameMode subclass.
            //
            // Examples of what modes return:
            //   TimeTrial  → { {"TIME", "1:45"} }
            //   FourtyLines → { {"LEFT", "17"} }
            //   StandardMode / ZenMode → {} (empty — no extras)
            long elapsed = System.currentTimeMillis() - startTimeMs;
            for (String[] stat : mode.getSidePanelStats(lines, elapsed))
                cy = drawStat(g2, stat[0], stat[1], ox, cy);

            // Controls hint — anchored near the bottom of the side panel
            cy = Math.max(cy, oy + BOARD_HEIGHT - 120);
            g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
            g2.setColor(Color.GRAY);
            String[] hints = {
                "← → Move", "↑ Rotate", "↓ Soft drop",
                "SPC Hard drop", "P Pause", "ESC Menu"
            };
            for (String h : hints) {
                g2.drawString(h, ox + 8, cy);
                cy += 14;
            }
        }

        /**
         * Draws a two-line stat block: small label above, bold value below.
         * Returns the updated Y cursor so callers can stack blocks vertically.
         *
         * DRY: replaces the 5-line label+value drawing pattern that was
         * copy-pasted separately for SCORE, LEVEL, LINES, TIME, and LEFT.
         */
        private int drawStat(Graphics2D g2, String label, String value, int ox, int cy) {
            g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(label, ox + 10, cy);
            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
            g2.setColor(Color.BLACK);
            g2.drawString(value, ox + 10, cy + 16);
            return cy + 30;
        }

        private int drawNextPiece(Graphics2D g2, int ox, int cy) {
            if (nextPiece == null) return cy + 80;
            int[][] shape = nextPiece.getPreviewShape();
            int cellSz    = 20;
            int startX    = ox + (SIDE_PANEL - 4 * cellSz) / 2;
            for (int r = 0; r < 4; r++)
                for (int c = 0; c < 4; c++)
                    if (shape[r][c] == 1)
                        drawCell(g2, startX + c * cellSz, cy + r * cellSz, nextPiece.getColor());
            return cy + 4 * cellSz + 8;
        }

        private void drawPauseOverlay(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 130));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 32));
            g2.setColor(Color.WHITE);
            String txt = "PAUSED";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(txt, (getWidth() - fm.stringWidth(txt)) / 2, getHeight() / 2);
            g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
            g2.setColor(Color.LIGHT_GRAY);
            String sub = "Press P to resume";
            g2.drawString(sub, (getWidth() - g2.getFontMetrics().stringWidth(sub)) / 2,
                    getHeight() / 2 + 24);
        }
    }
}