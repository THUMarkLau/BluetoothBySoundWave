package socket.test;

public class DataTransformer {
    public static byte[] doubleToByte(double[] data) {
        if (data == null) {
            return null;
        }
        byte[] result = new byte[data.length * 8];
        for(int i = 0; i < data.length; ++i) {
            long value = Double.doubleToRawLongBits(data[i]);
            for(int j = 0; j < 8; ++j) {
                result[i * 8 + j] = (byte)((value >> 8*j) & 0xff);
            }
        }
        return result;
    }

    public static double[] byteToDouble(byte[] data) {
        if (data == null)
            return null;
        double[] result = new double[data.length / 8];
        for(int i = 0; i < data.length / 8; ++i) {
            long value = 0;
            for(int j = 0; j < 8; ++j) {
                value |= ((long)(data[i * 8 + j] & 0xff)) << (8 * j);
            }
            result[i] = Double.longBitsToDouble(value);
        }
        return result;
    }

    public static double[] byteToDouble(Byte[] data) {
        if (data == null)
            return null;
        double[] result = new double[data.length / 8];
        for(int i = 0; i < data.length / 8; ++i) {
            long value = 0;
            for(int j = 0; j < 8; ++j) {
                value |= ((long)(data[i * 8 + j] & 0xff)) << (8 * j);
            }
            result[i] = Double.longBitsToDouble(value);
        }
        return result;
    }
}
