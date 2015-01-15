package me.crossle.demo.surfacetexture;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

@SuppressLint("ViewConstructor")
class VideoSurfaceView extends GLSurfaceView {

    VideoRender mRenderer;
    private MediaPlayer mMPSource = null;
    private MediaPlayer mMPEffect = null;
    private MediaPlayer mMPAlpha = null;
    private int mHeight, mWidth;
    private float mOffsetX, mOffsetY;
    private static final boolean ENCODING = false;


    public VideoSurfaceView(Context context, MediaPlayer mpSource, MediaPlayer mpEffect, MediaPlayer mpAlpha) {
        super(context);

        setEGLContextClientVersion(2);
        this.mMPSource = mpSource;
        this.mMPEffect = mpEffect;
        this.mMPAlpha = mpAlpha;
        mRenderer = new VideoRender(context);
        setRenderer(mRenderer);
        mDetector = new GestureDetector(context, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                int height = mHeight == 0 ? 1 : mHeight;
                int width = mWidth == 0 ? 1 : mWidth;
                mOffsetX -= distanceX;
                mOffsetY += distanceY;
                mRenderer.setTranslate(mOffsetX / width, mOffsetY / height, 0);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }
        });
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        this.mHeight = MeasureSpec.getSize(heightMeasureSpec);
        this.mWidth = MeasureSpec.getSize(widthMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    @Override
    public void onPause() {
        super.onPause();
//        try {
//            mRenderer.androidEncoder.finish();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onResume() {
        queueEvent(new Runnable() {
            public void run() {
                mRenderer.setMediaPlayer(mMPSource, mMPEffect, mMPAlpha);
            }
        });

        super.onResume();
    }


    private GestureDetector mDetector;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDetector.onTouchEvent(event);
        return true;
//        return super.onTouchEvent(event);
    }

    private static class VideoRender
            implements GLSurfaceView.Renderer {
        private static String TAG = "VideoRender";

        private static final int FLOAT_SIZE_BYTES = 4;
        private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
        private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
        private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
        private final float[] mTriangleVerticesData = {
                // X, Y, Z, U, V
                -1.0f, -1.0f, 0, 0.f, 0.f,
                1.0f, -1.0f, 0, 1.f, 0.f,
                -1.0f, 1.0f, 0, 0.f, 1.f,
                1.0f, 1.0f, 0, 1.f, 1.f,
        };

        private FloatBuffer mTriangleVertices;
        private File file = new File(Environment.getExternalStorageDirectory() + "/test.mp4");

        private final String mVertexShader =
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

        private final String mFragmentShader =
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
//                        "  if(pos < 0.6) {\n" +
//                        "     gl_FragColor = cSource;\n" +
//                        "  } else if(pos < 1.3) {\n" +
//                        "     gl_FragColor = cEffect;\n" +
//                        "  } else {\n" +
//                        "     gl_FragColor = cAlpha;\n" +
//                        "  }\n" +
//                        "}\n";
//                        "  gl_FragColor = cAlpha;\n" +
                        "  gl_FragColor = cSource * (vec4(1.0) - cAlpha) + cEffect * cAlpha;\n" +
//                        "  gl_FragColor = cSource;\n" +
//                        "  gl_FragColor = cSource * cAlpha;\n" +
//                        "  gl_FragColor = cAlpha * vec4(1.0);\n" +

                        "}\n";
        private float[] mMVPMatrix = new float[16];
        private float[] mSTMSource = new float[16];
        private float[] mSTMEffect = new float[16];

        private int mProgram;
        private int[] textures;
        private int muMVPMatrixHandle;
        private int muSTMatrixHandleSource;
        private int muSTMatrixHandleEffect;
        private int maPositionHandle;
        private int maTextureHandle;
        private int usTexSourceHandle;
        private int usTexEffectHandle;
        private int usTexAlphaHandle;

        private SurfaceTexture sTexSource;
        private SurfaceTexture sTexEffect;
        private SurfaceTexture sTexAlpha;
        private boolean updateSource = false;
        private boolean updateEffect = false;
        private boolean updateAlpha = false;

        private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

        private MediaPlayer mpSource;
        private MediaPlayer mpEffect;
        private MediaPlayer mpAlpha;

        private float transX, transY, transZ;

        private IntBuffer fbTexture;
//        private ByteBuffer fbTextureByte;

        private final int recWidth = 1084;
        private final int recHeight = 768;

        private int[] pixels = new int[recWidth * recHeight];
        private byte[] yuv = new byte[recWidth * recHeight * 3 / 2];

//        Bitmap cache;


        public void setTranslate(float x, float y, float z) {
            this.transX = x;
            this.transY = y;
            this.transZ = z;
        }

        public VideoRender(Context context) {
            mTriangleVertices = ByteBuffer.allocateDirect(
                    mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTriangleVertices.put(mTriangleVerticesData).position(0);

            Matrix.setIdentityM(mSTMSource, 0);
            Matrix.setIdentityM(mSTMEffect, 0);

            fbTexture = IntBuffer.allocate(recWidth * recHeight);
//            fbTextureByte = ByteBuffer.allocate(640 * 480 * 4);
        }

        public void setMediaPlayer(MediaPlayer pSource, MediaPlayer pEffect, MediaPlayer pAlpha) {
            mpSource = pSource;
            mpEffect = pEffect;
            mpAlpha = pAlpha;
        }


        boolean effectPaused = false;
        boolean alphaPaused = false;
        int effectPosition = 0;
        int alphaPosition = 0;
        final int diffTolerance = 10;

        @Override
        public void onDrawFrame(GL10 glUnused) {
            synchronized (this) {
                if (updateSource) {
                    sTexSource.updateTexImage();
                    sTexSource.getTransformMatrix(mSTMSource);
                    updateSource = false;
                }
                if (updateEffect && updateAlpha) {
                    sTexEffect.updateTexImage();
                    sTexAlpha.updateTexImage();
                    sTexEffect.getTransformMatrix(mSTMEffect);
                    updateEffect = false;
                    updateAlpha = false;
                }
//                int sourcePosition = mpSource.getCurrentPosition();
                effectPosition = mpEffect.getCurrentPosition();
                alphaPosition = mpAlpha.getCurrentPosition();
//                if (effectPosition > alphaPosition) {
//                    try {
//                        synchronized (mpEffect) {
//                            mpEffect.wait(effectPosition - alphaPosition);
//                        }
//                    } catch (InterruptedException e) {
//                        Log.e("position check", "interrupted  mpEffect");
//                        e.printStackTrace();
//                    }
//                } else if (effectPosition < alphaPosition) {
//                    try {
//                        synchronized (mpAlpha) {
//                            mpAlpha.wait(alphaPosition - effectPosition);
//                        }
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                        Log.e("position check", "interrupted  mpAlpha");
//                    }
//                }
                if (effectPaused) {
                    if (alphaPosition >= effectPosition) {
                        mpEffect.start();
                        effectPaused = false;
                        Log.e("position check", "resumed effect");
                    }
                } else if (alphaPaused) {
                    if (effectPosition >= alphaPosition) {
                        mpAlpha.start();

                        alphaPaused = false;
                        Log.e("position check", "resumed alpha");
                    }
                } else if (effectPosition - alphaPosition > diffTolerance) {
                    mpEffect.pause();
                    effectPaused = true;
                    Log.e("position check", "paused effect");
                } else if (alphaPosition - effectPosition > diffTolerance) {
                    mpAlpha.pause();
                    alphaPaused = true;
                    Log.e("position check", "paused alpha");
                }

//                int diff = effectPosition - alphaPosition;

                Log.e("position check", "e = " + effectPosition + " a = " + alphaPosition + " diff = " + (effectPosition - alphaPosition));
            }


            GLES20.glUseProgram(mProgram);
            checkGlError("glUseProgram");

            GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);


            GLES20.glUniform1i(usTexSourceHandle, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[0]);
            checkGlError("glBindTexture sTexSource");
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);


            GLES20.glUniform1i(usTexEffectHandle, 5);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[1]);
            checkGlError("glBindTexture sTexSource");
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);


            GLES20.glUniform1i(usTexAlphaHandle, 10);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE10);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[2]);
            checkGlError("glBindTexture sTexSource");
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);

