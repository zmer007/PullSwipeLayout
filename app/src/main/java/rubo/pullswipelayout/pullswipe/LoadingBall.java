package rubo.pullswipelayout.pullswipe;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;

class LoadingBall extends View implements LoadingProgress {

    static final float SCALE = .4f;
    static final float MIN_SCALE = .2f;

    static final long DURATION = 1200;

    static final int COLOR_1 = 0xff990000;
    static final int COLOR_2 = 0xff009900;

    float mWidth;
    float mHeight;


    float mCurrentCx;
    float mCurrentRadius;

    float mBaseRadius;
    float mGrowRadiusDelta;
    float mDeclineRadiusDelta;

    float mProgress;

    boolean isProgress = true;

    Paint mPaint1;
    Paint mPaint2;

    boolean change;


    public LoadingBall(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint1 = new Paint();
        mPaint1.setStyle(Style.FILL);
        mPaint1.setColor(COLOR_1);
        mPaint1.setAntiAlias(true);
        mPaint1.setDither(false);

        mPaint2 = new Paint(mPaint1);
        mPaint2.setColor(COLOR_2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (change) {
            float rBallRadius = mBaseRadius + mGrowRadiusDelta / 2;
            canvas.drawCircle(mCurrentCx, mHeight / 2, mCurrentRadius, mPaint1);
            canvas.drawCircle(mWidth / 2, mHeight / 2, rBallRadius, mPaint2);
        } else {
            float rBallRadius = mBaseRadius + mGrowRadiusDelta / 2;
            canvas.drawCircle(mWidth / 2, mHeight / 2, rBallRadius, mPaint2);
            canvas.drawCircle(mCurrentCx, mHeight / 2, mCurrentRadius, mPaint1);
        }
    }

    /**
     * @param progress 取值区间[0, 1]
     */
    @Override
    public void setProgress(float progress) {
        mCurrentCx = mBaseRadius + (mWidth - mBaseRadius * 2) * progress;
        if (progress > mProgress) {
            resetForegroundRadius(progress);
        } else {
            resetBackgroundRadius(progress);

        }
        invalidate();
        mProgress = progress;
    }

    private void resetForegroundRadius(float progress) {
        if (progress < .5f) {
            mCurrentRadius = mBaseRadius + mGrowRadiusDelta * 2 * progress;
        } else {
            mCurrentRadius = mBaseRadius + mGrowRadiusDelta * 2 * (1 - progress);
        }
    }

    private void resetBackgroundRadius(float progress) {
        if (progress < .5f) {
            mCurrentRadius = mBaseRadius - mDeclineRadiusDelta * 2 * progress;
        } else {
            mCurrentRadius = mBaseRadius - mDeclineRadiusDelta * 2 * (1 - progress);
        }
    }

    @Override
    public float getProgress() {
        return mProgress;
    }

    @Override
    public void resetProgress() {
        isProgress = false;
        mProgressAnim.reset();
        clearAnimation();
        setProgress(0);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mWidth == 0 && getMeasuredWidth() != 0) {
            mWidth = MeasureSpec.getSize(widthMeasureSpec);
            mHeight = MeasureSpec.getSize(heightMeasureSpec);

            mBaseRadius = mHeight / 2 * SCALE;
            mGrowRadiusDelta = mHeight / 2 - mBaseRadius;
            mDeclineRadiusDelta = mBaseRadius - mHeight / 2 * MIN_SCALE;

            mCurrentRadius = mBaseRadius;
            mCurrentCx = mBaseRadius;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    @Override
    public void startProgress() {
        isProgress = true;

        mProgressAnim.reset();
        mProgressAnim.setDuration(DURATION);
        mProgressAnim.setInterpolator(new DecelerateInterpolator());
        mProgressAnim.setRepeatCount(Animation.INFINITE);
        clearAnimation();
        startAnimation(mProgressAnim);
    }

    private final Animation mProgressAnim = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            if (interpolatedTime < .5f) {
                change = false;
                setProgress(interpolatedTime * 2);
            } else {
                change = true;
                setProgress(2 - interpolatedTime * 2);
            }
        }
    };

}
