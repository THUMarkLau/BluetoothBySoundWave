package socket.test;

import MatlabUtils.MatlabUtils;
import com.mathworks.toolbox.javabuilder.MWCharArray;
import com.mathworks.toolbox.javabuilder.MWClassID;
import com.mathworks.toolbox.javabuilder.MWNumericArray;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SignalProcessor implements Runnable{
    Socket controlSocket;
    Socket dataSocket;
    InputStream controlInStream;
    OutputStream controlOutStream;
    InputStream dataInStream;
    OutputStream dataOutStream;
    MatlabUtils processor;
    int dataSize = -1;

    public static final String STRING_TO_BIN = "STRING_TO_BIN";
    public static final String BIN_TO_STRING = "BIN_TO_STRING";
    public static final String STRING_TO_BIN_RESULT = "STRING_TO_BIN_RESULT";
    public static final String BIN_TO_STRING_RESULT = "BIN_TO_STRING_RESULT";
    public static final String FSKMOD = "FSKMOD";
    public static final String FSKDEMOD = "FSKDEMOD";
    public static final String FSKMOD_RESULT = "FSKMOD_RESULT";
    public static final String FSKDEMOD_RESULT = "FSKDEMOD_RESULT";
    public static final String QUIT = "QUIT";


    public SignalProcessor(Socket control, Socket data) {
        controlSocket = control;
        dataSocket = data;
        try {
            controlInStream = controlSocket.getInputStream();
            controlOutStream = controlSocket.getOutputStream();
            dataInStream = dataSocket.getInputStream();
            dataOutStream = dataSocket.getOutputStream();
            processor = new MatlabUtils();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        byte[] dataBytes = new byte[2048];
        String command;
        boolean quit = false;
        Thread curThread = Thread.currentThread();
        byte[] demodData = null;
        try {
            while (!quit) {
                Thread.sleep(100);
                command = readCommand();
                System.out.println(curThread.getName() + ": " + command);
                switch (command) {
                    case STRING_TO_BIN: {
                        byte[] data = readData();
                        String srcStr = new String(data, 0, data.length, "UTF-8");
                        byte[] binData = stringToBin(srcStr);
                        sendData(STRING_TO_BIN_RESULT, binData);
                        break;
                    }
                    case BIN_TO_STRING: {
                        byte[] data = readData();
                        String str = binToString(data);
                        sendData(BIN_TO_STRING_RESULT, str.getBytes());
                        break;
                    }
                    case FSKMOD: {
                        byte[] data = readData();
                        double[] modRes = fskMod(data);
                        sendData(FSKMOD_RESULT, DataTransformer.doubleToByte(modRes));
                        break;
                    }
                    case FSKDEMOD: {
                        byte[] data = readData();
                        double[] doubleData = DataTransformer.byteToDouble(data);
                        demodData = fskDemod(doubleData);
                        sendData(FSKDEMOD_RESULT, demodData);
                        break;
                    }
                    case QUIT: {
                        quit = true;
                        break;
                    }
                    default: {
                        System.out.println("Unrecognized command: " + command);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            for(int i = 0; i < demodData.length; ++i)
                System.out.print(demodData[i] + " ");
            System.out.println();
            e.printStackTrace();
        } finally {
            try {
                controlInStream.close();
                controlOutStream.close();
                dataInStream.close();
                dataOutStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    String readCommand() throws IOException {
        System.out.println("Reading command...");
        byte[] controlBytes = new byte[2048];
        int len;
        StringBuilder sb = new StringBuilder();
        while ((len = controlInStream.read(controlBytes)) != -1) {
            // 读取传输指令
            sb.append(new String(controlBytes, 0, len, "UTF-8"));
            if (sb.charAt(sb.length() - 1) == '\n')
                break;
        }
        sb.setLength(sb.length() - 1);
        Pattern p = Pattern.compile("([A-Z_]+)\\sdataSize:([0-9]+)");
        Matcher m = p.matcher(sb.toString());
        m.find();
        dataSize = Integer.valueOf(m.group(2));
        return m.group(1);
    }

    byte[] readData() throws IOException {
        byte[] bytes = new byte[dataSize];
        byte[] dataBytes = new byte[2048];
        int len;
        int totalLen = 0;
        while(totalLen < dataSize && (len = dataInStream.read(dataBytes)) != -1) {
            for(int i = 0; i < len; ++i)
                bytes[totalLen + i] = dataBytes[i];
            totalLen += len;
        }
        return bytes;
    }

    boolean sendData(String cmd, byte[] data) {
        try {
            controlOutStream.write((cmd + " dataSize:" + data.length + "\n").getBytes());
            controlOutStream.flush();
            dataOutStream = dataSocket.getOutputStream();
            dataOutStream.write(data);
            dataOutStream.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    double[] fskMod(byte[] data) {
        try{
            MWNumericArray dataArray = new MWNumericArray(data, MWClassID.INT8);
            MWNumericArray result = (MWNumericArray) processor.fskmod(1, dataArray)[0];
            return result.getDoubleData();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    byte[] fskDemod(double[] data) {
        try{
            MWNumericArray dataArray = new MWNumericArray(data, MWClassID.DOUBLE);
            MWNumericArray result = (MWNumericArray) processor.fskdemod(1, dataArray)[0];
            int[] intRes = result.getIntData();
            byte[] byteRes = new byte[intRes.length];
            for(int i = 0; i < intRes.length; ++i)
                byteRes[i] = (byte)intRes[i];
            return byteRes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    byte[] stringToBin(String str) {
        MWCharArray srcStr = new MWCharArray(str);
        try {
            Object[] result = processor.string2bin(1, srcStr);
            MWNumericArray numericArray = (MWNumericArray) result[0];
            return numericArray.getByteData();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    String binToString(byte[] srcData) {
        try{
            Object[] result = processor.bin2string(1, srcData);
            MWCharArray charArray = (MWCharArray) result[0];
            return charArray.toString();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
