package me.crossle.demo.surfacetexture;

import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

/**
 * Created by leapin on 2015/1/16.
 */
public class DecodeThread extends Thread {
    private final long mMinInterval;
    private long mLastPollTime = 0;
    private boolean mPendingPoll;
    private final VideoDecoder mDecoder;
    private OnDecodeStopListener mListener;

    public DecodeThread(String filePath, Surface surface, long minInterval) {
        mDecoder = new VideoDecoder(surface, filePath);
        this.mMinInterval = minInterval;
    }

    public void sendPollNextFrame() {
        if (!mPendingPoll) {
            mLastPollTime = SystemClock.uptimeMillis();
            mPendingPoll = true;
        }
    }

    @Override
    public synchronized void start() {
        mDecoder.start();
        super.start();
    }


    @Override
    public synchronized void run() {
        mLastPollTime = SystemClock.uptimeMillis();
        while (true) {
            Log.e("run", "run");
            if (mPendingPoll) {
                Log.e("decode", "beginPoll");
                boolean eos = !mDecoder.pollNextFrame();
                Log.e("decode", "endPoll result = " + eos);
                if (eos) {
                    if (null != mListener) {
                        mListener.onDecodeStopped();
                    }
                    break;
                }

                long intervalRemained = mMinInterval - SystemClock.uptimeMillis() + mLastPollTime;
                Log.e("decode", "intervalRemained = " + intervalRemained);
                if (intervalRemained > 200) {
                    try {
                        this.sleep(intervalRemained);
                    } catch (InterruptedException e) {
                        Log.e("decode", "thread exception");
                        e.printStackTrace();
                    }
                }
            }
        }
        Log.e("decode", "thread end");
    }

    public void setOnDecodeStopListener(OnDecodeStopListener listener) {
        this.mListener = listener;
    }

    public interface OnDecodeStopListener {
        public void onDecodeStopped();
    }
}
