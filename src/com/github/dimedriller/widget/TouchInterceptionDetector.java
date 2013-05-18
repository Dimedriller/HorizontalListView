package com.github.dimedriller.widget;

import android.view.MotionEvent;

/***********************************************************************************************************************
 * This class is designed to analyse touch events and make decision if an event is intercepted
 **********************************************************************************************************************/
public abstract class TouchInterceptionDetector {
    private int mPreviousPointerID;
    private float mPreviousX;
    private float mPreviousY;

    protected abstract boolean onDoInterception(float previousX, float previousY, float currentX, float currentY);

    public boolean doInterception(MotionEvent event) {
        int eventAction = event.getAction() & MotionEvent.ACTION_MASK;

        if (eventAction == MotionEvent.ACTION_DOWN) {
            mPreviousPointerID = event.getPointerId(0);
            mPreviousX = event.getX();
            mPreviousY = event.getY();
            return false;
        }
        if (eventAction == MotionEvent.ACTION_MOVE) {
            int pointerIndex = event.findPointerIndex(mPreviousPointerID);
            if (pointerIndex == -1) {
                mPreviousPointerID = event.getPointerId(0);
                return false;
            }

            float x = event.getX(pointerIndex);
            float y = event.getY(pointerIndex);
            boolean doInterception = onDoInterception(mPreviousX, mPreviousY, x, y);
            mPreviousX = x;
            mPreviousY = y;
            return doInterception;
        }
        if (eventAction == MotionEvent.ACTION_POINTER_UP) {
            int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
                    MotionEvent.ACTION_POINTER_INDEX_SHIFT;
            int pointerID = event.getPointerId(pointerIndex);
            if (pointerID == mPreviousPointerID){
                int newPointerIndex;
                if (pointerIndex == 0)
                    newPointerIndex = 1;
                else
                    newPointerIndex = 0;
                mPreviousPointerID = event.getPointerId(newPointerIndex);
                mPreviousX = event.getX(newPointerIndex);
                mPreviousY = event.getY(newPointerIndex);
            }
            return false;
        }
        if (eventAction == MotionEvent.ACTION_UP) {
            mPreviousPointerID = -1;
            mPreviousX = 0;
            mPreviousY = 0;
            return false;
        }
        return false;
    }
}
