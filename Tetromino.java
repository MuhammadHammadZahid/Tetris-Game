// package com.mycompany.tetrisgame;

import java.awt.Color;

/**
 * Tetromino.java — Abstract base class for all 7 Tetris pieces.
 *
 * ═══════════════════════════════════════════════════════════════════
 * DESIGN PATTERNS & PRINCIPLES APPLIED
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. TEMPLATE METHOD PATTERN (Behavioral)
 * ──────────────────────────────────────
 * WHERE : Tetromino constructor + defineRotations() / defineColor()
 * WHY : The construction sequence is always identical for every
 * piece: set rotationState → call defineRotations() → assign
 * shape → call defineColor(). This is the algorithm skeleton.
 * Subclasses (IPiece, OPiece, etc.) only fill in the two
 * "slots" that differ — their rotation grids and their colour.
 * They never touch the construction sequence itself.
 *
 * BEFORE: each subclass would have needed its own constructor
 * replicating the same 4-line setup.
 * AFTER : one constructor in the base class; subclasses only
 * override the two abstract hook methods.
 * CATEGORY: Behavioral — the base class controls the algorithm;
 * subclasses supply the variable parts.
 *
 * 2. PROTOTYPE PATTERN (Creational)
 * ──────────────────────────────────
 * WHERE : clone() method
 * WHY : The ghost-piece and preview features need copies of a
 * Tetromino without knowing its concrete type (IPiece, TPiece…).
 * Prototype lets the object copy itself:
 * Tetromino ghost = currentPiece.clone();
 * No instanceof checks, no factory call — the piece knows
 * how to reproduce itself.
 * The dead-code comment in the original (getShapeCopy()) was
 * a partial attempt at this — now formalised as a full clone().
 * CATEGORY: Creational — object creation by cloning an existing instance.
 *
 * 3. SINGLE RESPONSIBILITY PRINCIPLE (SOLID — S)
 * ───────────────────────────────────────────────
 * WHERE : Tetromino class scope
 * WHY : Tetromino is responsible for exactly one thing — representing
 * the state and behaviour of a single game piece (its shape,
 * colour, and rotation). It does NOT handle rendering, scoring,
 * or spawning — those belong to GameBoard and TetrominoFactory.
 *
 * 4. OPEN / CLOSED PRINCIPLE (SOLID — O)
 * ──────────────────────────────────────
 * WHERE : Abstract methods defineRotations() / defineColor() / getType()
 * WHY : Adding an 8th Tetromino (e.g. a custom piece) requires only
 * creating a new subclass in TetrominoFactory — zero changes
 * to Tetromino itself. The base class is closed for modification
 * but open for extension.
 *
 * 5. LISKOV SUBSTITUTION PRINCIPLE (SOLID — L)
 * ─────────────────────────────────────────────
 * WHERE : All concrete subclasses (IPiece, OPiece, TPiece … in
 * TetrominoFactory)
 * WHY : Every subclass can replace a Tetromino reference anywhere
 * in GameBoard without breaking the game. rotateClockwise(),
 * getShape(), getColor() all behave correctly regardless of
 * which concrete subclass is used. No subclass weakens the
 * base class contract.
 *
 * 6. DEPENDENCY INVERSION PRINCIPLE (SOLID — D)
 * ──────────────────────────────────────────────
 * WHERE : GameBoard holds a Tetromino reference (not IPiece / TPiece)
 * WHY : GameBoard depends on the Tetromino abstraction. It never
 * references a concrete piece class. This file reinforces that
 * contract by keeping all piece-specific logic in subclasses.
 *
 * ═══════════════════════════════════════════════════════════════════
 */
public abstract class Tetromino implements Cloneable {

    // ── Fields ────────────────────────────────────────────────────────────────

    /** Current active shape grid (4×4, 1 = filled cell, 0 = empty). */
    protected int[][] shape;

    /** Piece colour — set once by defineColor() during construction. */
    protected Color color;

    /** Index into the rotations array (0–3). Updated by rotate methods. */
    protected int rotationState;

    /**
     * All four rotation states of this piece.
     * Index 0 = spawn orientation (0°), 1 = 90°, 2 = 180°, 3 = 270°.
     * Populated once by defineRotations() during construction.
     */
    protected int[][][] rotations;

