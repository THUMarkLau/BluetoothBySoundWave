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
import android.os.Messenger;
import android.os.RemoteException;
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
    private MusicService musicService;
    String serverAddr;

    private final ControlHandler controlHandler = new ControlHandler(this);

    static class ControlHandler extends Handler {
        WeakReference<MainActivity> weakReference;

        public ControlHandler(MainActivity activity) {
            weakReference = new WeakReference<>(activity);
        }


        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == InternetUtils.MAKE_WAV_RESULT) {
                Toast.makeText(weakReference.get(), (CharSequence) msg.obj, Toast.LENGTH_SHORT).show();
            }
        }
    }

    ServiceConnection waveconn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            waveService = ((WaveService.WaveBinder) service).getService();
            waveService.setMsgHandler(controlHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            waveService = null;
        }
    };

    ServiceConnection musicconn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicService = ((MusicService.MusicBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GetPermission();
        Intent waveintent = new Intent(this, WaveService.class);
        bindService(waveintent, waveconn, Service.BIND_AUTO_CREATE);
        Intent musicintent = new Intent(this, MusicService.class);
        bindService(musicintent, musicconn, Service.BIND_AUTO_CREATE);
    }

    private void GetPermission() {

        /*在此处插入运行时权限获取的代码*/
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
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

    public void chooseWav(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/wav");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 1);  // 获取本地的音频文件
    }

    public void startPlayer(View view) {
        musicService.playMedia();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {//是否选择，没选择就不会继续
            musicService.setFileUri(data.getData());
//            Uri contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
//            final String docId = DocumentsContract.getDocumentId(uri);
//            final String[] split = docId.split(":");
//            final String[] selectionArgs = new String[]{split[1]};
//            openFilePath = getDataColumn(contentUri, selectionArgs);
        }
    }

}
