package me.crossle.demo.surfacetexture;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Choreographer;

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
    private boolean mRecording;
    private int mViewWidth, mViewHeight;

    public FXMovieView(Context context) {
        super(context);
    }

    public void config(Config config) {
        config.validate();
        this.mConfig = config;
        setStatus(STATUS_CONFIGURED);
        tryUpdateSize();
    }

    public void prepare() {
        setStatus(STATUS_PREPARED);
    }

    public void start() {
        setStatus(STATUS_STARTED);
    }

    public void stop() {
        setStatus(STATUS_STOPPED);
    }

    public void setOutputFilePath(String outputFilePath) {
        this.mOutputFilePath = outputFilePath;
    }

    public void toggleRecord(boolean rec) {
        if (rec && TextUtils.isEmpty(mOutputFilePath)) {
            throw new NullPointerException("set record to true while outputFilePath = " + mOutputFilePath);
        }
        if (rec ^ mRecording) {
            if (mStatus != STATUS_PREPARED) {
                throw new IllegalStateException("you have to prepare " + getClass() + " before ");
            }
            toggleRecordInternal();
        }
    }

    private void toggleRecordInternal() {

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
        }
    }

    private static class DecodeThread extends Thread {

    }

    private static class FXRender implements Renderer, Choreographer.FrameCallback {

        private int frameBuffer;
        private int offscreenTexture;
        private int depthBuffer;
        private long frameTimeNanoSec = 0L;
        private boolean recording = false;
        private Config validConfig;
        private boolean sourceStopped = true;
        private boolean sourceUpdated = false;
        private boolean effectStopped = true;
        private boolean effectUpdated = false;
        private boolean alhpaStopped = true;
        private boolean alphaUpdated = false;

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
                        "  float pos = vTexCoSource.x + vTexCoSource.y;\n" +
                        "  gl_FragColor = cSource * (vec4(1.0) - cAlpha) + cEffect * cAlpha;\n" +
                        "}\n";

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {

        }

        @Override
        public void onDrawFrame(GL10 gl) {

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

        private void checkUpdate() {

        }


        @Override
        public void doFrame(long frameTimeNanos) {
            Choreographer.getInstance().postFrameCallbackDelayed(this, frameTimeNanos);
        }
    }
}
