package me.crossle.demo.surfacetexture;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;

public class MediaPlayerSurfaceStubActivity extends Activity {

    private static final String TAG = "MediaPlayerSurfaceStubActivity";

    protected Resources mResources;

    private VideoSurfaceView mVideoView = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResources = getResources();
        MediaPlayer mpSource = new MediaPlayer();
        MediaPlayer mpEffect = new MediaPlayer();
        MediaPlayer mpAlpha = new MediaPlayer();

        try {
            AssetFileDescriptor afd0 = mResources.openRawResourceFd(R.raw.vid);
            mpSource.setDataSource(
                    afd0.getFileDescriptor(), afd0.getStartOffset(), afd0.getLength());
            afd0.close();
            AssetFileDescriptor afd1 = mResources.openRawResourceFd(R.raw.dragon);
            mpEffect.setDataSource(
                    afd1.getFileDescriptor(), afd1.getStartOffset(), afd1.getLength());
            afd1.close();
            AssetFileDescriptor afd2 = mResources.openRawResourceFd(R.raw.dragon_alpha);
            mpAlpha.setDataSource(
                    afd2.getFileDescriptor(), afd2.getStartOffset(), afd2.getLength());
            afd2.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        mVideoView = new VideoSurfaceView(this, mpSource, mpEffect, mpAlpha);
        setContentView(mVideoView);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.onResume();
    }
}
