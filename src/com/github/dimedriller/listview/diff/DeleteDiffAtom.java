package com.github.dimedriller.listview.diff;

/**
 ***********************************************************************************************************************
 * Shows atomic difference between two lists: element is deleted from list
 ***********************************************************************************************************************
 */
public class DeleteDiffAtom implements DiffAtom {
    private final int mListPosition;

    public DeleteDiffAtom(int listPosition) {
        mListPosition = listPosition;
    }

    public int getListPosition() {
        return mListPosition;
    }

    @Override
    public String toString() {
        return "DeleteDiffAtom { mListPosition = " + mListPosition + "}";
    }
}
