package me.crossle.demo.surfacetexture;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by leapin on 2015/1/15.
 */
public class MuxUtils {
    public static void muxDefaultFile() {
        MP4Writer writer = new MP4Writer();
        writer.addH264File(new MP4Writer.H264FileDescriptor(Environment.getExternalStorageDirectory() + File.separator + "encoded.h264", "eng", 10, 1));
        try {
            Log.e("write test", "start");
            writer.writeToMovieFile(Environment.getExternalStorageDirectory() + File.separator + "parsed.mp4");
            Log.e("write test", "end");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
