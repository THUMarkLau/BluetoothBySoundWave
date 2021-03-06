package socket.test;

import MatlabUtils.MatlabUtils;
import com.mathworks.toolbox.javabuilder.MWCharArray;
import com.mathworks.toolbox.javabuilder.MWClassID;
import com.mathworks.toolbox.javabuilder.MWNumericArray;

import java.io.*;
import java.net.Socket;
import java.util.Date;
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
    public static final String PARSE_WAV = "PARSE_WAV";
    public static final String PARSE_WAV_RESULT = "PARSE_WAV_RESULT";
    public static final String FSKMOD = "FSKMOD";
    public static final String FSKDEMOD = "FSKDEMOD";
    public static final String FSKMOD_RESULT = "FSKMOD_RESULT";
    public static final String FSKDEMOD_RESULT = "FSKDEMOD_RESULT";
    public static final String MAKE_WAV = "MAKE_WAV";
    public static final String MAKE_WAV_RESULT = "MAKE_WAV_RESULT";
    public static final String QUIT = "QUIT";
    static final String CACHE_DIR = "./cache/";


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
        String command;
        boolean quit = false;
        Thread curThread = Thread.currentThread();
        byte[] demodData = null;
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
                    case PARSE_WAV: {
                        byte[] data = readData();
                        String filename = writeWav(data);
                        double[] wavContent = parseWav(filename);
                        demodData = fskDemod(wavContent);
                        sendData(PARSE_WAV_RESULT, demodData);
                        break;
                    }
                    case MAKE_WAV: {
                        byte[] data = readData();
                        byte[] wavData = makeWav(data);
                        sendData(MAKE_WAV_RESULT, wavData);
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

    public void sendCommand(String cmd, int dataSize) throws IOException {
        controlOutStream.write((cmd+" dataSize:" + dataSize +"\n").getBytes());
        controlOutStream.flush();
    }

    double[] fskMod(byte[] data) {
        try{
            int n = data.length;
            byte[] newData = new byte[8 + data.length];
            byte[] numByte = encodeNumber(n);
            for(int i = 0; i < 8; ++i)
                newData[i] = numByte[i];
            for(int i = 0; i < data.length; ++i)
                newData[8+i] = data[i];
            MWNumericArray dataArray = new MWNumericArray(newData, MWClassID.INT8);
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
            int cnt = srcData.length / 8;
            byte[] nByte = new byte[cnt * 8];
            for(int i = 0; i < cnt * 8; ++i)
                nByte[i] = srcData[i];
            Object[] result = processor.bin2string(1, new MWNumericArray(nByte, MWClassID.DOUBLE));
            MWCharArray charArray = (MWCharArray) result[0];
            return charArray.toString();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    double[] parseWav(String filename) {
        try {
            Object[] tmp = processor.readWav(2, new MWCharArray(CACHE_DIR + filename));
            MWNumericArray _wavData = (MWNumericArray) tmp[0];
            double[] wavData = _wavData.getDoubleData();
            return wavData;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    String writeWav(byte[] data) {
        File cacheDir = new File(CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        try{
            String curTime = String.valueOf(new Date().getTime());
            String filename = curTime + Thread.currentThread().getName() +"_record.wav";
            FileOutputStream fileOutputStream = new FileOutputStream(CACHE_DIR + filename);
            fileOutputStream.write(data);
            return filename;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    byte[] makeWav(byte[] data) {
        try {
            String curTime = String.valueOf(new Date().getTime());
            String filename = curTime + Thread.currentThread().getName() + "_record.wav";
            processor.makeWav( new MWCharArray(CACHE_DIR + filename), new MWNumericArray(DataTransformer.byteToDouble(data), MWClassID.DOUBLE));
            FileInputStream fileInputStream = new FileInputStream(new File(CACHE_DIR + filename));
            long size = fileInputStream.getChannel().size();
            byte[] result = new byte[(int)size];
            byte[] bytes = new byte[4096];
            int len, totalLen = 0;
            while((len = fileInputStream.read(bytes)) != -1) {
                for(int i = 0; i < len; ++i)
                    result[totalLen + i] = bytes[i];
                totalLen += len;
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    byte[] encodeNumber(int n) {
        byte[] res = new byte[16];
        for(int i = 0; i < 16; i ++) {
            res[i] = Byte.valueOf(String.valueOf(n / (1 << (15-i))));
            n = n % (1<<(15-i));
        }
        return res;
    }
}
