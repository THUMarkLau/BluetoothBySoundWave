package com.wisys.service;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class VolumeService {

    private static final String TAG = "AudioRecord";
    static final int SAMPLE_RATE_IN_HZ = 44100;
    static final int SAMPLE_SIZE = SAMPLE_RATE_IN_HZ * 5;    // 一次录制5秒音频
    AudioRecord mAudioRecord;
    boolean isGetVoiceRun;
    String serverAddr;
    int port;
    private final WeakReference<MainActivity> mActivity;

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public VolumeService(MainActivity activity) {
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
                int totalSize = 0, startIdx = 0, endIdx = 0;
                boolean firstVol = true;
                while (isGetVoiceRun) {
                    int r = mAudioRecord.read(buffer, 0, SAMPLE_SIZE);
                    for (int j = 0; j < r; j++) {
                        if (buffer[j] > 3e3) {
                            if (firstVol) {
                                startIdx = totalSize + j;
                                j += SAMPLE_RATE_IN_HZ * 3 / 2;
                                firstVol = false;
                            } else {
                                endIdx = totalSize + j;
                                if (null != activity) {
                                    final int finalEndindex = endIdx;
                                    final int finalStartindex = startIdx;
                                    sendDistData((((double) (finalEndindex - finalStartindex)) / SAMPLE_RATE_IN_HZ) + "", serverAddr, port);
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            activity.toastMsg("间隔采样点数：" + (finalEndindex - finalStartindex));
                                            activity.writeText("间隔采样点数：" + (finalEndindex - finalStartindex));
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
                }
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;    // 仅仅release是不行的
            }
        }).start();
    }

    public static class DistanceProcessor extends Thread {
        byte[] data;
        OutputStream ostream;

        public DistanceProcessor(final byte[] data, final String serverAddr, int port) {
            this.data = data;
            try {
                Socket socket = new Socket(serverAddr, port);
                this.ostream = socket.getOutputStream();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                ostream.write(data);
                ostream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendDistData(String text, String serverAddr, int port) {
        byte[] input = text.getBytes(StandardCharsets.UTF_8);
        new DistanceProcessor(input, serverAddr, port).start();
    }
}