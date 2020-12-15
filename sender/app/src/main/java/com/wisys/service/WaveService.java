package com.wisys.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;

import com.wisys.service.util.InternetUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class WaveService extends Service {

    AudioTrack audioTrack;

    private WaveBinder mBinder = new WaveBinder();

    public class WaveBinder extends Binder {
        WaveService getService() {
            return WaveService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public void start(String text, InternetUtils internetUtils) {
        byte[] input = text.getBytes(StandardCharsets.UTF_8);
        byte[] wave = getModData(input, internetUtils);
        int length = wave.length;
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 48000,
                AudioFormat.CHANNEL_IN_STEREO, // CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_8BIT, length, AudioTrack.MODE_STREAM);
        //生成正弦波
        if (audioTrack != null) {
            audioTrack.play();
            audioTrack.write(wave, 0, length);
        }
    }

    public byte[] getModData(byte[] data, InternetUtils internetUtils) {
        byte[] rlt = new byte[0];
        try {
            internetUtils.sendCommand(InternetUtils.CMD_STRING_TO_BIN, data.length);
            internetUtils.sendData(data);
            byte[] bytes = internetUtils.readData();

            internetUtils.sendCommand(InternetUtils.CMD_FSKMOD, bytes.length);
            internetUtils.sendData(bytes);
            String cmd = internetUtils.readCommand();

            if (cmd.equals(InternetUtils.CMD_FSKMOD_RESULT)) rlt = internetUtils.readData();

//            Message controlMsg = new Message();
//            controlMsg.what = InternetUtils.STRING_TO_BIN_RESULT;
//            controlMsg.obj = cmd;
//            internetUtils.controlHandler.sendMessage(controlMsg);
//
//            Message dataMsg = new Message();
//            dataMsg.what = InternetUtils.STRING_TO_BIN_RESULT;
//            dataMsg.obj = bytes;
//            internetUtils.dataHandler.sendMessage(dataMsg);

            internetUtils.sendCommand(InternetUtils.CMD_QUIT, 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return rlt;
    }

    @Override
    public void onDestroy() {
        audioTrack.stop();
        audioTrack.release();
    }
}