    // ═════════════════════════════════════════════════════════════════════════
    // TEMPLATE METHOD — constructor defines the piece-creation skeleton
    // ─────────────────────────────────────────────────────────────────────────
    // The construction sequence is fixed:
    // 1. Start at rotation 0 (spawn orientation)
    // 2. Ask the subclass for its rotation grids ← hook: defineRotations()
    // 3. Set the active shape to the spawn rotation
    // 4. Ask the subclass for its colour ← hook: defineColor()
    //
    // Every concrete piece (IPiece, OPiece, …) goes through this same
    // sequence without needing its own constructor.
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Constructs a Tetromino using the Template Method pattern.
     * Subclasses must implement defineRotations() and defineColor();
     * this constructor calls them in the correct order automatically.
     */
    public Tetromino() {
        this.rotationState = 0; // always start at spawn rotation
        this.rotations = defineRotations(); // hook — subclass supplies grids
        this.shape = rotations[0]; // active shape = spawn orientation
        this.color = defineColor(); // hook — subclass supplies colour
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TEMPLATE METHOD hooks — abstract, must be implemented by subclasses
    // ─────────────────────────────────────────────────────────────────────────
    // These are the "variable parts" of the Template Method skeleton.
    // Protected visibility: accessible to subclasses, hidden from game logic.
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * HOOK — defines all four rotation grids for this piece.
     *
     * Each grid is a 4×4 int array (1 = filled, 0 = empty).
     * Index order: [0]=0°, [1]=90°, [2]=180°, [3]=270°.
     *
     * Called exactly once, during construction, by the Template Method.
     * The result is stored in {@code rotations} and never recomputed.
     *
     * @return 4-element array of 4×4 rotation grids
     */
    protected abstract int[][][] defineRotations();

    /**
     * HOOK — defines the display colour for this piece type.
     *
     * Called exactly once, during construction, by the Template Method.
     * Returning a different Color here is all that's needed to recolour
     * a piece — no other method requires changes.
     *
     * @return the Color used to draw every cell of this piece
     */
    protected abstract Color defineColor();

    /**
     * Returns the single-character type identifier for this piece.
     * Used by TetrominoFactory for type-safe piece creation and by
     * toString() for debug output.
     *
     * Examples: "I", "O", "T", "S", "Z", "L", "J"
     *
     * OCP: adding a new piece type requires a new subclass that returns
     * a new string here — Tetromino itself never changes.
     */
    public abstract String getType();

    // ═════════════════════════════════════════════════════════════════════════
    // PROTOTYPE PATTERN — clone()
    // ─────────────────────────────────────────────────────────────────────────
    // Allows GameBoard to copy the current piece without knowing its
    // concrete type. Used for ghost-piece calculation and next-piece preview.
    //
    // BEFORE: dead-code comment in original:
    // /** Returns a deep copy of the shape for preview or ghost rendering.
    // DEAD CODE FOR LATER IMPLEMENTATION
    // public int[][] getShapeCopy() { ... }
    // */
    // That was a partial, commented-out attempt at Prototype.
    // Now formalised as a proper clone() that copies the full piece state.
    //
    // PATTERN : Prototype (Creational) — object clones itself.
    // PRINCIPLE: DIP — caller asks for a Tetromino copy without knowing
    // whether it's an IPiece, TPiece, or any other subclass.
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Returns a deep copy of this Tetromino at its current rotation state.
     *
     * PATTERN — Prototype (Creational):
     * The piece reproduces itself. GameBoard can write:
     * Tetromino ghost = currentPiece.clone();
     * without any instanceof check or factory call.
     *
     * DEEP COPY: rotations array is shared (it is immutable — defineRotations()
     * sets it once and nothing ever modifies it), but shape is copied so the
     * clone's active shape is independent of the original's.
     *
     * @return a new Tetromino of the same type at the same rotation state
     */
    @Override
    public Tetromino clone() {
        try {
            // Use Java's built-in Object.clone() for the shallow base copy,
            // then deep-copy the mutable shape array so caller and clone
            // don't share the same active shape reference.
            Tetromino copy = (Tetromino) super.clone();
            copy.rotationState = this.rotationState;
            copy.rotations = this.rotations; // immutable — safe to share
            copy.color = this.color; // Color is immutable — safe to share

            // Deep-copy the active shape (mutable — must be independent)
            copy.shape = new int[this.shape.length][];
            for (int i = 0; i < this.shape.length; i++) {
                copy.shape[i] = this.shape[i].clone();
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            // Should never happen — Tetromino implements Cloneable (see below)
            throw new AssertionError("Tetromino.clone() failed unexpectedly", e);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Rotation methods
    // ─────────────────────────────────────────────────────────────────────────
    // These are part of Tetromino's single responsibility — managing piece
    // orientation. GameBoard calls these; it never manipulates rotationState
    // or the rotations array directly. (Encapsulation / SRP)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Advances to the next clockwise rotation state (0→1→2→3→0).
     * Updates the active shape to match the new rotation.
     *
     * GameBoard calls this on UP key press.
     * Wall-kick logic (offset correction) lives in GameBoard, not here —
     * keeping piece state management and collision resolution separate. (SRP)
     */
    public void rotateClockwise() {
        rotationState = (rotationState + 1) % 4;
        shape = rotations[rotationState];
    }

    /**
     * Reverses to the previous counter-clockwise rotation state (0→3→2→1→0).
     * Uses +3 instead of -1 to avoid negative modulo in Java.
     *
     * GameBoard calls this on C key press.
     */
    public void rotateCounterClockwise() {
        rotationState = (rotationState + 3) % 4; // +3 ≡ -1 (mod 4), avoids negatives
        shape = rotations[rotationState];
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Accessors
    // ─────────────────────────────────────────────────────────────────────────
    // Getters expose state without allowing external mutation of internal arrays.
    // Callers (GameBoard) read shape/color but never write them directly. (SRP)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Returns the active 4×4 shape grid for the current rotation.
     * Used by GameBoard for collision detection and rendering.
     *
     * NOTE: returns the live array reference — callers must not modify it.
     * Use clone() if a mutable copy is needed.
     */
    public int[][] getShape() {
        return shape;
    }

    /** Returns this piece's display colour. */
    public Color getColor() {
        return color;
    }

    /** Returns the current rotation index (0–3). */
    public int getRotationState() {
        return rotationState;
    }

    /**
     * Returns the spawn-orientation (0°) shape for preview rendering.
     * The next-piece panel always shows the piece in its spawn state,
     * regardless of the current rotation of the active piece.
     *
     * This is a read-only view of rotations[0] — safe to return directly
     * because the rotations array is immutable after construction.
     */
    public int[][] getPreviewShape() {
        return rotations[0];
    }

    // ═════════════════════════════════════════════════════════════════════════
    // toString — debug / logging utility
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Returns a human-readable description of this piece.
     * Useful for debug logging and unit test assertions.
     * getType() is the abstract hook — each subclass provides its identifier.
     */
    @Override
    public String toString() {
        return "Tetromino[type=" + getType() + ", rotation=" + rotationState + "]";
    }
}