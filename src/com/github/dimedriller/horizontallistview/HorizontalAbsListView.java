package com.github.dimedriller.horizontallistview;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;

import com.github.R;
import com.github.dimedriller.widget.Scroller;
import com.github.dimedriller.widget.GestureDetector;
import com.github.dimedriller.widget.TouchInterceptionDetector;

/***********************************************************************************************************************
 * This class is base class to provide horizontal list scrolling logic
 **********************************************************************************************************************/
public abstract class HorizontalAbsListView extends AdapterView<Adapter> {
    private int mFirstGlobalItemIndex;
    private ArrayList<ItemInfo> mItems;

    private Adapter mAdapter;
    private ViewCache mViewCache;
    private ArrayList<ItemInfo> mItemsCache;

    private final Scroller mScroller;
    private final GestureDetector mGestureDetector;
    private final MoveChildrenRunnable mMoveRunnable = new MoveChildrenRunnable(this);
    private final TouchInterceptionDetector mTouchInterceptionDetector = new TouchInterceptionDetector() {
        @Override
        protected boolean onDoInterception(float previousX, float previousY, float currentX, float currentY) {
            return Math.abs(currentX - previousX) > Math.abs(currentY - previousY);
        }
    };

    private final EdgeEffectCompat mLeftFadingEdge;
    private final EdgeEffectCompat mRightFadingEdge;

    private static final float VELOCITY_X_RATIO = 0.5f;

    protected HorizontalAbsListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mScroller = new Scroller(context);
        mGestureDetector = createGestureDetector(context);

        mLeftFadingEdge = new EdgeEffectCompat(context);
        mRightFadingEdge = new EdgeEffectCompat(context);

