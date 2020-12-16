package com.example.bluetoothreceiver;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InternetUtils  {
    public String serverAddr;
    public int contorlPort = 50000;
    public int dataPort = 50001;
    private Socket controlSocket;
    private Socket dataSocket;
    // Socket 对应的输入流    
    InputStream controlInStream = null;
    OutputStream controlOutStream = null;
    InputStream dataInStream = null;
    OutputStream dataOutStream = null;
    public Handler dataHandler;
    public Handler controlHandler;
    int dataSize = -1;
    int bufferSize = 2048;

    public static final int STRING_TO_BIN_RESULT = 0x123;
    public static final int BIN_TO_STRING_RESULT = 0x124;
    public static final int FSK_MOD_RESULT = 0x125;
    public static final int FSK_DEMOD_RESULT = 0X126;
    public static final String CMD_STRING_TO_BIN = "STRING_TO_BIN";
    public static final String CMD_BIN_TO_STRING = "BIN_TO_STRING";
    public static final String CMD_STRING_TO_BIN_RESULT = "STRING_TO_BIN_RESULT";
    public static final String CMD_BIN_TO_STRING_RESULT = "BIN_TO_STRING_RESULT";
    public static final String CMD_FSKMOD = "FSKMOD";
    public static final String CMD_FSKDEMOD = "FSKDEMOD";
    public static final String CMD_FSKMOD_RESULT = "FSKMOD_RESULT";
    public static final String CMD_FSKDEMOD_RESULT = "FSKDEMOD_RESULT";
    public static final String CMD_PARSE_WAV = "PARSE_WAV";
    public static final String CMD_PARSE_WAV_RESULT = "PARSE_WAV_RESULT";
    public static final String CMD_QUIT = "QUIT";

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public void init() {
        if (serverAddr == null) {
            // 如果没有设置好服务器地址或端口，退出
            return;
        }
        try{
            controlSocket = new Socket(serverAddr, contorlPort);
            dataSocket = new Socket(serverAddr, dataPort);
            controlOutStream = controlSocket.getOutputStream();
            controlInStream = controlSocket.getInputStream();
            dataOutStream = dataSocket.getOutputStream();
            dataInStream = dataSocket.getInputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String readCommand() throws IOException {
        byte[] bytes = new byte[bufferSize];
        int len;
        StringBuilder controlSB = new StringBuilder();
        while((len = controlInStream.read(bytes)) != -1) {
            controlSB.append(new String(bytes, 0, len, "UTF-8"));
            if (controlSB.charAt(controlSB.length() - 1) == '\n')
                break;
        }
        Pattern p = Pattern.compile("([A-Z_]+)\\sdataSize:([0-9]+)");
        Matcher matcher = p.matcher(controlSB.toString());
        matcher.find();
        dataSize = Integer.valueOf(matcher.group(2));
        return matcher.group(1);
    }

    public byte[] readData() throws IOException {
        byte[] bytes = new byte[bufferSize];
        int len;
        int totalLen = 0;
        byte[] result = new byte[dataSize];
        while(totalLen < dataSize && (len = dataInStream.read(bytes)) != -1) {
            for (int i = 0; i < len; ++i) {
                result[totalLen + i] = bytes[i];
            }
            totalLen += len;
        }
        return result;
    }

    public void sendCommand(String cmd, int dataSize) throws IOException {
        controlOutStream.write((cmd+" dataSize:" + dataSize +"\n").getBytes());
        controlOutStream.flush();
    }

    public void sendData(byte[] data) throws IOException {
        dataOutStream.write(data);
        dataOutStream.flush();
    }
}
