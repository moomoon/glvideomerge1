package me.crossle.demo.surfacetexture;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import java.lang.ref.WeakReference;

/**
 * Created by leapin on 2015/1/16.
 */
public class DecodeThread extends Thread implements VideoDecoder.DecoderCallbacks{
    private final long mMinInterval;
    private long mLastPollTime = 0;
    private boolean mPendingPoll;
    private boolean mPendingException;
    private final VideoDecoder mDecoder;
    private OnDecodeStopListener mListener;

    public DecodeThread(String filePath, Surface surface, long minInterval) {
        mDecoder = new VideoDecoder(surface, filePath);
        mDecoder.setDecoderCallbacks(this);
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
            if (mPendingPoll || mPendingException) {
                mPendingPoll = false;
                mPendingException = false;
                boolean eos = !mDecoder.pollNextFrame();
                if (eos) {
                    if (null != mListener) {
                        mListener.onDecodeStopped();
                    }
                    break;
                }

                long intervalRemained = mMinInterval - SystemClock.uptimeMillis() + mLastPollTime;
                Log.e("decode", "intervalRemained = " + intervalRemained);
                if (intervalRemained > 2) {
                    try {
                        this.sleep(intervalRemained);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void setOnDecodeStopListener(OnDecodeStopListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onOutputTimeOut() {
        Log.e("decode", "time out");
        mPendingException = true;
    }

    @Override
    public void onBufferChanged() {
        mPendingException = true;
    }

    @Override
    public void onFormatChanged() {
        mPendingException = true;
    }

    public interface OnDecodeStopListener {
        public void onDecodeStopped();
    }

    public static class DecodeHandler extends Handler {
        private final WeakReference<DecodeThread> threadRef;

        private DecodeHandler(DecodeThread thread) {
            this.threadRef = new WeakReference<DecodeThread>(thread);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }

        public void sendPoll() {

        }
    }
}