        initView(context);
    }

    protected HorizontalAbsListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScroller = new Scroller(context);
        mGestureDetector = createGestureDetector(context);

        mLeftFadingEdge = new EdgeEffectCompat(context);
        mRightFadingEdge = new EdgeEffectCompat(context);

        initView(context);
    }

    protected HorizontalAbsListView(Context context) {
        super(context);
        mScroller = new Scroller(context);
        mGestureDetector = createGestureDetector(context);

        mLeftFadingEdge = new EdgeEffectCompat(context);
        mRightFadingEdge = new EdgeEffectCompat(context);

        initView(context);
    }

    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(
                context,
                new GestureDetector.OnGestureListener() {
                    @Override
                    public boolean onUp(MotionEvent e) {
                        hidePressedState();
                        return true;
                    }

                    @Override
                    public boolean onDown(MotionEvent e) {
                        stopScrolling();
                        return true;
                    }

                    @Override
                    public void onShowPress(MotionEvent e) {
                        int x = Math.round(e.getX());
                        int y = Math.round(e.getY());

                        ItemInfo pressedItem = findItemInfoByXY(x, y);
                        if (pressedItem != null)
                            pressedItem.showPressed(x, y);
                    }

                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        hidePressedState();

                        int x = Math.round(e.getX());
                        int y = Math.round(e.getY());
                        handleItemTap(x, y);
                        return true;
                    }

                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                        Log.d("HorizontalListView", "distance = " + distanceX);
                        awakenScrollBars();
                        return startScroll(distanceX);
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        // No action
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        awakenScrollBars();
                        return startFling(-velocityX * VELOCITY_X_RATIO);
                    }
                });
        gestureDetector.setIsLongpressEnabled(false);
        return gestureDetector;
    }

    private void initView(Context context) {
        setWillNotDraw(false);
        setHorizontalFadingEdgeEnabled(true);
        setHorizontalScrollBarEnabled(true);

        try {
            TypedArray scrollBarStyle = context.getTheme().obtainStyledAttributes(R.styleable.ViewScrollBar);
            initializeScrollbars(scrollBarStyle);
            scrollBarStyle.recycle();
        } catch (Exception exc) {
            // On some devices stylable attributes for scrollbars can be absent
        }
    }

    @Override
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

        mFirstGlobalItemIndex = 0;
        mItems = new ArrayList<ItemInfo>();
        requestLayout();
    }

    @Override
    public Adapter getAdapter() {
        return mAdapter;
    }

    @Override
    public View getSelectedView() {
        // TODO Implement this method
        return null;
    }

    @Override
    public void setSelection(int position) {
        // TODO: Implement this method
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelable = super.onSaveInstanceState();
        ListState listState = new ListState(parcelable);
        listState.setFirstItemIndex(mFirstGlobalItemIndex);
        return listState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        ListState listState = (ListState) state;
        super.onRestoreInstanceState(listState.getSuperState());
        mFirstGlobalItemIndex = listState.getFirstItemIndex();
    }

    @Override
    public abstract LayoutParams generateDefaultLayoutParams();

    @Override
    public boolean addViewInLayout(View child, int index, LayoutParams params, boolean preventRequestLayout) {
        return super.addViewInLayout(child, index, params, preventRequestLayout);
    }

    protected abstract int getItemInfoCount(Adapter adapter);

    protected abstract ItemInfo createItemInfo();

    private ItemInfo getItemInfo(int globalIndex) {
        ArrayList<ItemInfo> cache = mItemsCache;
        ItemInfo itemInfo;
        if (cache.size() == 0)
            itemInfo = createItemInfo();
        else
            itemInfo = cache.remove(0) ;

        itemInfo.createItemViews(this, globalIndex, mAdapter, mViewCache);
        itemInfo.addItemViews(this);
        return itemInfo;
    }

    private void recycleItemInfo(int globalIndex, ItemInfo itemInfo) {
        itemInfo.removeItemViews(this);
        itemInfo.recycleItemViews(globalIndex, mAdapter, mViewCache);
        mItemsCache.add(itemInfo);
    }

    private int findItemInfoIndexByXY(int x, int y) {
        ArrayList<ItemInfo> items = mItems;
        int countItems = items.size();
        for(int counter = 0; counter < countItems; counter++)
            if (items.get(counter).containsXY(x, y))
                return counter;
        return -1;
    }

    private ItemInfo findItemInfoByXY(int x, int y) {
        int itemIndex = findItemInfoIndexByXY(x, y);
        if (itemIndex == -1)
            return null;
        else
            return mItems.get(itemIndex);
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

    private int getHeightWithoutPaddings() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
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

    private int measureAndLayoutItemLeft(ItemInfo item, int itemRightX) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int viewWidthWithoutPadding = getWidthWithoutPaddings();
        int viewHeightWithoutPadding = getHeight() - paddingTop - getPaddingBottom();

        item.measureViews(viewWidthWithoutPadding, viewHeightWithoutPadding);
        item.layoutViews(paddingLeft + itemRightX - item.getWidth(), paddingTop);
        return item.getWidth();

    }

    private int measureAndLayoutItemRight(ItemInfo item, int itemLeftX) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int viewWidthWithoutPadding = getWidthWithoutPaddings();
        int viewHeightWithoutPadding = getHeight() - paddingTop - getPaddingBottom();

        item.measureViews(viewWidthWithoutPadding, viewHeightWithoutPadding);
        item.layoutViews(paddingLeft + itemLeftX, paddingTop);
        return item.getWidth();
    }

    private boolean moveChildrenLeft(int dX) {
        int viewWidthWithoutPadding = getWidthWithoutPaddings();
        int firstItemX = getFirstItemOffset();

        ArrayList<ItemInfo> items = mItems;
        int firstGlobalItemIndex = mFirstGlobalItemIndex;
        int nextItemIndex = firstGlobalItemIndex + items.size();
        int countGlobalItems = getItemInfoCount(mAdapter);
        int currentRight = firstItemX - dX + getDisplayItemsFullWidth();

        while (  currentRight < viewWidthWithoutPadding
              && nextItemIndex < countGlobalItems) {
            ItemInfo newItem = getItemInfo(nextItemIndex);
            items.add(newItem);
            currentRight += measureAndLayoutItemRight(newItem, currentRight + dX);
            nextItemIndex++;
        }

        boolean forceFinishing;
        if (currentRight < viewWidthWithoutPadding) {
            forceFinishing = true;
            int extraDelta = viewWidthWithoutPadding - currentRight;
            dX -= extraDelta;
            mRightFadingEdge.onPull((float) extraDelta / viewWidthWithoutPadding);
            if (dX < 0)
                dX = 0;
        } else
            forceFinishing = false;

        int currentLeft = firstItemX - dX;
        ItemInfo itemToRemove = items.get(0);
        while (currentLeft + itemToRemove.getWidth() < 0) {
            currentLeft += itemToRemove.getWidth();
            items.remove(0);
            recycleItemInfo(firstGlobalItemIndex, itemToRemove);
            itemToRemove = items.get(0);
            firstGlobalItemIndex++;
        }
        mFirstGlobalItemIndex = firstGlobalItemIndex;

        shiftItems(-dX);
        return forceFinishing;
    }

    private boolean moveChildrenRight(int dX) {
        int firstItemX = getFirstItemOffset();

        ArrayList<ItemInfo> items = mItems;
        int firstGlobalItemIndex = mFirstGlobalItemIndex;
        int nextItemIndex = firstGlobalItemIndex - 1;
        int currentLeft = firstItemX - dX;

        while (  currentLeft >= 0
              && nextItemIndex >= 0) {
            ItemInfo newItem = getItemInfo(nextItemIndex);
            items.add(0, newItem);
            currentLeft -= measureAndLayoutItemLeft(newItem, currentLeft + dX);
            nextItemIndex--;
        }
        firstGlobalItemIndex = nextItemIndex + 1;
        mFirstGlobalItemIndex = firstGlobalItemIndex;

        boolean forceFinishing;
        int viewWidthWithoutPadding = getWidthWithoutPaddings();
        if (currentLeft > 0) {
            forceFinishing = true;
            dX += currentLeft;
            mLeftFadingEdge.onPull((float) currentLeft / viewWidthWithoutPadding);
            currentLeft = 0;
        } else
            forceFinishing = false;

        int currentRight = currentLeft + getDisplayItemsFullWidth();
        int indexToRemove = items.size() - 1;
        ItemInfo itemToRemove = items.get(indexToRemove);

        while (currentRight - itemToRemove.getWidth() > viewWidthWithoutPadding) {
            currentRight -= itemToRemove.getWidth();
            items.remove(indexToRemove);
            recycleItemInfo(firstGlobalItemIndex + indexToRemove, itemToRemove);
            indexToRemove--;
            itemToRemove = items.get(indexToRemove);
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
        int widthWithoutPaddings = getWidthWithoutPaddings();

        Log.d("HorizontalListView", "deltaX = " + deltaX + "(startX = " + startX + " endX = " + endX + ")");
        boolean forceFinished = false;
        if (deltaX > 0) {
            if (deltaX > widthWithoutPaddings)
                deltaX = widthWithoutPaddings;
            forceFinished = moveChildrenLeft(deltaX);
        }
        else if (deltaX < 0) {
            if (deltaX < -widthWithoutPaddings)
                deltaX = -widthWithoutPaddings;
            forceFinished = moveChildrenRight(deltaX);
        }
        invalidate();

        if (forceFinished)
            scroller.forceFinished(true);
        if (!scroller.isFinished())
            post(mMoveRunnable);
    }

    private void hidePressedState() {
        ArrayList<ItemInfo> items = mItems;
        int countItems = mItems.size();
        for(int counter = 0; counter < countItems; counter++)
            items.get(counter).hidePressed();
    }

    boolean startScroll(float dx) {
        if (mAdapter == null)
            return true;

        hidePressedState();
        mScroller.startScroll(0, 0, Math.round(dx), 0, 0);
        moveChildren();
        return true;
    }

    boolean startFling(float velocityX) {
        if (mAdapter == null)
            return true;

        hidePressedState();
        mScroller.fling(0, 0, Math.round(velocityX), 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0);
        moveChildren();
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mAdapter == null)
            return;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int horizontalPaddings = getPaddingLeft() + getPaddingRight();
        int verticalPaddings = getPaddingTop() + getPaddingBottom();

        int childWidth = 0;
        int childHeight = 0;
        int itemsCount = getItemInfoCount(mAdapter);
        if ( itemsCount > 0
           &&(  widthMode != MeasureSpec.EXACTLY
             || heightMode != MeasureSpec.EXACTLY)) {
            ArrayList<ItemInfo> existingItems = mItems;
            ItemInfo child;
            if (existingItems.size() > 0)
                child = existingItems.get(0);
            else
                child = getItemInfo(0);

            childWidth = child.getWidth();
            child.measureViewsBySpecs(widthMeasureSpec, horizontalPaddings, heightMeasureSpec, verticalPaddings);
            childHeight = child.getHeight();

            if (existingItems.size() == 0)
                recycleItemInfo(0, child);
        }

        if (widthMode == MeasureSpec.AT_MOST) {
            int newWidthSize = horizontalPaddings + childWidth * itemsCount;
            if (newWidthSize < widthSize)
                widthSize = newWidthSize;
        }
        if (heightMode == MeasureSpec.AT_MOST) {
            int newHeightSize = verticalPaddings + childHeight;
            if (newHeightSize < heightSize)
                heightSize = newHeightSize;
        }
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean isChanged, int l, int t, int r, int b) {
        if (mAdapter == null)
            return;

        int viewWidthWithoutPadding = getWidthWithoutPaddings();
        ArrayList<ItemInfo> items = mItems;
        int firstItemOffset = getFirstItemOffset();
        int currentRight = firstItemOffset;


        int globalItemsCount = getItemInfoCount(mAdapter);
        int firstGlobalItemIndex = mFirstGlobalItemIndex;
        int currentIndex = firstGlobalItemIndex;

        while (  currentRight < viewWidthWithoutPadding
              && currentIndex < globalItemsCount ) {
            int listItemIndex = currentIndex - firstGlobalItemIndex;
            ItemInfo currentItem;
            if (listItemIndex < items.size())
                currentItem = items.get(listItemIndex);
            else {
                currentItem = getItemInfo(currentIndex);
                items.add(currentItem);
            }
            currentRight += measureAndLayoutItemRight(currentItem, currentRight);
            currentIndex++;
        }

        if (currentRight < viewWidthWithoutPadding) {
            int currentLeft = firstItemOffset;
            int itemsFullWidth = currentRight - currentLeft;
            while (  itemsFullWidth < viewWidthWithoutPadding
                  && firstGlobalItemIndex > 0) {
                firstGlobalItemIndex--;
                ItemInfo item = getItemInfo(firstGlobalItemIndex);
                items.add(0, item);
                currentLeft -= measureAndLayoutItemLeft(item, currentLeft);
                itemsFullWidth += item.getWidth();
            }
            mFirstGlobalItemIndex = firstGlobalItemIndex;

            int horizontalOffset;
            if (itemsFullWidth < viewWidthWithoutPadding)
                horizontalOffset = -currentLeft;
            else
                horizontalOffset = viewWidthWithoutPadding - currentRight;
            shiftItems(horizontalOffset);
        } else
            while (currentIndex - firstGlobalItemIndex < items.size()) {
                ItemInfo currentItem = items.remove(items.size() - 1);
                recycleItemInfo(firstGlobalItemIndex + items.size() - 1, currentItem);
            }
    }

    private boolean drawEdge(Canvas canvas, EdgeEffectCompat edge, float rotation, float offsetX, float offsetY) {
        int restoreCount = canvas.save();
        canvas.rotate(rotation);
        canvas.translate(offsetX, offsetY);
        edge.setSize(getHeightWithoutPaddings(), getWidthWithoutPaddings());

        boolean doInvalidate = edge.draw(canvas);
        canvas.restoreToCount(restoreCount);

        return doInvalidate;
    }

    private void drawEdges(Canvas canvas) {
        int overscrollMode = ViewCompat.getOverScrollMode(this);

        if (  overscrollMode == ViewCompat.OVER_SCROLL_ALWAYS
           || overscrollMode == ViewCompat.OVER_SCROLL_IF_CONTENT_SCROLLS) {
            boolean doInvalidate = false;
            EdgeEffectCompat leftEdge = mLeftFadingEdge;
            if (!leftEdge.isFinished())
                doInvalidate |= drawEdge(canvas, leftEdge, 270f, -getHeight() + getPaddingTop(), getPaddingLeft());
            EdgeEffectCompat rightEdge = mRightFadingEdge;
            if (!rightEdge.isFinished())
                doInvalidate |= drawEdge(canvas, rightEdge, 90f, getPaddingTop(), getPaddingLeft() - getWidth());

            if (doInvalidate)
                ViewCompat.postInvalidateOnAnimation(this);
        }
            }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawEdges(canvas);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mTouchInterceptionDetector.doInterception(ev)  ;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        if (mFirstGlobalItemIndex != 0)
            return 1.0f;

        ArrayList<ItemInfo> items = mItems;
        if (items.size() == 0)
            return 0.0f;
       return (float) (-getFirstItemOffset() - getPaddingLeft()) / items.get(0).getWidth();
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        ArrayList<ItemInfo> items = mItems;
        int itemsGlobalCount = getItemInfoCount(mAdapter);
        int itemsVisibleCount = items.size();

        if (mFirstGlobalItemIndex + itemsVisibleCount != itemsGlobalCount)
            return 1.0f;

        if (itemsVisibleCount == 0)
            return 0.0f;
        ItemInfo lastItem = items.get(itemsVisibleCount - 1);
        return (float) (lastItem.getRight() - getWidth() + getPaddingRight()) / lastItem.getWidth();
    }

    @Override
    protected int computeHorizontalScrollExtent() {
        return getWidthWithoutPaddings();
    }

    @Override
    protected int computeHorizontalScrollRange() {
        ArrayList<ItemInfo> items = mItems;
        if (items.size() == 0)
            return 0;

        return items.get(0).getWidth() * mAdapter.getCount();
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        ArrayList<ItemInfo> items = mItems;
        if (items.size() == 0)
            return 0;

        return items.get(0).getWidth() * mFirstGlobalItemIndex;
    }

    void handleItemTap(int x, int y) {
        int tappedItemIndex = findItemInfoIndexByXY(x, y);
        if (tappedItemIndex == -1)
            return;

        ItemInfo tappedItem = mItems.get(tappedItemIndex);
        int adapterIndex = tappedItem.findAdapterItemIndex(mFirstGlobalItemIndex + tappedItemIndex, x, y);
        View adapterView = tappedItem.findAdapterViewItem(x, y);

        OnItemClickListener clickListener = getOnItemClickListener();
        if (clickListener != null)
            clickListener.onItemClick(this, adapterView, adapterIndex, 0);
    }

    protected abstract static class ItemInfo {
        private int mWidth;
        private int mHeight;

        private int mLeft;
        private int mTop;

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

        public abstract void measureViewsBySpecs(int parentSpecWidth, int paddingHorizontal, int parentSpecHeight, int paddingVertical);
        public abstract void measureViews(int parentWidth, int parentHeight);

        protected abstract void onLayoutViews(int left, int top);

        public void layoutViews(int left, int top) {
            mLeft = left;
            mTop = top;
            onLayoutViews(left, top);
        }

        protected abstract void onOffsetViews(int dX);

        public void offsetViews(int dX) {
            mLeft += dX;
            onOffsetViews(dX);
        }

        public boolean containsXY(int x, int y) {
            int left = mLeft;
            int top = mTop;
            return  x >= left
                    && x <= left + mWidth
                    && y >= top
                    && y <= top + mHeight;
        }

        public abstract void showPressed(int touchX, int touchY);
        public abstract void hidePressed();

        public abstract int findAdapterItemIndex(int index, int x, int y);
        public abstract View findAdapterViewItem(int x, int y);
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

    private static class ListState extends BaseSavedState {
        private int mFirstItemIndex;

        public ListState(Parcelable parcelable) {
            super(parcelable);
        }

        public void setFirstItemIndex(int firstItemIndex) {
            mFirstItemIndex = firstItemIndex;
        }

        public int getFirstItemIndex() {
            return mFirstItemIndex;
        }
    }
}
