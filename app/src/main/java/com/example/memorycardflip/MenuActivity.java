package com.example.memorycardflip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    private Button btnEasy, btnHard, btnSoundToggle;
    private ImageView gameIcon;
    private boolean isSoundEnabled = true;

    private SharedPreferences preferences;
    private static final String PREFS_NAME = "MemoryCardFlipPrefs";
    private static final String SOUND_ENABLED_KEY = "SoundEnabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Clear app history (SharedPreferences)
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        setContentView(R.layout.activity_menu);

        // Initialize UI elements - only do this after setContentView
        btnEasy = findViewById(R.id.btn_easy);
        btnHard = findViewById(R.id.btn_hard);
        btnSoundToggle = findViewById(R.id.btn_sound_toggle);
        gameIcon = findViewById(R.id.gameIcon);

        // Load sound preference
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isSoundEnabled = preferences.getBoolean(SOUND_ENABLED_KEY, true);

        // Set the initial button text
        btnSoundToggle.setText(isSoundEnabled ? R.string.sound_on : R.string.sound_off);

        // Set up button click listeners
        btnEasy.setOnClickListener(v -> startGame("easy"));
        btnHard.setOnClickListener(v -> startGame("hard"));
        btnSoundToggle.setOnClickListener(v -> toggleSound());
    }

    private void toggleSound() {
        isSoundEnabled = !isSoundEnabled;

        // Save preference
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SOUND_ENABLED_KEY, isSoundEnabled);
        editor.apply();

        // Update button text
        btnSoundToggle.setText(isSoundEnabled ? R.string.sound_on : R.string.sound_off);
    }

    private void startGame(String mode) {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("mode", mode);
            intent.putExtra("soundEnabled", isSoundEnabled);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
