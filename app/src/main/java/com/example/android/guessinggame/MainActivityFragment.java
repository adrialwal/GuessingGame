package com.example.android.guessinggame;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MainActivityFragment extends Fragment {

    private static final String TAG = "Guessing Game Activity";

    private static final int STATES_IN_QUIZ = 10;

    private List<String> fileNameList; // state file names
    private List<String> quizStateList; // states in current quiz
    private Set<String> regionsSet; // usa regions in current quiz
    private String correctAnswer; // correct country for the current state shape
    private int totalGuesses; // number of guesses made
    private int correctAnswers; // number of correct guesses
    private int guessRows; // number of rows displaying guess Buttons
    private SecureRandom random; // used to randomize the quiz
    private Handler handler; // used to delay loading next flag
    private Animation shakeAnimation; // animation for incorrect guess
    private static boolean soundOn = true; // sound is on by default

    private LinearLayout quizLinearLayout; // layout that contains the quiz
    private TextView questionNumberTextView; // shows current question #
    private ImageView stateImageView; // displays a state shape
    private LinearLayout[] guessLinearLayouts; // rows of answer Buttons
    private TextView answerTextView; // displays correct answer

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view =
                inflater.inflate(R.layout.fragment_main, container, false);

        fileNameList = new ArrayList<>();
        quizStateList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        // load the shake animation that's used for incorrect answers
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(),
                R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3); // animation repeats 3 times

        // get references to GUI components
        quizLinearLayout =
                (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView =
                (TextView) view.findViewById(R.id.questionNumberTextView);
        stateImageView = (ImageView) view.findViewById(R.id.stateImageView);
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] =
                (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] =
                (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] =
                (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] =
                (LinearLayout) view.findViewById(R.id.row4LinearLayout);
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);

        // configure listeners for the guess Buttons
        for (LinearLayout row : guessLinearLayouts) {
            for (int column = 0; column < row.getChildCount(); column++) {
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }

        // set questionNumberTextView's text
        questionNumberTextView.setText(
                getString(R.string.question, 1, STATES_IN_QUIZ));

        if (soundOn) {
            MediaPlayer mediaPlayer = MediaPlayer.create(getActivity(), R.raw.sfx_welcome);
            mediaPlayer.start();
        }

        return view; // return the fragment's view for display
    }

    // update soundOn based on value in SharedPreferences
    public void updateSoundOnOff(SharedPreferences sharedPreferences){
        // get the number of guess buttons that should be displayed
        String soundOnOff =
                sharedPreferences.getString(MainActivity.SOUNDS_ON_OFF, null);
        if (soundOnOff.compareToIgnoreCase("ON") == 0) {
            soundOn = true;
        } else{
            soundOn = false;
        }
    }

    // update guessRows based on value in SharedPreferences
    public void updateGuessRows(SharedPreferences sharedPreferences) {
        // get the number of guess buttons that should be displayed
        String choices =
                sharedPreferences.getString(MainActivity.CHOICES, null);
        guessRows = Integer.parseInt(choices) / 2;

        // hide all guess button LinearLayouts
        for (LinearLayout layout : guessLinearLayouts)
            layout.setVisibility(View.GONE);

        // display appropriate guess button LinearLayouts
        for (int row = 0; row < guessRows; row++)
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
    }

    // update state regions for quiz based on values in SharedPreferences
    public void updateRegions(SharedPreferences sharedPreferences) {
        regionsSet =
                sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    // set up and start the next quiz
    public void resetQuiz() {
        // use AssetManager to get image file names for enabled regions
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear();

        try {
            // loop through each region
            for (String region : regionsSet) {
                // get a list of all flag image files in this region
                String[] paths = assets.list(region);

                for (String path : paths){
                    fileNameList.add(path.replace(".png", ""));
                }
            }
        }
        catch (IOException exception) {
            Log.e(TAG, "Error loading image file names", exception);
        }

        correctAnswers = 0; // reset the number of correct answers made
        totalGuesses = 0; // reset the total number of guesses the user made
        quizStateList.clear(); // clear prior list of quiz state shapes

        int stateCounter = 1;
        int numberOfStates = fileNameList.size();

        // add STATES_IN_QUIZ random file names to the quizStateList
        while (stateCounter <= STATES_IN_QUIZ) {
            int randomIndex = random.nextInt(numberOfStates);

            // get the random file name
            String filename = fileNameList.get(randomIndex);

            // if the region is enabled and it hasn't already been chosen
            if (!quizStateList.contains(filename)) {
                quizStateList.add(filename);
                ++stateCounter;
            }
        }

        loadNextState(); // start the quiz by loading the first state shape
    }

    // after the user guesses a correct state, load the next state
    private void loadNextState() {
        // get file name of the next flag and remove it from the list
        String nextImage = quizStateList.remove(0);
        correctAnswer = nextImage; // update the correct answer
        answerTextView.setText(""); // clear answerTextView

        // display current question number
        questionNumberTextView.setText(getString(
                R.string.question, (correctAnswers + 1), STATES_IN_QUIZ));

        // extract the region from the next image's name
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        // use AssetManager to load next image from assets folder
        AssetManager assets = getActivity().getAssets();

        // get an InputStream to the asset representing the next question
        // and try to use the InputStream
        try (InputStream stream =
                     assets.open(region + "/" + nextImage + ".png")) {
            // load the asset as a Drawable and display on the stateImageView
            Drawable state = Drawable.createFromStream(stream, nextImage);
            stateImageView.setImageDrawable(state);
        }
        catch (IOException exception) {
            Log.e(TAG, "Error loading " + nextImage, exception);
        }

        Collections.shuffle(fileNameList); // shuffle file names

        // put the correct answer at the end of fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        // add 2, 4, 6 or 8 guess Buttons based on the value of guessRows
        for (int row = 0; row < guessRows; row++) {
            // place Buttons in currentTableRow
            for (int column = 0;
                 column < guessLinearLayouts[row].getChildCount();
                 column++) {
                // get reference to Button to configure
                Button newGuessButton =
                        (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                // get country name and set it as newGuessButton's text
                String filename = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getStateName(filename));
            }
        }

        // randomly replace one Button with the correct answer
        int row = random.nextInt(guessRows); // pick random row
        int column = random.nextInt(2); // pick random column
        LinearLayout randomRow = guessLinearLayouts[row]; // get the row
        String stateName = getStateName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(stateName);
    }

    // parses the states file name and returns the state name
    private String getStateName(String name) {
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }

    // animates the entire quizLinearLayout on or off screen
    private void animate(boolean animateOut) {
        if (correctAnswers == 0)
            return;

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(quizLinearLayout, "alpha", 0f);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                loadNextState();
            }
        });
        fadeOut.setDuration(500);
        ObjectAnimator mover = ObjectAnimator.ofFloat(quizLinearLayout, "translationY", -600f, 0f);
        mover.setDuration(500);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(quizLinearLayout, "alpha", 0f, 1f);
        fadeIn.setDuration(500);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(mover).with(fadeIn).after(fadeOut);
        animatorSet.start();
    }

    // called when a guess Button is touched
    private OnClickListener guessButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Button guessButton = ((Button) v);
            String guess = guessButton.getText().toString();
            String answer = getStateName(correctAnswer);
            ++totalGuesses; // increment number of guesses the user has made

            if (guess.equals(answer)) { // if the guess is correct
                ++correctAnswers; // increment the number of correct answers

                // display correct answer in green text
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(
                        getResources().getColor(R.color.correct_answer,
                                getContext().getTheme()));

                if (soundOn) {
                    MediaPlayer mediaPlayer = MediaPlayer.create(getActivity(), R.raw.game_sound_correct);
                    mediaPlayer.start();
                }

                disableButtons(); // disable all guess Buttons
                // if the user has correctly identified STATES_IN_QUIZ shapes
                if (correctAnswers == STATES_IN_QUIZ) {

                    AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                    alertDialog.setTitle("Game Results");
                    alertDialog.setMessage(
                        getString(R.string.results,
                                totalGuesses,
                                ((STATES_IN_QUIZ * 1000) / (double) totalGuesses)));
                    // "Reset Quiz" Button
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,getString(R.string.reset_quiz),
                        new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        if (soundOn) {
                                            MediaPlayer mediaPlayer = MediaPlayer.create(getActivity(), R.raw.sfx_welcome);
                                            mediaPlayer.start();
                                        }
                                        dialog.dismiss();
                                        resetQuiz();
                                    }
                        });
                    alertDialog.show();
                    }
                else { // answer is correct but quiz is not over
                    // load the next state after a 2-second delay
                    handler.postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    animate(true);
                                }
                            }, 1000); // 1000 milliseconds for 1-second delay
                }
            }
            else { // answer was incorrect

                stateImageView.startAnimation(shakeAnimation); // play shake

                Toast.makeText(getActivity(),
                        R.string.incorrect_message,
                        Toast.LENGTH_SHORT).show();

                // display "Incorrect!" in red
                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(getResources().getColor(
                        R.color.incorrect_answer, getContext().getTheme()));
                guessButton.setEnabled(false); // disable incorrect answer

                if (soundOn) {
                    MediaPlayer mediaPlayer = MediaPlayer.create(getActivity(), R.raw.game_sound_wrong);
                    mediaPlayer.start();
                }
                guessButton.setEnabled(false); // disable incorrect answer
            }
        }
    };

    // utility method that disables all answer Buttons
    private void disableButtons() {
        for (int row = 0; row < guessRows; row++) {
            LinearLayout guessRow = guessLinearLayouts[row];
            for (int i = 0; i < guessRow.getChildCount(); i++)
                guessRow.getChildAt(i).setEnabled(false);
        }
    }
}