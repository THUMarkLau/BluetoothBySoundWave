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
    String serverAddr;

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
    }

    public void setServerAddr(View view) {
        this.serverAddr = ((EditText) findViewById(R.id.addr)).getText().toString();
    }

    public void startListener(View view) {
        listenerStart = !listenerStart;
        EditText freq = findViewById(R.id.freq);
        String text = freq.getText().toString();
        TextView tv = findViewById(R.id.textview);
        tv.setText(text);
        waveService.start(text, serverAddr);
    }
}
