package me.crossle.demo.surfacetexture;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;

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
            AssetFileDescriptor afd0 = mResources.openRawResourceFd(R.raw.big_buck_bunny);
            mpSource.setDataSource(
                    afd0.getFileDescriptor(), afd0.getStartOffset(), afd0.getLength());
            afd0.close();

            String dir = Environment.getExternalStorageDirectory() + File.separator;
            mVideoView = new VideoSurfaceView(this, mpSource, dir + "pocoyo.mp4", dir + "pocoyo_alpha.mp4");
            setContentView(mVideoView);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

//        mVideoView = new VideoSurfaceView(this, mpSource, mpEffect, mpAlpha);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.onResume();
    }
}
