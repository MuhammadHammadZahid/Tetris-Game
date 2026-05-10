import java.awt.*;
import javax.swing.*;

/**
 * UserInterface.java — All Swing screens for Tetris.
 *
 * ═══════════════════════════════════════════════════════════════════
 * DESIGN PATTERNS & PRINCIPLES APPLIED
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. FACTORY METHOD PATTERN (Creational)
 * ─────────────────────────────────────
 * WHERE : ScreenFactory (inner class at bottom)
 * WHY : Screen creation was scattered — ModeSelectScreen hard-coded
 * all 4 mode instances inline, and GameOverScreen used a
 * private freshMode() method with a brittle instanceof chain:
 *
 * if (mode instanceof StandardMode) return new StandardMode();
 * if (mode instanceof FourtyLines) return new FourtyLines();
 * ...
 *
 * This violated OCP: adding a new GameMode required editing
 * GameOverScreen. The Factory delegates creation to GameMode
 * itself via mode.createFresh(), so no screen ever needs to
 * know the concrete subclass.
 * CATEGORY: Creational — deals with object instantiation.
 *
 * 2. TEMPLATE METHOD PATTERN (Behavioral)
 * ──────────────────────────────────────
 * WHERE : BaseScreen (abstract inner class)
 * WHY : Every screen repeated the same boilerplate:
 * setBackground(BG);
 * setPreferredSize(new Dimension(...));
 * setLayout(new GridBagLayout());
 * JPanel inner = new JPanel(); ...
 * This is a textbook Template Method situation. The abstract
 * BaseScreen defines the skeleton (setupUI → buildContent),
 * and each concrete screen only overrides buildContent() to
 * supply its unique widgets. Eliminates ~20 lines of duplicate
 * code per screen.
 * CATEGORY: Behavioral — defines how objects carry out their work.
 *
 * 3. SINGLE RESPONSIBILITY PRINCIPLE (SOLID — S)
 * ──────────────────────────────────────────────
 * WHERE : ScreenFactory extracted from GameOverScreen
 * WHY : GameOverScreen had two responsibilities: (a) display the
 * game-over UI, and (b) decide how to re-create a GameMode.
 * ScreenFactory now owns responsibility (b) exclusively.
 *
 * 4. OPEN / CLOSED PRINCIPLE (SOLID — O)
 * ──────────────────────────────────────
 * WHERE : ScreenFactory.createFreshMode() + GameMode.createFresh()
 * WHY : The old instanceof chain meant every new GameMode required
 * editing GameOverScreen (open for modification — bad).
 * Now GameMode subclasses implement createFresh() themselves,
 * so the UI is closed for modification but open for extension.
 *
 * 5. DEPENDENCY INVERSION PRINCIPLE (SOLID — D)
 * ─────────────────────────────────────────────
 * WHERE : BaseScreen depends on TetrisGame.TetrisFrame (abstraction),
 * not on any concrete screen type.
 * WHY : Screens never reference each other directly. All navigation
 * goes through the TetrisFrame interface, keeping screens
 * loosely coupled.
 *
 * 6. INTERFACE SEGREGATION PRINCIPLE (SOLID — I)
 * ────────────────────────────────────────────
 * WHERE : ButtonFactory (helper interface) is minimal — one method.
 * WHY : The original plainButton() was a static method on the outer
 * class mixed in with screen classes. Extracting it to a
 * focused helper makes the contract explicit and testable.
 *
 * NOTE ON BaseScreen USAGE
 * ─────────────────────────
 * BaseScreen's constructor calls buildContent() via setupUI(). Because
 * Java requires super() as the first statement, subclass instance fields
 * are NOT yet assigned when buildContent() runs.
 *
 * Screens with no fields beyond 'frame' (MainMenuScreen, HighScoreScreen)
 * safely extend BaseScreen.
 *
 * Screens that need additional fields (ModeSelectScreen, DifficultyScreen,
 * GameOverScreen) inline the same setup in their own constructors so all
 * fields are assigned before any UI is built.
 *
 * ═══════════════════════════════════════════════════════════════════
 */
public class UserInterface {

    // ── Shared plain-UI colours ───────────────────────────────────────────────
    static final Color BG       = Color.WHITE;
    static final Color BORDER   = Color.GRAY;
    static final Color BOARD_BG = Color.LIGHT_GRAY;

