package com.gameditors.a2048;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Toast;

public class ColorPickerActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener
{
    // UIs
    private FrameLayout mGameViewFrameLayout;
    private SeekBar mSeekBarRed;
    private SeekBar mSeekBarGreen;
    private SeekBar mSeekBarBlue;

    // Game view
    private MainView mGameView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_picker);

        mGameViewFrameLayout = findViewById(R.id.game_view_frame_layout);
        mSeekBarRed = findViewById(R.id.seekbar_red);
        mSeekBarGreen = findViewById(R.id.seekbar_green);
        mSeekBarBlue = findViewById(R.id.seekbar_blue);

        mGameView = new MainView(this);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        mGameView.setLayoutParams(params);

        mGameViewFrameLayout.addView(mGameView);

        mGameView.setBackgroundColor(Color.argb(255, mSeekBarRed.getProgress(),
                mSeekBarGreen.getProgress(), mSeekBarBlue.getProgress()));

        mSeekBarRed.setOnSeekBarChangeListener(this);
        mSeekBarGreen.setOnSeekBarChangeListener(this);
        mSeekBarBlue.setOnSeekBarChangeListener(this);
    }

    public void AcceptColor(View view)
    {
        MainMenuActivity.mBackgroundColor = Color.argb(255, mSeekBarRed.getProgress(),
                mSeekBarGreen.getProgress(), mSeekBarBlue.getProgress());

        Toast.makeText(this, getString(R.string.background_color_changed), Toast.LENGTH_SHORT).show();
        finish();
    }

    public void ResetToDefaultColor(View view)
    {
        MainMenuActivity.mBackgroundColor = 0xFFFAF8EF;
        Toast.makeText(this, getString(R.string.background_color_has_been_reset_to_default), Toast.LENGTH_SHORT).show();
        finish();
    }

    public void FinishActivity(View view) { finish(); }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        mGameView.setBackgroundColor(Color.argb(255, mSeekBarRed.getProgress(),
                mSeekBarGreen.getProgress(), mSeekBarBlue.getProgress()));
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}