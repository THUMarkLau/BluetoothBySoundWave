package com.wisys.service;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.lang.ref.WeakReference;

public class VolumeService {

    private static final String TAG = "AudioRecord";
    static final int SAMPLE_RATE_IN_HZ = 44100;
    static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
            AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
    AudioRecord mAudioRecord;
    boolean isGetVoiceRun;
    final Object mLock, nLock;
    private final WeakReference<MainActivity> mActivity;

    public VolumeService(MainActivity activity) {
        mLock = new Object();
        nLock = new Object();
        mActivity = new WeakReference<>(activity);
    }

    public void stop() {
        this.isGetVoiceRun = false;
    }

    public void start() {
        this.isGetVoiceRun = true;
    }

    public void getNoiseLevel() {
        if (!isGetVoiceRun) {
            return;
        }
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        isGetVoiceRun = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();
                short[] buffer = new short[BUFFER_SIZE];
                final MainActivity activity = mActivity.get();
                int totalSize = 0, startindex = 0, endindex = 0;
                int threshold = 0;
                while (isGetVoiceRun) {
                    int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
                    long v = 0;
                    for (short value : buffer) {
                        v += value * value;
                    }
                    double mean = v / (double) r;
                    final double volume = 10 * Math.log10(mean);
                    Log.d(TAG, "db value:" + volume);
//                    if (null != activity) {
//                        activity.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (volume > 80.d) {
//                                    activity.toastMsg("Current time: " + System.currentTimeMillis());
//                                    isGetVoiceRun = false;
//                                }
////                                activity.plateView.setValue(volume);
//                            }
//                        });
//                    }
                    if (volume > 60.d) {
                        if (threshold == 0) {
                            int i;
                            for (i = 0; i < r; i++)
                                if (buffer[i] >= 1e3) break;
                            startindex = totalSize + i;
                            threshold = startindex + 2 * SAMPLE_RATE_IN_HZ;
                            if (null != activity) {
                                final int finalStartindex = startindex;
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        activity.toastMsg("初始采样位置：" + finalStartindex);
//                                activity.plateView.setValue(volume);
                                    }
                                });
                            }
                        } else if (totalSize + BUFFER_SIZE >= threshold) {
                            int i;
                            for (i = 0; i < r; i++)
                                if (buffer[i] >= 1e3) break;
                            endindex = totalSize + i;
                            if (null != activity) {
                                final int finalEndindex = endindex;
                                final int finalStartindex = startindex;
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        activity.toastMsg("间隔采样点数：" + (finalEndindex - finalStartindex));
                                        isGetVoiceRun = false;
//                                activity.plateView.setValue(volume);
                                    }
                                });
                            }
                        }
                    }
                    totalSize += r;
                    // 大概一秒十次
                    synchronized (mLock) {
                        try {
                            mLock.wait(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;    // 仅仅release是不行的
//                if (null != activity) {
//                    activity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
////                            activity.plateView.setmMaxValue(0);
////                            activity.plateView.setValue(1);
//                        }
//                    });
//                }
            }
        }).start();
    }
}