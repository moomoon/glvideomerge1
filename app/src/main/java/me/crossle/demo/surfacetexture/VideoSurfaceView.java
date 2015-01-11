package me.crossle.demo.surfacetexture;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

@SuppressLint("ViewConstructor")
class VideoSurfaceView extends GLSurfaceView {

    VideoRender mRenderer;
    private MediaPlayer mMediaPlayer0 = null;
    private MediaPlayer mMediaPlayer1 = null;
    private int mHeight, mWidth;
    private float mOffsetX, mOffsetY;

    public VideoSurfaceView(Context context, MediaPlayer mp0, MediaPlayer mp1) {
        super(context);

        setEGLContextClientVersion(2);
        mMediaPlayer0 = mp0;
        mMediaPlayer1 = mp1;
        mRenderer = new VideoRender(context);
        setRenderer(mRenderer);
        mDetector = new GestureDetector(context,new GestureDetector.OnGestureListener(){
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
    public void onResume() {
        queueEvent(new Runnable() {
            public void run() {
                mRenderer.setMediaPlayer(mMediaPlayer0, mMediaPlayer1);
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

        private final String mVertexShader =
                "uniform mat4 uMVPMatrix;\n" +
                        "uniform mat4 uSTMatrix0;\n" +
                        "uniform mat4 uSTMatrix1;\n" +
                        "attribute vec4 aPosition;\n" +
                        "attribute vec4 aTextureCoord;\n" +
                        "varying vec2 vTextureCoord0;\n" +
                        "varying vec2 vTextureCoord1;\n" +
                        "void main() {\n" +
                        "  gl_Position = uMVPMatrix * aPosition;\n" +
                        "  vTextureCoord0 = (uSTMatrix0 * aTextureCoord).xy;\n" +
                        "  vTextureCoord1 = (uSTMatrix1 * aTextureCoord).xy;\n" +
                        "}\n";

        private final String mFragmentShader =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "varying vec2 vTextureCoord0;\n" +
                        "varying vec2 vTextureCoord1;\n" +
                        "uniform samplerExternalOES sTexture0;\n" +
                        "uniform samplerExternalOES sTexture1;\n" +
                        "void main() {\n" +
                        "  vec4 tempColor0 =  texture2D(sTexture0, vTextureCoord0);\n" +
                        "  vec4 tempColor1 =  texture2D(sTexture1, vTextureCoord1);\n" +
//                        "  gl_FragColor = vec4(tempColor0.x + tempColor1.x, tempColor1.y, 0, 1);\n" +
//                        "  gl_FragColor = tempColor0;\n" +
                        "gl_FragColor = tempColor0 + tempColor1 * 0.5;\n" +
                        "}\n";

        private float[] mMVPMatrix = new float[16];
        private float[] mSTMatrix0 = new float[16];
        private float[] mSTMatrix1 = new float[16];

        private int mProgram;
        private int[] textures;
        private int muMVPMatrixHandle;
        private int muSTMatrixHandle0;
        private int muSTMatrixHandle1;
        private int maPositionHandle;
        private int maTextureHandle;

        private SurfaceTexture surface0;
        private SurfaceTexture surface1;
        private boolean updateSurface0 = false;
        private boolean updateSurface1 = false;

        private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

        private MediaPlayer mediaPlayer0;
        private MediaPlayer mediaPlayer1;

        private float transX, transY, transZ;

        public void setTranslate(float x, float y, float z){
            this.transX = x;
            this.transY = y;
            this.transZ = z;
        }

        public VideoRender(Context context) {
            mTriangleVertices = ByteBuffer.allocateDirect(
                    mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTriangleVertices.put(mTriangleVerticesData).position(0);

            Matrix.setIdentityM(mSTMatrix0, 0);
            Matrix.setIdentityM(mSTMatrix1, 0);
        }

        public void setMediaPlayer(MediaPlayer player0, MediaPlayer player1) {
            mediaPlayer0 = player0;
            mediaPlayer1 = player1;
        }

        @Override
        public void onDrawFrame(GL10 glUnused) {
            synchronized (this) {
                if (updateSurface0) {
                    surface0.updateTexImage();
                    surface0.getTransformMatrix(mSTMatrix0);
                    updateSurface0 = false;
                }
                if (updateSurface1) {
                    surface1.updateTexImage();
                    surface1.getTransformMatrix(mSTMatrix1);
                    updateSurface1 = false;
                }
            }


            GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[0]);

            checkGlError("bind 0");
//            int t1h = GLES20.glGetUniformLocation ( mProgram, "sTexture0" );
//            //        GLES20.glUniform1i(t1h, textures[0]);
//            GLES20.glUniform1i(t1h, 1);
//            checkGlError("gluniform10");


            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[1]);
            checkGlError("bind 1");
//            int t2h = GLES20.glGetUniformLocation ( mProgram, "sTexture1" );
//            //        GLES20.glUniform1i(t1h, textures[0]);
//            GLES20.glUniform1i(t2h, 2);
//            checkGlError("gluniform11");

            GLES20.glUseProgram(mProgram);
            checkGlError("glUseProgram");

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
            GLES20.glUniformMatrix4fv(muSTMatrixHandle0, 1, false, mSTMatrix0, 0);
            GLES20.glUniformMatrix4fv(muSTMatrixHandle1, 1, false, mSTMatrix1, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            checkGlError("glDrawArrays");
            GLES20.glFinish();

        }

        @Override
        public void onSurfaceChanged(GL10 glUnused, int width, int height) {

        }

        @Override
        public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
            mProgram = createProgram(mVertexShader, mFragmentShader);
            if (mProgram == 0) {
                return;
            }
            GLES20.glUseProgram(mProgram);
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

            muSTMatrixHandle0 = GLES20.glGetUniformLocation(mProgram, "uSTMatrix0");
            checkGlError("glGetUniformLocation uSTMatrix");
            if (muSTMatrixHandle0 == -1) {
                throw new RuntimeException("Could not get attrib location for uSTMatrix");
            }
            muSTMatrixHandle1 = GLES20.glGetUniformLocation(mProgram, "uSTMatrix1");
            checkGlError("glGetUniformLocation uSTMatrix");
            if (muSTMatrixHandle1 == -1) {
                throw new RuntimeException("Could not get attrib location for uSTMatrix");
            }

            textures = new int[2];
            GLES20.glGenTextures(2, textures, 0);


            int t0h = GLES20.glGetUniformLocation(mProgram, "sTexture0");
            Log.e("t0h","= " + t0h);
            GLES20.glUniform1i(t0h, 0);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[0]);
//            int t0h = GLES20.glGetUniformLocation ( mProgram, "sTexture0" );
//            //        GLES20.glUniform1i(t1h, textures[0]);
//            GLES20.glUniform1i(t0h, 0);


//            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[0]);
            checkGlError("glBindTexture mTextureID");

            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);




            int t1h = GLES20.glGetUniformLocation(mProgram, "sTexture1");
            Log.e("t1h","= " + t1h);
            GLES20.glUniform1i(t1h, 1);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[1]);

            checkGlError("glBindTexture mTextureID");

            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);

            /*
             * Create the SurfaceTexture that will feed this textureID,
             * and pass it to the MediaPlayer
             */
            surface0 = new SurfaceTexture(textures[0]);
            surface0.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {

                    updateSurface0 = true;
                }
            });
            surface1 = new SurfaceTexture(textures[1]);
            surface1.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    updateSurface1 = true;
                }
            });

            Surface surface = new Surface(surface0);
            mediaPlayer0.setSurface(surface);
            mediaPlayer0.setScreenOnWhilePlaying(true);
            surface.release();
            surface = new Surface(surface1);
            mediaPlayer1.setSurface(surface);
            mediaPlayer1.setScreenOnWhilePlaying(true);
            surface.release();

            try {
                mediaPlayer0.prepare();
                mediaPlayer1.prepare();
            } catch (IOException t) {
                Log.e(TAG, "media player prepare failed");
            }

            synchronized (this) {
                updateSurface0 = false;
                updateSurface1 = false;
            }

            mediaPlayer0.start();
            mediaPlayer1.start();
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
