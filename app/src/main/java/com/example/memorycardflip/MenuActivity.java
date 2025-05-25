package com.example.memorycardflip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    private Button btnEasy, btnHard, btnSoundToggle;
    private ImageView gameIcon;
    private MediaPlayer backgroundMusic;
    private boolean isSoundEnabled = true;

    private SharedPreferences preferences;
    private static final String PREFS_NAME = "MemoryCardFlipPrefs";
    private static final String SOUND_ENABLED_KEY = "SoundEnabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Initialize UI elements
        btnEasy = findViewById(R.id.btn_easy);
        btnHard = findViewById(R.id.btn_hard);
        btnSoundToggle = findViewById(R.id.btn_sound_toggle);
        gameIcon = findViewById(R.id.gameIcon);

        // Load sound preference
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isSoundEnabled = preferences.getBoolean(SOUND_ENABLED_KEY, true);
        updateSoundButtonText();

        // Initialize background music
        setupBackgroundMusic();

        // Add animation to the game icon
        Animation pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        pulse.setDuration(1500);
        pulse.setRepeatCount(Animation.INFINITE);
        pulse.setRepeatMode(Animation.REVERSE);
        gameIcon.startAnimation(pulse);

        // Set up button click listeners with animations
        btnEasy.setOnClickListener(v -> {
            btnEasy.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
            startGame("easy");
        });

        btnHard.setOnClickListener(v -> {
            btnHard.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
            startGame("hard");
        });

        // Set up sound toggle button
        btnSoundToggle.setOnClickListener(v -> {
            btnSoundToggle.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
            toggleSound();
        });
    }

    /**
     * Sets up the background music
     */
    private void setupBackgroundMusic() {
        backgroundMusic = MediaPlayer.create(this, R.raw.mainmenu);
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.5f, 0.5f);

        if (isSoundEnabled) {
            backgroundMusic.start();
        }
    }

    /**
     * Toggles sound on/off
     */
    private void toggleSound() {
        isSoundEnabled = !isSoundEnabled;

        // Save preference
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SOUND_ENABLED_KEY, isSoundEnabled);
        editor.apply();

        // Update music playback
        if (isSoundEnabled) {
            if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
                backgroundMusic.start();
            }
        } else {
            if (backgroundMusic != null && backgroundMusic.isPlaying()) {
                backgroundMusic.pause();
            }
        }

        // Update button text
        updateSoundButtonText();
    }

    /**
     * Updates the sound button text based on sound state
     */
    private void updateSoundButtonText() {
        if (btnSoundToggle != null) {
            btnSoundToggle.setText(isSoundEnabled ? "Sound: ON" : "Sound: OFF");
        }
    }

    /**
     * Starts the game with the selected difficulty mode
     * @param mode The game difficulty mode
     */
    private void startGame(String mode) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("mode", mode);
        intent.putExtra("soundEnabled", isSoundEnabled);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isSoundEnabled && backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (backgroundMusic != null) {
            backgroundMusic.release();
            backgroundMusic = null;
        }
    }
}
