// package com.mycompany.tetrisgame;

import java.awt.Color;
import java.util.*;

/**
 * TetrominoFactory.java — Creates and manages all 7 Tetromino pieces.
 *
 * ═══════════════════════════════════════════════════════════════════
 * DESIGN PATTERNS & PRINCIPLES APPLIED
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. FACTORY METHOD PATTERN (Creational)
 * ──────────────────────────────────────
 * WHERE : create(String type) method
 * WHY : GameBoard never calls new IPiece() or new TPiece() directly.
 * It calls TetrominoFactory.createRandom() — it only knows
 * about the Tetromino abstraction, never the concrete subclass.
 * The factory centralises all "which class do I instantiate?"
 * decisions in one place.
 * Adding a new piece = one new subclass + one new case in
 * create(). Nothing else changes. (OCP)
 * CATEGORY: Creational — factory decides which object to instantiate.
 *
 * 2. SINGLETON PATTERN (Creational)
 * ──────────────────────────────────
 * WHERE : The bag state (bag list, bagIndex, random) — now instance
 * fields on a Singleton rather than raw static fields.
 * WHY : The original used static mutable fields as implicit global
 * state. Static mutable state is hard to reset between games,
 * hard to test, and can cause subtle bugs if multiple game
 * instances existed. Singleton wraps this state in one
 * controlled object with a clear lifecycle.
 * CATEGORY: Creational — one managed instance of the bag randomiser.
 *
 * 3. PROTOTYPE PATTERN (Creational)
 * ──────────────────────────────────
 * WHERE : Tetromino.clone() — used by createClone() helper
 * WHY : The ghost-piece feature needs a copy of the current piece.
 * createClone(Tetromino) delegates to the piece's own clone()
 * method (defined in Tetromino.java). The factory provides
 * a convenient static entry point: TetrominoFactory.createClone(t).
 * CATEGORY: Creational — cloning an existing instance.
 *
 * 4. TEMPLATE METHOD PATTERN (Behavioral)
 * ──────────────────────────────────────
 * WHERE : Every concrete piece class (IPiece, OPiece, …)
 * WHY : Each subclass only overrides the two hooks defined in
 * Tetromino: defineRotations() and defineColor(). The
 * construction sequence (Template Method) lives entirely in
 * the Tetromino base class — no piece subclass has its own
 * constructor.
 * CATEGORY: Behavioral — base class owns the algorithm skeleton.
 *
 * 5. OPEN / CLOSED PRINCIPLE (SOLID — O)
 * ──────────────────────────────────────
 * WHERE : create() switch + ALL_TYPES array
 * WHY : To add an 8th piece (e.g. a custom "U" piece):
 * 1. Add a new inner class UPiece extends Tetromino
 * 2. Add U_PIECE constant
 * 3. Add one case in create()
 * 4. Add U_PIECE to ALL_TYPES
 * Zero changes to GameBoard, Tetromino, or any other file.
 *
 * 6. SINGLE RESPONSIBILITY PRINCIPLE (SOLID — S)
 * ───────────────────────────────────────────────
 * WHERE : TetrominoFactory vs each piece subclass
 * WHY : TetrominoFactory is responsible for piece creation and
 * bag randomisation only. Each piece subclass (IPiece etc.)
 * is responsible for its own shape data and colour only.
 * No class mixes these two concerns.
 *
 * 7. LISKOV SUBSTITUTION PRINCIPLE (SOLID — L)
 * ─────────────────────────────────────────────
 * WHERE : All 7 concrete piece classes
 * WHY : Every piece subclass can substitute for a Tetromino
 * reference without breaking GameBoard. rotateClockwise(),
 * getShape(), getColor(), clone() all behave correctly
 * regardless of which concrete type is used.
 *
 * 8. DEPENDENCY INVERSION PRINCIPLE (SOLID — D)
 * ──────────────────────────────────────────────
 * WHERE : create() return type + createRandom() return type
 * WHY : Both methods return Tetromino (abstraction), never a
 * concrete subclass type. Callers depend on the abstraction.
 *
 * ═══════════════════════════════════════════════════════════════════
 */
public class TetrominoFactory implements Cloneable {

    // ═════════════════════════════════════════════════════════════════════════
    // SINGLETON — one managed factory instance owns the bag state
    // ─────────────────────────────────────────────────────────────────────────
    // The bag (7-bag randomiser) is stateful — it must persist across piece
    // spawns within a game. Making this a Singleton ensures:
    // - One bag shared across the whole game (correct 7-bag behaviour)
    // - State is encapsulated in one object (not scattered static fields)
    // - The bag can be reset cleanly between games via resetBag()
    // ═════════════════════════════════════════════════════════════════════════

