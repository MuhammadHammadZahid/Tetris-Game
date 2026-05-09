// package com.mycompany.tetrisgame;

import java.awt.Color; //piece colours

//abstract base class at template for all 7 concrete tetrominos to implement
public abstract class Tetromino {

    //fields
    protected int[][] shape; //4x4 grid for tetromino (1 = filled, 0 = empty)
    protected Color color;
    protected int rotationState; //tracks piece orientation
    
    //coordinates of all rotation states' of the piece (4 rotations)
    protected int[][][] rotations;

    public Tetromino() { //constructing tetromino
        this.rotationState = 0;
        this.rotations = defineRotations();
        this.shape = rotations[0];
        this.color = defineColor();
    }

    protected abstract int[][][] defineRotations();
    /*only accessible by subclasses
    defines piece coordinates on a 4x4 grid at four angles*/

    protected abstract Color defineColor(); //RGB colour value

    public abstract String getType(); //gives piece name

    //functions
    public void rotateClockwise() { //cycles through rotationstates
        rotationState = (rotationState + 1) % 4;
        shape = rotations[rotationState];
    }

    public void rotateCounterClockwise() { //cycles rotation states backwards
        rotationState = (rotationState + 3) % 4;
        shape = rotations[rotationState];
    }

    public int[][] getShape() { //draws piece, checks for collisions
        return shape;
    }

    public Color getColor() {
        return color;
    }

    public int getRotationState() {
        return rotationState;
    }

    /**
     * Returns a deep copy of the shape for preview or ghost rendering.
     DEAD CODE FOR LATER IMPLEMENTATION
    public int[][] getShapeCopy() {
        int[][] copy = new int[shape.length][];
        for (int i = 0; i < shape.length; i++) {
            copy[i] = shape[i].clone();
        }
        return copy;
    }
    */
    public int[][] getPreviewShape() { //previews next piece
        return rotations[0];
    }

    @Override
    public String toString() {
        return "Tetromino[" + getType() + ", rotation=" + rotationState + "]";
    }
}
