package com.hlv.horizontallistview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

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
        public void measureViews(int parentWidth, int parentHeight) {
            LayoutParams params = mView.getLayoutParams();

            int parentSpecWidth = MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.EXACTLY);
            int childSpecWidth = getChildMeasureSpec(parentSpecWidth, 0, params.width);

            int parentSpecHeight = MeasureSpec.makeMeasureSpec(parentHeight, MeasureSpec.EXACTLY);
            int childSpecHeight = getChildMeasureSpec(parentSpecHeight, 0, params.height);

            mView.measure(childSpecWidth, childSpecHeight);
            setWidth(mView.getMeasuredWidth());
            setHeight(mView.getMeasuredHeight());
        }

        @Override
        protected void onLayoutViews(int left, int top, int bottom) {
            mView.layout(left, top, left + getWidth(), bottom);
        }

        @Override
        protected void onOffsetViews(int dX) {
            mView.offsetLeftAndRight(dX);
        }
    }
}
