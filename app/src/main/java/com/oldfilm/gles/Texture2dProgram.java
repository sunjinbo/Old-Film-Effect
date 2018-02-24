/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oldfilm.gles;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import com.oldfilm.R;

import java.nio.FloatBuffer;
import java.util.Random;

import static com.oldfilm.gles.Texture2dProgram.ProgramType.TEXTURE_EXT_FILTER;
import static com.oldfilm.gles.Texture2dProgram.ProgramType.TEXTURE_OLD_CINEMA;

/**
 * GL program and supporting functions for textured 2D shapes.
 */
public class Texture2dProgram {
    private static final String TAG = GlUtil.TAG;

    public enum ProgramType {
        TEXTURE_2D, TEXTURE_EXT, TEXTURE_EXT_BW, TEXTURE_EXT_FILT, TEXTURE_EXT_FILTER, TEXTURE_OLD_CINEMA
    }

    // Simple vertex shader, used for all programs.
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uTexMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "    gl_Position = uMVPMatrix * aPosition;\n" +
            "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
            "}\n";

    // Simple fragment shader for use with "normal" 2D textures.
    private static final String FRAGMENT_SHADER_2D =
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform sampler2D sTexture;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
            "}\n";

    // Simple fragment shader for use with external 2D textures (e.g. what we get from
    // SurfaceTexture).
    private static final String FRAGMENT_SHADER_EXT =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
            "}\n";

    // Fragment shader that converts color to black & white with a simple transformation.
    private static final String FRAGMENT_SHADER_EXT_BW =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
            "    float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;\n" +
            "    gl_FragColor = vec4(color, color, color, 1.0);\n" +
            "}\n";

    // Fragment shader with a convolution filter.  The upper-left half will be drawn normally,
    // the lower-right half will have the filter applied, and a thin red line will be drawn
    // at the border.
    //
    // This is not optimized for performance.  Some things that might make this faster:
    // - Remove the conditionals.  They're used to present a half & half view with a red
    //   stripe across the middle, but that's only useful for a demo.
    // - Unroll the loop.  Ideally the compiler does this for you when it's beneficial.
    // - Bake the filter kernel into the shader, instead of passing it through a uniform
    //   array.  That, combined with loop unrolling, should reduce memory accesses.
    public static final int KERNEL_SIZE = 9;
    private static final String FRAGMENT_SHADER_EXT_FILT =
            "#extension GL_OES_EGL_image_external : require\n" +
            "#define KERNEL_SIZE " + KERNEL_SIZE + "\n" +
            "precision highp float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform float uKernel[KERNEL_SIZE];\n" +
            "uniform vec2 uTexOffset[KERNEL_SIZE];\n" +
            "uniform float uColorAdjust;\n" +
            "void main() {\n" +
            "    int i = 0;\n" +
            "    vec4 sum = vec4(0.0);\n" +
            "    if (vTextureCoord.x < vTextureCoord.y - 0.005) {\n" +
            "        for (i = 0; i < KERNEL_SIZE; i++) {\n" +
            "            vec4 texc = texture2D(sTexture, vTextureCoord + uTexOffset[i]);\n" +
            "            sum += texc * uKernel[i];\n" +
            "        }\n" +
            "    sum += uColorAdjust;\n" +
            "    } else if (vTextureCoord.x > vTextureCoord.y + 0.005) {\n" +
            "        sum = texture2D(sTexture, vTextureCoord);\n" +
            "    } else {\n" +
            "        sum.r = 1.0;\n" +
            "    }\n" +
            "    gl_FragColor = sum;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER_EXT_FILTER =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform float brightness;\n" +
            "uniform float contrast;\n" +
            "uniform float saturation;\n" +
            "const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);\n" +
            "void main() {\n" +
            "    vec4 textureColor = texture2D(sTexture, vTextureCoord);\n" +
            "    vec4 brightnessColor = vec4((textureColor.rgb + vec3(brightness)), textureColor.w);\n" +
            "    vec4 contrastColor = vec4(((brightnessColor.rgb - vec3(0.5)) * contrast + vec3(0.5)), brightnessColor.w);\n" +
            "    float luminance = dot(contrastColor.rgb, luminanceWeighting);\n" +
            "    vec3 greyScaleColor = vec3(luminance);\n" +
            "    gl_FragColor = vec4(mix(greyScaleColor, contrastColor.rgb, saturation), contrastColor.w);\n" +
            "}\n";

    private Context mContext;

    private ProgramType mProgramType;

    // Handles to the GL program and various components of it.
    private int mProgramHandle;
    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int muKernelLoc;
    private int muTexOffsetLoc;
    private int muColorAdjustLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;

    private int muBrightnessLoc;
    private int muContrastLoc;
    private int muSaturationLoc;

    private int mTextureTarget;

