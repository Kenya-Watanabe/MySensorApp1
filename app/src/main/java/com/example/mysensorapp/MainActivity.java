package com.example.mysensorapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

@SuppressLint({"ShowToast", "SimpleDateFormat"})
public class MainActivity extends Activity {

    @Override
    protected void onStart() {
        // TODO 自動生成されたメソッド・スタブ
        super.onStart();
    }

    @Override
    protected void onStop() {
        // TODO 自動生成されたメソッド・スタブ

        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onButtonClick(View v) {
        TextView buttonTextView = (TextView) findViewById(R.id.textStatDisp);

        //
        switch (v.getId()) {

            case R.id.startButton:
                buttonTextView.setText("start");
                //ここからServiceの開始
                Intent intentStart = new Intent(getApplication(), SensorService.class);
                startService(intentStart);
                break;

            case R.id.stopButton:
                buttonTextView.setText("stop");
                //ここからServiceの終了
                Intent intentStop = new Intent(getApplication(), SensorService.class);
                stopService(intentStop);
                break;
        }
    }

    @Override
    protected void onPause() {
        // TODO 自動生成されたメソッド・スタブ
        super.onPause();

    }

    @Override
    protected void onResume() {
        // TODO 自動生成されたメソッド・スタブ
        super.onResume();


    }
}










