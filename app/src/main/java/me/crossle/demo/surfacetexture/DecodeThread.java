package me.crossle.demo.surfacetexture;

import android.os.SystemClock;
import android.view.Surface;

/**
 * Created by leapin on 2015/1/16.
 */
public class DecodeThread extends Thread {
    private final long mMinInterval;
    private long mLastPollTime;
    private boolean mPendingPoll;
    private final VideoDecoder mDecoder;

    public DecodeThread(String filePath, Surface surface, long minInterval) {
        mDecoder = new VideoDecoder(surface, filePath);
        this.mMinInterval = minInterval;
    }

    @Override
    public void run() {
        while (!interrupted()) {
            if (mPendingPoll) {
                mDecoder.pollNextFrame(0);
            }
            try {
                this.wait(mMinInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
