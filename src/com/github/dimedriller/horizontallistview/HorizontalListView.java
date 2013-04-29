package com.github.dimedriller.horizontallistview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

/***********************************************************************************************************************
 * This class is designed to display horizontal list of elements
 **********************************************************************************************************************/
public class HorizontalListView extends HorizontalAbsListView {
    public HorizontalListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalListView(Context context) {
        super(context);
    }

    @Override
    public LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected int getItemInfoCount(Adapter adapter) {
        return adapter.getCount();
    }

    @Override
    protected ItemInfo createItemInfo() {
        return new ListItemInfo();
    }

    private static class ListItemInfo extends HorizontalAbsListView.ItemInfo {
        private View mView;

        @Override
        public void createItemViews(HorizontalAbsListView parent, int itemIndex, Adapter adapter, HorizontalAbsListView.ViewCache viewCache) {
            int viewType = adapter.getItemViewType(itemIndex);
            View cachedView = viewCache.poll(viewType);
            mView = adapter.getView(itemIndex, cachedView, parent);
        }

        @Override
        public void addItemViews(HorizontalAbsListView parent) {
            ViewGroup.LayoutParams params = mView.getLayoutParams();
            if (params == null) {
                params = parent.generateDefaultLayoutParams();
                mView.setLayoutParams(params);
            }
            parent.addViewInLayout(mView, -1, params, true);
        }

        @Override
        public void removeItemViews(HorizontalAbsListView parent) {
            parent.removeViewInLayout(mView);
        }

        @Override
        public void recycleItemViews(int itemIndex, Adapter adapter, HorizontalAbsListView.ViewCache viewCache) {
            int viewType = adapter.getItemViewType(itemIndex);
            viewCache.offer(viewType, mView);
            mView = null;
        }

        @Override
        public void measureViewsBySpecs(int parentSpecWidth, int paddingHorizontal, int parentSpecHeight, int paddingVertical) {
            LayoutParams params = mView.getLayoutParams();
            int childSpecWidth = getChildMeasureSpec(parentSpecWidth, paddingHorizontal, params.width);
            int childSpecHeight = getChildMeasureSpec(parentSpecHeight, paddingVertical, params.height);

            mView.measure(childSpecWidth, childSpecHeight);
            setWidth(mView.getMeasuredWidth());
            setHeight(mView.getMeasuredHeight());
        }

        @Override
        public void measureViews(int parentWidth, int parentHeight) {
            int parentSpecWidth = MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.EXACTLY);
            int parentSpecHeight = MeasureSpec.makeMeasureSpec(parentHeight, MeasureSpec.EXACTLY);

            measureViewsBySpecs(parentSpecWidth, 0, parentSpecHeight, 0);
        }

        @Override
        protected void onLayoutViews(int left, int top) {
            mView.layout(left, top, left + getWidth(), top + getHeight());
        }

        @Override
        protected void onOffsetViews(int dX) {
            mView.offsetLeftAndRight(dX);
        }

        @Override
        public void showPressed(int touchX, int touchY) {
            mView.setPressed(true);
        }

        @Override
        public void hidePressed() {
            mView.setPressed(false);
        }

        @Override
        public int findAdapterItemIndex(int index, int x, int y) {
            return index;
        }

        @Override
        public View findAdapterViewItem(int x, int y) {
            return mView;
        }
    }
}
