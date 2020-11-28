package com.wisys.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.IBinder;

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

    public interface StreamEncoding {
        byte[] encode(byte[] input, String mode);
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

    private static byte[] sin(int waveLen, int length) {
        byte[] wave = new byte[length];
        for (int i = 0; i < length; i++) {
            wave[i] = (byte) (127 * (1 - Math.sin(2 * Math.PI
                    * ((i % waveLen) * 1.00 / waveLen))));
        }
        return wave;
    }

    public void start(String text) {
        byte[] input = text.getBytes(StandardCharsets.UTF_8);
        int waveLen = 2;
        int length = 44100;
        byte[] wave = sin(waveLen, length);
        length = wave.length;
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_IN_STEREO, // CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_8BIT, length, AudioTrack.MODE_STREAM);
        //生成正弦波
        if (audioTrack != null) {
            audioTrack.play();
            audioTrack.write(wave, 0, length);
        }
    }

    @Override
    public void onDestroy() {
        audioTrack.stop();
        audioTrack.release();
    }
}