//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[0]);
//            checkGlError("bind texture 0");
//
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
//            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[1]);
//            checkGlError("bind textture 1");
//
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE10);
//            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[2]);
//            checkGlError("bind textture 2");


            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer maPosition");
            GLES20.glEnableVertexAttribArray(maPositionHandle);
            checkGlError("glEnableVertexAttribArray maPositionHandle");

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
            GLES20.glVertexAttribPointer(maTextureHandle, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer maTextureHandle");
            GLES20.glEnableVertexAttribArray(maTextureHandle);
            checkGlError("glEnableVertexAttribArray maTextureHandle");

            Matrix.setIdentityM(mMVPMatrix, 0);
            Matrix.translateM(mMVPMatrix, 0, transX, transY, transZ);
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muSTMatrixHandleSource, 1, false, mSTMSource, 0);
            GLES20.glUniformMatrix4fv(muSTMatrixHandleEffect, 1, false, mSTMEffect, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            checkGlError("glDrawArrays");
            fbTexture.position(0);


//            GLES20.glReadPixels(0, 0, 500, 500, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, fbTexture);
//            GLES20.glReadPixels(0, 0, 640, 480, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, fbTextureByte);


//            GLES20.glReadPixels(0, 0, recWidth, recHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, fbTexture);
//            fbTexture.position(0);
//            fbTexture.get(pixels);
//            if (ENCODING) {
//                FrameUtils.encodeYUV420SPFlipped(yuv, pixels, recWidth, recHeight);
//                try {
//                    encoder.encode(yuv, 0, yuv.length);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
            GLES20.glFlush();
            GLES20.glFinish();

        }

        private VideoEncoder encoder = VideoEncoder.create(Environment.getExternalStorageDirectory() + File.separator + "encoded.h264", recWidth, recHeight, 30);


        @Override
        public void onSurfaceChanged(GL10 glUnused, int width, int height) {

        }

        @Override
        public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
            if (file.exists()) {
                file.delete();
            }
            mProgram = createProgram(mVertexShader, mFragmentShader);
            if (mProgram == 0) {
                return;
            }