    /** The single managed instance. */
    private static TetrominoFactory instance = null;

    /**
     * Returns the single TetrominoFactory instance (lazy Singleton).
     * Creates the instance on first call.
     *
     * PATTERN — Singleton (Creational).
     */
    public static TetrominoFactory getInstance() {
        if (instance == null) {
            instance = new TetrominoFactory();
        }
        return instance;
    }

    /** Private constructor — enforces Singleton. */
    private TetrominoFactory() {
        // bag, bagIndex, and random are initialised as instance fields below
    }

    // ── Piece type constants (public — used by subclasses and callers) ────────
    // Named constants prevent magic string literals elsewhere. (DRY)
    public static final String I_PIECE = "I";
    public static final String O_PIECE = "O";
    public static final String T_PIECE = "T";
    public static final String S_PIECE = "S";
    public static final String Z_PIECE = "Z";
    public static final String L_PIECE = "L";
    public static final String J_PIECE = "J";

    /**
     * All piece types in spawn order — used to fill the 7-bag.
     * OCP: adding a new piece type = add its constant here + a new
     * subclass + one case in create(). No other changes needed.
     */
    private static final String[] ALL_TYPES = {
            I_PIECE, O_PIECE, T_PIECE, S_PIECE, Z_PIECE, L_PIECE, J_PIECE
    };

    // ── 7-bag randomiser state (instance fields — was static mutable state) ──
    private final List<String> bag = new ArrayList<>();
    private int bagIndex = 0;
    private final Random random = new Random();

