package com.example.bluetoothreceiver;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class Player {
    AudioTrack audioTrack;
    final int SAMPLE_RATE_IN_HZ = 8000;
    final MainActivity activity;

    Player(MainActivity activity) {
        audioTrack = null;
        this.activity = activity;
    }

    byte[] sin(int waveLen, int length) {
        byte[] wave = new byte[length];
        for (int i = 0; i < length; i++) {
            wave[i] = (byte) (127 * (1 - Math.sin(2 * Math.PI
                    * ((i % waveLen) * 1.00 / waveLen))));
        }
        return wave;
    }

    public void play() {
        int waveLen = 44100 / 3000; // 8000Hz
        final int length = waveLen * 3000;
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_IN_STEREO, // CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_8BIT, length, AudioTrack.MODE_STREAM);
        //生成正弦波
        final byte[] wave = sin(waveLen, length);
        if (audioTrack != null) {
            audioTrack.play();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    audioTrack.write(wave, 0, length);
                }
            }).start();

        }
    }


}
