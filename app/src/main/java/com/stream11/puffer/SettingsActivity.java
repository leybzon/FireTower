/*
 *       Copyright 2016 Gene Leybzon
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 */

package com.stream11.puffer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


public class SettingsActivity extends AppCompatActivity {

    private SeekBar mSpeedBar;
    private TextView mSpeedIndicator;
    private int mInitFreq;
    private EditText mBtleAddress;

    public static final String SETTINGS = "settings";
    public static final String SPEED = "speed";
    public static final String ADDRESS = "address";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mSpeedIndicator = (TextView) findViewById(R.id.speedValue);
        mBtleAddress = (EditText) findViewById(R.id.addressBTLE);

        final SharedPreferences settings = getSharedPreferences(SETTINGS, MODE_PRIVATE);
        mInitFreq = settings.getInt(SPEED, 120);

        mSpeedBar = (SeekBar) findViewById(R.id.speedBar);
        mSpeedBar.setMax(600);
        mSpeedBar.setProgress(mInitFreq);

        mSpeedBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int frequencyRate = mInitFreq;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                frequencyRate = progresValue;
                if (frequencyRate<=0) {
                    frequencyRate=1;
                }

                mSpeedIndicator.setText(frequencyRate + " (steps per minute)");
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.v(this.toString(), String.valueOf(frequencyRate));
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(SPEED, frequencyRate);
                editor.commit();
            }
        });

        //mBtleAddress.setOnEditorActionListener();
        //TODO!


    }

}
