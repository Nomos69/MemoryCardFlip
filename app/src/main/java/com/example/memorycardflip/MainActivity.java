package com.example.memorycardflip;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
    private Integer[] cardImages = {
            R.drawable.apple, R.drawable.banana, R.drawable.grapes,
            R.drawable.hippo, R.drawable.lion, R.drawable.monkey
    };

    // List to track card IDs and their positions
    private List<Integer> cardPositions;

    // SharedPreferences for saving high score
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "MemoryCardFlipPrefs";
    private static final String HIGH_SCORE_KEY = "HighScore";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
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

            // Set up reset button
            btnResetGame.setOnClickListener(v -> resetGame());

            // Initialize and setup the game
            setupGame();
        } catch (Exception e) {
            // Handle any exceptions during initialization
            Toast.makeText(this, "Error initializing game", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
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
            Toast.makeText(this, "Error setting up game", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
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
            // Ensure the imageIndex is valid
            int safeImageIndex = imageIndex % cardImages.length;

            // Create a new handler for delayed operations
            Handler handler = new Handler();

            // Simply show the image
            card.setImageResource(cardImages[safeImageIndex]);

            // If this is the first card flipped
            if (firstCard == null) {
                firstCard = card;
                firstCardId = safeImageIndex;
                isProcessing = false;
            } else {
                // This is the second card

                // Check if it's a match
                if (firstCardId == safeImageIndex) {
                    // Cards match
                    handler.postDelayed(() -> {
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
                    }, 300);
                } else {
                    // Cards don't match, add penalty
                    addPoints(PENALTY_FOR_MISMATCH);

                    // Delay before hiding cards again
                    handler.postDelayed(() -> {
                        // Flip both cards back
                        card.setImageResource(R.drawable.back_card);
                        firstCard.setImageResource(R.drawable.back_card);

                        // Reset for next attempt
                        firstCard = null;
                        firstCardId = -1;
                        isProcessing = false;
                    }, DELAY_BEFORE_HIDING);
                }
            }
        } catch (Exception e) {
            // If any error occurs, reset the game state
            card.setImageResource(R.drawable.back_card);
            isProcessing = false;
            e.printStackTrace();
        }
    }

    /**
     * Checks if the game is over (all pairs found)
     */
    private void checkGameOver() {
        if (pairsFound >= NUM_CARDS / 2) {
            // Game is over - show a dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Game Over");
            builder.setMessage("Final Score: " + currentScore);
            builder.setPositiveButton("OK", (dialog, which) -> resetGame());
            builder.show();

            // Update high score if necessary
            if (currentScore > highScore) {
                highScore = currentScore;
                preferences.edit().putInt(HIGH_SCORE_KEY, highScore).apply();
            }
        }
    }

    /**
     * Updates the score display on the UI
     */
    private void updateScoreDisplay() {
        tvCurrentScore.setText(String.valueOf(currentScore));
        tvHighScore.setText(String.valueOf(highScore));
    }

    /**
     * Adds points to the current score
     * @param points The number of points to add (can be negative for penalties)
     */
    private void addPoints(int points) {
        currentScore += points;
        // Ensure score doesn't go below zero
        if (currentScore < 0) {
            currentScore = 0;
        }
        updateScoreDisplay();
    }

    /**
     * Resets the current score to zero
     */
    private void resetCurrentScore() {
        currentScore = 0;
        updateScoreDisplay();
    }

    /**
     * Resets the game to its initial state
     */
    private void resetGame() {
        setupGame();
    }
}
