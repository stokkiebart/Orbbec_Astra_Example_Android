package com.example.michi.myapplication;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class UDPServer3Dcamera extends Thread {
    private static final String UDP_SERVER = "UDP_SERVER";
    private static UDPServer3Dcamera server = null;
    private final DatagramSocket socket;
    private boolean running;
    private final Context applicationContext;
    byte[] receiveBuf = new byte[256];
    private static final int PORT = 2100;

    public UDPServer3Dcamera(Context context) throws SocketException {
        applicationContext = context;
        socket = new DatagramSocket(PORT);
    }

    public static UDPServer3Dcamera getServer(Context applicationContext) throws SocketException {
        if (server == null) {
            server = new UDPServer3Dcamera(applicationContext);
        }
        return server;
    }

    public void run() {
        running = true;

        OrbbecCamAndroid orbbecCamAndroid = new OrbbecCamAndroid(applicationContext, 640, 480);

        try {
            Log.e(UDP_SERVER, "UDP Server is running with " + getIpAddress() + " and port " + PORT);

            while(running){
                DatagramPacket packet = new DatagramPacket(receiveBuf, receiveBuf.length);
                socket.receive(packet);

                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                Log.e(UDP_SERVER, "Request from: " + address + ":" + port + "\n");

                byte[] sendBuf = serialize(orbbecCamAndroid.get3DVectors());
                packet = new DatagramPacket(sendBuf, sendBuf.length, address, port);
                socket.send(packet);
                Log.e(UDP_SERVER, "Send response!");
            }

            Log.e(UDP_SERVER, "UDP Server ended");

        } catch (IOException e) {
            Log.e(UDP_SERVER, "UDP Server ended");
            e.printStackTrace();
        } finally {
            if(socket != null){
                socket.close();
            }
            Log.e(UDP_SERVER, "UDP Server ended");
        }
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    public void kill() {
        running = false;
        server = null;
    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
}

