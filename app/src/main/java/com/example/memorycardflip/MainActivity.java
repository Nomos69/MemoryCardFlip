package com.example.memorycardflip;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Variables for score tracking
    private int currentScore = 0;
    private int highScore = 0;

    // UI elements
    private TextView tvCurrentScore;
    private TextView tvHighScore;
    private GridLayout gameGrid;
    private Button btnResetGame;

    // Game logic variables
    private final int NUM_CARDS_EASY = 12; // 6 pairs
    private final int NUM_CARDS_HARD = 20; // 10 pairs
    private int NUM_CARDS = NUM_CARDS_EASY; // Default to easy mode
    private final int POINTS_PER_MATCH = 10;
    private final int PENALTY_FOR_MISMATCH = -2;
    private final int DELAY_BEFORE_HIDING = 1000; // 1 second

    // Sound control
    private boolean isSoundEnabled = true;

    private ImageView firstCard = null;
    private int firstCardId = -1;
    private boolean isProcessing = false; // Flag to prevent clicking during animations
    private int pairsFound = 0;

    // List of card image resources
    private Integer[] cardImages = {
            R.drawable.apple, R.drawable.banana, R.drawable.grapes,
            R.drawable.hippo, R.drawable.lion, R.drawable.monkey
    };

    // List to track card IDs and their positions
    private List<Integer> cardPositions;

    // Sound effects
    private SoundPool soundPool;
    private int soundClick;
    private int soundMatch;
    private int soundNoMatch;
    private MediaPlayer backgroundMusic;

    // SharedPreferences for saving high score
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "MemoryCardFlipPrefs";
    private static final String HIGH_SCORE_KEY = "HighScore";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        tvCurrentScore = findViewById(R.id.tvCurrentScore);
        tvHighScore = findViewById(R.id.tvHighScore);
        gameGrid = findViewById(R.id.gameGrid);
        btnResetGame = findViewById(R.id.btnResetGame);

        // Get game mode and sound settings from intent
        String gameMode = getIntent().getStringExtra("mode");
        isSoundEnabled = getIntent().getBooleanExtra("soundEnabled", true);

        // Set number of cards based on difficulty
        if ("hard".equals(gameMode)) {
            NUM_CARDS = NUM_CARDS_HARD;
            // Set up grid for hard mode (5x4 grid)
            gameGrid.setColumnCount(5);
            gameGrid.setRowCount(4);
        } else {
            NUM_CARDS = NUM_CARDS_EASY;
            // Set up grid for easy mode (4x3 grid)
            gameGrid.setColumnCount(4);
            gameGrid.setRowCount(3);
        }

        // Load high score from SharedPreferences
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        highScore = preferences.getInt(HIGH_SCORE_KEY, 0);
        updateScoreDisplay();

        // Initialize sounds
        initSounds();

        // Set up reset button with animation
        btnResetGame.setOnClickListener(v -> {
            // Add button press animation
            Animation buttonAnim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            btnResetGame.startAnimation(buttonAnim);

            // Play click sound if enabled
            if (isSoundEnabled) {
                soundPool.play(soundClick, 1.0f, 1.0f, 1, 0, 1.0f);
            }

            resetGame();
        });

        // Initialize and setup the game
        setupGame();
    }

    /**
     * Initialize sound effects and background music
     */
    private void initSounds() {
        // Set up SoundPool for short sound effects
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(audioAttributes)
                .build();

        // Load sound effects
        soundClick = soundPool.load(this, R.raw.click, 1);
        soundMatch = soundPool.load(this, R.raw.match, 1);
        soundNoMatch = soundPool.load(this, R.raw.notmatch, 1);

        // Set up background music
        backgroundMusic = MediaPlayer.create(this, R.raw.mainmenu);
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.5f, 0.5f);
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
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }

        if (backgroundMusic != null) {
            backgroundMusic.release();
            backgroundMusic = null;
        }
    }

    /**
     * Sets up the game board with cards
     */
    private void setupGame() {
        // Clear any existing cards
        gameGrid.removeAllViews();

        // Reset game state
        firstCard = null;
        firstCardId = -1;
        isProcessing = false;
        pairsFound = 0;
        resetCurrentScore();

        // Create shuffled positions for cards (pairs have same image)
        cardPositions = new ArrayList<>();
        for (int i = 0; i < NUM_CARDS / 2; i++) {
            // If we need more images than we have, cycle through them again
            int imageIndex = i % cardImages.length;
            cardPositions.add(imageIndex);
            cardPositions.add(imageIndex);
        }
        Collections.shuffle(cardPositions);

        // Determine the number of columns based on the current mode
        int numColumns = gameGrid.getColumnCount();

        // Create the cards and add them to the grid
        for (int i = 0; i < NUM_CARDS; i++) {
            final ImageView card = new ImageView(this);
            card.setImageResource(R.drawable.back_card);

            // Use proper layout parameters for the grid
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 0;
            params.columnSpec = GridLayout.spec(i % numColumns, 1, 1f);
            params.rowSpec = GridLayout.spec(i / numColumns, 1, 1f);
            params.setMargins(8, 8, 8, 8);

            card.setLayoutParams(params);
            card.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.setBackgroundResource(R.drawable.card_background);

            // Add card entrance animation
            Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            fadeIn.setStartOffset(i * 50); // Stagger the animations
            card.startAnimation(fadeIn);

            // Set tag to identify this card's position
            card.setTag(i);

            // Set up click listener for card
            final int position = i;
            card.setOnClickListener(v -> {
                if (isProcessing || firstCard == card) {
                    return; // Prevent clicking during animations or same card twice
                }

                // Play card click sound if enabled
                if (isSoundEnabled) {
                    soundPool.play(soundClick, 1.0f, 1.0f, 1, 0, 1.0f);
                }

                // Flip the card to show the image
                flipCard(card, cardPositions.get(position));
            });

            gameGrid.addView(card);
        }
    }

    /**
     * Handles card flipping logic with animation
     * @param card The card ImageView being flipped
     * @param imageIndex The index of the image to display
     */
    private void flipCard(ImageView card, int imageIndex) {
        isProcessing = true;

        // Ensure the imageIndex is valid
        int safeImageIndex = imageIndex % cardImages.length;

        try {
            // Create a new handler for animations
            Handler handler = new Handler();

            // Simple rotation animation instead of using AnimatorInflater
            card.animate()
                .rotationY(90)
                .setDuration(250)
                .withEndAction(() -> {
                    // When card is flipped halfway, change the image
                    card.setImageResource(cardImages[safeImageIndex]);

                    // Continue the flip animation
                    card.setRotationY(-90);
                    card.animate()
                        .rotationY(0)
                        .setDuration(250)
                        .start();

                    // If this is the first card flipped
                    if (firstCard == null) {
                        firstCard = card;
                        firstCardId = safeImageIndex;
                        isProcessing = false;
                    } else {
                        // This is the second card

                        // Check if it's a match
                        if (firstCardId == safeImageIndex) {
                            // Play match sound if enabled
                            if (isSoundEnabled && soundPool != null) {
                                soundPool.play(soundMatch, 1.0f, 1.0f, 1, 0, 1.0f);
                            }

                            // Cards match - add success animation
                            handler.postDelayed(() -> {
                                // Add a highlight effect for matched cards
                                card.setBackgroundColor(Color.parseColor("#4CAF50")); // Green background
                                firstCard.setBackgroundColor(Color.parseColor("#4CAF50"));

                                // Make matched cards pulse slightly
                                Animation pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
                                pulse.setDuration(400);
                                card.startAnimation(pulse);
                                firstCard.startAnimation(pulse);

                                // Add points with animated text
                                addPoints(POINTS_PER_MATCH);
                                animateScoreChange(true);

                                // Make cards unclickable
                                card.setOnClickListener(null);
                                firstCard.setOnClickListener(null);

                                // Increment pairs found counter
                                pairsFound++;

                                // Reset for next pair
                                firstCard = null;
                                firstCardId = -1;
                                isProcessing = false;

                                // Check if game is over (all pairs found)
                                checkGameOver();
                            }, 300);
                        } else {
                            // Play no match sound if enabled
                            if (isSoundEnabled && soundPool != null) {
                                soundPool.play(soundNoMatch, 1.0f, 1.0f, 1, 0, 1.0f);
                            }

                            // Cards don't match, add penalty
                            addPoints(PENALTY_FOR_MISMATCH);
                            animateScoreChange(false);

                            // Delay before hiding cards again
                            handler.postDelayed(() -> {
                                // Flip both cards back with animation
                                card.animate()
                                    .rotationY(90)
                                    .setDuration(250)
                                    .withEndAction(() -> {
                                        card.setImageResource(R.drawable.back_card);
                                        card.setRotationY(-90);
                                        card.animate()
                                            .rotationY(0)
                                            .setDuration(250)
                                            .start();
                                    })
                                    .start();

                                firstCard.animate()
                                    .rotationY(90)
                                    .setDuration(250)
                                    .withEndAction(() -> {
                                        firstCard.setImageResource(R.drawable.back_card);
                                        firstCard.setRotationY(-90);
                                        firstCard.animate()
                                            .rotationY(0)
                                            .setDuration(250)
                                            .withEndAction(() -> {
                                                // Reset for next attempt
                                                firstCard = null;
                                                firstCardId = -1;
                                                isProcessing = false;
                                            })
                                            .start();
                                    })
                                    .start();
                            }, DELAY_BEFORE_HIDING);
                        }
                    }
                })
                .start();
        } catch (Exception e) {
            // If animation fails, just show the card without animation
            card.setImageResource(cardImages[safeImageIndex]);

            // Reset game state to prevent lock-up
            isProcessing = false;
            e.printStackTrace();
        }
    }

    /**
     * Adds points to the current score
     * @param points The number of points to add (can be negative for penalties)
     */
    private void addPoints(int points) {
        currentScore += points;
        updateScoreDisplay();
    }

    /**
     * Updates the score display on the UI
     */
    private void updateScoreDisplay() {
        tvCurrentScore.setText(String.valueOf(currentScore));
        tvHighScore.setText(String.valueOf(highScore));
    }

    /**
     * Resets the current score to zero
     */
    private void resetCurrentScore() {
        currentScore = 0;
        updateScoreDisplay();
    }

    /**
     * Animates the score change with a visual effect
     * @param isPositive True if the score change is positive, false if negative
     */
    private void animateScoreChange(boolean isPositive) {
        TextView scoreView = tvCurrentScore;
        Animation scoreAnim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        scoreAnim.setDuration(300);

        // Change text color based on score change
        if (isPositive) {
            scoreView.setTextColor(Color.parseColor("#4CAF50")); // Green for positive
        } else {
            scoreView.setTextColor(Color.parseColor("#F44336")); // Red for negative
        }

        scoreView.startAnimation(scoreAnim);

        // Reset text color after animation
        new Handler().postDelayed(() ->
            scoreView.setTextColor(Color.parseColor("#1976D2")), 500);
    }

    /**
     * Checks if the game is over (all pairs found)
     */
    private void checkGameOver() {
        if (pairsFound == NUM_CARDS / 2) {
            // Game over - show a dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Game Over");
            builder.setMessage("Final Score: " + currentScore);
            builder.setPositiveButton("OK", (dialog, which) -> {
                // Reset the game
                resetGame();
            });
            builder.show();

            // Update high score if necessary
            if (currentScore > highScore) {
                highScore = currentScore;
                preferences.edit().putInt(HIGH_SCORE_KEY, highScore).apply();
            }
        }
    }

    /**
     * Resets the game to its initial state
     */
    private void resetGame() {
        setupGame();
    }
}

