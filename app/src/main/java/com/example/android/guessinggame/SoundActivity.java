package com.example.android.guessinggame;


import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

public class SoundActivity extends AppCompatActivity implements View.OnClickListener{

    Button play;

    static SoundPool soundPool;
    static int sound1 = -1; // welcome sound
    static int sound2 = -1; // correct sound
    static int sound3 = -1; // incorrect sound

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        play =(Button) findViewById(R.id.buttonPlay);
        play.setOnClickListener(this);

        soundPool = new SoundPool( 10, AudioManager.STREAM_MUSIC, 0);
        try{
            AssetManager assetManager = getAssets();
            AssetFileDescriptor descriptor;

            // welcome sound when "Play" button is pressed
            descriptor = assetManager.openFd("raw/sfx_welcome.wav");
            sound1 = soundPool.load(descriptor, 0);

            // correct sound when the correct answer is selected
            descriptor = assetManager.openFd("raw/game_sound_correct.wav");
            sound2 = soundPool.load(descriptor, 0);

            // incorrect sound when the wrong answer is selected
            descriptor = assetManager.openFd("raw/game_sound_wrong.wav");
            sound3 = soundPool.load(descriptor, 0);

        } catch (IOException e) {
        }

    }

    @Override
    public void onClick(View v) {

        soundPool.play(sound1, 1, 1, 0, 0, (float)2);
        startActivity(new Intent(this, MainActivity.class));

    }
}