//            GLES20.glUseProgram(mProgram);
            maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
            checkGlError("glGetAttribLocation aPosition");
            if (maPositionHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aPosition");
            }
            maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
            checkGlError("glGetAttribLocation aTextureCoord");
            if (maTextureHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aTextureCoord");
            }

            muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            checkGlError("glGetUniformLocation uMVPMatrix");
            if (muMVPMatrixHandle == -1) {
                throw new RuntimeException("Could not get attrib location for uMVPMatrix");
            }

            muSTMatrixHandleSource = GLES20.glGetUniformLocation(mProgram, "uSTMSource");
            checkGlError("glGetUniformLocation uSTMatrix");
            if (muSTMatrixHandleSource == -1) {
                throw new RuntimeException("Could not get attrib location for uSTMSource");
            }
            muSTMatrixHandleEffect = GLES20.glGetUniformLocation(mProgram, "uSTMEffect");
            checkGlError("glGetUniformLocation uSTMatrix");
            if (muSTMatrixHandleEffect == -1) {
                throw new RuntimeException("Could not get attrib location for uSTMEffect");
            }

            textures = new int[3];
            GLES20.glGenTextures(3, textures, 0);


            usTexSourceHandle = GLES20.glGetUniformLocation(mProgram, "sTexSource");
            usTexEffectHandle = GLES20.glGetUniformLocation(mProgram, "sTexEffect");
            usTexAlphaHandle = GLES20.glGetUniformLocation(mProgram, "sTexAlpha");
