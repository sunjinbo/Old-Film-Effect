package com.oldfilm.render;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import com.oldfilm.gles.EglCore;
import com.oldfilm.gles.WindowSurface;
import com.oldfilm.gles.GlUtil;
import com.oldfilm.gles.LogUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * VideoRenderer class.
 */
public class VideoRenderer implements MediaPlayer.OnVideoSizeChangedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener,
		MediaPlayer.OnSeekCompleteListener {

    private static final Object sLock = new Object();

    private Context mContext;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private EglCore mEglCore;
    private WindowSurface mWindowSurface;

    private SurfaceTexture mSurfaceTexture;
    private MediaPlayer mMediaPlayer;

    private boolean mIsDestroy = false;

    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mWindowWidth;
    private int mWindowHeight;

    private OffscreenDrawer mOffscreenDrawer;
    private OldFilmDrawer mEffectDrawer;
    private DirectDrawer mDirectDrawer;
    private boolean mIsEffectEnabled = true;

    private boolean mIsPrepared = false;

    private int mFramebuffer;
    private int mOffscreenTextureID;
    private int mTextureID;

    private ByteBuffer mRGBABuffer;
    private int mVideoWidth;
    private int mVideoHeight;

    public VideoRenderer(Context context, SurfaceView surfaceView) {
        mContext = context;
        mSurfaceView = surfaceView;
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(mSurfaceHolderCallback);
    }

    public void play() {
        if (mMediaPlayer != null && mIsPrepared) {
            mMediaPlayer.start();
        }
    }

    public void pause() {
        if (mMediaPlayer != null && mIsPrepared) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
        }
    }

    public void seekTo(int msec) {

        if (mMediaPlayer != null && mIsPrepared) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.seekTo(msec);
            }
        }
    }

    public boolean isPlaying() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }

    public void destroy() {
        releaseGl();
        releasePlayer();
    }

    public int getCurrentPosition() {
        if (mMediaPlayer != null && mIsPrepared) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (mMediaPlayer != null && mIsPrepared) {
            return mMediaPlayer.getDuration();
        }
        return 0;
    }

    public String getUrl() {
        return "vid.mp4";
    }

    public boolean isEffectEnabled() {
        return mIsEffectEnabled;
    }

    public void enableEffect(boolean isEffectEnabled) {
        mIsEffectEnabled = isEffectEnabled;
        if (mIsEffectEnabled) {
            mEffectDrawer.startRender();
            mDirectDrawer.stopRender();
        } else {
            mEffectDrawer.stopRender();
            mDirectDrawer.startRender();
        }
    }

    public void setSepiaValue(float sepia) {
        mEffectDrawer.setSepiaValue(sepia);
    }

    public void setNoiseValue(float noise) {
        mEffectDrawer.setNoiseValue(noise);
    }

    public void setScratchValue(float scratch) {
        mEffectDrawer.setScratchValue(scratch);
    }

    public void setVignettingValue(float vignetting) {
        mEffectDrawer.setVignettingValue(vignetting);
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        LogUtils.d("VideoRenderer.onVideoSizeChanged() - width = "+ width + " ,height = " + height);

        mVideoWidth = width;
        mVideoHeight = height;

        if (width < height) {
            //portrait video
            mSurfaceHeight = mWindowHeight;
            float ratioHeiht = (float) mSurfaceHeight / (float) mVideoHeight;
            mSurfaceWidth = (int) (ratioHeiht * mVideoWidth);
            ((VideoView) mSurfaceView).setAspectWithWidthHeight((float) mSurfaceWidth / (float) mSurfaceHeight);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            mSurfaceView.setLayoutParams(lp);
        } else {
            mSurfaceWidth = mWindowWidth;
            mSurfaceHeight = mWindowHeight;
        }

        mDirectDrawer.updateProjection(mSurfaceWidth, mSurfaceHeight, width, height);
        mEffectDrawer.updateProjection(mSurfaceWidth, mSurfaceHeight, width, height);
        if (mRGBABuffer == null) {
            mRGBABuffer = ByteBuffer.allocate(mSurfaceWidth * mSurfaceHeight * 4);
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int extra) {
        LogUtils.v("VideoRenderer.onBufferingUpdate() - extra = " + extra);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        LogUtils.v("VideoRenderer.onCompletion()");
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        LogUtils.e("VideoRenderer.onError()");
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        LogUtils.v("VideoRenderer.onInfo()");
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        LogUtils.d("VideoRenderer.onPrepared()");
        mediaPlayer.start();
        mIsPrepared = true;
    }

    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            releaseGl();
            releasePlayer();
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (mWindowSurface == null) {
                Surface surface = holder.getSurface();
                mEglCore = new EglCore(null, 0);
                mWindowSurface = new WindowSurface(mEglCore, surface, false);
                mWindowSurface.makeCurrent();

                // Create and configure the SurfaceTexture, which will receive frames from the
                // camera.  We set the textured rect's program to render from it.
                mTextureID = GlUtil.createTextureID();
                mSurfaceTexture = new SurfaceTexture(mTextureID);
                mSurfaceTexture.setOnFrameAvailableListener(mOnFrameListener);

                setupFramebuffer(holder.getSurfaceFrame().width(),
                        holder.getSurfaceFrame().height());

                mOffscreenDrawer = new OffscreenDrawer(mContext, mTextureID);

                mDirectDrawer = new DirectDrawer(mContext, mOffscreenTextureID);

                mEffectDrawer = new OldFilmDrawer(mContext, mOffscreenTextureID);
                mEffectDrawer.startRender();

                // Create and prepare media player.
                createPlayer();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mWindowWidth = width;
            mWindowHeight = height;
            // Use full window.
            GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
        }
    };

    private SurfaceTexture.OnFrameAvailableListener mOnFrameListener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            if (mIsDestroy) return;

            GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
            draw();
        }
    };


    private void draw() {
        synchronized (sLock) {
            mSurfaceTexture.updateTexImage();
            mWindowSurface.makeCurrent();

            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
            mRGBABuffer.rewind();
            mOffscreenDrawer.draw(mRGBABuffer, mSurfaceWidth, mSurfaceHeight);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

            if (mIsEffectEnabled) {
                mSurfaceTexture.getTransformMatrix(mEffectDrawer.getMatrix());
                mEffectDrawer.draw(mRGBABuffer, mSurfaceWidth, mSurfaceHeight, mMediaPlayer.getCurrentPosition());
            } else {
                mSurfaceTexture.getTransformMatrix(mDirectDrawer.getMatrix());
                mDirectDrawer.draw(mRGBABuffer, mSurfaceWidth, mSurfaceHeight, mMediaPlayer.getCurrentPosition());
            }

            mWindowSurface.swapBuffers();
        }
    }

    private void createPlayer() {
        try{
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnInfoListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setLooping(true);

            final AssetFileDescriptor afd = mContext.getResources().getAssets().openFd("vid.mp4");
            mMediaPlayer.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());

            Surface surface = new Surface(mSurfaceTexture);
            mMediaPlayer.setSurface(surface);
            surface.release();

            mMediaPlayer.prepareAsync();
        } catch (IOException e){
            LogUtils.e(e.getMessage());
        }
    }

    private void releaseGl() {
        mIsDestroy = true;
        synchronized (sLock) {

            if (mEffectDrawer != null) {
                mEffectDrawer.destroy();
            }

            if (mDirectDrawer != null) {
                mDirectDrawer.destroy();
            }

            if (mWindowSurface != null) {
                mWindowSurface.release();
                mWindowSurface = null;
            }

            GlUtil.checkGlError("releaseGl done");

            mEglCore.release();
        }
    }

    private void releasePlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mIsPrepared = false;
        }
    }

    private void setupFramebuffer(int width, int height) {
        int[] values = new int[1];

        // Create a texture object and bind it.  This will be the color buffer.
        GLES20.glGenTextures(1, values, 0);
        GlUtil.checkGlError("glGenTextures");
        mOffscreenTextureID = values[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOffscreenTextureID);
        GlUtil.checkGlError("glBindTexture " + mOffscreenTextureID);

        // Create texture storage.
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
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
        GlUtil.checkGlError("glTexParameter");

        // Create framebuffer object and bind it.
        GLES20.glGenFramebuffers(1, values, 0);
        mFramebuffer = values[0];    // expected > 0
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
        GlUtil.checkGlError("glBindFramebuffer " + mFramebuffer);

        // Create a depth buffer and bind it.
        GLES20.glGenRenderbuffers(1, values, 0);
        GlUtil.checkGlError("glGenRenderbuffers");
        int depthBuffer1 = values[0];
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthBuffer1);
        GlUtil.checkGlError("glBindRenderbuffer " + depthBuffer1);

        // Allocate storage for the depth buffer.
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);
        GlUtil.checkGlError("glRenderbufferStorage");

        // Attach the depth buffer and the texture (color buffer) to the framebuffer object.
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, depthBuffer1);
        GlUtil.checkGlError("glFramebufferRenderbuffer");
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mOffscreenTextureID, 0);
        GlUtil.checkGlError("glFramebufferTexture2D");

        // See if GLES is happy with all this.
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer not complete, status=" + status);
        }

        // Switch back to the default framebuffer.
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        if (mIsPrepared) {
            mMediaPlayer.start();
        }
    }
}