package me.crossle.demo.surfacetexture;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodecInfo;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Choreographer;
import android.view.Surface;


import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by leapin on 2015/1/16.
 */
public class FXMovieView extends GLSurfaceView {


    private static final int STATUS_CONFIGURED = 1;
    private static final int STATUS_PREPARED = 2;
    private static final int STATUS_STARTED = 3;
    private static final int STATUS_STOPPED = 4;

    private int mStatus = 0;
    private Config mConfig;
    private String mOutputFilePath = null;
    private boolean mRecording = false;
    private int mViewWidth, mViewHeight;
    private ViewHandler mHandler;
    private FXRenderer mRenderer;

    public FXMovieView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        mHandler = new ViewHandler(this);
        mRenderer = new FXRenderer(mHandler);
        setRenderer(mRenderer);
//        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public void config(Config config) {
        config.validate();
        this.mConfig = config;
        setStatus(STATUS_CONFIGURED);
        tryUpdateSize();
        mRenderer.setValidConfig(config);
    }

    public void prepare() {
        setStatus(STATUS_PREPARED);
        mRenderer.prepare(true);
    }

    public void start() {
        startInternal();
    }

    public void setAutoStart() {
        mRenderer.setAutoStart(true);
    }

    public void clearAutoStart() {
        mRenderer.setAutoStart(false);
    }

    private void startInternal() {
        setStatus(STATUS_STARTED);
        mRenderer.start();
    }

    public void stop() {
        setStatus(STATUS_STOPPED);
        mRenderer.stop();
    }

    private void selfStop() {
        setStatus(STATUS_STOPPED);
    }

    private void selfStart() {

        setStatus(STATUS_STARTED);
    }

    public void setOutputFilePath(String outputFilePath) {
        this.mOutputFilePath = outputFilePath;
    }

    public void toggleRecord(boolean rec) {
        if (rec && TextUtils.isEmpty(mOutputFilePath)) {
            throw new NullPointerException("set record to true while outputFilePath = " + mOutputFilePath);
        }
        if (rec ^ mRecording) {
            if (mStatus != STATUS_CONFIGURED) {
                throw new IllegalStateException("you have to configure " + getClass() + " before ");
            }
            toggleRecordInternal();
        }
    }

    private void toggleRecordInternal() {
        mRecording = !mRecording;
        mRenderer.setRecording(mRecording);
    }

    private synchronized void setStatus(int status) throws IllegalStateException {
        int illegalCurrentStatus = -1;
        switch (status) {
            case STATUS_CONFIGURED:
                if (mStatus == 0 || mStatus == STATUS_CONFIGURED) {
                    mStatus = status;
                } else {
                    illegalCurrentStatus = mStatus;
                }
                break;
            case STATUS_PREPARED:
                if (mStatus == STATUS_CONFIGURED) {
                    mStatus = status;
                } else {
                    illegalCurrentStatus = mStatus;
                }
                break;
            case STATUS_STARTED:
                if (mStatus == STATUS_PREPARED) {
                    mStatus = status;
                } else {
                    illegalCurrentStatus = mStatus;
                }
                break;
            case STATUS_STOPPED:
                if (mStatus == STATUS_STARTED) {
                    mStatus = status;
                } else {
                    illegalCurrentStatus = mStatus;
                }
                break;
            default:
                throw new IllegalStateException("unknown status " + status);
        }
        if (illegalCurrentStatus >= 0) {
            throw new IllegalStateException("setting status to " + status + " from " + illegalCurrentStatus);
        }
        Log.e("set status", "status = " + status);
    }

    private void tryUpdateSize() {
        if (null != mConfig && mViewWidth > 0 && mViewHeight > 0) {
            if (mConfig.displayWidth < 0) {
                mConfig.displayWidth = mViewWidth;
            }
            if (mConfig.displayHeight < 0) {
                mConfig.displayHeight = mViewHeight;
            }
            if (mConfig.sourceWidth < 0) {
                mConfig.sourceWidth = mConfig.displayWidth;
            }
            if (mConfig.sourceHeight < 0) {
                mConfig.sourceHeight = mConfig.displayHeight;
            }
            if (mConfig.effectWidth < 0) {
                mConfig.effectWidth = mConfig.displayWidth;
            }
            if (mConfig.effectHeight < 0) {
                mConfig.effectHeight = mConfig.displayHeight;
            }
            if (mConfig.recWidth < 0) {
                mConfig.recWidth = mConfig.sourceWidth;
            }
            if (mConfig.recHeight < 0) {
                mConfig.recHeight = mConfig.sourceHeight;
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.mViewWidth = MeasureSpec.getSize(widthMeasureSpec);
        this.mViewHeight = MeasureSpec.getSize(heightMeasureSpec);
        tryUpdateSize();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    public static class ViewHandler extends Handler {

        public static final int MSG_RENDER = 0;
        public static final int MSG_STOP = 1;
        public static final int MSG_AUTO_START = 2;
        private WeakReference<FXMovieView> viewRef;

        public ViewHandler(FXMovieView view) {
            this.viewRef = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            FXMovieView view = viewRef.get();
            if (null == view) {
                return;
            }
            switch (msg.what) {
                case MSG_RENDER:
                    view.requestRender();
                    break;
                case MSG_STOP:
                    view.selfStop();
                case MSG_AUTO_START:
                    view.selfStart();
            }
        }

        public void sendRender() {
            sendEmptyMessage(MSG_RENDER);
        }

        public void sendStop() {
            sendEmptyMessage(MSG_STOP);
        }

        public void sendAutoStart() {
            sendEmptyMessage(MSG_AUTO_START);
        }
    }


    public static class Config {
        public String sourcePath;
        public String effectPath;
        public String alphaPath;
        public int timeScale;
        public int frameTick;
        public int displayWidth = -1;
        public int displayHeight = -1;
        public int sourceWidth = -1;
        public int sourceHeight = -1;
        public int effectWidth = -1;
        public int effectHeight = -1;
        public int recWidth = -1;
        public int recHeight = -1;
        public int colorFormat = -1;

        private void validate() {
            if (TextUtils.isEmpty(sourcePath)) {
                throw new NullPointerException("validation failed sourPath = [" + sourcePath + "]");
            }
            if (TextUtils.isEmpty(effectPath)) {
                throw new NullPointerException("validation failed effectPath = [" + effectPath + "]");
            }
            if (TextUtils.isEmpty(alphaPath)) {
                throw new NullPointerException("validation failed alphaPath = [" + alphaPath + "]");
            }
            if (timeScale <= 0) {
                throw new RuntimeException("validation failed timeScale = [" + timeScale + "]");
            }
            if (frameTick <= 0) {
                throw new RuntimeException("validation failed frameTick = [" + frameTick + "]");
            }
            if (colorFormat <= 0) {
                throw new RuntimeException("validation failed colorFormat = [" + colorFormat + "]");
            }
        }
    }

    private static class FXRenderer implements Renderer, Choreographer.FrameCallback {
        private float transX, transY, transZ;

        private static final int FLOAT_SIZE_BYTES = 4;
        private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
        private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
        private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
        private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
        private boolean externalPrepared = false;
        private boolean surfaceCreated = false;
        private int program;

        private int frameBuffer;
        private int offscreenTexture;
        private int depthBuffer;


        private boolean autoStart = false;
        private long frameTimeNanoSec = 0L;
        private boolean recording = false;
        private Config validConfig;
        private boolean sourceStopped = true;
        private boolean sourceUpdated = false;
        private boolean effectStopped = true;
        private boolean effectUpdated = false;
        private boolean alphaStopped = true;
        private boolean alphaUpdated = false;
        private SurfaceTexture texSource;
        private SurfaceTexture texEffect;
        private SurfaceTexture texAlpha;
        private ViewHandler viewHandler;
        /**
         * aPosition
         */
        private int handlePosition;
        private int handleAttrTexCo;
        private int handleMVPMatrix;
        private int handleSTMatrixSource;
        private int handleSTMatrixEffect;
        private int handleTexSource;
        private int handleTexEffect;
        private int handleTexAlpha;
        private int texIdSource;
        private int texIdEffect;
        private int texIdAlpha;

        private float[] mvpMatrix = new float[16];
        private float[] stMSource = new float[16];
        private float[] stMEffect = new float[16];

        private DecodeThread sourceThread;
        private DecodeThread effectThread;
        private DecodeThread alphaThread;
        //        private VideoDecoder vdSource;
//        private VideoDecoder vdEffect;
//        private VideoDecoder vdAlpha;
        private VideoEncoder encoder;

        private int[] bufferARGB;
        private byte[] bufferYUV;
        private IntBuffer bufferTexture;

        private final String SHADER_VERTEX =
                "uniform mat4 uMVPMatrix;\n" +
                        "uniform mat4 uSTMSource;\n" +
                        "uniform mat4 uSTMEffect;\n" +
                        "attribute vec4 aPosition;\n" +
                        "attribute vec4 aTextureCoord;\n" +
                        "varying vec2 vTexCoSource;\n" +
                        "varying vec2 vTexCoEffect;\n" +
                        "void main() {\n" +
                        "  gl_Position = uMVPMatrix * aPosition;\n" +
                        "  vTexCoSource = (uSTMSource * aTextureCoord).xy;\n" +
                        "  vTexCoEffect = (uSTMEffect * aTextureCoord).xy;\n" +
                        "}\n";

        private final String SHADER_FRAGMENT =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "varying vec2 vTexCoSource;\n" +
                        "varying vec2 vTexCoEffect;\n" +
                        "uniform samplerExternalOES sTexSource;\n" +
                        "uniform samplerExternalOES sTexEffect;\n" +
                        "uniform samplerExternalOES sTexAlpha;\n" +
                        "void main() {\n" +
                        "  vec4 cSource =  texture2D(sTexSource, vTexCoSource);\n" +
                        "  vec4 cEffect =  texture2D(sTexEffect, vTexCoEffect);\n" +
                        "  vec4 cAlpha =  texture2D(sTexAlpha, vTexCoEffect);\n" +
                        "  gl_FragColor = cSource * (vec4(1.0) - cAlpha) + cEffect * cAlpha;\n" +
                        "}\n";

        private final float[] triangleVerticesData = {
                // X, Y, Z, U, V
                -1.0f, -1.0f, 0, 0.f, 0.f,
                1.0f, -1.0f, 0, 1.f, 0.f,
                -1.0f, 1.0f, 0, 0.f, 1.f,
                1.0f, 1.0f, 0, 1.f, 1.f,
        };

        private FloatBuffer triangleVertices;

        public FXRenderer(ViewHandler handler) {
            this.viewHandler = handler;

            triangleVertices = ByteBuffer.allocateDirect(
                    triangleVerticesData.length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            triangleVertices.put(triangleVerticesData).position(0);

            Matrix.setIdentityM(stMSource, 0);
            Matrix.setIdentityM(stMEffect, 0);
        }

        public void setRecording(boolean recording) {
            this.recording = recording;
        }

        public void setAutoStart(boolean autoStart) {
            this.autoStart = autoStart;
        }

        public void prepare(boolean external) {
            if (external) {
                externalPrepared = true;
            } else {
                surfaceCreated = true;
            }

            if (externalPrepared && surfaceCreated) {
                prepareInternal();
            }
        }


        /**
         * do not call directly
         */
        private void prepareInternal() {
            Log.e("prepareInternal", "prepared");
            program = GLUtils.createProgram(SHADER_VERTEX, SHADER_FRAGMENT);
            if (program == 0) {
                Log.e("prepare", "no program");
                return;
            }
            handlePosition = GLES20.glGetAttribLocation(program, "aPosition");
            GLUtils.checkGlError("glGetAttribLocation aPosition");
            if (handlePosition == -1) {
                throw new RuntimeException("Could not get attrib location for aPosition");
            }
            handleAttrTexCo = GLES20.glGetAttribLocation(program, "aTextureCoord");
            GLUtils.checkGlError("glGetAttribLocation aTextureCoord");
            if (handleAttrTexCo == -1) {
                throw new RuntimeException("Could not get attrib location for aTextureCoord");
            }

            handleMVPMatrix = GLES20.glGetUniformLocation(program, "uMVPMatrix");
            GLUtils.checkGlError("glGetUniformLocation uMVPMatrix");
            if (handleMVPMatrix == -1) {
                throw new RuntimeException("Could not get attrib location for uMVPMatrix");
            }

            handleSTMatrixSource = GLES20.glGetUniformLocation(program, "uSTMSource");
            GLUtils.checkGlError("glGetUniformLocation uSTMatrix");
            if (handleSTMatrixSource == -1) {
                throw new RuntimeException("Could not get attrib location for uSTMSource");
            }
            handleSTMatrixEffect = GLES20.glGetUniformLocation(program, "uSTMEffect");
            GLUtils.checkGlError("glGetUniformLocation uSTMatrix");
            if (handleSTMatrixEffect == -1) {
                throw new RuntimeException("Could not get attrib location for uSTMEffect");
            }


            prepareFrameBuffer();


            handleTexSource = GLES20.glGetUniformLocation(program, "sTexSource");
            handleTexEffect = GLES20.glGetUniformLocation(program, "sTexEffect");
            handleTexAlpha = GLES20.glGetUniformLocation(program, "sTexAlpha");
            Log.e("getHandle", "handleSource = " + handleTexSource + " handleEffect = " + handleTexEffect + " handleAlpha = " + handleTexAlpha);

            int[] textures = new int[3];
            GLES20.glGenTextures(3, textures, 0);
            texIdSource = textures[0];
            texIdEffect = textures[1];
            texIdAlpha = textures[2];

            Log.e("getTexture", "tSource = " + texIdSource + " tEffect = " + texIdEffect + " tAlpha = " + texIdAlpha);

            texSource = new SurfaceTexture(texIdSource);
            texSource.setOnFrameAvailableListener(
                    new SurfaceTexture.OnFrameAvailableListener() {
                        @Override
                        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                            Log.e("onFrameAvailable", "source");
                            synchronized (FXRenderer.this) {
                                sourceUpdated = true;
//Log.e("sync tes", "main ue = " + updateEffect + " ua = " + updateAlpha);
                                checkUpdate();
                            }
                        }
                    }

            );
            texEffect = new SurfaceTexture(texIdEffect);

            texEffect.setOnFrameAvailableListener(
                    new SurfaceTexture.OnFrameAvailableListener() {
                        @Override
                        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                            Log.e("onFrameAvailable", "effect");
                            synchronized (FXRenderer.this) {
                                effectUpdated = true;
                                checkUpdate();
                            }
                        }
                    }

            );

            texAlpha = new SurfaceTexture(texIdAlpha);

            texAlpha.setOnFrameAvailableListener(
                    new SurfaceTexture.OnFrameAvailableListener() {
                        @Override
                        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                            Log.e("onFrameAvailable", "alpha");
                            synchronized (FXRenderer.this) {
                                alphaUpdated = true;
                                checkUpdate();
                            }
                        }
                    }

            );

            frameTimeNanoSec = 1000000000L / validConfig.timeScale * validConfig.frameTick;
            final long minInterval = frameTimeNanoSec / 1000000L;

            Surface surface = new Surface(texSource);
//            vdSource = new VideoDecoder(surface, validConfig.sourcePath);
            sourceThread = new DecodeThread(validConfig.sourcePath, surface, minInterval);
            sourceThread.setOnDecodeStopListener(new DecodeThread.OnDecodeStopListener() {
                @Override
                public void onDecodeStopped() {
                    sourceStopped = true;
                    checkStop();
                }
            });
            surface.release();


            surface = new Surface(texEffect);
//            vdEffect = new VideoDecoder(surface, validConfig.effectPath);
            effectThread = new DecodeThread(validConfig.effectPath, surface, minInterval);
            effectThread.setOnDecodeStopListener(new DecodeThread.OnDecodeStopListener() {
                @Override
                public void onDecodeStopped() {
                    effectStopped = false;
                    checkStop();
                }
            });
            surface.release();

            surface = new Surface(texAlpha);
//            vdAlpha = new VideoDecoder(surface, validConfig.alphaPath);
            alphaThread = new DecodeThread(validConfig.alphaPath, surface, minInterval);
            alphaThread.setOnDecodeStopListener(new DecodeThread.OnDecodeStopListener() {
                @Override
                public void onDecodeStopped() {
                    alphaStopped = true;
                    checkStop();
                }
            });
            surface.release();


            synchronized (this) {
                sourceUpdated = false;
                effectUpdated = false;
                alphaUpdated = false;
            }

            final int frameSize = validConfig.recWidth * validConfig.recHeight;
            bufferARGB = new int[frameSize];
            if (recording) {
                bufferTexture = IntBuffer.allocate(frameSize);
                bufferYUV = new byte[frameSize * 3 / 2];
            }
            if (autoStart) {
                start();
                autoStart = false;
                viewHandler.sendAutoStart();
            }
        }

        private synchronized void checkStop() {
            if (sourceStopped && effectStopped && alphaStopped) {
                stop();
                viewHandler.sendStop();
            }
        }

        private void releaseRes() {
            GLUtils.checkGlError("release res start");

            if (program > 0) {
                GLES20.glDeleteProgram(program);
                program = -1;
            }
            int[] values = new int[1];
            if (offscreenTexture > 0) {
                values[0] = offscreenTexture;
                GLES20.glDeleteTextures(1, values, 0);
                offscreenTexture = -1;
            }
            if (frameBuffer > 0) {
                values[0] = frameBuffer;
                GLES20.glDeleteFramebuffers(1, values, 0);
                frameBuffer = -1;
            }
            if (depthBuffer > 0) {
                values[0] = depthBuffer;
                GLES20.glDeleteRenderbuffers(1, values, 0);
                depthBuffer = -1;
            }

            GLUtils.checkGlError("release res done");

        }


        public void start() {
            Log.e("start", "started");
            sourceThread.start();
            effectThread.start();
            alphaThread.start();
//            vdSource.start();
//            vdEffect.start();
//            vdAlpha.start();
            Looper.prepare();
            Choreographer.getInstance().postFrameCallback(this);
            Log.e("start", "poll first frame");
            pollNextFrame();
//            sourceThread.sendPollNextFrame();
//            effectThread.sendPollNextFrame();
//            alphaThread.sendPollNextFrame();
        }

        private void stop() {
            releaseRes();
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.e("onSurfaceCreated", "invoked");
            prepare(false);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {

        }

        @Override
        public void onDrawFrame(GL10 glUnused) {
            synchronized (this) {
                if (sourceUpdated && effectUpdated && alphaUpdated) {
                    texSource.updateTexImage();
                    texSource.getTransformMatrix(stMSource);
                    sourceUpdated = false;
                    texEffect.updateTexImage();
                    texAlpha.updateTexImage();
                    texEffect.getTransformMatrix(stMEffect);
                    effectUpdated = false;
                    alphaUpdated = false;
                }
                pollNextFrame();
            }


            GLES20.glUseProgram(program);
            GLUtils.checkGlError("glUseProgram");
            if (recording) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer);
                GLUtils.checkGlError("glBindFrameBuffer");
            }
            GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);


            GLES20.glUniform1i(handleTexSource, 0);
            GLUtils.checkGlError("glUniform1i set channel offset");
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texIdSource);
            GLUtils.checkGlError("glBindTexture sTexSource");
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);

            GLES20.glUniform1i(handleTexAlpha, 10);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE10);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texIdAlpha);
            GLUtils.checkGlError("glBindTexture sTexSource");
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);

            GLES20.glUniform1i(handleTexEffect, 5);
            GLUtils.checkGlError("glUniform1i set channel offset");
            GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texIdEffect);
            GLUtils.checkGlError("glBindTexture sTexSource");
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);


            triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(handlePosition, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
            GLUtils.checkGlError("glVertexAttribPointer maPosition");
            GLES20.glEnableVertexAttribArray(handlePosition);
            GLUtils.checkGlError("glEnableVertexAttribArray maPositionHandle");

            triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
            GLES20.glVertexAttribPointer(handleAttrTexCo, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
            GLUtils.checkGlError("glVertexAttribPointer maTextureHandle");
            GLES20.glEnableVertexAttribArray(handleAttrTexCo);
            GLUtils.checkGlError("glEnableVertexAttribArray maTextureHandle");

            Matrix.setIdentityM(mvpMatrix, 0);
            Matrix.translateM(mvpMatrix, 0, transX, transY, transZ);
            GLES20.glUniformMatrix4fv(handleMVPMatrix, 1, false, mvpMatrix, 0);
            GLES20.glUniformMatrix4fv(handleSTMatrixSource, 1, false, stMSource, 0);
            GLES20.glUniformMatrix4fv(handleSTMatrixEffect, 1, false, stMEffect, 0);


            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLUtils.checkGlError("glDrawArrays");
            if (recording) {
                recordFrame();
            }
            GLES20.glFlush();
            GLES20.glFinish();

        }

        private void recordFrame() {
            bufferTexture.rewind();
            GLES20.glReadPixels(0, 0, validConfig.recWidth, validConfig.recHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bufferTexture);
            bufferTexture.get(bufferARGB);
            switch (validConfig.colorFormat) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                    FrameUtils.encodeYUV420PFlipped(bufferYUV, bufferARGB, validConfig.recWidth, validConfig.recHeight);
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    FrameUtils.encodeYUV420SPFlipped(bufferYUV, bufferARGB, validConfig.recWidth, validConfig.recHeight);
                    break;
                default:
                    Log.e("recordFrame", "unsupported color format : [" + validConfig.colorFormat + "]");
            }
            try {
                encoder.encode(bufferYUV, 0, bufferYUV.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void setValidConfig(Config validConfig) {
            this.validConfig = validConfig;
        }

        private void prepareFrameBuffer() {
            int[] values = new int[1];


            // Create a texture object and bind it.  This will be the color buffer.
            GLES20.glGenTextures(1, values, 0);
            GLUtils.checkGlError("glGenTextures");
            offscreenTexture = values[0];   // expected > 0
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, offscreenTexture);
            GLUtils.checkGlError("glBindTexture " + offscreenTexture);


            // Create texture storage.
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, validConfig.recWidth, validConfig.recHeight, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

            // Set parameters.  We're probably using non-power-of-two dimensions, so
            // some values may not be available for use.
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLUtils.checkGlError("glTexParameter");


            GLES20.glGenFramebuffers(1, values, 0);
            GLUtils.checkGlError("glGenFramebuffers");
            frameBuffer = values[0];    // expected > 0
            Log.e("frame buffer", " = " + frameBuffer);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer);
            GLUtils.checkGlError("glBindFramebuffer " + frameBuffer);

            // Create a depth buffer and bind it.
            GLES20.glGenRenderbuffers(1, values, 0);
            GLUtils.checkGlError("glGenRenderbuffers");
            depthBuffer = values[0];    // expected > 0
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthBuffer);
            GLUtils.checkGlError("glBindRenderbuffer " + depthBuffer);

            // Allocate storage for the depth buffer.
            GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
                    validConfig.recWidth, validConfig.recHeight);
            GLUtils.checkGlError("glRenderbufferStorage");


            // Attach the depth buffer and the texture (color buffer) to the framebuffer object.
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                    GLES20.GL_RENDERBUFFER, depthBuffer);
            GLUtils.checkGlError("glFramebufferRenderbuffer");
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, offscreenTexture, 0);
            GLUtils.checkGlError("glFramebufferTexture2D");

            // See if GLES is happy with all this.
            int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("Framebuffer not complete, status=" + status);
            }
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }

        private boolean checkUpdate() {
            boolean update = sourceUpdated;
            update &= effectUpdated || effectStopped;
            update &= alphaUpdated || alphaStopped;
            if (update) {
                viewHandler.sendRender();
            }
            return update;
        }

        private void pollNextFrame() {
            sourceThread.sendPollNextFrame();
            effectThread.sendPollNextFrame();
            alphaThread.sendPollNextFrame();
//            vdAlpha.pollNextFrame();
//            vdEffect.pollNextFrame();
//            vdSource.pollNextFrame();
        }


        @Override
        public void doFrame(long frameTimeNanos) {
            Log.e("doFrame", "invoked");

//            if (checkUpdate()) {
//                Choreographer.getInstance().postFrameCallbackDelayed(this, frameTimeNanoSec);
//            } else {
//                Choreographer.getInstance().postFrameCallback(this);
//            }
            checkUpdate();
            Choreographer.getInstance().postFrameCallback(this);
            pollNextFrame();
        }
    }
}
