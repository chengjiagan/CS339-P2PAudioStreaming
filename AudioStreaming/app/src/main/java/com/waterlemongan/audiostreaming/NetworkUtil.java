package com.waterlemongan.audiostreaming;

import android.provider.ContactsContract;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

class NetworkUtil {
    private BroadcastReceiveThread broadcastReceiver = null;
    private MessageReceiveThread messageReceiver = null;

    public static final int broadcastPort = 40011;
    public static final int controlPort = 40012;

    public static void sendBroadcastMessage(String msg) {
        new BroadcastThread(msg).start();
    }

    public static void sendMessage(InetAddress address, String msg) {
        new MessageSendThread(address, msg).start();
    }

    public void receiveBroadcastMessage(MessageListener listener) {
        if (broadcastReceiver != null) {
            broadcastReceiver.setRunning(false);
        }
        broadcastReceiver = new BroadcastReceiveThread(listener);
        broadcastReceiver.start();
        log("start udp broadcast listening");
    }

    public void stopReceiveBroadcast() {
        if (broadcastReceiver != null) {
            broadcastReceiver.interrupt();
            broadcastReceiver.close();
            broadcastReceiver = null;
        }
    }

    public void receiveMessage(MessageListener listener) {
        if (messageReceiver != null) {
            messageReceiver.setRunning(false);
        }
        messageReceiver = new MessageReceiveThread(listener);
        messageReceiver.start();
        log("start tcp listening");
    }

    public void stopReceiveMessage() {
        if (messageReceiver != null) {
            messageReceiver.interrupt();
            messageReceiver.close();
            messageReceiver = null;
        }
    }

    public void stop() {
        stopReceiveBroadcast();
        stopReceiveMessage();
    }

    public interface MessageListener {
        void onReceiveMessage(InetAddress address, String msg);
    }

    private static void log(String logMsg) {
        Log.d("NetworkUtil", logMsg);
    }

    private static class BroadcastThread extends Thread {
        private String msg;

        BroadcastThread(String msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            Enumeration e;
            try {
                e = NetworkInterface.getNetworkInterfaces();
            } catch (SocketException ex) {
                log("get network interface failed");
                ex.printStackTrace();
                return;
            }
            DatagramSocket s;
            try {
                s = new DatagramSocket();
                s.setBroadcast(true);
            } catch (SocketException ex) {
                log("create udp socket failed");
                ex.printStackTrace();
                return;
            }

            byte[] buf = msg.getBytes();
            while(e.hasMoreElements())
            {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                while (ee.hasMoreElements())
                {
                    InetAddress i = (InetAddress) ee.nextElement();
                    if (!i.isLoopbackAddress()) {
                        byte[] b = i.getAddress();
                        b[3] = (byte) 0xFF;
                        try {
                            i = InetAddress.getByAddress(b);
                            log(i.getHostAddress());
                        } catch (UnknownHostException ex) {
                            log("unknown host address");
                            ex.printStackTrace();
                            return;
                        }

                        DatagramPacket p = new DatagramPacket(buf, buf.length, i, broadcastPort);
                        try {
                            s.send(p);
                        } catch (IOException ex) {
                            log("send upd packet failed");
//                        ex.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private static class BroadcastReceiveThread extends Thread {
        private MessageListener listener;
        private boolean running;
        private DatagramSocket s = null;

        BroadcastReceiveThread(MessageListener listener) {
            this.running = true;
            this.listener = listener;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        public void close() {
            if (s != null) {
                log("close broadcast socket");
                s.close();
                running = false;
                s = null;
            }
        }

        @Override
        public void run() {
            try {
                s = new DatagramSocket(broadcastPort);
            } catch (SocketException e) {
                log("create listening udp socket failed");
                e.printStackTrace();
                return;
            }

            byte[] buf = new byte[6];
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            while (running) {
                try {
                    s.receive(p);
                    if (p.getAddress().isLoopbackAddress()) {
                        continue;
                    }

                    listener.onReceiveMessage(p.getAddress(), new String(p.getData()));
                } catch (IOException e) {
                    log("receive udp packet failed");
                    e.printStackTrace();
                }
            }
        }
    }

    private static class MessageReceiveThread extends Thread {
        private MessageListener listener;
        private boolean running;
        private ServerSocket ss;

        MessageReceiveThread(MessageListener listener) {
            this.running = true;
            this.listener = listener;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        public void close() {
            if (ss != null) {
                try {
                    log("close server socket");
                    ss.close();
                } catch (IOException e) {
                    log("close server socket failed");
                    e.printStackTrace();
                }
                running = false;
                ss = null;
            }
        }

        @Override
        public void run() {
            try {
                ss = new ServerSocket(controlPort);
            } catch (IOException e) {
                log("create server socket failed");
                e.printStackTrace();
                return;
            }

            while (running) {
                Socket s = null;
                try {
                    s = ss.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    String msg = reader.readLine();
                    listener.onReceiveMessage(s.getInetAddress(), msg);
                    reader.close();
                } catch (IOException e) {
                    log("receive message failed");
                    e.printStackTrace();
                } finally {
                    if (s != null) {
                        try {
                            s.close();
                            s = null;
                        } catch (IOException ex) {
                            log("close socket failed");
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private static class MessageSendThread extends Thread {
        private InetAddress address;
        private String msg;

        MessageSendThread(InetAddress address, String msg) {
            this.address = address;
            this.msg = msg;
        }

        @Override
        public void run() {
            Socket s = null;
            try {
                s =  new Socket(address, controlPort);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                writer.write(msg);
                writer.close();
            } catch (IOException e) {
                log("send message failed");
                e.printStackTrace();

            } finally {
                if (s != null) {
                    try {
                        s.close();
                    } catch (IOException ex) {
                        log("close socket failed");
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}
