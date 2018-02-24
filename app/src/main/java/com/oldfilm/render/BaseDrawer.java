package com.oldfilm.render;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.annotation.RawRes;

import com.oldfilm.gles.ShaderUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * BaseDrawer class.
 */
public class BaseDrawer {

    protected static final float[] sVertexData = {
            1f, -1f, 0f,
            -1f, -1f, 0f,
            1f, 1f, 0f,
            -1f, 1f, 0f
    };

    protected static final float[] sTextureVertexData = {
            1f, 0f,
            0f, 0f,
            1f, 1f,
            0f, 1f
    };

    protected static final float[] sProjectionMatrix = new float[16];

    protected Context mContext;
    protected int mTextureID;

    protected int mProgramId;

    protected FloatBuffer mVertexBuffer;
    protected FloatBuffer mTextureVertexBuffer;

    protected int aPositionLocation;
    protected int uMatrixLocation;
    protected int uTextureSamplerLocation;
    protected int aTextureCoordLocation;
    protected int uSTMMatrixHandle;

    protected float[] mSTMatrix = new float[16];

    protected int mScreenWidth;
    protected int mScreenHeight;

    protected int mVideoWidth;
    protected int mVideoHeight;

    protected boolean mRendererStarted = false;

    protected BaseDrawer(Context context, int textureID) {
        mContext = context;
        mTextureID = textureID;
    }

    protected BaseDrawer(Context context, int textureID, @RawRes final int vertexResId, @RawRes final int fragmentResId) {
        this(context, textureID);

        mVertexBuffer = ByteBuffer.allocateDirect(sVertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(sVertexData);
        mVertexBuffer.position(0);

        mTextureVertexBuffer = ByteBuffer.allocateDirect(sTextureVertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(sTextureVertexData);
        mTextureVertexBuffer.position(0);

        final String vertexShaderString = ShaderUtils.readTextFromRawResource(context, vertexResId);
        final String fragmentShaderString = ShaderUtils.readTextFromRawResource(context, fragmentResId);

        final int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderString);
        final int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderString);

        mProgramId = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgramId, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgramId, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgramId);                  // creates OpenGL ES program executables

        aPositionLocation = GLES20.glGetAttribLocation(mProgramId, "aPosition");
        uMatrixLocation = GLES20.glGetUniformLocation(mProgramId, "uMatrix");
        uSTMMatrixHandle = GLES20.glGetUniformLocation(mProgramId, "uSTMatrix");
        uTextureSamplerLocation = GLES20.glGetUniformLocation(mProgramId, "sTexture");
        aTextureCoordLocation = GLES20.glGetAttribLocation(mProgramId, "aTexCoord");
    }

    public void startRender() {
        mRendererStarted = true;
    }

    public void stopRender() {
        mRendererStarted = false;
    }

    public float[] getMatrix() {
        return mSTMatrix;
    }

    public void updateProjection(int screenWidth, int screenHeight, int videoWidth, int videoHeight) {
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;

        mVideoWidth = videoWidth;
        mVideoHeight = videoHeight;

        float screenRatio = (float) screenWidth / screenHeight;
        float videoRatio = (float) videoWidth / videoHeight;
        if (videoRatio > screenRatio) {
            Matrix.orthoM(sProjectionMatrix, 0, -1f, 1f, -videoRatio / screenRatio, videoRatio / screenRatio, -1f, 1f);
        } else {
            Matrix.orthoM(sProjectionMatrix, 0, -screenRatio / videoRatio, screenRatio / videoRatio, -1f, 1f, -1f, 1f);
        }
    }

    public void draw(ByteBuffer byteBuffer, int width, int height, long timestamp) {
        if (!mRendererStarted) {
            return;
        }

        GLES20.glUseProgram(mProgramId);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_SAMPLER_2D, mTextureID);

        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, sProjectionMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTMMatrixHandle, 1, false, mSTMatrix, 0);

        mVertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, 3, GLES20.GL_FLOAT, false,
                12, mVertexBuffer);

        mTextureVertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
        GLES20.glVertexAttribPointer(aTextureCoordLocation, 2, GLES20.GL_FLOAT, false, 8, mTextureVertexBuffer);

        GLES20.glViewport(0, 0, mScreenWidth, mScreenHeight);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glUseProgram(0);
    }

    public void destroy() {
        stopRender();
    }

    protected static int loadShader(int type, String shaderCode) {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }
}
