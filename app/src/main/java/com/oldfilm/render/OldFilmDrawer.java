package com.oldfilm.render;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.oldfilm.R;
import com.oldfilm.gles.GlUtil;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * OldFilmDrawer class.
 */
public class OldFilmDrawer extends BaseDrawer {

    private int muSepiaLoc;
    private int muNoiseLoc;
    private int muScratchLoc;
    private int muInnerVignettingLoc;
    private int muOuterVignettingLoc;
    private int muRandomLoc;
    private int muTimeLapseLoc;

    private float mSepiaValue = 0.5f; // 0.0 - 1.0.
    private float mNoiseValue = 0.5f; // 0.0 - 1.0.
    private float mScratchValue = 0.5f; // 0.0 - 1.0.
    private float mVignettingValue = 0.5f; // 0.0 - 1.0.

    private float mRandomValue = 0.0f;
    private float mTimeLapse = 0.0f;

    private Random mRandom = new Random();
    private Random mTimeLapseRandom = new Random();

    public OldFilmDrawer(Context context, int textureID) {
        super(context, textureID, R.raw.old_film_vertex_shader, R.raw.old_film_fragment_shader);

        muSepiaLoc = GLES20.glGetUniformLocation(mProgramId, "SepiaValue");
        GlUtil.checkLocation(muSepiaLoc, "SepiaValue");
        muNoiseLoc = GLES20.glGetUniformLocation(mProgramId, "NoiseValue");
        GlUtil.checkLocation(muNoiseLoc, "NoiseValue");
        muScratchLoc = GLES20.glGetUniformLocation(mProgramId, "ScratchValue");
        GlUtil.checkLocation(muScratchLoc, "ScratchValue");

        muInnerVignettingLoc = GLES20.glGetUniformLocation(mProgramId, "InnerVignetting");
        GlUtil.checkLocation(muInnerVignettingLoc, "InnerVignetting");
        muOuterVignettingLoc = GLES20.glGetUniformLocation(mProgramId, "OuterVignetting");
        GlUtil.checkLocation(muOuterVignettingLoc, "OuterVignetting");
        muRandomLoc = GLES20.glGetUniformLocation(mProgramId, "RandomValue");
        GlUtil.checkLocation(muRandomLoc, "RandomValue");
        muTimeLapseLoc = GLES20.glGetUniformLocation(mProgramId, "TimeLapse");
        GlUtil.checkLocation(muTimeLapseLoc, "TimeLapse");
    }

    public void setSepiaValue(float sepia) {
        mSepiaValue = sepia;
    }

    public void setNoiseValue(float noise) {
        mNoiseValue = noise;
    }

    public void setScratchValue(float scratch) {
        mScratchValue = scratch;
    }

    public void setVignettingValue(float vignetting) {
        mVignettingValue = vignetting;
    }

    @Override
    public void draw(ByteBuffer byteBuffer, int width, int height, long timestamp) {
        if (!mRendererStarted) {
            return;
        }

        mRandomValue = (float) mRandom.nextInt(100) / 100f;
        mTimeLapse = 1000 * ((float) mTimeLapseRandom.nextInt(100) / 50f);

        GLES20.glUseProgram(mProgramId);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GLES20.GL_SAMPLER_2D, mTextureID);
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);

        GLES20.glUniform1f(muSepiaLoc, mSepiaValue);
        GlUtil.checkGlError("glUniform1f - mSepiaValue");
        GLES20.glUniform1f(muNoiseLoc, mNoiseValue);
        GlUtil.checkGlError("glUniform1f - mNoiseValue");
        GLES20.glUniform1f(muScratchLoc, mScratchValue);
        GlUtil.checkGlError("glUniform1f - mScratchValue");

        GLES20.glUniform1f(muInnerVignettingLoc, 1.0f - mVignettingValue);
        GlUtil.checkGlError("glUniform1f - mInnerVignetting");
        GLES20.glUniform1f(muOuterVignettingLoc, 1.4f - mVignettingValue);
        GlUtil.checkGlError("glUniform1f - mOuterVignetting");
        GLES20.glUniform1f(muRandomLoc, mRandomValue);
        GlUtil.checkGlError("glUniform1f - mRandomValue");
        GLES20.glUniform1f(muTimeLapseLoc, mTimeLapse);
        GlUtil.checkGlError("glUniform1f - mTimeLapse");

        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, sProjectionMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTMMatrixHandle, 1, false, mSTMatrix, 0);

        mVertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, 3, GLES20.GL_FLOAT, false,
                12, mVertexBuffer);

        mTextureVertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
        GLES20.glVertexAttribPointer(aTextureCoordLocation, 2, GLES20.GL_FLOAT, false, 8, mTextureVertexBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glUseProgram(0);
    }
}