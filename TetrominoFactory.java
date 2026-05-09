// package com.mycompany.tetrisgame;
import java.awt.Color;   //tetromino colour
import java.util.*; //randomiser

/*factory to handle all details of piece generation and management,
gameboard simply calls on a tetromino function*/
public class TetrominoFactory {
    
    private static final List<String> bag = new ArrayList<>(); //randomiser bag
    private static int bagIndex = 0; //init bag at zero for traversal
    private static final Random random = new Random();

    //piece type constants
    public static final String I_PIECE = "I";
    public static final String O_PIECE = "O";
    public static final String T_PIECE = "T";
    public static final String S_PIECE = "S";
    public static final String Z_PIECE = "Z";
    public static final String L_PIECE = "L";
    public static final String J_PIECE = "J";

    private static final String[] ALL_TYPES = { //used by createRandom()
        I_PIECE, O_PIECE, T_PIECE, S_PIECE, Z_PIECE, L_PIECE, J_PIECE
    };

    
    public static Tetromino create(String type) {
        return switch (type) {
            case I_PIECE -> new IPiece();
            case O_PIECE -> new OPiece();
            case T_PIECE -> new TPiece();
            case S_PIECE -> new SPiece();
            case Z_PIECE -> new ZPiece();
            case L_PIECE -> new LPiece();
            case J_PIECE -> new JPiece();
            default -> throw new IllegalArgumentException("Unknown tetromino type: " + type); //for error prevention
        };
    }

    public static Tetromino createRandom() {
        if (bagIndex >= bag.size()) {
        refillBag(); //refils bag when traversed
        }
        return create(bag.get(bagIndex++));
        }

    private static void refillBag() {
        bag.clear();
        bag.addAll(Arrays.asList(ALL_TYPES));    //put all 7 types in
        Collections.shuffle(bag, random);        //shuffle true randomly
        bagIndex = 0;                            //reset the read head
        }

    //concrete subclasses
    static class IPiece extends Tetromino {
        @Override
        protected int[][][] defineRotations() {
            return new int[][][] {
                { {0,0,0,0}, {1,1,1,1}, {0,0,0,0}, {0,0,0,0} }, // 00 degs
                { {0,0,1,0}, {0,0,1,0}, {0,0,1,0}, {0,0,1,0} }, // 90 degs
                { {0,0,0,0}, {0,0,0,0}, {1,1,1,1}, {0,0,0,0} }, //180 degs
                { {0,1,0,0}, {0,1,0,0}, {0,1,0,0}, {0,1,0,0} }  //270 degs
            };
        }

        @Override
        protected Color defineColor() {
            return new Color(0, 240, 240); //cyan
        }

        @Override
        public String getType() { return I_PIECE; } //type checking (identifies piece)
    }

    static class OPiece extends Tetromino {
        @Override
        protected int[][][] defineRotations() {
            int[][] shape = { {0,1,1,0}, {0,1,1,0}, {0,0,0,0}, {0,0,0,0} }; //symmetric piece
            return new int[][][] { shape, shape, shape, shape };
        }

        @Override
        protected Color defineColor() {
            return new Color(240, 240, 0); //yellow
        }

        @Override
        public String getType() { return O_PIECE; }
    }

    static class TPiece extends Tetromino {
        @Override
        protected int[][][] defineRotations() {
            return new int[][][] {
                { {0,1,0,0}, {1,1,1,0}, {0,0,0,0}, {0,0,0,0} },
                { {0,1,0,0}, {0,1,1,0}, {0,1,0,0}, {0,0,0,0} },
                { {0,0,0,0}, {1,1,1,0}, {0,1,0,0}, {0,0,0,0} },
                { {0,1,0,0}, {1,1,0,0}, {0,1,0,0}, {0,0,0,0} }
            };
        }

        @Override
        protected Color defineColor() {
            return new Color(160, 0, 240); //purple
        }

        @Override
        public String getType() { return T_PIECE; }
    }

    static class SPiece extends Tetromino {
        @Override
        protected int[][][] defineRotations() {
            return new int[][][] {
                { {0,1,1,0}, {1,1,0,0}, {0,0,0,0}, {0,0,0,0} },
                { {0,1,0,0}, {0,1,1,0}, {0,0,1,0}, {0,0,0,0} },
                { {0,0,0,0}, {0,1,1,0}, {1,1,0,0}, {0,0,0,0} },
                { {1,0,0,0}, {1,1,0,0}, {0,1,0,0}, {0,0,0,0} }
            };
        }

        @Override
        protected Color defineColor() {
            return new Color(0, 240, 0); //green
        }

        @Override
        public String getType() { return S_PIECE; }
    }

    static class ZPiece extends Tetromino {
        @Override
        protected int[][][] defineRotations() {
            return new int[][][] {
                { {1,1,0,0}, {0,1,1,0}, {0,0,0,0}, {0,0,0,0} },
                { {0,0,1,0}, {0,1,1,0}, {0,1,0,0}, {0,0,0,0} },
                { {0,0,0,0}, {1,1,0,0}, {0,1,1,0}, {0,0,0,0} },
                { {0,1,0,0}, {1,1,0,0}, {1,0,0,0}, {0,0,0,0} }
            };
        }

        @Override
        protected Color defineColor() {
            return new Color(240, 0, 0); //red
        }

        @Override
        public String getType() { return Z_PIECE; }
    }

    static class LPiece extends Tetromino {
        @Override
        protected int[][][] defineRotations() {
            return new int[][][] {
                { {1,0,0,0}, {1,0,0,0}, {1,1,0,0}, {0,0,0,0} },
                { {0,0,0,0}, {1,1,1,0}, {1,0,0,0}, {0,0,0,0} },
                { {1,1,0,0}, {0,1,0,0}, {0,1,0,0}, {0,0,0,0} },
                { {0,0,1,0}, {1,1,1,0}, {0,0,0,0}, {0,0,0,0} }
            };
        }

        @Override
        protected Color defineColor() {
            return new Color(240, 160, 0); //orange
        }

        @Override
        public String getType() { return L_PIECE; }
    }

    static class JPiece extends Tetromino {
        @Override
        protected int[][][] defineRotations() {
            return new int[][][] {
                { {0,1,0,0}, {0,1,0,0}, {1,1,0,0}, {0,0,0,0} },
                { {1,0,0,0}, {1,1,1,0}, {0,0,0,0}, {0,0,0,0} },
                { {1,1,0,0}, {1,0,0,0}, {1,0,0,0}, {0,0,0,0} },
                { {0,0,0,0}, {1,1,1,0}, {0,0,1,0}, {0,0,0,0} }
            };
        }

        @Override
        protected Color defineColor() {
            return new Color(0, 0, 240); //blue
        }

        @Override
        public String getType() { return J_PIECE; }
    }   
}
