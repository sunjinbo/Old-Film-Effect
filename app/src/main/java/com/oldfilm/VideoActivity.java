package com.oldfilm;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.oldfilm.render.VideoRenderer;
import com.oldfilm.render.VideoView;

public class VideoActivity extends Activity {

    private VideoView mVideoView;
    private ControllerView mControllerView;
    private VideoRenderer mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_video);

        mVideoView = findViewById(R.id.video_view);
        mVideoView.getHolder().setFormat(PixelFormat.RGBA_8888);
        mRenderer = new VideoRenderer(this, mVideoView);
        mControllerView = findViewById(R.id.view_controller);
        mControllerView.setRenderer(mRenderer);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
