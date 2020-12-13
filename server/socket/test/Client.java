package socket.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client {
    public static void main(String[] args) throws IOException {
        String data = "This is a Test";
        int bufferSize = 2048;

        String host = "183.172.126.142";
        Socket controlSocket = new Socket(host, Server.controlPort);
        Socket dataSocket = new Socket(host, Server.dataPort);
        OutputStream controlOutStream = controlSocket.getOutputStream();
        InputStream controlInStream = controlSocket.getInputStream();
        OutputStream dataOutStream = dataSocket.getOutputStream();
        InputStream dataInStream = dataSocket.getInputStream();

        // 发送指令，调用 STRING TO BIN 函数
        controlOutStream.write(SignalProcessor.STRING_TO_BIN .getBytes());
        controlOutStream.flush();
        // 发送数据
        dataOutStream.write(data.getBytes());
        dataOutStream.flush();

        // 读取结果
        List<Byte> byteList = new ArrayList<>();
        byte[] bytes = new byte[bufferSize];
        int len;
        StringBuilder controlSB = new StringBuilder();
        while((len = controlInStream.read(bytes)) != -1) {
            controlSB.append(new String(bytes, 0, len, "UTF-8"));
            if (len != bufferSize)
                break;
        }
        if (controlSB.toString().equals(SignalProcessor.STRING_TO_BIN_RESULT)) {
            while((len = dataInStream.read(bytes)) != -1) {
                for (int i = 0; i < len; ++i) {
                    byteList.add(bytes[i]);
                }
                if (len != bufferSize)
                    break;
            }
            System.out.println("The binary result of \"" + data + "\" is");
            for(int i = 0; i < byteList.size(); ++i) {
                System.out.print(byteList.get(i) + " ");
            }
            System.out.println();
        }

        // 还原
        byte[] dataToRec = new byte[byteList.size()];
        for(int i = 0; i < byteList.size(); ++i)
            dataToRec[i] = byteList.get(i);
        controlOutStream.write(SignalProcessor.BIN_TO_STRING.getBytes());
        dataOutStream.write(dataToRec);
        // 读取结果
        byteList.clear();
        controlSB.setLength(0);
        while((len = controlInStream.read(bytes)) != -1) {
            controlSB.append(new String(bytes, 0, len, "UTF-8"));
            if (len != bufferSize)
                break;
        }
        while((len = dataInStream.read(bytes)) != -1) {
            for(int i = 0; i < len; ++i)
                byteList.add(bytes[i]);
            if (len != bufferSize)
                break;
        }
        byte[] strByte = new byte[byteList.size()];
        for(int i = 0; i < byteList.size(); ++i)
            strByte[i] = byteList.get(i);
        String resStr = new String(strByte, 0, strByte.length, "UTF-8");
        System.out.println("The recovering result is \"" + resStr + "\"");
        controlOutStream.write(SignalProcessor.QUIT.getBytes());
    }
}
