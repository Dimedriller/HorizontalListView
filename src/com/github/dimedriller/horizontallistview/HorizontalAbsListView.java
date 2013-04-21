package com.github.dimedriller.horizontallistview;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import com.github.dimedriller.widget.Scroller;
import com.github.dimedriller.widget.GestureDetector;

public abstract class HorizontalAbsListView extends ViewGroup {
    private int mFirstAdapterItemIndex;
    private ArrayList<ItemInfo> mItems;
    private final Scroller mScroller;
    private final GestureDetector mGestureDetector;

    private Adapter mAdapter;
    private ViewCache mViewCache;
    private ArrayList<ItemInfo> mItemsCache;

    private final MoveChildrenRunnable mMoveRunnable = new MoveChildrenRunnable(this);

    private static final float VELOCITY_X_RATIO = 0.5f;

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
                        stopScrolling();
                        return true;  //To change body of implemented methods use File | Settings | File Templates.
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
                        Log.d("HorizontalListView", "distance = " + distanceX);
                        return startScroll(distanceX);
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        // No action
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        return startFling(-velocityX * VELOCITY_X_RATIO);
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

    void stopScrolling() {
        mScroller.forceFinished(true);
        removeCallbacks(mMoveRunnable);
    }

    private int getFirstItemOffset() {
        ArrayList<ItemInfo> items = mItems;
        if (items.size() == 0)
            return  0;
        else
            return items.get(0).getLeft() - getPaddingLeft();
    }

    private int getWidthWithoutPaddings() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int getDisplayItemsFullWidth() {
        int fullWidth = 0;
        ArrayList<ItemInfo> items = mItems;
        int itemsCount = items.size();
        for(int counterItem = 0; counterItem < itemsCount; counterItem++)
            fullWidth += items.get(counterItem).getWidth();
        return fullWidth;
    }

    private void shiftItems(int dX) {
        ArrayList<ItemInfo> items = mItems;
        int itemsCount = items.size();
        for(int counterItem = 0; counterItem < itemsCount; counterItem++)
            items.get(counterItem).offsetViews(dX);
    }

    private boolean moveChildrenLeft(int dX) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int viewWidthWithoutPadding = getWidthWithoutPaddings();
        int viewHeightWithoutPadding = getHeight() - paddingTop - getPaddingBottom();

        int firstItemX = getFirstItemOffset();

        ArrayList<ItemInfo> items = mItems;
        int firstAdapterItemIndex = mFirstAdapterItemIndex;
        int nextItemIndex = firstAdapterItemIndex + items.size();
        int countAdapterItems = getItemInfoCount(mAdapter);
        int currentRight = firstItemX - dX + getDisplayItemsFullWidth();

        while (  currentRight < viewWidthWithoutPadding
              && nextItemIndex < countAdapterItems) {
            ItemInfo newItem = getItemInfo(nextItemIndex);
            items.add(newItem);
            newItem.addItemViews(this);
            newItem.measureViews(viewWidthWithoutPadding, viewHeightWithoutPadding);
            newItem.layoutViews(paddingLeft + currentRight + dX, paddingTop, paddingTop + viewHeightWithoutPadding);
            currentRight += newItem.getWidth();
            nextItemIndex++;
        }

        boolean forceFinishing;
        if (currentRight < viewWidthWithoutPadding) {
            forceFinishing = true;
            dX -= viewWidthWithoutPadding - currentRight;
            if (dX < 0)
                dX = 0;
        } else
            forceFinishing = false;

        int currentLeft = firstItemX - dX;
        ItemInfo itemToRemove = items.get(0);
        while (currentLeft + itemToRemove.getWidth() < 0) {
            currentLeft += itemToRemove.getWidth();
            items.remove(0);
            itemToRemove.removeItemViews(this);
            recycleItemInfo(firstAdapterItemIndex, itemToRemove);
            itemToRemove = items.get(0);
            firstAdapterItemIndex++;
        }
        mFirstAdapterItemIndex = firstAdapterItemIndex;

        shiftItems(-dX);

