package com.oldfilm.render;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.oldfilm.R;
import com.oldfilm.gles.ShaderUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * OffscreenDrawer class.
 */
public class OffscreenDrawer {

    private static final short sDrawOrder[] = {0, 1, 2, 0, 2, 3}; // order to draw vertices

    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX = 2;

    private static final int sVertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    private static final float sSquareCoords[] = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f,
            1.0f, 1.0f,
    };

    private static final float sTextureVertices[] = {
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
    };

    private FloatBuffer mVertexBuffer, mTextureVerticesBuffer;
    private ShortBuffer mDrawListBuffer;

    private final int mProgram;

    private int mPositionHandle;
    private int mTextureCoordHandle;

    private int mTextureID;

    public OffscreenDrawer(Context context, int textureID) {
        this.mTextureID = textureID;

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(sSquareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(sSquareCoords);
        mVertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(sDrawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        mDrawListBuffer = dlb.asShortBuffer();
        mDrawListBuffer.put(sDrawOrder);
        mDrawListBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(sTextureVertices.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        mTextureVerticesBuffer = bb2.asFloatBuffer();
        mTextureVerticesBuffer.put(sTextureVertices);
        mTextureVerticesBuffer.position(0);

        final String vertexShaderString = ShaderUtils.readTextFromRawResource(context, R.raw.offscreen_vertex_shader);
        final String fragmentShaderString = ShaderUtils.readTextFromRawResource(context, R.raw.offscreen_fragment_shader);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderString);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderString);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables
    }

    public OffscreenDrawer(int textureID, String vertex, String fragment) {
        this.mTextureID = textureID;

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(sSquareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(sSquareCoords);
        mVertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(sDrawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        mDrawListBuffer = dlb.asShortBuffer();
        mDrawListBuffer.put(sDrawOrder);
        mDrawListBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(sTextureVertices.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        mTextureVerticesBuffer = bb2.asFloatBuffer();
        mTextureVerticesBuffer.put(sTextureVertices);
        mTextureVerticesBuffer.position(0);

        final String vertexShaderString = vertex;
        final String fragmentShaderString = fragment;

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderString);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderString);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables
    }

    public void draw(ByteBuffer byteBuffer, int width, int height) {
        GLES20.glUseProgram(mProgram);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);

        mVertexBuffer.position(0);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle); // Enable a handle to the triangle vertices
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, sVertexStride, mVertexBuffer);

        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
        GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, sVertexStride, mTextureVerticesBuffer);

        // Draw elements to OpenGL rect.
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, sDrawOrder.length, GLES20.GL_UNSIGNED_SHORT, mDrawListBuffer);

        if (byteBuffer != null) {
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);

//            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//            bitmap.copyPixelsFromBuffer(byteBuffer);
//            BmpUtils.saveBitmapToFile(SystemClock.elapsedRealtime() + ".jpg", bitmap);
        }

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle);

        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private int loadShader(int type, String shaderCode) {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }
}
