package rubo.pullswipelayout.pullswipe;

interface LoadingProgress {
    float getProgress();
    void setProgress(float progress);
    void startProgress();
    void resetProgress();
}
