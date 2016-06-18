package rubo.pullswipelayout;

import android.content.Context;
import android.os.Build.VERSION;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.FrameLayout;

public class PullSwipeLayout extends FrameLayout implements NestedScrollingParent, NestedScrollingChild {

    private static final float DRAG_RATE = .5f;
    private static final int INVALID_POINTER = -1;
    private static final int DEFAULT_DRAG_DISTANCE = 64;
    private static final int ANIMATE_TO_START_DURATION = 200;

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;

    private View mTarget;
    private OnRefreshListener mListener;
    private boolean mRefreshing = false;
    private int mTouchSlop;
    private float mTotalDragDistance = -1;

    private float mSpinnerFinalOffset;
    private int mCurrentTargetOffsetTop;

    private float mInitialMotionY;
    private float mInitialDownY;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;

    private final DecelerateInterpolator mDecelerateInterpolator;

    private float mTotalUnconsumed;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];
    private boolean mNestedScrollInProgress;

    ViewGroup mContentContainer;

    public PullSwipeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        inflate(context, R.layout.layout_pull_swipe, this);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mContentContainer = (ViewGroup) findViewById(R.id.pull_swipe_content);

        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        mTotalDragDistance = DEFAULT_DRAG_DISTANCE * metrics.density;
        mSpinnerFinalOffset = mTotalDragDistance;

        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);

        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (child.getId() == R.id.pull_swipe_back || child.getId() == R.id.pull_swipe_content) {
            super.addView(child, params);
        } else {
            mContentContainer.addView(child, params);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = MotionEventCompat.getActionMasked(ev);

        if (!isEnabled() || canChildScrollUp()
                || mRefreshing || mNestedScrollInProgress) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                setTargetOffsetTopAndBottom(-mContentContainer.getTop(), true);
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                final float initialDownY = getMotionEventY(ev, mActivePointerId);
                if (initialDownY == -1) {
                    return false;
                }
                mInitialDownY = initialDownY;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }

                final float y = getMotionEventY(ev, mActivePointerId);
                if (y == -1) {
                    return false;
                }
                final float yDiff = y - mInitialDownY;
                if (yDiff > mTouchSlop && !mIsBeingDragged) {
                    mInitialMotionY = mInitialDownY + mTouchSlop;
                    mIsBeingDragged = true;
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingDragged;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    private void ensureTarget() {
        if (mTarget == null) {
            if (mContentContainer.getChildCount() != 1) {
                throw new RuntimeException("PullSwipeLayout有且只有一个child");
            }
            mTarget = mContentContainer.getChildAt(0);
        }
    }

    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mTarget, -1) || mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        int pointerIndex;

        if (!isEnabled() || canChildScrollUp() || mNestedScrollInProgress) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(event, 0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                pointerIndex = MotionEventCompat.findPointerIndex(event, mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                final float y = MotionEventCompat.getY(event, pointerIndex);
                final float overScrollTop = (y - mInitialMotionY) * DRAG_RATE;
                if (mIsBeingDragged) {
                    if (overScrollTop > 0) {
                        moveContent(overScrollTop);
                    } else {
                        return false;
                    }
                }
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                pointerIndex = MotionEventCompat.getActionIndex(event);
                if (pointerIndex < 0) {
                    return false;
                }
                mActivePointerId = MotionEventCompat.getPointerId(event, pointerIndex);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                break;

            case MotionEvent.ACTION_UP: {
                pointerIndex = MotionEventCompat.findPointerIndex(event, mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }

                final float y = MotionEventCompat.getY(event, pointerIndex);
                final float overScrollTop = (y - mInitialMotionY) * DRAG_RATE;
                mIsBeingDragged = false;
                finishContent(overScrollTop);
                mActivePointerId = INVALID_POINTER;
                return false;
            }

            case MotionEvent.ACTION_CANCEL:
                return false;
        }
        return true;
    }

    private void finishContent(float overScrollTop) {
//        if (overScrollTop > mTotalDragDistance) {
//            setRefreshing(true, true /* notify */);
//        } else {
        mRefreshing = false;
        animateOffsetToStartPosition(mCurrentTargetOffsetTop);
//        }
    }

    protected int mFrom;

    private void animateOffsetToStartPosition(int from) {
        mFrom = from;
        mAnimateToStartPosition.reset();
        mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
        mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
        mContentContainer.clearAnimation();
        mContentContainer.startAnimation(mAnimateToStartPosition);
    }

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);
        }
    };

    private void moveToStart(float interpolatedTime) {
        int targetTop = mFrom - (int) (mFrom * interpolatedTime);
        int offset = targetTop - mContentContainer.getTop();
        setTargetOffsetTopAndBottom(offset, false /* requires update */);
    }

    private void setTargetOffsetTopAndBottom(int offset, boolean requiresUpdate) {
        mContentContainer.offsetTopAndBottom(offset);
        mCurrentTargetOffsetTop = mContentContainer.getTop();
        if (requiresUpdate && VERSION.SDK_INT < 11) {
            invalidate();
        }
    }

    private void moveContent(float overScrollTop) {
        float originalDragPercent = overScrollTop / mTotalDragDistance;

        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
        float extraOS = Math.abs(overScrollTop) - mTotalDragDistance;
        float slingshotDist = mSpinnerFinalOffset;
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2) / slingshotDist);
        float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow((tensionSlingshotPercent / 4), 2)) * 2f;
        float extraMove = slingshotDist * tensionPercent * 2;

        int targetY = (int) ((slingshotDist * dragPercent) + extraMove);

        setTargetOffsetTopAndBottom(targetY - mCurrentTargetOffsetTop, true);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        if ((android.os.Build.VERSION.SDK_INT < 21 && mTarget instanceof AbsListView)
                || (mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget))) {
            // no-op
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return isEnabled() && !mRefreshing
                && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = 0;
        mNestedScrollInProgress = true;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;
            }
            moveContent(mTotalUnconsumed);
        }

        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;
        if (mTotalUnconsumed > 0) {
            finishContent(mTotalUnconsumed);
            mTotalUnconsumed = 0;
        }
        stopNestedScroll();
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);

        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if (dy < 0 && !canChildScrollUp()) {
            mTotalUnconsumed += Math.abs(dy);
            moveContent(mTotalUnconsumed);
        }
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX,
                                    float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY,
                                 boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    public interface OnRefreshListener {
        public void onRefresh();
    }
}
