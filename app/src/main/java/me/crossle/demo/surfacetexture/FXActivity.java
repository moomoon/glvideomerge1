package me.crossle.demo.surfacetexture;

import android.app.Activity;
import android.media.MediaCodecInfo;
import android.os.Bundle;
import android.os.Environment;

import java.io.File;

/**
 * Created by leapin on 2015/1/16.
 */
public class FXActivity extends Activity {
    private FXMovieView mView;
    private boolean configured = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mView = new FXMovieView(this);
        setContentView(mView);
    }

    @Override
    protected void onResume() {
        super.onResume();

//        public String sourcePath;
//        public String effectPath;
//        public String alphaPath;
//        public int timeScale;
//        public int frameTick;

        if (!configured) {
            FXMovieView.Config config = new FXMovieView.Config();
            String dir = Environment.getExternalStorageDirectory() + File.separator;
            config.sourcePath = dir + "big_buck_bunny.mp4";
//        config.effectPath = dir + "big_buck_bunny.mp4";
//        config.alphaPath = dir + "big_buck_bunny.mp4";
            config.effectPath = dir + "pocoyo.mp4";
            config.alphaPath = dir + "pocoyo_alpha.mp4";
            config.timeScale = 30;
            config.frameTick = 1;
            config.colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
            config.displayHeight = 1000;
            config.displayWidth = 1000;
            config.sourceWidth = 1000;
            config.sourceHeight = 1000;
            config.effectWidth = 1000;
            config.effectHeight = 1000;
            config.recHeight = 1000;
            config.recHeight = 1000;
            mView.config(config);
            mView.setAutoStart();
            mView.prepare();
            configured = true;
        }
    }
}

