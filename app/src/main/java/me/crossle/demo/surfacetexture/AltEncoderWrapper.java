package me.crossle.demo.surfacetexture;

import android.util.Log;

import java.io.IOException;

/**
 * Created by leapin on 2015/1/13.
 */
public class AltEncoderWrapper {
    private AltEncoder mEncoder;
    private IvfWriter mWriter;
    private int frameRate;
    private int frameIndex = 0;
    private byte[] h264;
    private byte[] yuv420sp;
    private int width, height;
    private boolean stopped = false;

    public AltEncoderWrapper(int width, int height, int frameRate, int bitRate, String file) {
        try {
            this.width = width;
            this.height = height;
            this.frameRate = frameRate;
            mWriter = new IvfWriter(file, width, height);
            mEncoder = new AltEncoder(width, height, frameRate, bitRate);
            h264 = new byte[width * height * 3 / 2];
            yuv420sp = new byte[width * height * 3 / 2];
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void start() {
    }

    public void feed(int[] argb) {
        if (stopped) {
            Log.e("alt", "blocked");
            return;
        }
        FrameUtils.encodeYUV420SP(yuv420sp, argb, width, height);
        mEncoder.offerEncoder(yuv420sp, h264);
        int timeStamp = frameIndex * 1000000 / frameRate;
        try {
            mWriter.writeFrame(h264, timeStamp);
        } catch (IOException e) {
            stop();
            e.printStackTrace();
        }
    }

    public void stop() {
        stopped = true;
        try {
            mWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
