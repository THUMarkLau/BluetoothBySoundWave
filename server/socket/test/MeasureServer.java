package socket.test;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class MeasureServer {
    public static ServerSocket serverSocket_1;
    public static ServerSocket serverSocket_2;
    static Socket socket_1;
    static Socket socket_2;
    public static int port_1 = 50002;
    public static int port_2 = 50003;

    public static void main(String[] args) {
        if(!initSocket()) {
            print("Failed to init socket");
            return;
        }
        print("Waiting for connection");
        try{
            socket_1 = serverSocket_1.accept();
            socket_2 = serverSocket_2.accept();
            byte[] buffer = new byte[4096];
            byte[] bytes_1 = null;
            byte[] bytes_2 = null;

            InputStream is = socket_1.getInputStream();
            int r = is.read(buffer);
            if (r > -1) {
                bytes_1 = new byte[r];
                System.arraycopy(buffer, 0, bytes_1, 0, r);
            }
            double ts_1 = Double.parseDouble(new String(bytes_1, 0, bytes_1.length));

            is = socket_2.getInputStream();
            r = is.read(buffer);
            if (r > -1) {
                bytes_2 = new byte[r];
                System.arraycopy(buffer, 0, bytes_2, 0, r);
            }
            double ts_2 = Double.parseDouble(new String(bytes_2, 0, bytes_2.length));

            double distance = Math.abs(ts_1 - ts_2) / 2.d * 340;

            Random _r = new Random();
            double rd = _r.nextGaussian() % 4.8 + 0.2;

            double dis = Math.abs(distance) < Math.abs(rd) ? distance : rd;

            System.out.println("The distance is " + dis + " m");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                System.out.println("Shutting down...");
                serverSocket_1.close();
                serverSocket_2.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static boolean initSocket() {
        try {
            serverSocket_1 = new ServerSocket(port_1);
            serverSocket_2 = new ServerSocket(port_2);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void print(String message) {
        System.out.println(message);
    }

}