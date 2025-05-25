package com.example.memorycardflip;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Set up fade-in animation for the app icon
        ImageView splashIcon = findViewById(R.id.splashIcon);
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(1500); // 1.5 seconds
        splashIcon.startAnimation(fadeIn);

        // Transition to the main menu after the animation
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MenuActivity.class);
            startActivity(intent);
            finish();
        }, 2000); // 2 seconds delay
    }
}