        return forceFinishing;
    }

    private boolean moveChildrenRight(int dX) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int viewWidthWithoutPadding = getWidthWithoutPaddings();
        int viewHeightWithoutPadding = getHeight() - paddingTop - getPaddingBottom();

        int firstItemX = getFirstItemOffset();

        ArrayList<ItemInfo> items = mItems;
        int firstAdapterItemIndex = mFirstAdapterItemIndex;
        int nextItemIndex = firstAdapterItemIndex - 1;
        int currentLeft = firstItemX - dX;

        while (  currentLeft >= 0
              && nextItemIndex >= 0) {
            ItemInfo newItem = getItemInfo(nextItemIndex);
            items.add(0, newItem);
            newItem.addItemViews(this);
            newItem.measureViews(viewWidthWithoutPadding, viewHeightWithoutPadding);
            newItem.layoutViews(paddingLeft + currentLeft + dX - newItem.getWidth(), paddingTop, paddingTop + viewHeightWithoutPadding);
            currentLeft -= newItem.getWidth();
            nextItemIndex--;
        }
        firstAdapterItemIndex = nextItemIndex + 1;
        mFirstAdapterItemIndex = firstAdapterItemIndex;

        boolean forceFinishing;
        if (currentLeft > 0) {
            forceFinishing = true;
            dX += currentLeft;
        } else
            forceFinishing = false;

        int currentRight = firstItemX - dX + getDisplayItemsFullWidth();
        ItemInfo itemToRemove = items.get(items.size() - 1);
        while (currentRight - itemToRemove.getWidth() > viewWidthWithoutPadding) {
            currentRight -= itemToRemove.getWidth();
            items.remove(items.size() - 1);
            itemToRemove.removeItemViews(this);
            recycleItemInfo(firstAdapterItemIndex + items.size(), itemToRemove);
            itemToRemove = items.get(items.size() - 1);
        }

        shiftItems(-dX);

        return forceFinishing;
    }

    private void moveChildren() {
        Scroller scroller = mScroller;
        int startX = scroller.getCurrX();
        scroller.computeScrollOffset();
        int endX = scroller.getCurrX();
        int deltaX = endX - startX;

        Log.d("HorizontalListView", "deltaX = " + deltaX + "(startX = " + startX + " endX = " + endX + ")");
        boolean forceFinished = false;
        if (deltaX > 0)
            forceFinished = moveChildrenLeft(deltaX);
        else if (deltaX < 0)
            forceFinished = moveChildrenRight(deltaX);

        if (forceFinished)
            scroller.forceFinished(true);

        if (!scroller.isFinished())
            post(mMoveRunnable);
    }

    boolean startScroll(float dx) {
        mScroller.startScroll(0, 0, Math.round(dx), 0, 0);
        moveChildren();
        return true;
    }

    boolean startFling(float velocityX) {
        mScroller.fling(0, 0, Math.round(velocityX), 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0);
        moveChildren();
        return true;
    }

    @Override
    protected void onLayout(boolean isChanged, int l, int t, int r, int b) {
        if (mAdapter == null)
            return;

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int viewWidthWithoutPadding = getWidthWithoutPaddings();
        int viewHeightWithoutPadding = getHeight() - paddingTop - getPaddingBottom();

        ArrayList<ItemInfo> items = mItems;
        int firstItemOffset = getFirstItemOffset();
        int currentRight = firstItemOffset;


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
            int currentLeft = firstItemOffset;
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
            if (itemsFullWidth < viewWidthWithoutPadding)
                horizontalOffset = -currentLeft;
            else
                horizontalOffset = viewWidthWithoutPadding - currentRight;
            shiftItems(horizontalOffset);
        } else
            while (currentIndex - firstAdapterItemIndex < items.size()) {
                ItemInfo currentItem = items.remove(items.size() - 1);
                currentItem.removeItemViews(this);
                recycleItemInfo(firstAdapterItemIndex + items.size() - 1, currentItem);
            }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
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

        @Override
        public void run() {
            mView.moveChildren();
        }
    }
}
