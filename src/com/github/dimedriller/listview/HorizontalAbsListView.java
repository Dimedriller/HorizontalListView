package com.github.dimedriller.listview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Adapter;
import android.widget.AdapterView;
import com.github.dimedriller.R;
import com.github.dimedriller.widget.GestureDetector;
import com.github.dimedriller.widget.Scroller;
import com.github.dimedriller.widget.TouchInterceptionDetector;

import java.util.ArrayList;

/**
 ***********************************************************************************************************************
 * This class is base class to provide horizontal list view scrolling logic
 ***********************************************************************************************************************
 */
public abstract class HorizontalAbsListView extends AdapterView<Adapter> {
    protected int mFirstGlobalItemIndex;
    protected ArrayList<ItemInfo> mItems;

    /* This field is used only when state is restored */
    private int mFirstItemOffset;

    private ItemInfoManager mItemsManager;

    private final Scroller mScroller;
    private final GestureDetector mGestureDetector;
    private final MoveChildrenRunnable mMoveRunnable = new MoveChildrenRunnable(this);
    private final TouchInterceptionDetector mTouchInterceptionDetector = new TouchInterceptionDetector() {
        @Override
        protected boolean onDoInterception(float previousX, float previousY, float currentX, float currentY) {
            float deltaX = Math.abs(currentX - previousX);
            return deltaX > Math.abs(currentY - previousY);
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

    private GestureDetector createGestureDetector(final Context context) {
        GestureDetector gestureDetector = new GestureDetector(
                context,
                new GestureDetector.OnGestureListener() {
                    private final float mTouchSlope = ViewConfiguration.get(context).getScaledTouchSlop();
                    private float mMovingDistance;

                    @Override
                    public boolean onUp(MotionEvent e) {
                        hidePressedState();
                        return Math.abs(mMovingDistance) >= mTouchSlope;
                    }

                    @Override
                    public boolean onDown(MotionEvent e) {
                        mMovingDistance = 0;

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
                        if (Math.abs(mMovingDistance) > mTouchSlope)
                            return false;

                        hidePressedState();

                        int x = Math.round(e.getX());
                        int y = Math.round(e.getY());
                        return handleItemTap(x, y, TapType.CLICK);
                    }

                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                        mMovingDistance += distanceX;

                        awakenScrollBars();
                        startScroll(distanceX);
                        return Math.abs(distanceX) > Math.abs(distanceY);
                    }

                    @Override
                    public void onLongPress(MotionEvent e) {
                        if (Math.abs(mMovingDistance) > mTouchSlope)
                            return;

                        hidePressedState();

                        int x = Math.round(e.getX());
                        int y = Math.round(e.getY());
                        handleItemTap(x, y, TapType.LONG_CLICK);
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        awakenScrollBars();
                        startFling(-velocityX * VELOCITY_X_RATIO);
                        if (Math.abs(velocityX) > Math.abs(velocityY)) {
                            mMovingDistance = mTouchSlope + 1;
                            return true;
                        } else
                            return false;
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
            // On some devices styleable attributes for scrollbars can be absent
        }

        mItemsManager = createItemInfoManager(null);
        mItems = new ArrayList<ItemInfo>(0);
    }

    /**
     *******************************************************************************************************************
     * Creates instance of {@code ItemInfoManager} for specific scion of this class
     *******************************************************************************************************************
     */
    protected abstract ItemInfoManager createItemInfoManager(Adapter adapter);

    /**
     *******************************************************************************************************************
     * @return instance of {@code ItemInfoManager} associated with this class
     *******************************************************************************************************************
     */
    protected ItemInfoManager getItemsManager() {
        return mItemsManager;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        mItemsManager = createItemInfoManager(adapter);

        removeAllViewsInLayout();

        mFirstGlobalItemIndex = 0;
        mItems = new ArrayList<ItemInfo>();
        requestLayout();
    }

    @Override
    public Adapter getAdapter() {
        return mItemsManager.getAdapter();
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
        listState.setFirstItemOffset(getFirstItemOffset());
        return listState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        ListState listState = (ListState) state;
        super.onRestoreInstanceState(listState.getSuperState());
        mFirstGlobalItemIndex = listState.getFirstItemIndex();
        mFirstItemOffset = listState.getFirstItemOffset();
    }

    @Override
    public abstract LayoutParams generateDefaultLayoutParams();

    /**
     *******************************************************************************************************************
     * It was overriden to provide public access
     *******************************************************************************************************************
     */
    @Override
    public boolean addViewInLayout(View child, int index, LayoutParams params, boolean preventRequestLayout) {
        return super.addViewInLayout(child, index, params, preventRequestLayout);
    }

    @Override
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        super.setOnItemLongClickListener(listener);
        mGestureDetector.setIsLongpressEnabled(listener != null);
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        // Do not dispatch press state to the children. The list view is responsible for setting press state
    }

    private int findItemInfoIndexByXY(int x, int y) {
        ArrayList<ItemInfo> items = mItems;
        int countItems = items.size();
        for (int counter = 0; counter < countItems; counter++)
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

    public void stopScrolling() {
        mScroller.forceFinished(true);
        removeCallbacks(mMoveRunnable);
    }

    protected int getFirstItemOffset() {
        ArrayList<ItemInfo> items = mItems;
        if (items.size() == 0)
            return 0;
        else
            return items.get(0).getLeft();
    }

    protected int getLastItemRight() {
        ArrayList<ItemInfo> items = mItems;
        int numItems = items.size();
        if (numItems == 0)
            return 0;
        else
            return items.get(numItems - 1).getRight();
    }

    protected int getWidthWithoutPaddings() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    protected int getHeightWithoutPaddings() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    protected int getDisplayedItemsFullWidth() {
        ArrayList<ItemInfo> items = mItems;
        int itemsCount = items.size();
        if (itemsCount == 0)
            return 0;

        return items.get(itemsCount - 1).getRight() - items.get(0).getLeft();
    }

    /**
     *******************************************************************************************************************
     * Moves {@code itemsCount} items starting from {@code itemFirstIndex} on {@code dX} points left or right
     * depending on {@code dX} sign
     * @param itemFirstIndex - index of first item being moved
     * @param itemsCount - number of items being moved
     * @param dX - horizontal offset amount
     *******************************************************************************************************************
     */
    protected void shiftItems(int itemFirstIndex, int itemsCount, int dX) {
        ArrayList<ItemInfo> items = mItems;
        int allItemsCount = items.size();

        if (itemFirstIndex + itemsCount > allItemsCount)
            itemsCount = allItemsCount - itemFirstIndex;
        for (int counterItem = itemFirstIndex; counterItem < itemFirstIndex + itemsCount; counterItem++)
            items.get(counterItem).offsetViews(dX);
    }

    /**
     *******************************************************************************************************************
     * Moves all items on {@code dX} points left or right depending on {@code dX} sign
     * @param dX - horizontal offset amount
     *******************************************************************************************************************
     */
    protected void shiftItems(int dX) {
        shiftItems(0, mItems.size(), dX);
    }

    private int measureAndLayoutItemLeft(ItemInfo item, int itemRightX) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int viewWidthWithoutPadding = getWidthWithoutPaddings();
        int viewHeightWithoutPadding = getHeight() - paddingTop - getPaddingBottom();

        item.measureViews(viewWidthWithoutPadding, viewHeightWithoutPadding);
        item.layoutViews(itemRightX - item.getWidth(), paddingLeft, paddingTop);
        return item.getWidth();

    }

    private int measureAndLayoutItemRight(ItemInfo item, int itemLeftX) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int viewWidthWithoutPadding = getWidthWithoutPaddings();
        int viewHeightWithoutPadding = getHeight() - paddingTop - getPaddingBottom();

        item.measureViews(viewWidthWithoutPadding, viewHeightWithoutPadding);
        item.layoutViews(itemLeftX, paddingLeft, paddingTop);
        return item.getWidth();
    }

    /**
     *******************************************************************************************************************
     * Adds new views to right side of last view displayed
     * @param dX - items offset
     * @return corrected delta X
     *******************************************************************************************************************
     */
    protected int addItemsRight(int dX) {
        int viewWidthWithoutPadding = getWidthWithoutPaddings();
        int firstItemX = getFirstItemOffset();

        ItemInfoManager itemsManager = mItemsManager;
        ArrayList<ItemInfo> items = mItems;
        int nextItemIndex = mFirstGlobalItemIndex + items.size();
        int countGlobalItems = itemsManager.getItemInfoCount();
        int currentRight = firstItemX - dX + getDisplayedItemsFullWidth();

        while (  currentRight < viewWidthWithoutPadding
              && nextItemIndex < countGlobalItems) {
            ItemInfo newItem = itemsManager.createItemInfo(this, nextItemIndex);
            items.add(newItem);
            currentRight += measureAndLayoutItemRight(newItem, currentRight + dX);
            nextItemIndex++;
        }

        if (currentRight <= viewWidthWithoutPadding) {
            dX -= viewWidthWithoutPadding - currentRight;
            if (dX < 0)
                return 0;
            else
                return dX;
        }
        else
            return dX;
    }

    /**
     *******************************************************************************************************************
     * Removes invisible views from left side of first view displayed
     * @param dX - items offset
     *******************************************************************************************************************
     */
    protected void removeItemsLeft(int dX) {
        ItemInfoManager itemsManager = mItemsManager;
        ArrayList<ItemInfo> items = mItems;

        ItemInfo itemToRemove = items.get(0);
        int firstGlobalItemIndex = mFirstGlobalItemIndex;
        int currentLeft = getFirstItemOffset() - dX;

        while (currentLeft + itemToRemove.getWidth() < 0) {
            currentLeft += itemToRemove.getWidth();
            items.remove(0);
            itemsManager.recycleItemInfo(this, itemToRemove);
            itemToRemove = items.get(0);
            firstGlobalItemIndex++;
        }
        mFirstGlobalItemIndex = firstGlobalItemIndex;
    }

    private boolean moveItemsLeft(int dX) {
        int newDX = addItemsRight(dX);

        if (newDX < dX) {
            int viewWidthWithoutPadding = getWidthWithoutPaddings();
            mRightFadingEdge.onPull((float) (dX - newDX) / viewWidthWithoutPadding);
        }

        removeItemsLeft(newDX);

        shiftItems(-newDX);
        return newDX < dX;
    }

    /**
     *******************************************************************************************************************
     * Adds new views to left side of first view displayed
     * @param dX - items offset
     * @return corrected delta X
     *******************************************************************************************************************
     */
    protected int addItemsLeft(int dX) {
        int firstItemX = getFirstItemOffset();

        ItemInfoManager itemsManager = mItemsManager;
        ArrayList<ItemInfo> items = mItems;
        int firstGlobalItemIndex = mFirstGlobalItemIndex;
        int nextItemIndex = firstGlobalItemIndex - 1;
        int currentLeft = firstItemX - dX;

        while (  currentLeft >= 0
              && nextItemIndex >= 0) {
            ItemInfo newItem = itemsManager.createItemInfo(this, nextItemIndex);
            items.add(0, newItem);
            currentLeft -= measureAndLayoutItemLeft(newItem, currentLeft + dX);
            nextItemIndex--;
        }
        firstGlobalItemIndex = nextItemIndex + 1;
        mFirstGlobalItemIndex = firstGlobalItemIndex;

        if (currentLeft > 0)
            return dX + currentLeft;
        else
            return dX;
    }

    /**
     *******************************************************************************************************************
     * Removes invisible items from right side of the last view displayed
     * @param dX - items offset
     *******************************************************************************************************************
     */
    protected void removeItemsRight(int dX) {
        ItemInfoManager itemsManager = mItemsManager;
        ArrayList<ItemInfo> items = mItems;

        int indexToRemove = items.size() - 1;
        ItemInfo itemToRemove = items.get(indexToRemove);
        int currentRight = itemToRemove.getRight() - dX;
        int viewWidthWithoutPadding = getWidthWithoutPaddings();

        while (currentRight - itemToRemove.getWidth() > viewWidthWithoutPadding) {
            currentRight -= itemToRemove.getWidth();
            items.remove(indexToRemove);
            itemsManager.recycleItemInfo(this, itemToRemove);
            indexToRemove--;
            itemToRemove = items.get(indexToRemove);
        }
    }

    private boolean moveItemsRight(int dX) {
        int newDX = addItemsLeft(dX);

        if (newDX > dX) {
            int viewWidthWithoutPadding = getWidthWithoutPaddings();
            mLeftFadingEdge.onPull((float) (newDX - dX) / viewWidthWithoutPadding);
        }

        removeItemsRight(newDX);

        shiftItems(-newDX);
        return newDX > dX;
    }

    private void moveItems() {
        Scroller scroller = mScroller;
        int startX = scroller.getCurrX();
        scroller.computeScrollOffset();
        int endX = scroller.getCurrX();
        int deltaX = endX - startX;
        int widthWithoutPaddings = getWidthWithoutPaddings();

        boolean forceFinished = false;
        if (deltaX > 0) {
            if (deltaX > widthWithoutPaddings)
                deltaX = widthWithoutPaddings;
            forceFinished = moveItemsLeft(deltaX);
        } else if (deltaX < 0) {
            if (deltaX < -widthWithoutPaddings)
                deltaX = -widthWithoutPaddings;
            forceFinished = moveItemsRight(deltaX);
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
        for (int counter = 0; counter < countItems; counter++)
            items.get(counter).hidePressed();
    }

    private boolean startScroll(float dx) {
        if (mItemsManager.getItemInfoCount() == 0)
            return true;

        hidePressedState();
        mScroller.startScroll(0, 0, Math.round(dx), 0, 0);
        moveItems();
        return true;
    }

    private boolean startFling(float velocityX) {
        if (mItemsManager.getItemInfoCount() == 0)
            return true;

        hidePressedState();
        mScroller.fling(0, 0, Math.round(velocityX), 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0);
        moveItems();
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        ItemInfoManager itemsManager = mItemsManager;
        int itemsCount = itemsManager.getItemInfoCount();
        if (itemsCount == 0)
            return;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int horizontalPaddings = getPaddingLeft() + getPaddingRight();
        int verticalPaddings = getPaddingTop() + getPaddingBottom();

        int childWidth = 0;
        int childHeight = 0;

        if (itemsCount > 0
                && (widthMode != MeasureSpec.EXACTLY
                || heightMode != MeasureSpec.EXACTLY)) {
            ArrayList<ItemInfo> existingItems = mItems;
            ItemInfo child;
            if (existingItems.size() > 0)
                child = existingItems.get(0);
            else
                child = itemsManager.createItemInfo(this, 0);

            childWidth = child.getWidth();
            child.measureViewsBySpecs(widthMeasureSpec, horizontalPaddings, heightMeasureSpec, verticalPaddings);
            childHeight = child.getHeight();

            if (existingItems.size() == 0)
                itemsManager.recycleItemInfo(this, child);
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
        ItemInfoManager itemsManager = mItemsManager;
        int globalItemsCount = itemsManager.getItemInfoCount();
        if (globalItemsCount == 0)
            return;

        int viewWidthWithoutPadding = getWidthWithoutPaddings();
        ArrayList<ItemInfo> items = mItems;
        int firstItemOffset = getFirstItemOffset() + mFirstItemOffset;
        mFirstItemOffset = 0;
        int currentRight = firstItemOffset;


        int firstGlobalItemIndex = mFirstGlobalItemIndex;
        if (firstGlobalItemIndex > globalItemsCount)
            firstGlobalItemIndex = globalItemsCount;
        int currentIndex = firstGlobalItemIndex;

        while (  currentRight < viewWidthWithoutPadding
              && currentIndex < globalItemsCount) {
            int listItemIndex = currentIndex - firstGlobalItemIndex;
            ItemInfo currentItem;
            if (listItemIndex < items.size())
                currentItem = items.get(listItemIndex);
            else {
                currentItem = itemsManager.createItemInfo(this, currentIndex);
                items.add(currentItem);
            }
            currentRight += measureAndLayoutItemRight(currentItem, currentRight);
            currentIndex++;
        }

        if (currentRight < viewWidthWithoutPadding) {
            int currentLeft = firstItemOffset;
            int itemsFullWidth = currentRight - currentLeft;
            while (itemsFullWidth < viewWidthWithoutPadding
                    && firstGlobalItemIndex > 0) {
                firstGlobalItemIndex--;
                ItemInfo item = itemsManager.createItemInfo(this, firstGlobalItemIndex);
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
                itemsManager.recycleItemInfo(this, currentItem);
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
                doInvalidate = drawEdge(canvas, leftEdge, 270f, -getHeight() + getPaddingTop(), getPaddingLeft());
            EdgeEffectCompat rightEdge = mRightFadingEdge;
            if (!rightEdge.isFinished())
                doInvalidate |= drawEdge(canvas, rightEdge, 90f, getPaddingTop(), getPaddingLeft() - getWidth());

            if (doInvalidate)
                ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawEdges(canvas);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if ((ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN)
            mGestureDetector.onTouchEvent(ev);
        return mTouchInterceptionDetector.doInterception(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        if (mFirstGlobalItemIndex != 0)
            return 1.0f;

        ArrayList<ItemInfo> items = mItems;
        if (items.size() == 0)
            return 0.0f;
        return (float) - getFirstItemOffset() / items.get(0).getWidth();
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        ArrayList<ItemInfo> items = mItems;
        int itemsGlobalCount = mItemsManager.getItemInfoCount();
        int itemsVisibleCount = items.size();

        if (mFirstGlobalItemIndex + itemsVisibleCount < itemsGlobalCount)
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

        return items.get(0).getWidth() * mItemsManager.getItemInfoCount();
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        ArrayList<ItemInfo> items = mItems;
        if (items.size() == 0)
            return 0;

        return items.get(0).getWidth() * mFirstGlobalItemIndex - getFirstItemOffset();
    }

    protected boolean isTapItemAvailable() {
        return true;
    }

    private boolean handleItemTap(int x, int y, TapType tapType) {
        if (  !isTapItemAvailable()
           || !tapType.isAvailable(this))
            return false;

        int tappedItemIndex = findItemInfoIndexByXY(x, y);
        if (tappedItemIndex == -1)
            return false;

        ItemInfo tappedItem = mItems.get(tappedItemIndex);
        int adapterIndex = tappedItem.findAdapterItemIndex(mFirstGlobalItemIndex + tappedItemIndex, x, y);
        View adapterView = tappedItem.findAdapterViewItem(x, y);

        return tapType.handleTap(this, adapterView, adapterIndex, 0);
    }

    protected abstract static class ItemInfo {
        private int mWidth;
        private int mHeight;

        private int mLeft;
        private int mTop;
        private int mRight;

        private boolean mIsRecyclingAvailable;

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
            return mRight;
        }

        public int getTop() {
            return mTop;
        }

        public abstract void createItemViews(HorizontalAbsListView parent, int index, Adapter adapter, ViewCache viewCache);

        public abstract void addItemViews(HorizontalAbsListView parent);

        public abstract void removeItemViews(HorizontalAbsListView parent);

        public abstract void recycleItemViews(ViewCache viewCache);

        public abstract void measureViewsBySpecs(int parentSpecWidth,
                int paddingHorizontal,
                int parentSpecHeight,
                int paddingVertical);

        public abstract void measureViews(int parentWidth, int parentHeight);

        protected abstract void onLayoutViews(int left, int top, int width);

        public void layoutViews(int left, int paddingLeft, int paddingTop, int width) {
            mLeft = left;
            mTop = 0;
            mRight = left + width;
            onLayoutViews(left + paddingLeft, paddingTop, width);
        }

        public void layoutViews(int left, int paddingLeft, int paddingTop) {
            layoutViews(left, paddingLeft, paddingTop, mWidth);
        }

        protected abstract void onOffsetViews(int dX);

        public void offsetViews(int dX) {
            mLeft += dX;
            mRight += dX;
            onOffsetViews(dX);
        }

        public boolean containsXY(int x, int y) {
            int left = mLeft;
            int top = mTop;
            return x >= left
                    && x <= left + mWidth
                    && y >= top
                    && y <= top + mHeight;
        }

        public abstract void showPressed(int touchX, int touchY);

        public abstract void hidePressed();

        public abstract int findAdapterItemIndex(int index, int x, int y);

        public abstract View findAdapterViewItem(int x, int y);

        public void setRecyclingAvailable(boolean isAvailable) {
            mIsRecyclingAvailable = isAvailable;
        }

        public boolean isRecyclingAvailable() {
            return mIsRecyclingAvailable;
        }
    }

    private enum TapType {
        CLICK {
            @Override
            public boolean isAvailable(AdapterView listView) {
                return listView.getOnItemClickListener() != null;
            }

            @Override
            public boolean handleTap(AdapterView listView, View view, int index, long id) {
                OnItemClickListener listener = listView.getOnItemClickListener();
                listener.onItemClick(listView, view, index, id);
                return true;
            }
        },
        LONG_CLICK {
            @Override
            public boolean isAvailable(AdapterView listView) {
                return listView.getOnItemLongClickListener() != null;
            }

            @Override
            public boolean handleTap(AdapterView listView, View view, int index, long id) {
                OnItemLongClickListener listener = listView.getOnItemLongClickListener();
                return listener.onItemLongClick(listView, view, index, id);
            }
        };

        public abstract boolean isAvailable(AdapterView listView);
        public abstract boolean handleTap(AdapterView listView, View view, int index, long id);
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

    protected static abstract class ItemInfoManager {
        private final Adapter mAdapter;
        private final ViewCache mViewCache;
        private final ArrayList<ItemInfo> mItemsCache;

        protected ItemInfoManager(Adapter adapter) {
            int viewTypesCount;
            if (adapter == null)
                viewTypesCount = 0;
            else
                viewTypesCount = adapter.getViewTypeCount();
            mAdapter = adapter;
            mViewCache = new ViewCache(viewTypesCount);
            mItemsCache = new ArrayList<ItemInfo>();
        }

        public Adapter getAdapter() {
            return mAdapter;
        }

        protected abstract int onGetItemInfoCount(Adapter adapter);

        public int getItemInfoCount() {
            Adapter adapter = mAdapter;
            if (adapter == null)
                return 0;
            else
                return onGetItemInfoCount(adapter);
        }

        protected abstract ItemInfo onCreateItemInfo();

        public ItemInfo createItemInfo(HorizontalAbsListView view, int globalIndex) {
            ArrayList<ItemInfo> cache = mItemsCache;
            ItemInfo itemInfo;
            if (cache.size() == 0)
                itemInfo = onCreateItemInfo();
            else
                itemInfo = cache.remove(0);

            itemInfo.createItemViews(view, globalIndex, mAdapter, mViewCache);
            itemInfo.addItemViews(view);
            return itemInfo;
        }

        public void recycleItemInfo(HorizontalAbsListView view, ItemInfo itemInfo) {
            itemInfo.removeItemViews(view);
            itemInfo.recycleItemViews(mViewCache);
            if (itemInfo.isRecyclingAvailable())
                mItemsCache.add(itemInfo);
        }
    }

    private static class MoveChildrenRunnable implements Runnable {
        private final HorizontalAbsListView mView;

        private MoveChildrenRunnable(HorizontalAbsListView view) {
            mView = view;
        }

        @Override
        public void run() {
            mView.moveItems();
        }
    }

    public static class ListState extends BaseSavedState {
        private int mFirstItemIndex;
        private int mFirstItemOffset;

        public ListState(Parcelable parcelable) {
            super(parcelable);
        }

        public ListState() {
            super(BaseSavedState.EMPTY_STATE);
        }

        public void setFirstItemIndex(int firstItemIndex) {
            mFirstItemIndex = firstItemIndex;
        }

        public int getFirstItemIndex() {
            return mFirstItemIndex;
        }

        private void setFirstItemOffset(int firstItemOffset) {
            mFirstItemOffset = firstItemOffset;
        }

        private int getFirstItemOffset() {
            return mFirstItemOffset;
        }
    }
}
