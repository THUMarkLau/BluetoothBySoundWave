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


public class SignalProcessor implements Runnable{
    Socket controlSocket;
    Socket dataSocket;
    InputStream controlInStream;
    OutputStream controlOutStream;
    InputStream dataInStream;
    OutputStream dataOutStream;
    MatlabUtils processor;

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
        try {
            while (!quit) {
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
                        String srcStr = new String(data, 0, data.length, "UTF-8");
                        double[] modRes = fskMod(stringToBin(srcStr));
                        sendData(FSKMOD_RESULT, DataTransformer.doubleToByte(modRes));
                        break;
                    }
                    case FSKDEMOD: {
                        byte[] data = readData();
                        double[] doubleData = DataTransformer.byteToDouble(data);
                        byte[] demodData = fskDemod(doubleData);
                        sendData(FSKDEMOD_RESULT, binToString(demodData).getBytes());
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
        byte[] controlBytes = new byte[2048];
        int len;
        StringBuilder sb = new StringBuilder();
        while ((len = controlInStream.read(controlBytes)) != -1) {
            // 读取传输指令
            sb.append(new String(controlBytes, 0, len, "UTF-8"));
            if (len != 2048)
                break;
        }
        return sb.toString();
    }

    byte[] readData() throws IOException {
        List<Byte> bytes = new ArrayList<>();
        byte[] dataBytes = new byte[2048];
        int len;
        while((len = dataInStream.read(dataBytes)) != -1) {
            for(int i = 0; i < len; ++i)
                bytes.add(dataBytes[i]);
            if (len != 2048)
                break;
        }
        byte[] result = new byte[bytes.size()];
        for(int i = 0; i < bytes.size(); ++i)
            result[i] = bytes.get(i);
        return result;
    }

    boolean sendData(String cmd, byte[] data) {
        try {
            controlOutStream.write(cmd.getBytes());
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
            return result.getByteData();
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
