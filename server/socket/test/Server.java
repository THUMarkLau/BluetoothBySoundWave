package socket.test;

import jdk.internal.util.xml.impl.Input;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class Server {
    public static ServerSocket controlSocket;
    public static ServerSocket dataSocket;
    public static int controlPort = 50000;
    public static int dataPort = 50001;

    public static void main(String[] args) {
        if(!initSocket()) {
            print("Failed to init socket");
            return;
        }
        print("Waiting for connection");
        try{
            while(true) {
                SignalProcessor processor = waitForConnection();
                if (processor != null) {
                    System.out.println("Get connection");
                    new Thread(processor).start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                System.out.println("Shutting down...");
                controlSocket.close();
                dataSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static boolean initSocket() {
        try {
            controlSocket = new ServerSocket(controlPort);
            dataSocket = new ServerSocket(dataPort);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void print(String message) {
        System.out.println(message);
    }

    static SignalProcessor waitForConnection() {
        try {
            Socket control = controlSocket.accept();
            Socket data = dataSocket.accept();
            SignalProcessor processor = new SignalProcessor(control, data);
            return processor;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