//            GLES20.glUniform1i(, 0);
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[0]);
//            checkGlError("glBindTexture sTexSource");
//            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
//                    GLES20.GL_NEAREST);
//            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
//                    GLES20.GL_LINEAR);
//
//
//            GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgram, "sTexEffect"), 5);
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
//            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[1]);
//            checkGlError("glBindTexture sTexEffect");
//            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
//                    GLES20.GL_NEAREST);
//            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
//                    GLES20.GL_LINEAR);
//
//
//            GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgram, "sTexAlpha"), 10);
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
//            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[2]);
//            checkGlError("glBindTexture sTexAlpha");
//            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
//                    GLES20.GL_NEAREST);
//            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
//                    GLES20.GL_LINEAR);

            /*
             * Create the SurfaceTexture that will feed this textureID,
             * and pass it to the MediaPlayer
             */
            sTexSource = new SurfaceTexture(textures[0]);
            sTexSource.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    synchronized (VideoRender.this) {
                        updateSource = true;
                    }
                }
            });
            sTexEffect = new SurfaceTexture(textures[1]);
            sTexEffect.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    synchronized (VideoRender.this) {
                        updateEffect = true;
                    }
                }
            });
            sTexAlpha = new SurfaceTexture(textures[2]);
            sTexAlpha.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    synchronized (VideoRender.this) {
                        updateAlpha = true;
                    }
                }
            });


            Surface surface = new Surface(sTexSource);
            mpSource.setSurface(surface);
            mpSource.setScreenOnWhilePlaying(true);
            surface.release();
            surface = new Surface(sTexEffect);
            mpEffect.setSurface(surface);
            mpEffect.setScreenOnWhilePlaying(true);
            surface.release();
            surface = new Surface(sTexAlpha);
            mpAlpha.setSurface(surface);
            mpAlpha.setScreenOnWhilePlaying(true);
            surface.release();

            try {
                mpSource.prepare();
                mpEffect.prepare();
                mpAlpha.prepare();
            } catch (IOException e) {
                Log.e(TAG, "media player prepare failed");
                e.printStackTrace();
            }

            synchronized (this) {
                updateSource = false;
                updateEffect = false;
                updateAlpha = false;
                mpSource.start();
                mpEffect.start();
                mpAlpha.start();
            }

            if (ENCODING) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.e("encode end", "send eos");
                            encoder.encode(null, 0, -1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 20000);
            }
        }


        private int loadShader(int shaderType, String source) {
            int shader = GLES20.glCreateShader(shaderType);
            if (shader != 0) {
                GLES20.glShaderSource(shader, source);
                GLES20.glCompileShader(shader);
                int[] compiled = new int[1];
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
                if (compiled[0] == 0) {
                    Log.e(TAG, "Could not compile shader " + shaderType + ":");
                    Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                    GLES20.glDeleteShader(shader);
                    shader = 0;
                }
            }
            return shader;
        }

        private int createProgram(String vertexSource, String fragmentSource) {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            if (vertexShader == 0) {
                return 0;
            }
            int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            if (pixelShader == 0) {
                return 0;
            }

            int program = GLES20.glCreateProgram();
            if (program != 0) {
                GLES20.glAttachShader(program, vertexShader);
                checkGlError("glAttachShader");
                GLES20.glAttachShader(program, pixelShader);
                checkGlError("glAttachShader");
                GLES20.glLinkProgram(program);
                int[] linkStatus = new int[1];
                GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
                if (linkStatus[0] != GLES20.GL_TRUE) {
                    Log.e(TAG, "Could not link program: ");
                    Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                    GLES20.glDeleteProgram(program);
                    program = 0;
                }
            }
            return program;
        }

        private void checkGlError(String op) {
            int error;
            while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                Log.e(TAG, op + ": glError " + error);
                throw new RuntimeException(op + ": glError " + error);
            }
        }

    }  // End of class VideoRender.

}  // End of class VideoSurfaceView.
