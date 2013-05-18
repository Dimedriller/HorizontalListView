package com.github.dimedriller.listview.diff;

/**
 ***********************************************************************************************************************
 * Shows atomic difference between two lists: element is inserted to list
 ***********************************************************************************************************************
 */
public class InsertDiffAtom implements DiffAtom {
    private final int mListPosition;
    private final int mAdapterPosition;

    public InsertDiffAtom(int listPosition, int adapterPosition) {
        mListPosition = listPosition;
        mAdapterPosition = adapterPosition;
    }

    public int getListPosition() {
        return mListPosition;
    }

    public int getAdapterPosition() {
        return mAdapterPosition;
    }

    @Override
    public String toString() {
        return "InsertDiffAtom { mListPosition = " + mListPosition + ", mAdapterPosition = " + mAdapterPosition + "}";
    }
}
