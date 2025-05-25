package com.example.memorycardflip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    private Button btnEasy, btnHard, btnSound;
    private ImageView gameIcon;
    private boolean isSoundEnabled = true;

    private SharedPreferences preferences;
    private static final String PREFS_NAME = "MemoryCardFlipPrefs";
    private static final String SOUND_ENABLED_KEY = "SoundEnabled";

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_menu);

            // Initialize MediaPlayer for background music
            mediaPlayer = MediaPlayer.create(this, R.raw.mainmenu);
            mediaPlayer.setLooping(true);

            // Play music if sound is enabled
            if (isSoundEnabled) {
                mediaPlayer.start();
            }

            // Initialize UI elements - with null checks
            btnEasy = findViewById(R.id.btn_easy);
            btnHard = findViewById(R.id.btn_hard);
            btnSound = findViewById(R.id.btn_sound);
            gameIcon = findViewById(R.id.gameIcon);

            // Check if all UI elements were found
            if (btnEasy == null || btnHard == null || btnSound == null) {
                Toast.makeText(this, "Error: UI elements not found in menu", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Initialize SharedPreferences
            preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

            // Clear app history (SharedPreferences) - Optional, remove if you want to keep high scores
            // SharedPreferences.Editor editor = preferences.edit();
            // editor.clear();
            // editor.apply();

            // Load sound preference
            isSoundEnabled = preferences.getBoolean(SOUND_ENABLED_KEY, true);

            // Set the initial button text with fallback
            try {
                btnSound.setText(isSoundEnabled ? R.string.sound_on : R.string.sound_off);
            } catch (Exception e) {
                // Fallback text if strings don't exist
                btnSound.setText(isSoundEnabled ? "Sound: ON" : "Sound: OFF");
            }

            // Set up button click listeners
            btnEasy.setOnClickListener(v -> startGame("easy"));
            btnHard.setOnClickListener(v -> startGame("hard"));
            btnSound.setOnClickListener(v -> toggleSound());

        } catch (Exception e) {
            Toast.makeText(this, "Error initializing menu: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            finish();
        }
    }

    private void toggleSound() {
        try {
            isSoundEnabled = !isSoundEnabled;

            // Save preference
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(SOUND_ENABLED_KEY, isSoundEnabled);
            editor.apply();

            // Update button text
            btnSound.setText(isSoundEnabled ? R.string.sound_on : R.string.sound_off);

            // Start or stop music based on sound preference
            if (isSoundEnabled) {
                if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                }
            } else {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error toggling sound", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void startGame(String mode) {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("mode", mode);
            intent.putExtra("soundEnabled", isSoundEnabled);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error starting game: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && isSoundEnabled) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}

