package com.example.bluetoothreceiver;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.lang.ref.WeakReference;

public class Listener {
    final String TAG = "LISTENER";
    AudioRecord audioRecord;
    final int SAMPLE_RATE_IN_HZ = 44100;
    final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
            AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
    final WeakReference<MainActivity> mainActivityWeakReference;
    boolean stop;
    final Object mLock, nLock;

    public Listener(MainActivity activity) {
        mLock = new Object();
        nLock = new Object();
        mainActivityWeakReference = new WeakReference<>(activity);
    }

    public void start() {
        stop = false;
        if (audioRecord == null) {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_IN_HZ,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                audioRecord.startRecording();
                byte[] buffer = new byte[BUFFER_SIZE];
                final MainActivity activity = mainActivityWeakReference.get();
                int totalSize = 0, startIdx = 0, endIdx = 0;
                int threshold = 0;
                while(!stop) {
                    int r = audioRecord.read(buffer, 0, BUFFER_SIZE);
                    long v = 0;
                    for(byte value : buffer) {
                        v += value * value;
                    }
                    double mean = v / (double) r;
                    final double volume = 10 * Math.log10(mean);
                    Log.d(TAG, "db value: " + volume);
                    if (volume > 60.d) {
                        if (threshold == 0) {
                            int i;
                            for(i = 0; i < r; i++) {
                                if (buffer[i] >= 1e3) break;
                            }
                            startIdx = totalSize + i;
                            threshold = startIdx + 2 * SAMPLE_RATE_IN_HZ;
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.play();
                                }
                            });
                        } else if (totalSize + BUFFER_SIZE >= threshold) {
                            int i;
                            for(i = 0; i < r; i++) {
                                if (buffer[i] >= 1e3)
                                    break;
                            }
                            endIdx = totalSize + i;
                            if (null != activity) {
                                final int finalEndIdx = endIdx;
                                final int finalStartIdx = startIdx;
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        activity.showMsg("采样点数：" + (finalEndIdx - finalStartIdx));
                                        stop = true;
                                    }
                                });
                            }
                        }
                    }
                    totalSize += r;
                    synchronized (mLock) {
                        try{
                            mLock.wait(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                if (null != activity) {
                    activity.showMsg("Exit");
                }
            }
        }).start();
    }

    public void stop() {
        stop = true;
    }
}
