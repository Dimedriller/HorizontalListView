package com.dimedriller.alternativeui.widget;

import android.content.Context;
import android.view.MotionEvent;

/***********************************************************************************************************************
 * This is class designed to analyse touch events and launch callbacks which correspond to specific user gestures.
 * Besides standard Android @link android.view.GestureDetector gestures the class allows to detect onUp event.
 **********************************************************************************************************************/
public class GestureDetector extends android.view.GestureDetector {
    private OnGestureListener mGestureListener;

    public GestureDetector(Context context, OnGestureListener listener) {
        super(context, listener);
        mGestureListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean wasConsumed = super.onTouchEvent(ev);

        if (wasConsumed)
            return true;

        if (ev.getAction() == MotionEvent.ACTION_UP)
            return mGestureListener.onUp(ev);
        else
            return false;
    }

    public interface OnGestureListener extends android.view.GestureDetector.OnGestureListener {
        public boolean onUp(MotionEvent e);
    }
}
