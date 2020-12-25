package com.wisys.service;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class MusicService extends Service {

    private MediaPlayer mediaPlayer;
    private final MusicBinder mBinder = new MusicBinder();

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO
        this.playMedia();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
    }

    @Override

    public void onDestroy() {
        super.onDestroy();
        // TODO
        mediaPlayer.release();
    }

    public void playMedia() {
        // TODO
        try {
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setFileUri(Uri uri) {
        mediaPlayer.reset();
        try {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.setDataSource(this, uri);
        } catch (IOException e) {
            Log.e("setFileUri: ", e.getMessage());
            e.printStackTrace();
        }
    }

}
