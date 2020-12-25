package com.wisys.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.wisys.service.util.InternetUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class WaveService extends Service {

    AudioTrack audioTrack;

    private final WaveBinder mBinder = new WaveBinder();
    public static final int SAMPLE_RATE_IN_HZ = 44100;

    private final DataHandler dataHandler = new DataHandler(this);
    private final ControlHandler controlHandler = new ControlHandler(this);
    private Handler msgHandler;

    public void setMsgHandler(Handler msgHandler) {
        this.msgHandler = msgHandler;
    }

    static class DataHandler extends Handler {
        WeakReference<WaveService> weakReference;

        public DataHandler(WaveService service) {
            weakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case InternetUtils.STRING_TO_BIN_RESULT: {
                    byte[] bytes = (byte[]) msg.obj;
                    System.out.println("The binary of \"Test\" is:");
                    for (byte aByte : bytes) System.out.print(aByte + " ");
                    System.out.println();
                    break;
                }
                case InternetUtils.BIN_TO_STRING_RESULT: {
                    break;
                }
                case InternetUtils.FSK_MOD_RESULT: {
                    float[] wave = (float[]) msg.obj;
                    int length = wave.length;
                    weakReference.get().audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 48000,
                            AudioFormat.CHANNEL_IN_STEREO, // CHANNEL_CONFIGURATION_MONO,
                            AudioFormat.ENCODING_PCM_FLOAT, length, AudioTrack.MODE_STREAM);
                    AudioTrack audioTrack = weakReference.get().audioTrack;
                    //生成正弦波
                    audioTrack.play();
                    audioTrack.write(wave, 0, length, AudioTrack.WRITE_BLOCKING);
                    break;
                }
                case InternetUtils.MAKE_WAV_RESULT: {
                    weakReference.get().writeData((byte[]) msg.obj);
                    break;
                }
                case InternetUtils.FSK_DEMOD_RESULT: {
                    break;
                }
            }
        }
    }

    private static byte[] sin(int waveLen, int length) {
        byte[] wave = new byte[length];
        for (int i = 0; i < length; i++) {
            wave[i] = (byte) (127 * (1 - Math.sin(2 * Math.PI
                    * ((i % waveLen) * 1.00 / waveLen))));
        }
        return wave;
    }

    public void playSinWave() {
        int waveLen = 44100 / 2000; // 8000Hz
        int length = waveLen * 2000;
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_IN_STEREO, // CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_8BIT, length, AudioTrack.MODE_STREAM);
        //生成正弦波
        byte[] wave = sin(waveLen, length);
        try {
            Thread.sleep(3100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (audioTrack != null) {
            audioTrack.play();
            audioTrack.write(wave, 0, length);
        }
    }

    static class ControlHandler extends Handler {
        WeakReference<WaveService> weakReference;

        public ControlHandler(WaveService service) {
            weakReference = new WeakReference<>(service);
        }


        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case InternetUtils.STRING_TO_BIN_RESULT: {
                    System.out.println("Recieve cmd:" + (String) msg.obj);
                    break;
                }
                case InternetUtils.BIN_TO_STRING_RESULT: {
                    break;
                }
                case InternetUtils.FSK_MOD_RESULT: {
                    break;
                }
                case InternetUtils.FSK_DEMOD_RESULT: {
                    break;
                }
            }
        }
    }

    public class WaveBinder extends Binder {
        WaveService getService() {
            return WaveService.this;
        }
    }

    public class SignalProcessor extends Thread {
        byte[] data;
        String serverAddr;
        InternetUtils internetUtils;

        public SignalProcessor(final byte[] data, final String serverAddr) {
            this.data = data;
            this.serverAddr = serverAddr;
        }

        @Override
        public void run() {
            internetUtils = new InternetUtils();
            internetUtils.setServerAddr(serverAddr);
            internetUtils.init();
            internetUtils.dataHandler = dataHandler;
            internetUtils.controlHandler = controlHandler;
            getModData(data, internetUtils);
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

    public void start(String text, String serverAddr) {
        byte[] input = text.getBytes(StandardCharsets.UTF_8);
        new SignalProcessor(input, serverAddr).start();
    }

    public static float[] byteToDouble(byte[] data) {
        if (data == null)
            return null;
        float[] result = new float[data.length / 8];
        for (int i = 0; i < data.length / 8; ++i) {
            long value = 0;
            for (int j = 0; j < 8; ++j) {
                value |= ((long) (data[i * 8 + j] & 0xff)) << (8 * j);
            }
            result[i] = (float) Double.longBitsToDouble(value);
        }
        return result;
    }

    private void writeData(byte[] wavdata) {

        String filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Music/test.wav";
        File file = new File(filepath);
        if (file.exists()) file.delete();
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(wavdata);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e("AudioRecorder", Objects.requireNonNull(e.getMessage()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Message msg = Message.obtain();
        msg.what = InternetUtils.MAKE_WAV_RESULT;
        msg.obj = "录音文件存储在/Music/test.wav";
        msgHandler.sendMessage(msg);
    }

    public void getModData(byte[] data, InternetUtils internetUtils) {
        byte[] rlt = new byte[0];
        try {
            internetUtils.sendCommand(InternetUtils.CMD_STRING_TO_BIN, data.length);
            internetUtils.sendData(data);
            internetUtils.readCommand();
            byte[] bytes = internetUtils.readData();

            internetUtils.sendCommand(InternetUtils.CMD_FSKMOD, bytes.length);
            internetUtils.sendData(bytes);
            String cmd = internetUtils.readCommand();

            if (cmd.equals(InternetUtils.CMD_FSKMOD_RESULT)) rlt = internetUtils.readData();

            internetUtils.sendCommand(InternetUtils.CMD_MAKE_WAV, rlt.length);
            internetUtils.sendData(rlt);
            cmd = internetUtils.readCommand();
            byte[] wavdata = internetUtils.readData();

//            Message controlMsg = new Message();
//            controlMsg.what = InternetUtils.FSK_MOD_RESULT;
//            controlMsg.obj = cmd;
//            internetUtils.controlHandler.sendMessage(controlMsg);

            Message dataMsg = new Message();
            dataMsg.what = InternetUtils.MAKE_WAV_RESULT;
            dataMsg.obj = wavdata;
            internetUtils.dataHandler.sendMessage(dataMsg);

            internetUtils.sendCommand(InternetUtils.CMD_QUIT, 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        audioTrack.stop();
        audioTrack.release();
    }
}
