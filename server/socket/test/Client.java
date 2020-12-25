package socket.test;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
    /*
     * Client 和 Server 的通信使用两个Socket，一个负责命令的传输，一个负责数据的传输
     * 指令具体的内容可以使用 SignalProcessor 中的静态 String, 并且需要提供数据的大小作为参数
     * 传输的数据应该转换成 byte 数组之后再传输，DataTransformer 中提供了将 byte 数组
     * 和 double 数组相互转换的静态方法
     */
    static String data = "This is a test";
    static int bufferSize = 2048;
    static InputStream dataInStream;
    static InputStream controlInStream;
    static OutputStream dataOutStream;
    static OutputStream controlOutStream;
    static boolean fskdemod = false;
    static int dataSize = -1;
    public static void main(String[] args) throws IOException, InterruptedException {
        String data = "This is a Test";

        // Socket 设置
        String host = "183.172.124.61";
        Socket controlSocket = new Socket(host, Server.controlPort);
        Socket dataSocket = new Socket(host, Server.dataPort);
        controlOutStream = controlSocket.getOutputStream();
        controlInStream = controlSocket.getInputStream();
        dataOutStream = dataSocket.getOutputStream();
        dataInStream = dataSocket.getInputStream();

        // 发送指令，调用 STRING TO BIN 函数
        sendCommand(SignalProcessor.STRING_TO_BIN, data.getBytes().length);
        sendData(data.getBytes());

        String cmd = readCommand();
        byte[] binStr = readData();

        sendCommand(SignalProcessor.FSKMOD, binStr.length);
        sendData(binStr);

        cmd = readCommand();
        byte[] fskData = readData();

        sendCommand(SignalProcessor.MAKE_WAV, fskData.length);
        sendData(fskData);

        cmd = readCommand();
        byte[] __ = readData();

        sendCommand(SignalProcessor.QUIT, 0);
        Thread.sleep(1000);
        controlOutStream.close();
        controlInStream.close();

    }

    static String readCommand() throws IOException {
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

    static byte[] readData() throws IOException {
        byte[] bytes = new byte[bufferSize];
        int len;
        int totalLen = 0;
        byte[] result = new byte[dataSize];
        while(totalLen < dataSize && (len = dataInStream.read(bytes)) != -1) {
            for (int i = 0; i < len; ++i) {
                result[totalLen + i] = bytes[i];
//                if (fskdemod)
//                    System.out.print(bytes[i] + " ");
            }
//            if (fskdemod)
//                System.out.println();
            totalLen += len;
        }
        return result;
    }

    static void sendCommand(String cmd, int dataSize) throws IOException {
        controlOutStream.write((cmd+" dataSize:" + dataSize +"\n").getBytes());
        controlOutStream.flush();
    }

    static void sendData(byte[] data) throws IOException {
        dataOutStream.write(data);
        dataOutStream.flush();
    }

}
