package com.wisys.service;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wisys.service.util.InternetUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.Provider;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    boolean listenerStart = false;
    private WaveService waveService;
    InternetUtils internetUtils;

    private final DataHandler dataHandler = new DataHandler(this);;
    private final ControlHandler controlHandler = new ControlHandler(this);

    static class DataHandler extends Handler {
        WeakReference<MainActivity> weakReference;

        public DataHandler(MainActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case InternetUtils.STRING_TO_BIN_RESULT: {
                    byte[] bytes = (byte[]) msg.obj;
                    System.out.println("The binary of \"Test\" is:");
                    for (byte aByte : bytes) System.out.print(aByte + " ");
                    System.out.println();
                    break;
                }
                case InternetUtils.BIN_TO_STRING_RESULT: {
                    break;
                }
                case InternetUtils.FSK_MOD_RESULT: {
                    break;
                }
                case InternetUtils.FSK_DEMOD_RESULT: {
                    break;
                }
            }
        }
    }

    static class ControlHandler extends Handler {
        WeakReference<MainActivity> weakReference;

        public ControlHandler(MainActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case InternetUtils.STRING_TO_BIN_RESULT: {
                    System.out.println("Recieve cmd:" + (String) msg.obj);
                    break;
                }
                case InternetUtils.BIN_TO_STRING_RESULT: {
                    break;
                }
                case InternetUtils.FSK_MOD_RESULT: {
                    break;
                }
                case InternetUtils.FSK_DEMOD_RESULT: {
                    break;
                }
            }
        }
    }

    ServiceConnection waveconn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            waveService = ((WaveService.WaveBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            waveService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        GetPermission();
        Intent waveintent = new Intent(this, WaveService.class);
        bindService(waveintent, waveconn, Service.BIND_AUTO_CREATE);

        internetUtils.dataHandler = dataHandler;
        internetUtils.controlHandler = controlHandler;
    }

    public void startListener(View view) {
        listenerStart = !listenerStart;
        EditText freq = findViewById(R.id.freq);
        String text = freq.getText().toString();
        TextView tv = findViewById(R.id.textview);
        tv.setText(text);
        waveService.start(text, internetUtils);
    }
}
