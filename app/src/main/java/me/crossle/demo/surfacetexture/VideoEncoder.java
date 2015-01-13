package me.crossle.demo.surfacetexture;

import android.content.res.Resources;
import android.graphics.Movie;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by leapin on 2015/1/13.
 */
public class VideoEncoder {
    private static final String TAG = "VP8EncoderTest";
    private static final String VP8_MIME = "video/x-vnd.on2.vp8";
    private static final String MP4_MIME = "video/mp4v-es";
    private static final String H264_MIME = "video/avc";
    private static final String VPX_DECODER_NAME = "OMX.google.vpx.decoder";
    private static final String VPX_ENCODER_NAME = "OMX.google.vp8.encoder";
    private static final String BASIC_IVF = "video_176x144_vp8_basic.ivf";
    private static final long DEFAULT_TIMEOUT_US = 50000;
    private Resources mResources;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;
    private String mOutputFilename;
    private int mFrameWidth;
    private int mFrameHeight;
    private int mFrameRate;
    private int mFrameSize;
    private IvfWriter mIvfWriter;
    private MediaCodec mEncoder;
    private int mFrameIndex = 0;
    boolean started = false;
    private boolean stopped = false;

    private VideoEncoder(int frameWidth, int frameHeight, int frameRate, String outputFilename) {
        this.mFrameWidth = frameWidth;
        this.mFrameHeight = frameHeight;
        this.mFrameRate = frameRate;
        this.mOutputFilename = outputFilename;
        this.mFrameSize = mFrameWidth * mFrameHeight * 3 / 2;

        String mimeType = MP4_MIME;

//        for (int i = 1; i < 44; i++) {
//            Log.e("color format", "trying " + i);
        try {
//            MediaFormat format = MediaFormat.createVideoFormat(VP8_MIME, mFrameWidth, mFrameHeight);
//            MediaFormat format = MediaFormat.createVideoFormat(MP4_MIME, mFrameWidth, mFrameHeight);
//            Log.e("color test", "0");
//            format.setInteger(MediaFormat.KEY_BIT_RATE, 125000);
//
//            Log.e("color test", "1");
//            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
////                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
////                        i);
//                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
//            Log.e("color test", "2");
//            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
//            Log.e("color test", "3");

            MediaFormat mediaFormat = MediaFormat.createVideoFormat(mimeType, 640, 480);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 768000);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
//            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
            int colorFormat = getColorFormat("video/avc");
            Log.e("colorFormat", "format = " + colorFormat);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);

//            mEncoder = MediaCodec.createByCodecName(VPX_ENCODER_NAME);
//            mEncoder = MediaCodec.createEncoderByType("video/x-vnd.on2.vp8");
//                mEncoder = MediaCodec.createEncoderByType(MP4_MIME);
            mEncoder = MediaCodec.createEncoderByType(mimeType);
            Log.e("color test", "4");
//                Log.e("color encoder name", "" + mEncoder.getName());
//            mEncoder = MediaCodec.createByCodecName("OMX.qcom.video.encoder.mpeg4");
//            mEncoder = MediaCodec.createByCodecName("OMX.google.mpeg4.encoder");
            mEncoder.configure(mediaFormat,
                    null,  // surface
                    null,  // crypto
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
            Log.e("color test", "5");


//            mBufferInfo = new MediaCodec.BufferInfo();
//            encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
//        }
    }


    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        ;
        return null;
    }


    private int getColorFormat(String mimeType) {
        int selectedColorFormat = 0;
        MediaCodecInfo.CodecCapabilities capabilities = selectCodec(mimeType).getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length && selectedColorFormat == 0; i++) {
            int format = capabilities.colorFormats[i];
            switch (format) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
                case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                case MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar:
                    selectedColorFormat = format;
                    break;
                default:
                    Log.e("color format", "Unsupported color format " + format);
                    break;
            }
        }
        return selectedColorFormat;
    }

    public static VideoEncoder create(String outputFilename, int frameWidth, int frameHeight, int frameRate) {
        VideoEncoder encoder = new VideoEncoder(frameWidth, frameHeight, frameRate, outputFilename);
        try {
            encoder.mIvfWriter = new IvfWriter(outputFilename, frameWidth, frameHeight);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return encoder;
    }

    public void start() {
        mEncoder.start();
        mInputBuffers = mEncoder.getInputBuffers();
        mOutputBuffers = mEncoder.getOutputBuffers();
    }


    public synchronized void encode(byte[] frame, int offset, int size) throws IOException {
        if (stopped) {
            Log.e("encode end", "blocked");
            return;
        }
        boolean sawInputEOS = size < 0;
        boolean sawOutputEOS = false;
        try {
            stopped |= sawInputEOS;
            if (stopped) {
                size = 0;
            } else {

                if (!started) {
                    started = true;
                    start();
                }
                int inputBufIndex = mEncoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                if (inputBufIndex >= 0) {
                    mInputBuffers[inputBufIndex].clear();
                    Log.e("bufferCount", "remain = " + mInputBuffers[inputBufIndex].remaining() + " frameLength = " + frame.length);
                    mInputBuffers[inputBufIndex].put(frame);
                    mInputBuffers[inputBufIndex].rewind();
                    int presentationTimeUs = (mFrameIndex * 1000000) / mFrameRate;
                    Log.d(TAG, "Encoding frame at index " + mFrameIndex);
                    mEncoder.queueInputBuffer(
                            inputBufIndex,
                            offset,  // offset
                            size,  // size
                            presentationTimeUs,
                            stopped ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    mFrameIndex++;
                }
            }
            if (!stopped) {
                sawOutputEOS = dequeueOutput(false);
            } else {
                Log.e("encode end", "saw input eos");
                int i = 0;
                while (!sawOutputEOS) {
                    Log.e("encode end", "before dequeue");
                    sawOutputEOS = dequeueOutput(true);
                    Log.e("encode end", "appending frame " + i++ + " sawOutputEOS = " + sawOutputEOS);
                }
                Log.e("encode end", "before encoder stop");

                mEncoder.stop();
                Log.e("encode end", "encoder stopped");
                mEncoder.release();
                Log.e("encode end", "encoder released");
            }

        } finally {
            if (stopped && mIvfWriter != null) {
                Log.e("encode end", "writer stopped");
                mIvfWriter.close();
                mIvfWriter = null;
            }

        }
    }

    /**
     * @return sawOutputEOS
     */
    private boolean dequeueOutput(boolean dropTimeOut) throws IOException {
        boolean sawOutputEOS = false;
        int result = mEncoder.dequeueOutputBuffer(mBufferInfo, DEFAULT_TIMEOUT_US);
        Log.e("encode end output", "" + result + " presentationTimeUs = " + mBufferInfo.presentationTimeUs);
        if (result >= 0) {
            int outputBufIndex = result;
            byte[] buffer = new byte[mBufferInfo.size];
            mOutputBuffers[outputBufIndex].rewind();
            mOutputBuffers[outputBufIndex].get(buffer, 0, mBufferInfo.size);
            Log.e("encode end flag", String.format("%h", mBufferInfo.flags));
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                sawOutputEOS = true;
            } else {
                mIvfWriter.writeFrame(buffer, mBufferInfo.presentationTimeUs);
            }
            mEncoder.releaseOutputBuffer(outputBufIndex,
                    false);  // render
        } else if (result == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            mOutputBuffers = mEncoder.getOutputBuffers();
        } else if (dropTimeOut && result == MediaCodec.INFO_TRY_AGAIN_LATER) {
            sawOutputEOS = true;
        }
        return sawOutputEOS;
    }


    /**
     * A basic vp8 encode loop.
     * <p/>
     * MediaCodec will raise an IllegalStateException
     * whenever vp8 encoder fails to encode a frame.
     * <p/>
     * In addition to that written IVF file can be tested
     * to be decodable in order to verify the bitstream produced.
     * <p/>
     * Color format of input file should be YUV420, and mFrameWidth,
     * mFrameHeight should be supplied correctly as raw input file doesn't
     * include any header data.
     * <p/>
     * outputFilename The name of the IVF file to write encoded bitsream
     *
     * @param rawInputFd File descriptor for the raw input file (YUV420)
     *                   mFrameWidth    Frame width of input file
     *                   mFrameHeight   Frame height of input file
     *                   frameRate      Frame rate of input file in frames per second
     */
//    private void encode(int rawInputFd
//    ) throws Exception {
//        // Create a media format signifying desired output
//
//        Log.d(TAG, "Creating encoder");
//
//        InputStream rawStream = null;
//        try {
//            rawStream = mResources.openRawResource(rawInputFd);
//            // encode loop
//            long presentationTimeUs = 0;
//            int frameIndex = 0;
//            boolean sawInputEOS = false;
//            boolean sawOutputEOS = false;
//            while (!sawOutputEOS) {
//                if (!sawInputEOS) {
//                    int inputBufIndex = encoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
//                    if (inputBufIndex >= 0) {
//                        byte[] frame = new byte[frameSize];
//                        int bytesRead = rawStream.read(frame);
//                        if (bytesRead == -1) {
//                            sawInputEOS = true;
//                            bytesRead = 0;
//                        }
//                        mInputBuffers[inputBufIndex].clear();
//                        mInputBuffers[inputBufIndex].put(frame);
//                        mInputBuffers[inputBufIndex].rewind();
//                        presentationTimeUs = (frameIndex * 1000000) / mFrameRate;
//                        Log.d(TAG, "Encoding frame at index " + frameIndex);
//                        encoder.queueInputBuffer(
//                                inputBufIndex,
//                                0,  // offset
//                                bytesRead,  // size
//                                presentationTimeUs,
//                                sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
//                        frameIndex++;
//                    }
//                }
//                int result = encoder.dequeueOutputBuffer(mBufferInfo, DEFAULT_TIMEOUT_US);
//                if (result >= 0) {
//                    int outputBufIndex = result;
//                    byte[] buffer = new byte[mBufferInfo.size];
//                    mOutputBuffers[outputBufIndex].rewind();
//                    mOutputBuffers[outputBufIndex].get(buffer, 0, mBufferInfo.size);
//                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                        sawOutputEOS = true;
//                    } else {
//                        mIvfWriter.writeFrame(buffer, mBufferInfo.presentationTimeUs);
//                    }
//                    mEncoder.releaseOutputBuffer(outputBufIndex,
//                            false);  // render
//                } else if (result == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
//                    mOutputBuffers = encoder.getOutputBuffers();
//                }
//            }
//            encoder.stop();
//            encoder.release();
//        } finally {
//            if (mIvfWriter != null) {
//                mIvfWriter.close();
//            }
//            if (rawStream != null) {
//                rawStream.close();
//            }
//        }
//    }
}
