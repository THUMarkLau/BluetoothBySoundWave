package com.example.bluetoothreceiver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.dd.CircularProgressButton;
import com.dd.processbutton.FlatButton;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.util.Calendar;
import java.util.Date;

import cafe.adriel.androidaudiorecorder.AndroidAudioRecorder;
import cafe.adriel.androidaudiorecorder.model.AudioChannel;
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate;
import cafe.adriel.androidaudiorecorder.model.AudioSource;

public class MainActivity extends AppCompatActivity {

    FlatButton beginRecordBtn;
    FlatButton stopRecordBtn;
    boolean recording = false;
    String rawFilePath;
    String wavFilePath;
    AndroidAudioRecorder recorder;
    int sampleRate = 48000;
    int channel = AudioFormat.CHANNEL_IN_STEREO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    int bufferSize = 0;

    private final int GET_RECODE_AUDIO = 1;
    private String[] PERMISSION_AUDIO = {
            Manifest.permission.RECORD_AUDIO
    };
    private String[] PERMISSION_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyAudioPermissions();
        beginRecordBtn = (FlatButton)findViewById(R.id.begin_record);
        stopRecordBtn = (FlatButton)findViewById(R.id.stop_record);
        rawFilePath = this.getExternalFilesDir(null).getAbsolutePath() + "/raw.wav";
        wavFilePath = this.getExternalFilesDir(null).getAbsolutePath() + "/result.wav";
        final int color = getResources().getColor(R.color.colorPrimaryDark);
        final int requestCode = 0;

        beginRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beginRecordBtn.setVisibility(View.INVISIBLE);
                stopRecordBtn.setVisibility(View.VISIBLE);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        recording = true;
                        startRecord(rawFilePath);
                        copyWaveFile(rawFilePath, wavFilePath);
                    }
                });
                thread.start();
            }
        });
        stopRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recording = false;
                stopRecordBtn.setVisibility(View.INVISIBLE);
                beginRecordBtn.setVisibility(View.VISIBLE);
                // TODO: 读取文件，分析结果
            }
        });


    }

    public void verifyAudioPermissions() {
        int permission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSION_AUDIO,
                    GET_RECODE_AUDIO);
        }
        permission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSION_STORAGE,
                    3);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void startRecord(String name) {
        File file = new File(name);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            OutputStream os = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);

            bufferSize = AudioRecord.getMinBufferSize(sampleRate, channel, audioEncoding);
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channel, audioEncoding, bufferSize);
            byte[] buffer = new byte[bufferSize];
            audioRecord.startRecording();
            recording = true;
            while(recording) {
                int bufferReadSize = audioRecord.read(buffer, 0, bufferSize);
                for(int i = 0; i < bufferReadSize; ++i) {
                    dos.write(buffer[i]);
                }
            }
            audioRecord.stop();
            dos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void copyWaveFile(String inFileName, String outFileName) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        //wav文件比原始数据文件多出了44个字节，除去表头和文件大小的8个字节剩余文件长度比原始数据多36个字节
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = sampleRate;
        int channels = 2;
        //每分钟录到的数据的字节数
        long byteRate = 16 * sampleRate * channels / 8;

        byte[] data = new byte[bufferSize];
        try
        {
            File f = new File(outFileName);
            if (f.exists())
                f.delete();
            f.createNewFile();
            in = new FileInputStream(inFileName);
            out = new FileOutputStream(outFileName);
            //获取真实的原始数据长度
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            //为wav文件写文件头
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
            //把原始数据写入到wav文件中。
            while(in.read(data) != -1)
            {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // WAV type format = 1
        header[21] = 0;
        header[22] = (byte) channels; //指示是单声道还是双声道
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff); //采样频率
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff); //每分钟录到的字节数
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff); //真实数据的长度
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        //把header写入wav文件
        out.write(header, 0, 44);
    }
}