    private float[] mKernel = new float[KERNEL_SIZE];
    private float[] mTexOffset;
    private float mColorAdjust;

    private float mBrightnessValue = 0.0f;  // (-0.2f, 0.35f)   亮度
    private float mContrastValue = 1.0f;    // (0.9f, 1.6f)     对比度
    private float mSaturationValue = 1.0f;  // (0.15f, 2f)      饱和度

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

    /**
     * Prepares the program in the current EGL context.
     */
    public Texture2dProgram(Context context, ProgramType programType) {
        mContext = context;
        useProgramType(programType);
    }

    /**
     * Releases the program.
     * <p>
     * The appropriate EGL context must be current (i.e. the one that was used to create
     * the program).
     */
    public void release() {
        Log.d(TAG, "deleting program " + mProgramHandle);
        GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }

    /**
     * Returns the program type.
     */
    public ProgramType getProgramType() {
        return mProgramType;
    }

    public void useProgramType(ProgramType programType) {
        mProgramType = programType;

        switch (programType) {
            case TEXTURE_2D:
                mTextureTarget = GLES20.GL_TEXTURE_2D;
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_2D);
                break;
            case TEXTURE_EXT:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT);
                break;
            case TEXTURE_EXT_BW:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_BW);
                break;
            case TEXTURE_EXT_FILT:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_FILT);
                break;
            case TEXTURE_EXT_FILTER:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_FILTER);
                break;
            case TEXTURE_OLD_CINEMA:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                String fragmentString = ShaderUtils.readTextFromRawResource(mContext, R.raw.old_film_fragment_shader);
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, fragmentString);
                break;
            default:
                throw new RuntimeException("Unhandled type " + programType);
        }
        if (mProgramHandle == 0) {
            throw new RuntimeException("Unable to create program");
        }
        Log.d(TAG, "Created program " + mProgramHandle + " (" + programType + ")");

        // get locations of attributes and uniforms

        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        GlUtil.checkLocation(maPositionLoc, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        GlUtil.checkLocation(maTextureCoordLoc, "aTextureCoord");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        GlUtil.checkLocation(muMVPMatrixLoc, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        GlUtil.checkLocation(muTexMatrixLoc, "uTexMatrix");
        muKernelLoc = GLES20.glGetUniformLocation(mProgramHandle, "uKernel");

        if (muKernelLoc < 0) {
            // no kernel in this one
            muKernelLoc = -1;
            muTexOffsetLoc = -1;
            muColorAdjustLoc = -1;
        } else {
            // has kernel, must also have tex offset and color adj
            muTexOffsetLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexOffset");
            GlUtil.checkLocation(muTexOffsetLoc, "uTexOffset");
            muColorAdjustLoc = GLES20.glGetUniformLocation(mProgramHandle, "uColorAdjust");
            GlUtil.checkLocation(muColorAdjustLoc, "uColorAdjust");

            // initialize default values
            setKernel(new float[] {0f, 0f, 0f,  0f, 1f, 0f,  0f, 0f, 0f}, 0f);
            setTexSize(256, 256);
        }

        if (mProgramType == TEXTURE_EXT_FILTER) {
            muBrightnessLoc = GLES20.glGetUniformLocation(mProgramHandle, "brightness");
            GlUtil.checkLocation(muBrightnessLoc, "brightness");
            muContrastLoc = GLES20.glGetUniformLocation(mProgramHandle, "contrast");
            GlUtil.checkLocation(muContrastLoc, "contrast");
            muSaturationLoc = GLES20.glGetUniformLocation(mProgramHandle, "saturation");
            GlUtil.checkLocation(muSaturationLoc, "saturation");
        }

        if (mProgramType == TEXTURE_OLD_CINEMA) {
            muSepiaLoc = GLES20.glGetUniformLocation(mProgramHandle, "SepiaValue");
            GlUtil.checkLocation(muSepiaLoc, "SepiaValue");
            muNoiseLoc = GLES20.glGetUniformLocation(mProgramHandle, "NoiseValue");
            GlUtil.checkLocation(muNoiseLoc, "NoiseValue");
            muScratchLoc = GLES20.glGetUniformLocation(mProgramHandle, "ScratchValue");
            GlUtil.checkLocation(muScratchLoc, "ScratchValue");

            muInnerVignettingLoc = GLES20.glGetUniformLocation(mProgramHandle, "InnerVignetting");
            GlUtil.checkLocation(muInnerVignettingLoc, "InnerVignetting");
            muOuterVignettingLoc = GLES20.glGetUniformLocation(mProgramHandle, "OuterVignetting");
            GlUtil.checkLocation(muOuterVignettingLoc, "OuterVignetting");
            muRandomLoc = GLES20.glGetUniformLocation(mProgramHandle, "RandomValue");
            GlUtil.checkLocation(muRandomLoc, "RandomValue");
            muTimeLapseLoc = GLES20.glGetUniformLocation(mProgramHandle, "TimeLapse");
            GlUtil.checkLocation(muTimeLapseLoc, "TimeLapse");
        }
    }

    public void setFilterValues(float brightness, float contrast, float saturation) {
        mBrightnessValue = brightness;
        mContrastValue = contrast;
        mSaturationValue = saturation;
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

    /**
     * Creates a texture object suitable for use with this program.
     * <p>
     * On exit, the texture will be bound.
     */
    public int createTextureObject() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GlUtil.checkGlError("glGenTextures");

        int texId = textures[0];
        GLES20.glBindTexture(mTextureTarget, texId);
        GlUtil.checkGlError("glBindTexture " + texId);

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        GlUtil.checkGlError("glTexParameter");

        return texId;
    }

    /**
     * Configures the convolution filter values.
     *
     * @param values Normalized filter values; must be KERNEL_SIZE elements.
     */
    public void setKernel(float[] values, float colorAdj) {
        if (values.length != KERNEL_SIZE) {
            throw new IllegalArgumentException("Kernel size is " + values.length +
                    " vs. " + KERNEL_SIZE);
        }
        System.arraycopy(values, 0, mKernel, 0, KERNEL_SIZE);
        mColorAdjust = colorAdj;
        //Log.d(TAG, "filt kernel: " + Arrays.toString(mKernel) + ", adj=" + colorAdj);
    }

    /**
     * Sets the size of the texture.  This is used to find adjacent texels when filtering.
     */
    public void setTexSize(int width, int height) {
        float rw = 1.0f / width;
        float rh = 1.0f / height;

        // Don't need to create a new array here, but it's syntactically convenient.
        mTexOffset = new float[] {
            -rw, -rh,   0f, -rh,    rw, -rh,
            -rw, 0f,    0f, 0f,     rw, 0f,
            -rw, rh,    0f, rh,     rw, rh
        };
        //Log.d(TAG, "filt size: " + width + "x" + height + ": " + Arrays.toString(mTexOffset));
    }

    /**
     * Issues the draw call.  Does the full setup on every call.
     *
     * @param mvpMatrix The 4x4 projection matrix.
     * @param vertexBuffer Buffer with vertex position data.
     * @param firstVertex Index of first vertex to use in vertexBuffer.
     * @param vertexCount Number of vertices in vertexBuffer.
     * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
     * @param vertexStride Width, in bytes, of the position data for each vertex (often
     *        vertexCount * sizeof(float)).
     * @param texMatrix A 4x4 transformation matrix for texture coords.  (Primarily intended
     *        for use with SurfaceTexture.)
     * @param texBuffer Buffer with vertex texture data.
     * @param texStride Width, in bytes, of the texture data for each vertex.
     */
    public void draw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex,
                     int vertexCount, int coordsPerVertex, int vertexStride,
                     float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride) {
        GlUtil.checkGlError("draw start");

        mRandomValue = (float) mRandom.nextInt(100) / 100f;
        mTimeLapse = 1000 * ((float) mTimeLapseRandom.nextInt(100) / 50f);

        // Select the program.
        GLES20.glUseProgram(mProgramHandle);
        GlUtil.checkGlError("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, textureId);

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        GlUtil.checkGlError("glUniformMatrix4fv");

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        GlUtil.checkGlError("glUniformMatrix4fv");

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        GlUtil.checkGlError("glEnableVertexAttribArray");

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex,
            GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        GlUtil.checkGlError("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        GlUtil.checkGlError("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false, texStride, texBuffer);
            GlUtil.checkGlError("glVertexAttribPointer");

        // Populate the convolution kernel, if present.
        if (muKernelLoc >= 0) {
            GLES20.glUniform1fv(muKernelLoc, KERNEL_SIZE, mKernel, 0);
            GLES20.glUniform2fv(muTexOffsetLoc, KERNEL_SIZE, mTexOffset, 0);
            GLES20.glUniform1f(muColorAdjustLoc, mColorAdjust);
        }

        if (mProgramType == TEXTURE_EXT_FILTER) {
            GLES20.glUniform1f(muBrightnessLoc, mBrightnessValue);
            GlUtil.checkGlError("glUniform1f - mBrightnessValue");
            GLES20.glUniform1f(muContrastLoc, mContrastValue);
            GlUtil.checkGlError("glUniform1f - mContrastValue");
            GLES20.glUniform1f(muSaturationLoc, mSaturationValue);
            GlUtil.checkGlError("glUniform1f - mSaturationValue");
        }

        if (mProgramType == TEXTURE_OLD_CINEMA) {
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
        }

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
        GlUtil.checkGlError("glDrawArrays");

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(mTextureTarget, 0);
        GLES20.glUseProgram(0);
    }
}
