// package com.mycompany.tetrisgame;

import java.awt.*;          //.awt numerous GUI elements
import java.awt.event.*;
import java.io.*;           //.io and .nio needed to read-write files (highscores)
import java.nio.file.*;
import java.util.ArrayList; //.util tracks line clear animations
import java.util.List;
import javax.swing.*;       //platform independant GUI elements, buttons etc.

/**
 * TetrisGame — Main class containing:
 *  - TetrisFrame     : window/screen manager (delegates UI to UserInterface.java)
 *  - GameBoard       : core game logic + rendering
 *  - HighScoreManager: reads/writes one high score per difficulty to CSV
 *  - main()          : entry point
 *
 * UI screens live in UserInterface.java.
 * Game modes and difficulties live in GameModes.java.
 * Sound effect stubs live in SoundEffects.java.
 */
public class TetrisGame {

    // ─── Board / layout constants ─────────────────────────────────────────────

    static final int BOARD_COLS   = 10;
    static final int BOARD_ROWS   = 20;
    static final int CELL_SIZE    = 24;
    static final int BOARD_WIDTH  = BOARD_COLS * CELL_SIZE;
    static final int BOARD_HEIGHT = BOARD_ROWS * CELL_SIZE;
    static final int SIDE_PANEL   = 180;
    static final int PADDING      = 16;

    /** Drop interval in ms at level 1; speeds up each level. */
    static final int BASE_DROP_MS = 800;

    // ─── Entry Point ──────────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TetrisFrame frame = new TetrisFrame();
            frame.showStartScreen();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HighScoreManager — CSV-backed, one score per difficulty
    // ─────────────────────────────────────────────────────────────────────────

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

