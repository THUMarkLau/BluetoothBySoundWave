package socket.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client {
    /*
     * Client 和 Server 的通信使用两个Socket，一个负责命令的传输，一个负责数据的传输
     * 指令具体的内容可以使用 SignalProcessor 中的静态 String
     * 传输的数据应该转换成 byte 数组之后再传输，DataTransformer 中提供了将 byte 数组
     * 和 double 数组相互转换的静态方法
     */
    static String data = "This is a test";
    static int bufferSize = 2048;
    static InputStream dataInStream;
    static InputStream controlInStream;
    static OutputStream dataOutStream;
    static OutputStream controlOutStream;
    public static void main(String[] args) throws IOException, InterruptedException {
        String data = "This is a Test";

        // Socket 设置
        String host = "183.172.126.142";
        Socket controlSocket = new Socket(host, Server.controlPort);
        Socket dataSocket = new Socket(host, Server.dataPort);
        controlOutStream = controlSocket.getOutputStream();
        controlInStream = controlSocket.getInputStream();
        dataOutStream = dataSocket.getOutputStream();
        dataInStream = dataSocket.getInputStream();

        // 发送指令，调用 STRING TO BIN 函数
        sendCommand(SignalProcessor.STRING_TO_BIN);
        // 发送数据
        sendData(data.getBytes());

        // 读取结果
        String resultCmd = readCommand();
        byte[] binStr = null;
        if (resultCmd.equals(SignalProcessor.STRING_TO_BIN_RESULT)) {
            binStr = readData();
            System.out.println("The binary result of \"" + data + "\" is");
            for(int i = 0; i < binStr.length; ++i) {
                System.out.print(binStr[i] + " ");
            }
            System.out.println();
        }

        // 调制
        sendCommand(SignalProcessor.FSKMOD);
        sendData(binStr);

        // 获取调制结果
        resultCmd = readCommand();
        double[] doubleData = null;
        if (resultCmd.equals(SignalProcessor.FSKMOD_RESULT)) {
            byte[] fskBin = readData();
            doubleData = DataTransformer.byteToDouble(fskBin);
            System.out.println("The fsk mod result is ");
//            for(int i = 0; i < doubleData.length; ++i)
//                System.out.print(doubleData[i] + " ");
            System.out.println();
        }
        Thread.sleep(500);
        // 解调制
        sendCommand(SignalProcessor.FSKDEMOD);
        sendData(DataTransformer.doubleToByte(doubleData));

        // 获取解调制结果
        resultCmd = readCommand();
        byte[] fskDemodBin = null;
        if (resultCmd.equals(SignalProcessor.FSKDEMOD_RESULT)) {
            fskDemodBin = readData();
            System.out.println("FSK demodulation result is:");
            for(int i = 0; i < fskDemodBin.length; ++i)
                System.out.print(fskDemodBin[i] + " ");
            System.out.println();
        }

        // 还原
        sendCommand(SignalProcessor.BIN_TO_STRING);
        sendData(fskDemodBin);
        // 读取结果
        resultCmd = readCommand();
        byte[] strByte = readData();
        String resStr = new String(strByte, 0, strByte.length, "UTF-8");
        System.out.println("The recovering result is \"" + resStr + "\"");
        controlOutStream.write(SignalProcessor.QUIT.getBytes());
    }

    static String readCommand() throws IOException {
        byte[] bytes = new byte[bufferSize];
        int len;
        StringBuilder controlSB = new StringBuilder();
        while((len = controlInStream.read(bytes)) != -1) {
            controlSB.append(new String(bytes, 0, len, "UTF-8"));
            if (len != bufferSize)
                break;
        }
        return controlSB.toString();
    }

    static byte[] readData() throws IOException {
        byte[] bytes = new byte[bufferSize];
        int len;
        List<Byte> byteList = new ArrayList<>();
        while((len = dataInStream.read(bytes)) != -1) {
            for (int i = 0; i < len; ++i) {
                byteList.add(bytes[i]);
            }
            if (len != bufferSize)
                break;
        }
        byte[] result = new byte[byteList.size()];
        for(int i = 0; i < byteList.size(); ++i)
            result[i] = byteList.get(i);
        return result;
    }

    static void sendCommand(String cmd) throws IOException {
        controlOutStream.write(cmd.getBytes());
        controlOutStream.flush();
    }

    static void sendData(byte[] data) throws IOException {
        dataOutStream.write(data);
        dataOutStream.flush();
    }
}
