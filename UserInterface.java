import java.awt.*;
import javax.swing.*;


/**
 * UserInterface.java — All Swing screens for Tetris.
 *
 * Screens managed:
 *   - MainMenuScreen   : Start / High Scores / Exit
 *   - ModeSelectScreen : radio buttons for game mode + Back
 *   - DifficultyScreen : three difficulty buttons + Back
 *   - HighScoreScreen  : plain display of one score per difficulty
 *   - GameOverScreen   : shows result stats, Play Again / Main Menu
 *
 * HOW TO WIRE IN:
 *   TetrisFrame (in TetrisGame.java) calls UserInterface.show*() methods.
 *   No other file needs to be edited to use this class.
 *
 * STYLE CONTRACT:
 *   - White background, plain system/default font everywhere.
 *   - Grey border around the game board and sidebar (drawn in GameBoard).
 *   - No ghost piece (removed from GameBoard).
 *   - No custom fonts, no animations, no decorative graphics.
 */
public class UserInterface {

    // ── Shared plain-UI colours ───────────────────────────────────────────────
    static final Color BG       = Color.WHITE;
    static final Color BORDER   = Color.GRAY;
    static final Color BOARD_BG = Color.LIGHT_GRAY;

    // ─────────────────────────────────────────────────────────────────────────
    // MainMenuScreen
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The opening screen of the game.
     * Contains three buttons: Start, High Scores, Exit.
     * On "Start" → navigates to ModeSelectScreen.
     */
    public static class MainMenuScreen extends JPanel {

        private final TetrisGame.TetrisFrame frame;