        /**
         * Returns the stored high score for the given mode, or 0 if none.
         */
        public static int getHighScore(GameModes.GameMode mode) {
            try {
                File f = new File(FILE_NAME);
                if (!f.exists()) return 0;
                for (String line : Files.readAllLines(f.toPath())) {
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2 && parts[0].trim().equalsIgnoreCase(mode.getModeName())) {
                        return Integer.parseInt(parts[1].trim());
                    }
                }
            } catch (Exception ignored) {}
            return 0;
        }

        /**
         * Saves the score for the given mode if it is a new high score.
         * Rewrites the entire CSV preserving all other entries.
         * Only call this when mode.recordsHighScore() is true.
         */
        public static void saveIfHighScore(GameModes.GameMode mode, int score) {
            if (score <= getHighScore(mode)) return;

            List<String> lines = new ArrayList<>();
            boolean found = false;
            try {
                File f = new File(FILE_NAME);
                if (f.exists()) {
                    for (String line : Files.readAllLines(f.toPath())) {
                        String[] parts = line.split(",", 2);
                        if (parts.length == 2 && parts[0].trim().equalsIgnoreCase(mode.getModeName())) {
                            lines.add(mode.getModeName() + "," + score);
                            found = true;
                        } else {
                            lines.add(line);
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (!found) lines.add(mode.getModeName() + "," + score);

            try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
                for (String line : lines) pw.println(line);
            } catch (IOException ignored) {}
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
        public void showGameOver(String[][] stats, GameModes.GameMode mode, GameModes.Difficulty difficulty) {
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

    // ─────────────────────────────────────────────────────────────────────────
    // GameBoard — Core game logic + rendering
    // ─────────────────────────────────────────────────────────────────────────

    static class GameBoard extends JPanel {

        // ── Board state ──────────────────────────────────────────────────────
        private final Color[][] board = new Color[BOARD_ROWS][BOARD_COLS];

        // ── Pieces ───────────────────────────────────────────────────────────
        private Tetromino currentPiece;
        private Tetromino nextPiece;
        private int pieceX, pieceY;

        // ── Game state ───────────────────────────────────────────────────────
        private int     score    = 0;
        private int     level    = 1;
        private int     lines    = 0;
        private boolean paused   = false;
        private boolean gameOver = false;
        private long    startTimeMs;

        // ── Mode & difficulty ─────────────────────────────────────────────────
        private final GameModes.GameMode   mode;
        private final GameModes.Difficulty difficulty;

        // ── Timers ───────────────────────────────────────────────────────────
        private Timer dropTimer;
        private Timer renderTimer;

        // ── Line-clear flash ─────────────────────────────────────────────────
        private final List<Integer> flashRows   = new ArrayList<>();
        private       int           flashFrames = 0;

        private final TetrisFrame frame;

        // ── Colours (plain theme from UserInterface) ─────────────────────────
        private static final Color BOARD_BG    = UserInterface.BOARD_BG;
        private static final Color BORDER_COL  = UserInterface.BORDER;
        private static final Color GRID_COLOR  = new Color(180, 180, 180);

        GameBoard(TetrisFrame frame, GameModes.GameMode mode, GameModes.Difficulty difficulty) {
            this.frame      = frame;
            this.mode       = mode;
            this.difficulty = difficulty;

            int w = BOARD_WIDTH + SIDE_PANEL + PADDING * 3;
            int h = BOARD_HEIGHT + PADDING * 2;
            setPreferredSize(new Dimension(w, h));
            setBackground(Color.WHITE);
            setFocusable(true);
            setupKeyBindings();
        }

        // ─── Key Bindings ────────────────────────────────────────────────────

        private void setupKeyBindings() {
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (gameOver) return;
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT  -> { SoundEffects.onMove();     moveLeft();     }
                        case KeyEvent.VK_RIGHT -> { SoundEffects.onMove();     moveRight();    }
                        case KeyEvent.VK_DOWN  -> { SoundEffects.onSoftDrop(); softDrop();     }
                        case KeyEvent.VK_UP    -> { SoundEffects.onRotate();   rotatePieceClock();  }
                        case KeyEvent.VK_SPACE -> { SoundEffects.onHardDrop(); hardDrop();     }
                        case KeyEvent.VK_C     -> { SoundEffects.onRotate();   rotatePieceCounter();  }
                        case KeyEvent.VK_P     -> togglePause();
                        case KeyEvent.VK_ESCAPE -> frame.showStartScreen();
                    }
                }
            });
        }

        // ─── Game Lifecycle ──────────────────────────────────────────────────

        void startGame() {
            for (Color[] row : board) java.util.Arrays.fill(row, null);
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

            renderTimer = new Timer(16, e -> {         // ~60 fps
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

        /*private void spawnPiece() {
            currentPiece = nextPiece;
            nextPiece    = TetrominoFactory.createRandom();
            pieceX       = BOARD_COLS / 2 - 2;
            pieceY       = 0;

            if (!isValidPosition(currentPiece.getShape(), pieceX, pieceY)) {
                // Stack topped out — end the game regardless of mode
                endGame();
            }
        }*/
        
        private void spawnPiece() {
        currentPiece = nextPiece;
        nextPiece    = TetrominoFactory.createRandom();
        pieceX       = BOARD_COLS / 2 - 2;
        pieceY       = 0;

            if (!isValidPosition(currentPiece.getShape(), pieceX, pieceY)) {
                if (mode.shouldClearOnTopOut()) {   // ask the mode what to do
                clearBoard();                   // zen: wipe and continue
            } else {
                endGame();                      // all other modes: end normally
                }
            }
        }
        
        private void clearBoard() {
            for (Color[] row : board) {
            java.util.Arrays.fill(row, null);  // null = empty cell
            }
        // piece and state are already set by spawnPiece() above,
        // so play continues immediately with no further changes needed
        }

        private void endGame() {
            if (gameOver) return;
            gameOver = true;
            dropTimer.stop();
            renderTimer.stop();

            long elapsed = System.currentTimeMillis() - startTimeMs;

            // Save high score if this mode supports it
            if (mode.recordsHighScore()) {
                HighScoreManager.saveIfHighScore(mode, score);
                if (score >= HighScoreManager.getHighScore(mode)) {
                    SoundEffects.onHighScore();
                }
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

        private void rotatePieceClock() {
            currentPiece.rotateClockwise();
            int[] offsets = {0, -1, 1, -2, 2};
            boolean placed = false;
            for (int dx : offsets) {
                if (isValidPosition(currentPiece.getShape(), pieceX + dx, pieceY)) {
                    pieceX += dx;
                    placed = true;
                    break;
                }
            }
            if (!placed) currentPiece.rotateClockwise();
        }
        
        
        private void rotatePieceCounter() {
            currentPiece.rotateCounterClockwise();
            int[] offsets = {0, -1, 1, -2, 2};
            boolean placed = false;
            for (int dx : offsets) {
                if (isValidPosition(currentPiece.getShape(), pieceX + dx, pieceY)) {
                    pieceX += dx;
                    placed = true;
                    break;
                }
            }
            if (!placed) currentPiece.rotateCounterClockwise();
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
            if (paused) dropTimer.stop();
            else { dropTimer.setDelay(getDropInterval()); dropTimer.start(); }
        }

        // ─── Game Step ───────────────────────────────────────────────────────

        private void step() {
            // Check mode-based game-over condition each tick
            long elapsed = System.currentTimeMillis() - startTimeMs;
            if (mode.isGameOver(lines, elapsed)) {
                endGame();
                return;
            }

            if (flashFrames > 0) return; // waiting for flash to finish

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
                flashFrames = 12;  // ~200 ms at 60 fps
                int cleared = flashRows.size();
                addScore(cleared);
                lines += cleared;

                // Level up only if mode doesn't fix the level
                if (!mode.fixedLevel()) {
                    level = difficulty.getStartingLevel() + lines / 10;
                    dropTimer.setDelay(getDropInterval());
                }

                // Sound
                if (cleared == 4) SoundEffects.onTetris();
                else              SoundEffects.onLineClear(cleared);
            }
        }

        private void addScore(int clearedLines) {
            int[] pts = {0, 100, 300, 500, 800};
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
                        if (by >= 0 && board[by][bx] != null) return false;
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
            // Grey board background
            g2.setColor(BOARD_BG);
            g2.fillRect(ox, oy, BOARD_WIDTH, BOARD_HEIGHT);

            // Light grid lines
            g2.setColor(GRID_COLOR);
            g2.setStroke(new BasicStroke(0.5f));
            for (int c = 0; c <= BOARD_COLS; c++)
                g2.drawLine(ox + c * CELL_SIZE, oy, ox + c * CELL_SIZE, oy + BOARD_HEIGHT);
            for (int r = 0; r <= BOARD_ROWS; r++)
                g2.drawLine(ox, oy + r * CELL_SIZE, ox + BOARD_WIDTH, oy + r * CELL_SIZE);

            // Placed cells
            for (int r = 0; r < BOARD_ROWS; r++) {
                for (int c = 0; c < BOARD_COLS; c++) {
                    if (board[r][c] != null && !flashRows.contains(r)) {
                        drawCell(g2, ox + c * CELL_SIZE, oy + r * CELL_SIZE, board[r][c]);
                    }
                }
            }
        }

        private void drawCurrentPiece(Graphics2D g2, int ox, int oy) {
            if (currentPiece == null) return;
            int[][] shape = currentPiece.getShape();
            for (int r = 0; r < 4; r++)
                for (int c = 0; c < 4; c++)
                    if (shape[r][c] == 1)
                        drawCell(g2, ox + (pieceX + c) * CELL_SIZE,
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
            String modeName = mode.getModeName().toUpperCase();
            g2.drawString(modeName, ox + 10, cy);
            cy += 20;

            // Next piece
            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("NEXT", ox + 10, cy);
            cy += 5;
            cy = drawNextPiece(g2, ox, cy);
            cy += 10;

            // Score (hidden for ZenMode)
            if (mode.showScore()) {
                g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("SCORE", ox + 10, cy);
                cy += 2;
                g2.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
                g2.setColor(Color.BLACK);
                g2.drawString(String.valueOf(score), ox + 10, cy + 16);
                cy += 30;
            }

            // Level (show unless Zen fixed-level)
            g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("LEVEL", ox + 10, cy);
            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
            g2.setColor(Color.BLACK);
            g2.drawString(String.valueOf(level), ox + 10, cy + 16);
            cy += 30;

            // Lines
            g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("LINES", ox + 10, cy);
            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
            g2.setColor(Color.BLACK);
            g2.drawString(String.valueOf(lines), ox + 10, cy + 16);
            cy += 30;

            // Time Trial: show countdown
            if (mode instanceof GameModes.TimeTrial tt) {
                long elapsed  = System.currentTimeMillis() - startTimeMs;
                long remaining = tt.getRemainingMs(elapsed);
                long sec      = remaining / 1000;
                String timeStr = String.format("%d:%02d", sec / 60, sec % 60);
                g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("TIME", ox + 10, cy);
                g2.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
                g2.setColor(Color.BLACK);
                g2.drawString(timeStr, ox + 10, cy + 16);
                cy += 30;
            }

            // 40-Lines: show target remaining
            if (mode instanceof GameModes.FourtyLines) {
                int remaining = Math.max(0, 40 - lines);
                g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("LEFT", ox + 10, cy);
                g2.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
                g2.setColor(Color.BLACK);
                g2.drawString(String.valueOf(remaining), ox + 10, cy + 16);
                cy += 30;
            }

            // Controls hint
            cy = Math.max(cy, oy + BOARD_HEIGHT - 120);
            g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
            g2.setColor(Color.GRAY);
            String[] hints = {"← → Move", "↑ Rotate", "↓ Soft drop", "SPC Hard drop", "P Pause", "ESC Menu"};
            for (String h : hints) {
                g2.drawString(h, ox + 8, cy);
                cy += 14;
            }
        }

        private int drawNextPiece(Graphics2D g2, int ox, int cy) {
            if (nextPiece == null) return cy + 80;
            int[][] shape = nextPiece.getPreviewShape();
            int cellSz = 20;
            int startX = ox + (SIDE_PANEL - 4 * cellSz) / 2;
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    if (shape[r][c] == 1) {
                        drawCell(g2, startX + c * cellSz, cy + r * cellSz, nextPiece.getColor());
                    }
                }
            }
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
