package com.example.memorycardflip;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.media.MediaPlayer;

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
    private boolean isProcessing = false; // Flag to prevent clicking during card processing
    private int pairsFound = 0;

    // List of card image resources
    private final Integer[] cardImagesEasy = {
        R.drawable.apple, R.drawable.banana, R.drawable.grapes,
        R.drawable.hippo, R.drawable.lion, R.drawable.monkey
    };
    private final Integer[] cardImagesHard = {
        R.drawable.apple, R.drawable.banana, R.drawable.grapes,
        R.drawable.hippo, R.drawable.lion, R.drawable.monkey,
        R.drawable.orange, R.drawable.img_1, R.drawable.hello,
        R.drawable.fish, R.drawable.watermelon
    };
    private Integer[] cardImages = cardImagesEasy; // Default

    // List to track card IDs and their positions
    private List<Integer> cardPositions;

    // SharedPreferences for saving high score
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "MemoryCardFlipPrefs";
    private static final String HIGH_SCORE_KEY = "HighScore";

    // MediaPlayer instances for sound effects
    private MediaPlayer clickSound;
    private MediaPlayer matchSound;
    private MediaPlayer mismatchSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);

            // Initialize UI elements with null checks
            tvCurrentScore = findViewById(R.id.tvCurrentScore);
            tvHighScore = findViewById(R.id.tvHighScore);
            gameGrid = findViewById(R.id.gameGrid);
            btnResetGame = findViewById(R.id.btnResetGame);

            // Check if all UI elements were found
            if (tvCurrentScore == null || tvHighScore == null || gameGrid == null || btnResetGame == null) {
                Toast.makeText(this, "Error: UI elements not found", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Get game mode and sound settings from intent with safe defaults
            String gameMode = getIntent().getStringExtra("mode");
            if (gameMode == null) {
                gameMode = "easy"; // Default fallback
            }
            isSoundEnabled = getIntent().getBooleanExtra("soundEnabled", true);

            // Set number of cards and images based on difficulty
            if ("hard".equals(gameMode)) {
                NUM_CARDS = NUM_CARDS_HARD;
                cardImages = cardImagesHard;
                gameGrid.setColumnCount(5);
                gameGrid.setRowCount(4);
            } else {
                NUM_CARDS = NUM_CARDS_EASY;
                cardImages = cardImagesEasy;
                gameGrid.setColumnCount(4);
                gameGrid.setRowCount(3);
            }

            // Load high score from SharedPreferences
            preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            highScore = preferences.getInt(HIGH_SCORE_KEY, 0);
            updateScoreDisplay();

            // Set up reset button
            btnResetGame.setOnClickListener(v -> resetGame());

            // Initialize and setup the game
            setupGame();

            // Initialize sound effects
            clickSound = MediaPlayer.create(this, R.raw.click);
            matchSound = MediaPlayer.create(this, R.raw.match);
            mismatchSound = MediaPlayer.create(this, R.raw.notmatch);

        } catch (Exception e) {
            // Handle any exceptions during initialization
            logError("Error initializing game: " + e.getMessage(), e);
            finish();
        }
    }

    /**
     * Sets up the game board with cards
     */
    private void setupGame() {
        try {
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
            int numPairs = NUM_CARDS / 2;

            // Ensure we have enough images
            for (int i = 0; i < numPairs; i++) {
                // Use modulo to safely cycle through available images
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

                // Set default image first
                try {
                    card.setImageResource(R.drawable.back_card);
                } catch (Exception e) {
                    // Fallback if back_card doesn't exist
                    card.setImageResource(android.R.drawable.ic_menu_gallery);
                }

                // Use proper layout parameters for the grid
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = 0;
                params.columnSpec = GridLayout.spec(i % numColumns, 1, 1f);
                params.rowSpec = GridLayout.spec(i / numColumns, 1, 1f);
                params.setMargins(8, 8, 8, 8);

                card.setLayoutParams(params);
                card.setScaleType(ImageView.ScaleType.CENTER_CROP);

                // Set card background based on mode
                if (NUM_CARDS == NUM_CARDS_HARD) {
                    card.setBackgroundResource(R.drawable.card_hard_mode);
                } else {
                    card.setBackgroundResource(R.drawable.card_background);
                }

                // Set tag to identify this card's position
                card.setTag(i);

                // Set up click listener for card
                final int position = i;
                card.setOnClickListener(v -> {
                    if (isProcessing || firstCard == card) {
                        return; // Prevent clicking during animations or same card twice
                    }

                    // Flip the card to show the image
                    flipCard(card, cardPositions.get(position));
                });

                gameGrid.addView(card);
            }
        } catch (Exception e) {
            logError("Error setting up game: " + e.getMessage(), e);
        }
    }

    /**
     * Handles card flipping logic
     * @param card The card ImageView being flipped
     * @param imageIndex The index of the image to display
     */
    private void flipCard(ImageView card, int imageIndex) {
        isProcessing = true;

        try {
            // Play click sound
            playSound(clickSound);

            // Ensure the imageIndex is valid
            int safeImageIndex = Math.abs(imageIndex) % cardImages.length;

            // Create a new handler for delayed operations - Fixed: Use Looper.getMainLooper()
            Handler handler = new Handler(Looper.getMainLooper());

            // Simply show the image
            try {
                card.setImageResource(cardImages[safeImageIndex]);
            } catch (Exception e) {
                // Fallback image
                card.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // If this is the first card flipped
            if (firstCard == null) {
                firstCard = card;
                firstCardId = safeImageIndex;
                isProcessing = false;
            } else {
                // This is the second card

                // Check if it's a match
                if (firstCardId == safeImageIndex) {
                    // Play match sound
                    playSound(matchSound);

                    // Cards match
                    handler.postDelayed(() -> {
                        try {
                            // Add a highlight effect for matched cards
                            card.setBackgroundColor(Color.parseColor("#4CAF50")); // Green background
                            firstCard.setBackgroundColor(Color.parseColor("#4CAF50"));

                            // Add points
                            addPoints(POINTS_PER_MATCH);

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
                        } catch (Exception e) {
                            logError("Error processing match: " + e.getMessage(), e);
                            isProcessing = false;
                        }
                    }, 300);
                } else {
                    // Play mismatch sound
                    playSound(mismatchSound);

                    // Cards don't match, add penalty
                    addPoints(PENALTY_FOR_MISMATCH);

                    // Delay before hiding cards again
                    handler.postDelayed(() -> {
                        try {
                            // Flip both cards back
                            card.setImageResource(R.drawable.back_card);
                            firstCard.setImageResource(R.drawable.back_card);
                        } catch (Exception e) {
                            // Fallback
                            card.setImageResource(android.R.drawable.ic_menu_gallery);
                            firstCard.setImageResource(android.R.drawable.ic_menu_gallery);
                        }

                        // Reset for next attempt
                        firstCard = null;
                        firstCardId = -1;
                        isProcessing = false;
                    }, DELAY_BEFORE_HIDING);
                }
            }
        } catch (Exception e) {
            // If any error occurs, reset the game state
            try {
                card.setImageResource(R.drawable.back_card);
            } catch (Exception ex) {
                card.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            isProcessing = false;
            logError("Error flipping card: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if the game is over (all pairs found)
     */
    private void checkGameOver() {
        if (pairsFound >= NUM_CARDS / 2) {
            // Update high score if necessary
            if (currentScore > highScore) {
                highScore = currentScore;
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(HIGH_SCORE_KEY, highScore);
                editor.apply();
                updateScoreDisplay();
            }

            // Game is over - show a dialog
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Game Over");
                builder.setMessage("Final Score: " + currentScore);
                builder.setPositiveButton("OK", (dialog, which) -> resetGame());
                builder.setCancelable(false);

                AlertDialog dialog = builder.create();
                if (!isFinishing() && !isDestroyed()) {
                    dialog.show();
                }
            } catch (Exception e) {
                logError("Error showing game over dialog: " + e.getMessage(), e);
                // Fallback: just reset the game
                resetGame();
            }
        }
    }

    /**
     * Updates the score display on the UI
     */
    private void updateScoreDisplay() {
        try {
            if (tvCurrentScore != null) {
                tvCurrentScore.setText(String.valueOf(currentScore));
            }
            if (tvHighScore != null) {
                tvHighScore.setText(String.valueOf(highScore));
            }
        } catch (Exception e) {
            logError("Error updating score display: " + e.getMessage(), e);
        }
    }

    private void resetGame() {
        setupGame();
    }

    private void resetCurrentScore() {
        currentScore = 0;
        updateScoreDisplay();
    }

    private void addPoints(int points) {
        currentScore += points;
        updateScoreDisplay();
    }

    private void playSound(MediaPlayer sound) {
        if (isSoundEnabled && sound != null) {
            sound.start();
        }
    }

    private void logError(String message, Exception e) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        e.printStackTrace();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clickSound != null) clickSound.release();
        if (matchSound != null) clickSound.release();
        if (mismatchSound != null) clickSound.release();
    }
}
