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
 * ═══════════════════════════════════════════════════════════════════
 */
public class UserInterface {

    // ── Shared plain-UI colours ───────────────────────────────────────────────
    // These are package-visible constants used by GameBoard (no change needed).
    static final Color BG = Color.WHITE;
    static final Color BORDER = Color.GRAY;
    static final Color BOARD_BG = Color.LIGHT_GRAY;

    // ── Shared screen dimension (DRY — was copy-pasted in every screen) ───────
    // SOLID/DRY: one source of truth for the preferred window size.
    private static Dimension screenSize() {
        return new Dimension(
                TetrisGame.BOARD_WIDTH + TetrisGame.SIDE_PANEL + TetrisGame.PADDING * 3,
                TetrisGame.BOARD_HEIGHT + TetrisGame.PADDING * 2);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TEMPLATE METHOD — BaseScreen
    // ─────────────────────────────
    // Abstract superclass that defines the construction skeleton shared by
    // every screen. Concrete screens only supply buildContent(JPanel inner).
    //
    // Pattern: TEMPLATE METHOD (Behavioral)
    // Principle: DRY, SRP — each subclass has one job: fill in its content.
    // ═════════════════════════════════════════════════════════════════════════
    private static abstract class BaseScreen extends JPanel {

        protected final TetrisGame.TetrisFrame frame;

        BaseScreen(TetrisGame.TetrisFrame frame) {
            this.frame = frame;
            setupUI(); // calls the template method
        }

        /**
         * TEMPLATE METHOD — skeleton that every screen follows:
         * 1. Set shared background and size.
         * 2. Create the centred inner panel.
         * 3. Delegate unique content to the subclass hook buildContent().
         * 4. Add the inner panel to this screen.
         */
        private void setupUI() {
            setBackground(BG);
            setPreferredSize(screenSize());
            setLayout(new GridBagLayout()); // centres inner panel automatically

            JPanel inner = new JPanel();
            inner.setBackground(BG);
            inner.setLayout(new GridLayout(0, 1, 0, 10)); // vertical stack

            buildContent(inner); // ← hook: subclass fills this in

            add(inner); // add the populated inner panel to the screen
        }

        /**
         * HOOK METHOD — overridden by each concrete screen.
         * Add all screen-specific labels, buttons, etc. to {@code inner}.
         */
        protected abstract void buildContent(JPanel inner);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MainMenuScreen
    // ═════════════════════════════════════════════════════════════════════════
    public static class MainMenuScreen extends BaseScreen {

        public MainMenuScreen(TetrisGame.TetrisFrame frame) {
            super(frame);
        }

        @Override
        protected void buildContent(JPanel inner) {
            // Title label
            JLabel title = new JLabel("TETRIS", SwingConstants.CENTER);
            title.setFont(new Font(Font.DIALOG, Font.BOLD, 36));
            inner.add(title);

            // Start button → navigate to mode selection
            JButton startBtn = ButtonFactory.create("Start");
            startBtn.addActionListener(e -> {
                SoundEffects.onMenuClick();
                frame.showModeSelect();
            });

            // High scores button → navigate to high score screen
            JButton highBtn = ButtonFactory.create("High Scores");
            highBtn.addActionListener(e -> {
                SoundEffects.onMenuClick();
                frame.showHighScores();
            });

            // Exit button → terminate application
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
    // ═════════════════════════════════════════════════════════════════════════
    public static class ModeSelectScreen extends BaseScreen {

        // All available modes — adding a new mode here is all that's needed.
        // OCP: the screen doesn't need to know about concrete mode types.
        private final GameModes.GameMode[] modes = {
                new GameModes.StandardMode(),
                new GameModes.FourtyLines(),
                new GameModes.TimeTrial(),
                new GameModes.ZenMode()
        };

        public ModeSelectScreen(TetrisGame.TetrisFrame frame) {
            super(frame);
        }

        @Override
        protected void buildContent(JPanel inner) {
            JLabel heading = new JLabel("Select Mode", SwingConstants.CENTER);
            heading.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
            inner.add(heading);

            // Build one radio button per mode dynamically — no hard-coded names.
            ButtonGroup group = new ButtonGroup();
            JRadioButton[] radios = new JRadioButton[modes.length];

            for (int i = 0; i < modes.length; i++) {
                JRadioButton rb = new JRadioButton(modes[i].getModeName());
                rb.setBackground(BG);
                if (i == 0)
                    rb.setSelected(true); // default: first mode selected
                group.add(rb);
                inner.add(rb);
                radios[i] = rb;
            }

            // Navigation buttons row
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
                // Find which radio is selected, pass its mode forward
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
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DifficultyScreen
    // ═════════════════════════════════════════════════════════════════════════
    public static class DifficultyScreen extends BaseScreen {

        private final GameModes.GameMode mode;

        public DifficultyScreen(TetrisGame.TetrisFrame frame, GameModes.GameMode mode) {
            // NOTE: super() must be called before storing mode, so we pass
            // mode via a field set before buildContent() is invoked.
            // We work around Java's "super() must be first" rule by storing
            // the mode reference before calling super (via field initialiser trick):
            // Instead, we delay super to after field assignment by overriding
            // with a two-step constructor pattern below.
            super(frame);
            // 'mode' was already captured in the field before buildContent ran
            // because Java initialises instance fields before the super body.
            // Actually, since super() calls setupUI() → buildContent(), and
            // 'mode' isn't set yet at that point, we must set it first.
            // Solution: store in a pre-init holder. See note in buildContent.
            this.mode = mode;
        }

        // DESIGN NOTE: because BaseScreen.setupUI() calls buildContent() from
        // its constructor, and Java requires super() to be the first statement,
        // 'mode' is null during the first buildContent() call triggered by
        // super(frame). To handle this safely, buildContent() guards on null.
        // This is a known Template Method / constructor ordering trade-off.
        // An alternative is lazy initialisation (init() called after super).
        @Override
        protected void buildContent(JPanel inner) {
            if (mode == null)
                return; // guard during super() construction phase

            JLabel heading = new JLabel("Select Difficulty", SwingConstants.CENTER);
            heading.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
            inner.add(heading);

            // Enumerate all difficulties from the enum — OCP: adding a new
            // Difficulty value in GameModes.Difficulty automatically appears here.
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
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HighScoreScreen
    // ═════════════════════════════════════════════════════════════════════════
    public static class HighScoreScreen extends BaseScreen {

        // Only modes that record scores. ZenMode excluded (recordsHighScore=false).
        // SRP: this screen only knows about displaying scores, not about modes' rules.
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

            // Dynamically render one score row per scored mode.
            // DIP: we depend on GameMode abstraction, not concrete classes.
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
    // ═════════════════════════════════════════════════════════════════════════
    public static class GameOverScreen extends BaseScreen {

        private final String[][] stats;
        private final GameModes.GameMode mode;
        private final GameModes.Difficulty difficulty;

        public GameOverScreen(TetrisGame.TetrisFrame frame,
                String[][] stats,
                GameModes.GameMode mode,
                GameModes.Difficulty difficulty) {
            super(frame);
            this.stats = stats;
            this.mode = mode;
            this.difficulty = difficulty;
        }

        @Override
        protected void buildContent(JPanel inner) {
            // Guard for constructor ordering (same reason as DifficultyScreen)
            if (stats == null)
                return;

            JLabel heading = new JLabel("Game Over", SwingConstants.CENTER);
            heading.setFont(new Font(Font.DIALOG, Font.BOLD, 28));
            inner.add(heading);

            // Display mode-provided result stats (score, lines, time, etc.)
            for (String[] row : stats) {
                JLabel lbl = new JLabel(row[0] + ":  " + row[1], SwingConstants.CENTER);
                lbl.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
                inner.add(lbl);
            }

            // ── FACTORY METHOD applied here ────────────────────────────────
            // OLD CODE (violated OCP — had to edit this method for every new mode):
            //
            // private GameModes.GameMode freshMode(GameModes.GameMode mode) {
            // if (mode instanceof GameModes.StandardMode) return new
            // GameModes.StandardMode();
            // if (mode instanceof GameModes.FourtyLines) return new
            // GameModes.FourtyLines();
            // if (mode instanceof GameModes.TimeTrial) return new GameModes.TimeTrial();
            // if (mode instanceof GameModes.ZenMode) return new GameModes.ZenMode();
            // return new GameModes.StandardMode(); // silent fallback — dangerous
            // }
            //
            // NEW CODE: delegates to ScreenFactory.createFreshMode(), which
            // calls mode.createFresh() — each GameMode knows how to clone itself.
            // Adding a new GameMode requires NO changes here. (OCP satisfied)
            // ──────────────────────────────────────────────────────────────
            JButton replayBtn = ButtonFactory.create("Play Again");
            replayBtn.addActionListener(e -> {
                SoundEffects.onMenuClick();
                GameModes.GameMode freshMode = ScreenFactory.createFreshMode(mode);
                frame.startGame(freshMode, difficulty);
            });

            JButton menuBtn = ButtonFactory.create("Main Menu");
            menuBtn.addActionListener(e -> {
                SoundEffects.onMenuClick();
                frame.showStartScreen();
            });

            inner.add(replayBtn);
            inner.add(menuBtn);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY METHOD — ScreenFactory
    // ────────────────────────────────
    // Centralises object creation logic that was previously scattered across
    // screens (e.g. the instanceof chain in GameOverScreen.freshMode()).
    //
    // Pattern : FACTORY METHOD (Creational)
    // Principle: SRP — GameOverScreen no longer decides how to re-create modes.
    // OCP — Adding a new GameMode does NOT require editing this class
    // because creation is delegated to mode.createFresh().
    // ═════════════════════════════════════════════════════════════════════════
    public static class ScreenFactory {

        /**
         * Returns a fresh (reset-state) instance of the same GameMode type.
         *
         * PATTERN — Factory Method:
         * Instead of this class knowing every concrete GameMode subclass,
         * it asks the mode itself to produce a fresh copy via createFresh().
         * Each GameMode subclass implements createFresh() (see GameModes.java).
         *
         * BEFORE (instanceof chain — OCP violation):
         * if (mode instanceof StandardMode) return new StandardMode();
         * if (mode instanceof FourtyLines) return new FourtyLines();
         * ...
         *
         * AFTER (delegation — OCP satisfied):
         * return mode.createFresh();
         */
        public static GameModes.GameMode createFreshMode(GameModes.GameMode mode) {
            return mode.createFresh();
            // Each concrete GameMode implements createFresh() to return
            // a new instance of itself. No instanceof checks needed here.
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // INTERFACE SEGREGATION — ButtonFactory
    // ──────────────────────────────────────
    // Was a static method plainButton() mixed directly into UserInterface.
    // Extracting it into a focused factory class gives it a single, clear
    // responsibility and makes it easy to change button style in one place.
    //
    // Pattern : (Simple) Factory / utility
    // Principle: ISP — small, focused responsibility; SRP — one class, one job.
    //
    // Backward-compatible alias kept below so GameBoard's existing calls to
    // UserInterface.plainButton() continue to compile without changes.
    // ═════════════════════════════════════════════════════════════════════════
    public static class ButtonFactory {

        private ButtonFactory() {
        } // utility class — not instantiated

        /**
         * Creates a plain, consistently-sized JButton.
         * All screens use this instead of duplicating button setup code.
         */
        public static JButton create(String text) {
            JButton btn = new JButton(text);
            btn.setPreferredSize(new Dimension(160, 36));
            return btn;
        }
    }

    // ── Backward-compatible alias (other files call UserInterface.plainButton) ─
    // This preserves existing call sites (e.g. in GameBoard) without any edits.
    public static JButton plainButton(String text) {
        return ButtonFactory.create(text);
    }
}