package com.aichess.engine.movegen;

public class MoveList {
    private int[] moves = new int[256];
    private int count = 0;

    public void add(int move) {
        moves[count++] = move;
    }

    public int get(int index) {
        return moves[index];
    }

    public int size() {
        return count;
    }

    public void clear() {
        count = 0;
    }

    public int[] getArray() {
        return moves;
    }
}
