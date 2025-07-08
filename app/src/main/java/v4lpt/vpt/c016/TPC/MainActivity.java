package v4lpt.vpt.c016.TPC;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private CircleTimerView circleTimerView;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 60 * 60 * 1000; // 1 hour
    private int currentSection = 0;
    private MediaPlayer shortBreakSound;
    private MediaPlayer longBreakSound;
    private boolean timerRunning = false;
    private Button backButton;
    private Handler handler = new Handler();
    private Button infoButton;
    private MaterialButton soundToggleButton;
    private MaterialButton vibrateToggleButton;
    private Vibrator vibrator;
    private SharedPreferences preferences;
    private boolean isSoundEnabled = true;
    private boolean isVibrationEnabled = true;
    private static final long LONG_VIBRATION_DURATION = 3000;
    private static final long SHORT_VIBRATION_DURATION = 1000;
    private static final String PREF_SOUND_ENABLED = "sound_enabled";
    private static final String PREF_VIBRATION_ENABLED = "vibration_enabled";
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_layout);

        showSystemUI();

        preferences = getPreferences(MODE_PRIVATE);
        isSoundEnabled = preferences.getBoolean(PREF_SOUND_ENABLED, true);
        isVibrationEnabled = preferences.getBoolean(PREF_VIBRATION_ENABLED, true);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        shortBreakSound = MediaPlayer.create(this, R.raw.kanlgschalde);
        longBreakSound = MediaPlayer.create(this, R.raw.kanlgschalde);
        Button startTimerButton = findViewById(R.id.startTimerButton);

        setupControls();
        setupStartTimerButton(startTimerButton, savedInstanceState);
    }

    private void setupControls() {
        soundToggleButton = findViewById(R.id.soundToggleButton);
        vibrateToggleButton = findViewById(R.id.vibrateToggleButton);

        if (soundToggleButton == null || vibrateToggleButton == null) {
            Log.e(TAG, "Toggle buttons not found in layout");
            return;
        }

        updateSoundButtonIcon();
        updateVibrateButtonIcon();

        soundToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSoundEnabled = !isSoundEnabled;
                updateSoundButtonIcon();
                savePreferences();
                if (isSoundEnabled) {
                    // Play a short test sound
                    MediaPlayer testSound = MediaPlayer.create(MainActivity.this, R.raw.kanlgschalde);
                    testSound.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            mp.release();
                        }
                    });
                    testSound.start();
                }
            }
        });

        vibrateToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isVibrationEnabled = !isVibrationEnabled;
                updateVibrateButtonIcon();
                savePreferences();
                if (isVibrationEnabled && vibrator != null) {
                    vibrate(200); // Test vibration
                }
            }
        });
    }


    private void updateSoundButtonIcon() {
        if (soundToggleButton != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    soundToggleButton.setIconResource(isSoundEnabled ?
                            R.drawable.sound_on : R.drawable.sound_off);
                    Log.d(TAG, "Sound icon updated: " + (isSoundEnabled ? "ON" : "OFF"));
                }
            });
        }
    }

    private void updateVibrateButtonIcon() {
        if (vibrateToggleButton != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    vibrateToggleButton.setIconResource(isVibrationEnabled ?
                            R.drawable.vibrator_on : R.drawable.vibrator_off);
                    Log.d(TAG, "Vibration icon updated: " + (isVibrationEnabled ? "ON" : "OFF"));
                }
            });
        }
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_SOUND_ENABLED, isSoundEnabled);
        editor.putBoolean(PREF_VIBRATION_ENABLED, isVibrationEnabled);
        editor.apply();
        Log.d(TAG, "Preferences saved - Sound: " + isSoundEnabled + ", Vibration: " + isVibrationEnabled);
    }

    private void vibrate(long duration) {
        if (vibrator == null || !isVibrationEnabled) {
            Log.d(TAG, "Vibration skipped: " + (vibrator == null ? "no vibrator" : "disabled"));
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                // Deprecated in API 26
                vibrator.vibrate(duration);
            }
            Log.d(TAG, "Vibrating for " + duration + "ms");
        } catch (Exception e) {
            Log.e(TAG, "Error during vibration: " + e.getMessage());
        }
    }

    private void playTransitionEffects(boolean isLongBreak) {
        Log.d(TAG, "Playing transition effects - Long break: " + isLongBreak +
                ", Sound enabled: " + isSoundEnabled +
                ", Vibration enabled: " + isVibrationEnabled);

        if (isSoundEnabled) {
            MediaPlayer playerToUse = isLongBreak ? longBreakSound : shortBreakSound;
            if (playerToUse != null) {
                // If the sound is already playing, pause it first.
                if (playerToUse.isPlaying()) {
                    playerToUse.pause();
                }
                playerToUse.seekTo(0); // Rewind to the beginning
                playerToUse.start();   // Play the sound
            }
        }

        if (isVibrationEnabled) {
            vibrate(isLongBreak ? LONG_VIBRATION_DURATION : SHORT_VIBRATION_DURATION);
        }
    }

    private void checkSectionTransition() {
        int[] sections = {25, 5, 25, 5}; // in minutes
        long totalTime = 60 * 60 * 1000; // 1 hour in milliseconds
        long elapsedTime = totalTime - timeLeftInMillis;

        int newSection = 0;
        long accumulatedTime = 0;
        for (int i = 0; i < sections.length; i++) {
            accumulatedTime += sections[i] * 60 * 1000;
            if (elapsedTime < accumulatedTime) {
                newSection = i;
                break;
            }
        }

        if (newSection != currentSection) {
            Log.d(TAG, "Section transition: " + currentSection + " -> " + newSection);
            currentSection = newSection;
            playTransitionEffects(currentSection % 2 == 0);
        }
    }
    private void setupStartTimerButton(Button startTimerButton, final Bundle savedInstanceState) {
        infoButton = findViewById(R.id.infoButton);
        infoButton.setOnClickListener(v -> openInfoFragment());

        startTimerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSystemUI();
                setContentView(R.layout.activity_main);


                circleTimerView = findViewById(R.id.circleTimerView);
                backButton = findViewById(R.id.backButton);
                View rootView = findViewById(android.R.id.content);
                rootView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showBackButton();
                    }
                });

                backButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        resetTimerAndGoBack();
                        showSystemUI();
                    }
                });

                if (savedInstanceState != null) {
                    timeLeftInMillis = savedInstanceState.getLong("timeLeftInMillis");
                    timerRunning = savedInstanceState.getBoolean("timerRunning");
                    if (timerRunning) {
                        startTimer();
                    } else {
                        updateTimerUI();
                    }
                }
                setupTimer();
            }
        });
    }

    private void setupTimer() {
        circleTimerView = findViewById(R.id.circleTimerView);

        // This single call now correctly handles both sound and vibration
        // for the start of the timer.
        playTransitionEffects(true);

        startTimer();
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // Adjust the layout params of the rating layout
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        getWindow().setAttributes(params);
    }

    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        // Reset the layout params
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags &= ~(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setAttributes(params);
    }
    private void showBackButton() {
        backButton.setVisibility(View.VISIBLE);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                backButton.setVisibility(View.INVISIBLE);
            }
        }, 1727); // 1.727 seconds
    }
    private void openInfoFragment() {
        InfoFragment infoFragment = new InfoFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, infoFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
    public void closeInfoFragment() {
        getSupportFragmentManager().popBackStack();
    }
    private void resetTimerAndGoBack() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timeLeftInMillis = 60 * 60 * 1000; // Reset to 1 hour
        updateTimerUI();
        setContentView(R.layout.welcome_layout);

        // Re-initialize buttons with correct states
        Button startTimerButton = findViewById(R.id.startTimerButton);
        infoButton = findViewById(R.id.infoButton);
        soundToggleButton = findViewById(R.id.soundToggleButton);
        vibrateToggleButton = findViewById(R.id.vibrateToggleButton);

        // Restore button states and listeners
        setupControls();
        setupStartTimerButton(startTimerButton, null);

        // Force update the icons to match current state
        updateSoundButtonIcon();
        updateVibrateButtonIcon();
    }
    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 50) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerUI();
            }

            @Override
            public void onFinish() {
                // This call plays the sound for the start of a new cycle.
                // 'true' corresponds to the work session sound/vibration.
                playTransitionEffects(true);

                timeLeftInMillis = 60 * 60 * 1000; // Reset to 1 hour
                currentSection = 0;
                startTimer(); // Restart the timer
            }
        }.start();
        timerRunning = true;
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("timeLeftInMillis", timeLeftInMillis);
        outState.putBoolean("timerRunning", timerRunning);
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // The timer will continue running as is
    }
    private void updateTimerUI() {
        circleTimerView.setTimeLeft(timeLeftInMillis);
        checkSectionTransition();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (shortBreakSound != null) {
            shortBreakSound.release();
        }
        if (longBreakSound != null) {
            longBreakSound.release();
        }
    }
}