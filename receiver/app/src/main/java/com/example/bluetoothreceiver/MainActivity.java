package com.example.bluetoothreceiver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.processbutton.FlatButton;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import cafe.adriel.androidaudiorecorder.AndroidAudioRecorder;

public class MainActivity extends AppCompatActivity {

    FlatButton beginRecordBtn;
    FlatButton stopRecordBtn;
    FlatButton parseBtn;
    FlatButton connectBtn;
    FlatButton lstBtn;
    EditText serverAddrEditText;
    TextView resultTv;
    boolean recording = false;
    String rawFilePath;
    String wavFilePath;
    String txtFilePath;
    int sampleRate = 48000;
    int channel = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    int bufferSize = 0;
    Handler dataHandler;
    Handler controlHandler;
    InternetUtils internetUtils;
    Listener listener;
    Player player;

    static final int SHOW_RESULT = 0X123;

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
        parseBtn = (FlatButton)findViewById(R.id.parse_btn);
        connectBtn = (FlatButton)findViewById(R.id.connect_btn);
        serverAddrEditText = (EditText)findViewById(R.id.server_addr);
        lstBtn = (FlatButton)findViewById(R.id.lst_btn);
        resultTv = (TextView)findViewById(R.id.result);
        rawFilePath = this.getExternalFilesDir(null).getAbsolutePath() + "/raw.wav";
        wavFilePath = this.getExternalFilesDir(null).getAbsolutePath() + "/result.wav";
        txtFilePath = this.getExternalFilesDir(null).getAbsolutePath() + "/res.txt";
        listener = new Listener(this);
        player = new Player(this);

        dataHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == SHOW_RESULT) {
                    String str = (String)msg.obj;
                    resultTv.setText(str);
                }
            }
        };

        controlHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case InternetUtils.STRING_TO_BIN_RESULT: {
                        System.out.println("Recieve cmd:" + (String)msg.obj);
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
        };


        beginRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beginRecordBtn.setVisibility(View.INVISIBLE);
                parseBtn.setVisibility(View.INVISIBLE);
                connectBtn.setVisibility(View.INVISIBLE);
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
                parseBtn.setVisibility(View.VISIBLE);
                connectBtn.setVisibility(View.VISIBLE);
                // TODO: 读取文件，分析结果
            }
        });
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String serverAddr = serverAddrEditText.getText().toString();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (internetUtils == null) {
                            internetUtils = new InternetUtils();
                            internetUtils.setServerAddr(serverAddr);
                            internetUtils.init();
                            internetUtils.dataHandler = dataHandler;
                            internetUtils.controlHandler = controlHandler;
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Connect Successfully", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).start();
            }
        });
        parseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runParse();
            }
        });
        lstBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startListen();
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

    public void runParse() {
        // 读取 wav 文件中的内容，发送到服务器中进行分析
        // final String serverAddr = serverAddrEditText.getText().toString();
        final String serverAddr = "183.172.121.15";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (internetUtils == null) {
                        return;
                    }
                    byte[] data = readData(wavFilePath);
                    internetUtils.sendCommand(InternetUtils.CMD_PARSE_WAV, data.length);
                    internetUtils.sendData(data);
                    String cmd = internetUtils.readCommand();
                    byte[] demodData = internetUtils.readData();
                    internetUtils.sendCommand(InternetUtils.CMD_BIN_TO_STRING, demodData.length);
                    internetUtils.sendData(demodData);
                    cmd = internetUtils.readCommand();
                    byte[] strData = internetUtils.readData();
                    String str = new String(strData, 0, strData.length, "UTF-8");

                    Message dataMsg = new Message();
                    dataMsg.what = SHOW_RESULT;
                    dataMsg.obj = str;
                    dataHandler.sendMessage(dataMsg);

                    internetUtils.sendCommand(InternetUtils.CMD_QUIT, 0);
                    internetUtils = null;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public byte[] readData(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            return null;
        }
        byte[] buffer = new byte[4096];
        byte[] result = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(filename);
            int len;
            long dataSize = fileInputStream.getChannel().size();
            int totalLen = 0;
            result = new byte[(int) dataSize];
            while ((len = fileInputStream.read(buffer)) != -1) {
                for(int i = 0; i < len; ++i) {
                    result[totalLen + i] = buffer[i];
                }
                totalLen += len;
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
        int channels = 1;
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

    public void showMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void startListen() {
        listener.start();
    }

    public void play() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000 * 3);
                    player.play();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}