package rubo.pullswipelayout.pullswipe;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import rubo.pullswipelayout.R;

public class LoadingWidget extends FrameLayout implements LoadingProgress {

    LoadingBall mLoadingBall;


    public LoadingWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.pull_swipe_loading_widget, this);
        mLoadingBall = (LoadingBall) findViewById(R.id.loading_widget_loadingBall);
    }

    @Override
    public float getProgress() {
        return mLoadingBall.getProgress();
    }

    @Override
    public void setProgress(float progress) {
        mLoadingBall.setProgress(progress);
    }

    @Override
    public void startProgress() {
        mLoadingBall.startProgress();
    }

    @Override
    public void resetProgress() {
        mLoadingBall.resetProgress();
    }
}