    // ═════════════════════════════════════════════════════════════════════════
    // FACTORY METHOD — create(String type)
    // ─────────────────────────────────────
    // Centralises all "which concrete class do I instantiate?" logic.
    // Callers (GameBoard) never call new IPiece() directly — they go through
    // this method and receive a Tetromino abstraction. (DIP)
    //
    // PATTERN : Factory Method (Creational)
    // PRINCIPLE: OCP — new piece = new subclass + one new case here.
    // DIP — returns Tetromino abstraction, not concrete type.
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Creates and returns a new Tetromino of the specified type.
     *
     * PATTERN — Factory Method (Creational):
     * This method is the single point where concrete piece classes are
     * instantiated. Callers depend only on the Tetromino abstraction.
     *
     * @param type one of the piece type constants (I_PIECE, O_PIECE, …)
     * @return a fresh Tetromino instance at rotation state 0
     * @throws IllegalArgumentException if type is not a known piece constant
     */
    public Tetromino createPiece(String type) {
        // Switch expression — exhaustive, clean, no fall-through risk.
        // Each case returns a concrete subclass as a Tetromino (DIP).
        return switch (type) {
            case I_PIECE -> new IPiece();
            case O_PIECE -> new OPiece();
            case T_PIECE -> new TPiece();
            case S_PIECE -> new SPiece();
            case Z_PIECE -> new ZPiece();
            case L_PIECE -> new LPiece();
            case J_PIECE -> new JPiece();
            // Default throws — a missing case is a programmer error, not
            // a runtime condition. Fail fast with a clear message. (SRP)
            default -> throw new IllegalArgumentException(
                    "[TetrominoFactory] Unknown piece type: \"" + type + "\". "
                            + "Valid types: I, O, T, S, Z, L, J");
        };
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 7-BAG RANDOMISER — createRandom()
    // ──────────────────────────────────
    // Tetris guideline: every set of 7 pieces contains exactly one of each
    // type, shuffled randomly. This prevents long droughts of any one piece.
    //
    // SRP: bag logic lives here, not in GameBoard. GameBoard just calls
    // createRandom() and receives a piece — it knows nothing about bags.
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Returns the next piece from the 7-bag randomiser.
     * Automatically refills and reshuffles the bag when exhausted.
     *
     * @return a fresh Tetromino drawn from the current bag
     */
    public Tetromino createRandom() {
        if (bagIndex >= bag.size()) {
            refillBag(); // reshuffle when the current bag is exhausted
        }
        return createPiece(bag.get(bagIndex++));
    }

    /**
     * Resets the bag to a fresh shuffled set of all 7 piece types.
     * Called automatically by createRandom() when the bag runs out.
     * Can also be called explicitly to reset state between games.
     */
    public void resetBag() {
        refillBag();
    }

    /**
     * Internal bag refill — clears, repopulates, and shuffles.
     * Private: callers use resetBag() for intentional resets.
     */
    private void refillBag() {
        bag.clear();
        bag.addAll(Arrays.asList(ALL_TYPES)); // all 7 types
        Collections.shuffle(bag, random); // true random shuffle
        bagIndex = 0; // reset the read head
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PROTOTYPE — createClone(Tetromino)
    // ───────────────────────────────────
    // Provides a factory-level entry point for piece cloning.
    // Delegates to Tetromino.clone() (the Prototype pattern defined there).
    //
    // Use case: GameBoard clones the current piece to compute the ghost piece
    // position without modifying the real piece's rotation state.
    //
    // PATTERN : Prototype (Creational) — clone via the object itself.
    // PRINCIPLE: DIP — caller receives a Tetromino; never knows the subtype.
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Returns a deep copy of the given Tetromino at its current rotation state.
     *
     * PATTERN — Prototype (Creational):
     * Delegates to the piece's own clone() method so no instanceof check
     * or subtype knowledge is needed here.
     *
     * Example usage in GameBoard:
     * Tetromino ghost = TetrominoFactory.createClone(currentPiece);
     *
     * @param source the Tetromino to copy
     * @return a new independent Tetromino with the same type and rotation
     */
    public static Tetromino createClone(Tetromino source) {
        return source.clone(); // Prototype — piece clones itself
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STATIC FORWARDING METHODS — backward-compatible API
    // ─────────────────────────────────────────────────────
    // GameBoard currently calls TetrominoFactory.create(...) and
    // TetrominoFactory.createRandom() as static methods. These forwarders
    // preserve that API exactly while routing through the Singleton instance.
    // Zero changes required in GameBoard. (Backward compatibility)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Static forwarder — preserves existing call sites.
     * 
     * @see #createPiece(String)
     */
    public static Tetromino create(String type) {
        return getInstance().createPiece(type);
    }

    /**
     * Static forwarder — preserves existing call sites.
     * 
     * @see #createRandom()
     */
    public static Tetromino createRandom() {
        return getInstance().createRandom();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CONCRETE PIECE SUBCLASSES
    // ──────────────────────────
    // Each class has exactly ONE responsibility: define its rotation grids
    // and colour. Nothing else. (SRP)
    //
    // Each is package-private (no 'public') — only TetrominoFactory and
    // GameBoard (same package) need access. Callers outside the package
    // hold a Tetromino reference, never a concrete subclass type. (DIP)
    //
    // TEMPLATE METHOD: each subclass only overrides the two hooks
    // (defineRotations, defineColor) — the constructor skeleton lives in
    // Tetromino and runs automatically. (Behavioral)
    //
    // PROTOTYPE: each subclass inherits clone() from Tetromino — no
    // additional clone logic needed here. (Creational)
    //
    // LSP: every subclass can substitute for Tetromino without breaking
    // any caller. (Liskov Substitution Principle)
    // ═════════════════════════════════════════════════════════════════════════

    // ── I-Piece — cyan, 4-cell horizontal/vertical bar ───────────────────────
    static class IPiece extends Tetromino {

        @Override
        protected int[][][] defineRotations() {
            // TEMPLATE METHOD hook: supplies the 4 rotation grids.
            return new int[][][] {
                    { { 0, 0, 0, 0 }, { 1, 1, 1, 1 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } }, // 0°
                    { { 0, 0, 1, 0 }, { 0, 0, 1, 0 }, { 0, 0, 1, 0 }, { 0, 0, 1, 0 } }, // 90°
                    { { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 1, 1, 1, 1 }, { 0, 0, 0, 0 } }, // 180°
                    { { 0, 1, 0, 0 }, { 0, 1, 0, 0 }, { 0, 1, 0, 0 }, { 0, 1, 0, 0 } } // 270°
            };
        }

        @Override
        protected Color defineColor() {
            return new Color(0, 240, 240);
        } // cyan

        @Override
        public String getType() {
            return I_PIECE;
        }
    }

    // ── O-Piece — yellow, 2×2 square (rotation-invariant) ───────────────────
    static class OPiece extends Tetromino {

        @Override
        protected int[][][] defineRotations() {
            // Symmetric: all 4 rotation states are identical.
            int[][] shape = { { 0, 1, 1, 0 }, { 0, 1, 1, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } };
            return new int[][][] { shape, shape, shape, shape };
        }

        @Override
        protected Color defineColor() {
            return new Color(240, 240, 0);
        } // yellow

        @Override
        public String getType() {
            return O_PIECE;
        }
    }

    // ── T-Piece — purple, T-shape ─────────────────────────────────────────────
    static class TPiece extends Tetromino {

        @Override
        protected int[][][] defineRotations() {
            return new int[][][] {
                    { { 0, 1, 0, 0 }, { 1, 1, 1, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } }, // 0°
                    { { 0, 1, 0, 0 }, { 0, 1, 1, 0 }, { 0, 1, 0, 0 }, { 0, 0, 0, 0 } }, // 90°
                    { { 0, 0, 0, 0 }, { 1, 1, 1, 0 }, { 0, 1, 0, 0 }, { 0, 0, 0, 0 } }, // 180°
                    { { 0, 1, 0, 0 }, { 1, 1, 0, 0 }, { 0, 1, 0, 0 }, { 0, 0, 0, 0 } } // 270°
            };
        }

        @Override
        protected Color defineColor() {
            return new Color(160, 0, 240);
        } // purple

        @Override
        public String getType() {
            return T_PIECE;
        }
    }

    // ── S-Piece — green, S-shape ──────────────────────────────────────────────
    static class SPiece extends Tetromino {

        @Override
        protected int[][][] defineRotations() {
            return new int[][][] {
                    { { 0, 1, 1, 0 }, { 1, 1, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } }, // 0°
                    { { 0, 1, 0, 0 }, { 0, 1, 1, 0 }, { 0, 0, 1, 0 }, { 0, 0, 0, 0 } }, // 90°
                    { { 0, 0, 0, 0 }, { 0, 1, 1, 0 }, { 1, 1, 0, 0 }, { 0, 0, 0, 0 } }, // 180°
                    { { 1, 0, 0, 0 }, { 1, 1, 0, 0 }, { 0, 1, 0, 0 }, { 0, 0, 0, 0 } } // 270°
            };
        }

        @Override
        protected Color defineColor() {
            return new Color(0, 240, 0);
        } // green

        @Override
        public String getType() {
            return S_PIECE;
        }
    }

    // ── Z-Piece — red, Z-shape ────────────────────────────────────────────────
    static class ZPiece extends Tetromino {

        @Override
        protected int[][][] defineRotations() {
            return new int[][][] {
                    { { 1, 1, 0, 0 }, { 0, 1, 1, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } }, // 0°
                    { { 0, 0, 1, 0 }, { 0, 1, 1, 0 }, { 0, 1, 0, 0 }, { 0, 0, 0, 0 } }, // 90°
                    { { 0, 0, 0, 0 }, { 1, 1, 0, 0 }, { 0, 1, 1, 0 }, { 0, 0, 0, 0 } }, // 180°
                    { { 0, 1, 0, 0 }, { 1, 1, 0, 0 }, { 1, 0, 0, 0 }, { 0, 0, 0, 0 } } // 270°
            };
        }

        @Override
        protected Color defineColor() {
            return new Color(240, 0, 0);
        } // red

        @Override
        public String getType() {
            return Z_PIECE;
        }
    }

    // ── L-Piece — orange, L-shape ─────────────────────────────────────────────
    static class LPiece extends Tetromino {

        @Override
        protected int[][][] defineRotations() {
            return new int[][][] {
                    { { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 1, 1, 0, 0 }, { 0, 0, 0, 0 } }, // 0°
                    { { 0, 0, 0, 0 }, { 1, 1, 1, 0 }, { 1, 0, 0, 0 }, { 0, 0, 0, 0 } }, // 90°
                    { { 1, 1, 0, 0 }, { 0, 1, 0, 0 }, { 0, 1, 0, 0 }, { 0, 0, 0, 0 } }, // 180°
                    { { 0, 0, 1, 0 }, { 1, 1, 1, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } } // 270°
            };
        }

        @Override
        protected Color defineColor() {
            return new Color(240, 160, 0);
        } // orange

        @Override
        public String getType() {
            return L_PIECE;
        }
    }

    // ── J-Piece — blue, J-shape ───────────────────────────────────────────────
    static class JPiece extends Tetromino {

        @Override
        protected int[][][] defineRotations() {
            return new int[][][] {
                    { { 0, 1, 0, 0 }, { 0, 1, 0, 0 }, { 1, 1, 0, 0 }, { 0, 0, 0, 0 } }, // 0°
                    { { 1, 0, 0, 0 }, { 1, 1, 1, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } }, // 90°
                    { { 1, 1, 0, 0 }, { 1, 0, 0, 0 }, { 1, 0, 0, 0 }, { 0, 0, 0, 0 } }, // 180°
                    { { 0, 0, 0, 0 }, { 1, 1, 1, 0 }, { 0, 0, 1, 0 }, { 0, 0, 0, 0 } } // 270°
            };
        }

        @Override
        protected Color defineColor() {
            return new Color(0, 0, 240);
        } // blue

        @Override
        public String getType() {
            return J_PIECE;
        }
    }
}