    // ── Shared screen dimension ───────────────────────────────────────────────
    private static Dimension screenSize() {
        return new Dimension(
                TetrisGame.BOARD_WIDTH + TetrisGame.SIDE_PANEL + TetrisGame.PADDING * 3,
                TetrisGame.BOARD_HEIGHT + TetrisGame.PADDING * 2);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TEMPLATE METHOD — BaseScreen
    // Used only by screens that have no instance fields beyond 'frame'.
    // ═════════════════════════════════════════════════════════════════════════
    private static abstract class BaseScreen extends JPanel {

        protected final TetrisGame.TetrisFrame frame;

        BaseScreen(TetrisGame.TetrisFrame frame) {
            this.frame = frame;
            setupUI();
        }

        private void setupUI() {
            setBackground(BG);
            setPreferredSize(screenSize());
            setLayout(new GridBagLayout());

            JPanel inner = new JPanel();
            inner.setBackground(BG);
            inner.setLayout(new GridLayout(0, 1, 0, 10));

            buildContent(inner);
            add(inner);
        }

        protected abstract void buildContent(JPanel inner);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MainMenuScreen — extends BaseScreen (no extra fields needed)
    // ═════════════════════════════════════════════════════════════════════════
    public static class MainMenuScreen extends BaseScreen {

        public MainMenuScreen(TetrisGame.TetrisFrame frame) {
            super(frame);
        }

        @Override
        protected void buildContent(JPanel inner) {
            JLabel title = new JLabel("TETRIS", SwingConstants.CENTER);
            title.setFont(new Font(Font.DIALOG, Font.BOLD, 36));
            inner.add(title);

            JButton startBtn = ButtonFactory.create("Start");
            startBtn.addActionListener(e -> {
                SoundEffects.onMenuClick();
                frame.showModeSelect();
            });

            JButton highBtn = ButtonFactory.create("High Scores");
            highBtn.addActionListener(e -> {
                SoundEffects.onMenuClick();
                frame.showHighScores();
            });

            JButton exitBtn = ButtonFactory.create("Exit");
            exitBtn.addActionListener(e -> {
                SoundEffects.onMenuClick();
                System.exit(0);
            });

            inner.add(startBtn);
            inner.add(highBtn);
            inner.add(exitBtn);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ModeSelectScreen
    // ─────────────────
    // Does NOT extend BaseScreen because 'modes' is an instance field that
    // would be null when BaseScreen calls buildContent() during super().
    // Constructor inlines the setup so 'modes' is assigned first.
    // ═════════════════════════════════════════════════════════════════════════
    public static class ModeSelectScreen extends JPanel {

        public ModeSelectScreen(TetrisGame.TetrisFrame frame) {
            // Assign modes before building any UI.
            GameModes.GameMode[] modes = {
                    new GameModes.StandardMode(),
                    new GameModes.FourtyLines(),
                    new GameModes.TimeTrial(),
                    new GameModes.ZenMode()
            };

            setBackground(BG);
            setPreferredSize(screenSize());
            setLayout(new GridBagLayout());

            JPanel inner = new JPanel();
            inner.setBackground(BG);
            inner.setLayout(new GridLayout(0, 1, 0, 10));

            JLabel heading = new JLabel("Select Mode", SwingConstants.CENTER);
            heading.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
            inner.add(heading);

            ButtonGroup group = new ButtonGroup();
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

            JButton backBtn = ButtonFactory.create("Back");
            backBtn.addActionListener(e -> {
                SoundEffects.onMenuBack();
                frame.showStartScreen();
            });

            JButton nextBtn = ButtonFactory.create("Next");
            nextBtn.addActionListener(e -> {
                SoundEffects.onMenuClick();
                GameModes.GameMode selected = modes[0];
                for (int i = 0; i < radios.length; i++) {
                    if (radios[i].isSelected()) {
                        selected = modes[i];
                        break;
                    }
                }
                frame.showDifficultySelect(selected);
            });

            buttons.add(backBtn);
            buttons.add(nextBtn);
            inner.add(buttons);

            add(inner);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DifficultyScreen
    // ─────────────────
    // Does NOT extend BaseScreen — 'mode' must be assigned before UI builds.
    // ═════════════════════════════════════════════════════════════════════════
    public static class DifficultyScreen extends JPanel {

        public DifficultyScreen(TetrisGame.TetrisFrame frame, GameModes.GameMode mode) {
            setBackground(BG);
            setPreferredSize(screenSize());
            setLayout(new GridBagLayout());

            JPanel inner = new JPanel();
            inner.setBackground(BG);
            inner.setLayout(new GridLayout(0, 1, 0, 10));

            JLabel heading = new JLabel("Select Difficulty", SwingConstants.CENTER);
            heading.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
            inner.add(heading);

            for (GameModes.Difficulty diff : GameModes.Difficulty.values()) {
                JButton btn = ButtonFactory.create(diff.getDisplayName());
                btn.addActionListener(e -> {
                    SoundEffects.onMenuClick();
                    frame.startGame(mode, diff);
                });
                inner.add(btn);
            }

            JButton backBtn = ButtonFactory.create("Back");
            backBtn.addActionListener(e -> {
                SoundEffects.onMenuBack();
                frame.showModeSelect();
            });
            inner.add(backBtn);

            add(inner);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HighScoreScreen — extends BaseScreen (SCORED_MODES is static, not instance)
    // ═════════════════════════════════════════════════════════════════════════
    public static class HighScoreScreen extends BaseScreen {

        private static final GameModes.GameMode[] SCORED_MODES = {
                new GameModes.StandardMode(),
                new GameModes.FourtyLines(),
                new GameModes.TimeTrial()
        };

        public HighScoreScreen(TetrisGame.TetrisFrame frame) {
            super(frame);
        }

        @Override
        protected void buildContent(JPanel inner) {
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

            JButton backBtn = ButtonFactory.create("Back");
            backBtn.addActionListener(e -> {
                SoundEffects.onMenuBack();
                frame.showStartScreen();
            });
            inner.add(backBtn);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GameOverScreen
    // ───────────────
    // Does NOT extend BaseScreen — 'stats', 'mode', 'difficulty' must be
    // assigned before UI builds.
    // ═════════════════════════════════════════════════════════════════════════
    public static class GameOverScreen extends JPanel {

        public GameOverScreen(TetrisGame.TetrisFrame frame,
                              String[][] stats,
                              GameModes.GameMode mode,
                              GameModes.Difficulty difficulty) {
            setBackground(BG);
            setPreferredSize(screenSize());
            setLayout(new GridBagLayout());

            JPanel inner = new JPanel();
            inner.setBackground(BG);
            inner.setLayout(new GridLayout(0, 1, 0, 10));

            JLabel heading = new JLabel("Game Over", SwingConstants.CENTER);
            heading.setFont(new Font(Font.DIALOG, Font.BOLD, 28));
            inner.add(heading);

            for (String[] row : stats) {
                JLabel lbl = new JLabel(row[0] + ":  " + row[1], SwingConstants.CENTER);
                lbl.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
                inner.add(lbl);
            }

            // FACTORY METHOD: mode.createFresh() — no instanceof needed (OCP).
            JButton replayBtn = ButtonFactory.create("Play Again");
            replayBtn.addActionListener(e -> {
                SoundEffects.onMenuClick();
                frame.startGame(ScreenFactory.createFreshMode(mode), difficulty);
            });

            JButton menuBtn = ButtonFactory.create("Main Menu");
            menuBtn.addActionListener(e -> {
                SoundEffects.onMenuClick();
                frame.showStartScreen();
            });

            inner.add(replayBtn);
            inner.add(menuBtn);

            add(inner);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY METHOD — ScreenFactory
    // ═════════════════════════════════════════════════════════════════════════
    public static class ScreenFactory {

        public static GameModes.GameMode createFreshMode(GameModes.GameMode mode) {
            return mode.createFresh();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ButtonFactory
    // ═════════════════════════════════════════════════════════════════════════
    public static class ButtonFactory {

        private ButtonFactory() {}

        public static JButton create(String text) {
            JButton btn = new JButton(text);
            btn.setPreferredSize(new Dimension(160, 36));
            return btn;
        }
    }

    // ── Backward-compatible alias ─────────────────────────────────────────────
    public static JButton plainButton(String text) {
        return ButtonFactory.create(text);
    }
}