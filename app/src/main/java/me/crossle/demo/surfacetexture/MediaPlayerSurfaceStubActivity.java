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
	private MediaPlayer mMediaPlayer0 = null;
	private MediaPlayer mMediaPlayer1 = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mResources = getResources();
		mMediaPlayer0 = new MediaPlayer();
		mMediaPlayer1 = new MediaPlayer();

		try {
			AssetFileDescriptor afd0 = mResources.openRawResourceFd(R.raw.testvideo);
			mMediaPlayer0.setDataSource(
			        afd0.getFileDescriptor(), afd0.getStartOffset(), afd0.getLength());
			afd0.close();
            AssetFileDescriptor afd1 = mResources.openRawResourceFd(R.raw.big_buck_bunny);
			mMediaPlayer1.setDataSource(
			        afd1.getFileDescriptor(), afd1.getStartOffset(), afd1.getLength());
			afd1.close();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		}

		mVideoView = new VideoSurfaceView(this, mMediaPlayer1, mMediaPlayer0);
		setContentView(mVideoView);

	}

	@Override
	protected void onResume() {
		super.onResume();
		mVideoView.onResume();
	}
}
