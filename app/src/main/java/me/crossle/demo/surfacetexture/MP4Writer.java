package me.crossle.demo.surfacetexture;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.H264TrackImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by leapin on 2015/1/14.
 */
public class MP4Writer {
    private List<H264FileDescriptor> mH264Files;
    private List<AACFileDescriptor> mAACFiles;

    public MP4Writer() {
        mH264Files = new ArrayList<>();
        mAACFiles = new ArrayList<>();
    }

    public void addH264File(H264FileDescriptor fd) {
        mH264Files.add(fd);
    }

    public void addAACFile(AACFileDescriptor fd) {
        mAACFiles.add(fd);
    }

    public void writeToMovieFile(String filePath) throws IOException {
        Movie movie = new Movie();
        for (H264FileDescriptor h264file : mH264Files) {
            H264TrackImpl h264Track = new H264TrackImpl(new FileDataSourceImpl(h264file.filePath), h264file.lang, h264file.timeScale, h264file.frameTicks);
            movie.addTrack(h264Track);
        }
        for (AACFileDescriptor aacFile : mAACFiles) {
            AACTrackImpl aacTrack = new AACTrackImpl(new FileDataSourceImpl(aacFile.filePath), aacFile.lang);
            movie.addTrack(aacTrack);
        }
        Container mp4file = new DefaultMp4Builder().build(movie);
        FileChannel fc = new FileOutputStream(new File(filePath)).getChannel();
        mp4file.writeContainer(fc);
        fc.close();
    }

    public static class H264FileDescriptor {
        private int timeScale;
        private int frameTicks;
        private String lang;
        private String filePath;

        public H264FileDescriptor(String filePath, String lang, int timeScale, int frameTicks) {
            this.filePath = filePath;
            this.lang = lang;
            this.timeScale = timeScale;
            this.frameTicks = frameTicks;
        }

        public H264FileDescriptor(String filePath) {
            this(filePath, "eng", -1, -1);
        }
    }

    public static class AACFileDescriptor {
        private String lang;
        private String filePath;

        public AACFileDescriptor(String filePath, String lang) {
            this.filePath = filePath;
            this.lang = lang;
        }

        public AACFileDescriptor(String filePath) {
            this(filePath, "eng");
        }
    }


}