        public MainMenuScreen(TetrisGame.TetrisFrame frame) {
            this.frame = frame;
            setBackground(BG);
            setPreferredSize(new Dimension(
                TetrisGame.BOARD_WIDTH + TetrisGame.SIDE_PANEL + TetrisGame.PADDING * 3,
                TetrisGame.BOARD_HEIGHT + TetrisGame.PADDING * 2
            ));
            setLayout(new GridBagLayout());

            JPanel inner = new JPanel();
            inner.setBackground(BG);
            inner.setLayout(new GridLayout(4, 1, 0, 12));

            JLabel title = new JLabel("TETRIS", SwingConstants.CENTER);
            title.setFont(new Font(Font.DIALOG, Font.BOLD, 36));
            inner.add(title);

            JButton startBtn = plainButton("Start");
            startBtn.addActionListener(e -> {
                SoundEffects.onMenuClick();
                frame.showModeSelect();
            });

            JButton highBtn = plainButton("High Scores");
            highBtn.addActionListener(e -> {
                SoundEffects.onMenuClick();
                frame.showHighScores();
            });

            JButton exitBtn = plainButton("Exit");
            exitBtn.addActionListener(e -> {
                SoundEffects.onMenuClick();
                System.exit(0);
            });

            inner.add(startBtn);
            inner.add(highBtn);
            inner.add(exitBtn);
            add(inner);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ModeSelectScreen
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Mode selection screen.
     * Shows a radio button for each GameMode.
     * "Next" → DifficultyScreen  |  "Back" → MainMenuScreen.
     */
    public static class ModeSelectScreen extends JPanel {

        private final TetrisGame.TetrisFrame frame;
        private final ButtonGroup group = new ButtonGroup();

        // Mode radio buttons — order must match modes array
        private final GameModes.GameMode[] modes = {
            new GameModes.StandardMode(),
            new GameModes.FourtyLines(),
            new GameModes.TimeTrial(),
            new GameModes.ZenMode()
        };

        public ModeSelectScreen(TetrisGame.TetrisFrame frame) {
            this.frame = frame;
            setBackground(BG);
            setPreferredSize(new Dimension(
                TetrisGame.BOARD_WIDTH + TetrisGame.SIDE_PANEL + TetrisGame.PADDING * 3,
                TetrisGame.BOARD_HEIGHT + TetrisGame.PADDING * 2
            ));
            setLayout(new GridBagLayout());

            JPanel inner = new JPanel();
            inner.setBackground(BG);
            inner.setLayout(new GridLayout(0, 1, 0, 8));

            JLabel heading = new JLabel("Select Mode", SwingConstants.CENTER);
            heading.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
            inner.add(heading);

            JRadioButton[] radios = new JRadioButton[modes.length];
            for (int i = 0; i < modes.length; i++) {
                JRadioButton rb = new JRadioButton(modes[i].getModeName());
                rb.setBackground(BG);
                if (i == 0) rb.setSelected(true);
                group.add(rb);
                inner.add(rb);
                radios[i] = rb;
            }

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
            buttons.setBackground(BG);

            JButton nextBtn = plainButton("Next");
            nextBtn.addActionListener(e -> {
                SoundEffects.onMenuClick();
                GameModes.GameMode selected = modes[0];
                for (int i = 0; i < radios.length; i++) {
                    if (radios[i].isSelected()) { selected = modes[i]; break; }
                }
                frame.showDifficultySelect(selected);
            });

            JButton backBtn = plainButton("Back");
            backBtn.addActionListener(e -> {
                SoundEffects.onMenuBack();
                frame.showStartScreen();
            });

            buttons.add(backBtn);
            buttons.add(nextBtn);
            inner.add(buttons);

            add(inner);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DifficultyScreen
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Difficulty selection screen.
     * Shows Slow / Medium / Fast buttons plus a Back button.
     * Selecting a difficulty starts the game with the chosen mode + difficulty.
     */
    public static class DifficultyScreen extends JPanel {

        private final TetrisGame.TetrisFrame frame;
        private final GameModes.GameMode mode;

        public DifficultyScreen(TetrisGame.TetrisFrame frame, GameModes.GameMode mode) {
            this.frame = frame;
            this.mode  = mode;
            setBackground(BG);
            setPreferredSize(new Dimension(
                TetrisGame.BOARD_WIDTH + TetrisGame.SIDE_PANEL + TetrisGame.PADDING * 3,
                TetrisGame.BOARD_HEIGHT + TetrisGame.PADDING * 2
            ));
            setLayout(new GridBagLayout());

            JPanel inner = new JPanel();
            inner.setBackground(BG);
            inner.setLayout(new GridLayout(0, 1, 0, 12));

            JLabel heading = new JLabel("Select Difficulty", SwingConstants.CENTER);
            heading.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
            inner.add(heading);

            for (GameModes.Difficulty diff : GameModes.Difficulty.values()) {
                JButton btn = plainButton(diff.getDisplayName());
                btn.addActionListener(e -> {
                    SoundEffects.onMenuClick();
                    frame.startGame(mode, diff);
                });
                inner.add(btn);
            }

            JButton backBtn = plainButton("Back");
            backBtn.addActionListener(e -> {
                SoundEffects.onMenuBack();
                frame.showModeSelect();
            });
            inner.add(backBtn);

            add(inner);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HighScoreScreen
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Plain high score display.
     * Shows one high score per scoring mode (Standard, 40 Lines, Time Trial).
     * Zen mode is excluded as it does not record scores.
     * "Back" returns to the main menu.
     */
    public static class HighScoreScreen extends JPanel {

        private final TetrisGame.TetrisFrame frame;

        // All modes that record a high score
        private static final GameModes.GameMode[] SCORED_MODES = {
            new GameModes.StandardMode(),
            new GameModes.FourtyLines(),
            new GameModes.TimeTrial()
        };

        public HighScoreScreen(TetrisGame.TetrisFrame frame) {
            this.frame = frame;
            setBackground(BG);
            setPreferredSize(new Dimension(
                TetrisGame.BOARD_WIDTH + TetrisGame.SIDE_PANEL + TetrisGame.PADDING * 3,
                TetrisGame.BOARD_HEIGHT + TetrisGame.PADDING * 2
            ));
            setLayout(new GridBagLayout());

            JPanel inner = new JPanel();
            inner.setBackground(BG);
            inner.setLayout(new GridLayout(0, 1, 0, 10));

            JLabel heading = new JLabel("High Scores", SwingConstants.CENTER);
            heading.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
            inner.add(heading);

            for (GameModes.GameMode mode : SCORED_MODES) {
                int hs = TetrisGame.HighScoreManager.getHighScore(mode);
                String text = mode.getModeName() + ":  " + (hs > 0 ? hs : "--");
                JLabel lbl = new JLabel(text, SwingConstants.CENTER);
                lbl.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
                inner.add(lbl);
            }

            JButton backBtn = plainButton("Back");
            backBtn.addActionListener(e -> {
                SoundEffects.onMenuBack();
                frame.showStartScreen();
            });
            inner.add(backBtn);

            add(inner);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GameOverScreen
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Shown after a game ends.
     * Displays mode-specific result stats (provided by GameMode.getResultStats()).
     * Offers "Play Again" (same mode + difficulty) and "Main Menu".
     */
    public static class GameOverScreen extends JPanel {

        private final TetrisGame.TetrisFrame frame;

        public GameOverScreen(TetrisGame.TetrisFrame frame,
                              String[][] stats,
                              GameModes.GameMode mode,
                              GameModes.Difficulty difficulty) {
            this.frame = frame;
            setBackground(BG);
            setPreferredSize(new Dimension(
                TetrisGame.BOARD_WIDTH + TetrisGame.SIDE_PANEL + TetrisGame.PADDING * 3,
                TetrisGame.BOARD_HEIGHT + TetrisGame.PADDING * 2
            ));
            setLayout(new GridBagLayout());

            JPanel inner = new JPanel();
            inner.setBackground(BG);
            inner.setLayout(new GridLayout(0, 1, 0, 10));

            JLabel heading = new JLabel("Game Over", SwingConstants.CENTER);
            heading.setFont(new Font(Font.DIALOG, Font.BOLD, 28));
            inner.add(heading);

            // Result stats from the game mode
            for (String[] row : stats) {
                JLabel lbl = new JLabel(row[0] + ":  " + row[1], SwingConstants.CENTER);
                lbl.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
                inner.add(lbl);
            }

            JButton replayBtn = plainButton("Play Again");
            replayBtn.addActionListener(e -> {
                SoundEffects.onMenuClick();
                // Re-instantiate the mode so state is fresh
                GameModes.GameMode freshMode = freshMode(mode);
                frame.startGame(freshMode, difficulty);
            });

            JButton menuBtn = plainButton("Main Menu");
            menuBtn.addActionListener(e -> {
                SoundEffects.onMenuClick();
                frame.showStartScreen();
            });

            inner.add(replayBtn);
            inner.add(menuBtn);

            add(inner);
        }

        /** Create a fresh instance of the same mode type. */
        private GameModes.GameMode freshMode(GameModes.GameMode mode) {
            if (mode instanceof GameModes.StandardMode) return new GameModes.StandardMode();
            if (mode instanceof GameModes.FourtyLines)  return new GameModes.FourtyLines();
            if (mode instanceof GameModes.TimeTrial)    return new GameModes.TimeTrial();
            if (mode instanceof GameModes.ZenMode)      return new GameModes.ZenMode();
            return new GameModes.StandardMode();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared helper
    // ─────────────────────────────────────────────────────────────────────────

    /** Creates a plain, unstyled JButton with a sensible preferred size. */
    public static JButton plainButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(160, 36));
        return btn;
    }
}
