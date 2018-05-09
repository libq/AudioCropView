package com.libq.audiocropviewdemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.libq.audiocropview.AudioCropView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final AudioCropView acv = (AudioCropView) findViewById(R.id.acv_crop);
        acv.setAudioDuration(10000);
        acv.setAudioCropDuration(2000);
        acv.setOnCropAreaChangedListener(new AudioCropView.OnCropAreaChangedListener() {
            @Override
            public void onChange(int startTime, int endTime) {
                Log.e("xxxxx","startTime = " + startTime +"  endTime ="+endTime);
            }
        });

        findViewById(R.id.reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acv.reset();
            }
        });

    }
}
