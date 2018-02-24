package com.oldfilm;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.oldfilm.gles.LogUtils;
import com.oldfilm.render.VideoRenderer;

import java.util.Timer;
import java.util.TimerTask;

/**
 * ControllerView class.
 */
public class ControllerView extends FrameLayout implements SeekBar.OnSeekBarChangeListener {

    private VideoRenderer mController;
    private Timer mTimer;

    private View mRootView;

    private TextView mTitleTextView;
    private TextView mCurrentPositionTextView;
    private TextView mDurationTextView;
    private ProgressBar mProgressBar;

    private VerticalSeekBar mSepiaSeekBar;
    private VerticalSeekBar mNoiseSeekBar;
    private VerticalSeekBar mScratchSeekBar;
    private VerticalSeekBar mVignettingSeekBar;

    private ViewGroup mParametersViewGroup;

    private int mDisplayTime = 3000;

    public ControllerView(@NonNull Context context) {
        super(context);
        initView();
    }

    public ControllerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public ControllerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDisplayTime = 3000;
        if (mRootView.getVisibility() == INVISIBLE) {
            mRootView.setVisibility(VISIBLE);
        }

        return false;
    }

    public void setRenderer(VideoRenderer renderer) {
        mController = renderer;
    }

    public void destroy() {
        mTimer.cancel();
    }

    private void initView() {
        mRootView = LayoutInflater.from(getContext()).inflate(R.layout.media_player, this, false);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        mRootView.setLayoutParams(params);
        addView(mRootView);

        final ImageView backImageView = mRootView.findViewById(R.id.iv_back);
        backImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ((Activity)getContext()).finish();
            }
        });

        final ImageView arOriginalImageView = mRootView.findViewById(R.id.iv_ar_original);
        arOriginalImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mController != null) {
                    if (mController.isEffectEnabled()) {
                        mParametersViewGroup.setVisibility(INVISIBLE);
                        arOriginalImageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_original));
                        mController.enableEffect(false);
                    } else {
                        mParametersViewGroup.setVisibility(VISIBLE);
                        arOriginalImageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_ar));
                        mController.enableEffect(true);
                    }
                }
            }
        });

        final ImageView controlImageView = mRootView.findViewById(R.id.iv_control);
        controlImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mController != null) {
                    if (mController.isPlaying()) {
                        controlImageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_play));
                        mController.pause();
                    } else {
                        controlImageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_pause));
                        mController.play();
                    }
                }
            }
        });

        mTitleTextView = mRootView.findViewById(R.id.tv_title);
        mCurrentPositionTextView = mRootView.findViewById(R.id.tv_position);
        mDurationTextView = mRootView.findViewById(R.id.tv_duration);
        mProgressBar = mRootView.findViewById(R.id.progressbar);

        mParametersViewGroup = mRootView.findViewById(R.id.ly_parameters);

        mSepiaSeekBar = mRootView.findViewById(R.id.seek_bar_sepia);
        mSepiaSeekBar.setOnSeekBarChangeListener(this);
        mNoiseSeekBar = mRootView.findViewById(R.id.seek_bar_noise);
        mNoiseSeekBar.setOnSeekBarChangeListener(this);
        mScratchSeekBar = mRootView.findViewById(R.id.seek_bar_scratching);
        mScratchSeekBar.setOnSeekBarChangeListener(this);
        mVignettingSeekBar = mRootView.findViewById(R.id.seek_bar_vignetting);
        mVignettingSeekBar.setOnSeekBarChangeListener(this);

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Message msg;
                msg = mHandler.obtainMessage();
                msg.what = 0;
                mHandler.sendMessage(msg);
            }
        }, 0, 50);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (null != mController) {
                if (TextUtils.isEmpty(mTitleTextView.getText())) {
                    mTitleTextView.setText(mController.getUrl());
                }

                int currentPosition = (int)((float)mController.getCurrentPosition() / 1000.0f);
                int duration = (int)((float)mController.getDuration() / 1000.0f);
                if (currentPosition >= 0 && duration >= 0l) {
                    int progress = (int) ((float) currentPosition / (float) duration * 100);
                    mProgressBar.setProgress(progress);
                    mCurrentPositionTextView.setText(TimeUtils.formatNumberToHourMinuteSecond((double)currentPosition));
                    mDurationTextView.setText(TimeUtils.formatNumberToHourMinuteSecond((double)duration));
                    LogUtils.v("actual progress is " + progress + "%");
                }
            }

            mDisplayTime -= 50;
            if (mDisplayTime <= 0 && mRootView.getVisibility() == VISIBLE) {
                mRootView.setVisibility(INVISIBLE);
            }
        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        mDisplayTime = 3000;

        if (mController == null) return;

        if (seekBar.getId() == R.id.seek_bar_sepia) {
            mController.setSepiaValue((float) seekBar.getProgress() / 100f);
        } else if (seekBar.getId() == R.id.seek_bar_noise) {
            mController.setNoiseValue((float) seekBar.getProgress() / 100f);
        } else if (seekBar.getId() == R.id.seek_bar_scratching) {
            mController.setScratchValue((float) seekBar.getProgress() / 100f);
        } else if (seekBar.getId() == R.id.seek_bar_vignetting) {
            mController.setVignettingValue((float) seekBar.getProgress() / 100f);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mDisplayTime = 3000;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mDisplayTime = 3000;
    }
}
