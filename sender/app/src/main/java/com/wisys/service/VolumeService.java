package com.wisys.service;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Date;

public class VolumeService {

    private static final String TAG = "AudioRecord";
    static final int SAMPLE_RATE_IN_HZ = 44100;
    static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
            AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
    static final int SAMPLE_SIZE = SAMPLE_RATE_IN_HZ * 5;    // 一次录制5秒音频
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
                AudioFormat.ENCODING_PCM_16BIT, SAMPLE_SIZE);
        isGetVoiceRun = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();
                short[] buffer = new short[SAMPLE_SIZE];
                final MainActivity activity = mActivity.get();
                int totalSize = 0, startindex = 0, endindex = 0;
                int threshold = 0;
                long startTimestamp = 0, endTimestamp;
                while (isGetVoiceRun) {
                    int r = mAudioRecord.read(buffer, 0, SAMPLE_SIZE);
//                    long v = 0;
//                    for (short value : buffer) {
//                        v += value * value;
//                    }
//                    double mean = v / (double) r;
//                    final double volume = 10 * Math.log10(mean);
//                    Log.d(TAG, "db value:" + volume);
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
                    for (int j = 0; j < r; j++) {
                        if (buffer[j] > 3e3) {
                            if (threshold == 0) {
                                startindex = totalSize + j;
                                j += SAMPLE_RATE_IN_HZ * 3 / 2;
                                startTimestamp = new Date().getTime();
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
                            } else {
                                endTimestamp = new Date().getTime();
                                endindex = totalSize + j;
                                if (null != activity) {
                                    final int finalEndindex = endindex;
                                    final int finalStartindex = startindex;
                                    final long finalEndTimestamp = endTimestamp;
                                    final long finalStartTimestamp = startTimestamp;
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            activity.toastMsg("间隔采样点数：" + (finalEndindex - finalStartindex));
                                            long finalDelta = (finalEndTimestamp - finalStartTimestamp);
//                                        finalDelta -= (long) ((30 / 372.d) * finalDelta);
                                            activity.writeText("间隔时间：" + finalDelta + " 间隔采样点：" + (finalEndindex - finalStartindex));
//                                activity.plateView.setValue(volume);
                                        }
                                    });
                                }
                                isGetVoiceRun = false;
                                break;
                            }
                        }
                    }
                    if (!isGetVoiceRun) break;
                    totalSize += SAMPLE_SIZE;
                    // 大概一秒十次
//                    synchronized (mLock) {
//                        try {
//                            mLock.wait(100);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
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