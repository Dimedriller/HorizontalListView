package com.hlv.horizontallistview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Scroller;

import java.util.ArrayList;

public abstract class HorizontalAbsListView extends ViewGroup {
    private int mFirstAdapterItemIndex;
    private int mFirstItemOffset;
    private ArrayList<ItemInfo> mItems;
    private final Scroller mScroller;
    private final GestureDetector mGestureDetector;

    private Adapter mAdapter;
    private ViewCache mViewCache;
    private ArrayList<ItemInfo> mItemsCache;

    private final MoveChildrenRunnable mMoveRunnable = new MoveChildrenRunnable(this);

    protected HorizontalAbsListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mScroller = new Scroller(context);
        mGestureDetector = createGestureDetector(context);
    }

    protected HorizontalAbsListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScroller = new Scroller(context);
        mGestureDetector = createGestureDetector(context);
    }

    protected HorizontalAbsListView(Context context) {
        super(context);
        mScroller = new Scroller(context);
        mGestureDetector = createGestureDetector(context);
    }

    private GestureDetector createGestureDetector(Context context) {
        return new GestureDetector(
                context,
                new GestureDetector.OnGestureListener() {
                    @Override
                    public boolean onUp(MotionEvent e) {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void onShowPress(MotionEvent e) {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                        return startScroll(distanceX);
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        // No action
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        return startFling(velocityX);
                    }
                });
    }

    public void setAdapter(Adapter adapter) {
        mAdapter = adapter;
        if (adapter == null) {
            mViewCache = new ViewCache(0);
            mItemsCache = new ArrayList<ItemInfo>(0);
        }
        else {
            mViewCache = new ViewCache(adapter.getViewTypeCount());
            mItemsCache = new ArrayList<ItemInfo>();
        }
        removeAllViewsInLayout();

        mFirstAdapterItemIndex = 0;
        mFirstItemOffset = 0;
        mItems = new ArrayList<ItemInfo>();
        requestLayout();
    }

    @Override
    public abstract LayoutParams generateDefaultLayoutParams();

    @Override
    public boolean addViewInLayout(View child, int index, LayoutParams params, boolean preventRequestLayout) {
        return super.addViewInLayout(child, index, params, preventRequestLayout);
    }

    protected abstract int getItemInfoCount(Adapter adapter);

    protected abstract ItemInfo createItemInfo();

    private ItemInfo getItemInfo(int adapterIndex) {
        ArrayList<ItemInfo> cache = mItemsCache;
        ItemInfo itemInfo;
        if (cache.size() == 0)
            itemInfo = createItemInfo();
        else
            itemInfo = cache.remove(0) ;

        itemInfo.createItemViews(this, adapterIndex, mAdapter, mViewCache);
        return itemInfo;
    }

    private void recycleItemInfo(int index, ItemInfo itemInfo) {
        itemInfo.recycleItemViews(index, mAdapter, mViewCache);
    }

    private int addItemsToLeft() {
        return 0;
    }

    private int addItemsToRight() {
        return 0;
    }

    private int correctChildrenOffset() {
        return 0;
    }

    private int removeItemsFromLeft() {
        return 0;
    }

    private int removeItemsFromRight() {
        return 0;
    }

    private boolean moveChildren() {
        Scroller scroller = mScroller;
        boolean isScrollingFinished = scroller.computeScrollOffset();

        return !isScrollingFinished;
    }

    boolean startScroll(float dx) {
        return true;
    }

    boolean startFling(float velocityX) {
        return true;
    }

    @Override
    protected void onLayout(boolean isChanged, int l, int t, int r, int b) {
        if (mAdapter == null)
            return;

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int viewWidthWithoutPadding = getWidth() - paddingLeft - getPaddingRight();
        int viewHeightWithoutPadding = getHeight() - paddingTop - getPaddingBottom();
        int currentRight = mFirstItemOffset;

        ArrayList<ItemInfo> items = mItems;
        int adapterItemsCount = getItemInfoCount(mAdapter);
        int firstAdapterItemIndex = mFirstAdapterItemIndex;
        int currentIndex = firstAdapterItemIndex;

        while (  currentRight < viewWidthWithoutPadding
              && currentIndex < adapterItemsCount ) {
            int listItemIndex = currentIndex - firstAdapterItemIndex;
            ItemInfo currentItem;
            if (listItemIndex < items.size())
                currentItem = items.get(listItemIndex);
            else {
                currentItem = getItemInfo(currentIndex);
                items.add(currentItem);
                currentItem.addItemViews(this);
            }
            currentItem.measureViews(viewWidthWithoutPadding, viewHeightWithoutPadding);
            currentItem.layoutViews(paddingLeft + currentRight, paddingTop, paddingTop + viewHeightWithoutPadding);
            currentRight += currentItem.getWidth();
            currentIndex++;
        }

        if (currentRight < viewWidthWithoutPadding) {
            int currentLeft = mFirstItemOffset;
            int itemsFullWidth = currentRight - currentLeft;
            while (  itemsFullWidth < viewWidthWithoutPadding
                  && firstAdapterItemIndex > 0) {
                firstAdapterItemIndex--;
                ItemInfo item = getItemInfo(firstAdapterItemIndex);
                items.add(0, item);
                item.addItemViews(this);
                item.measureViews(viewWidthWithoutPadding, viewHeightWithoutPadding);
                itemsFullWidth += item.getWidth();
                currentLeft -= item.getWidth();
                item.layoutViews(paddingLeft + currentLeft, paddingTop, paddingTop + viewHeightWithoutPadding);

            }
            mFirstAdapterItemIndex = firstAdapterItemIndex;

            int horizontalOffset;
            if (itemsFullWidth < viewWidthWithoutPadding) {
                horizontalOffset = -currentLeft;
                mFirstItemOffset = 0;
            } else {
                horizontalOffset = viewWidthWithoutPadding - currentRight;
                mFirstItemOffset = viewWidthWithoutPadding - itemsFullWidth;
            }
            for(int counterItem = 0; counterItem < items.size(); counterItem++)
                items.get(counterItem).offsetViews(horizontalOffset);
        } else
            while (currentIndex - firstAdapterItemIndex < items.size()) {
                ItemInfo currentItem = items.remove(items.size() - 1);
                currentItem.removeItemViews(this);
                recycleItemInfo(firstAdapterItemIndex + items.size() - 1, currentItem);
            }
    }

    protected abstract static class ItemInfo {
        private int mWidth;
        private int mHeight;

        private int mLeft;

        protected void setWidth(int width) {
            mWidth = width;
        }

        public int getWidth() {
            return mWidth;
        }

        protected void setHeight(int height) {
            mHeight = height;
        }

        public int getHeight() {
            return mHeight;
        }

        public int getLeft() {
            return mLeft;
        }

        public int getRight() {
            return mLeft + mWidth;
        }

        public abstract void createItemViews(HorizontalAbsListView parent, int index, Adapter adapter, ViewCache viewCache);
        public abstract void addItemViews(HorizontalAbsListView parent);
        public abstract void removeItemViews(HorizontalAbsListView parent);
        public abstract void recycleItemViews(int index, Adapter adapter, ViewCache viewCache);

        public abstract void measureViews(int parentWidth, int parentHeight);

        protected abstract void onLayoutViews(int left, int top, int bottom);

        public void layoutViews(int left, int top, int bottom) {
            mLeft = left;
            onLayoutViews(left, top, bottom);
        }

        protected abstract void onOffsetViews(int dX);

        public void offsetViews(int dX) {
            mLeft += dX;
            onOffsetViews(dX);
        }
    }

    protected static class ViewCache {
        private final SparseArray<ArrayList<View>> mCache;

        private static final int CACHE_LIMIT = 10;

        public ViewCache(int typeCount) {
            mCache = new SparseArray<ArrayList<View>>(typeCount);
        }

        private ArrayList<View> getViewList(int typeID) {
            ArrayList<View> viewList = mCache.get(typeID);
            if (viewList == null) {
                viewList = new ArrayList<View>();
                mCache.append(typeID, viewList);
            }
            return viewList;
        }

        public View poll(int typeID) {
            ArrayList<View> viewList = getViewList(typeID);

            int listSize = viewList.size();
            if (listSize == 0)
                return null;

            View view = viewList.get(listSize - 1);
            viewList.remove(listSize - 1);
            return view;
        }

        public void offer(int typeID, View view) {
            ArrayList<View> viewList = getViewList(typeID);
            if (viewList.size() > CACHE_LIMIT)
                return;

            viewList.add(view);
        }
    }

    private static class MoveChildrenRunnable implements Runnable {
        private final HorizontalAbsListView mView;

        private MoveChildrenRunnable(HorizontalAbsListView view) {
            mView = view;
        }

        public void start() {
            mView.post(this);
        }

        @Override
        public void run() {
            boolean scrollFurther = mView.moveChildren();
            if (scrollFurther)
                mView.post(this);
        }
    }
}
