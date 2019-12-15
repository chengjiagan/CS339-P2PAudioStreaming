package com.waterlemongan.audiostreaming;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

class Server {
    private NetworkUtil network;
    private List<InetAddress> clientList;

    public Server(final EventListener listener) {
        network = new NetworkUtil();
        clientList = new ArrayList<>();

        network.receiveBroadcastMessage(new NetworkUtil.MessageListener() {
            @Override
            public void onReceiveMessage(InetAddress address, String msg) {
                if (msg.equals("client")) {
                    NetworkUtil.sendMessage(address, "server");
                }
            }
        });

        network.receiveMessage(new NetworkUtil.MessageListener() {
            @Override
            public void onReceiveMessage(InetAddress address, String msg) {
                switch (msg) {
                    case "play":
                        clientList.add(address);
                        listener.onPlay(address);
                        NetworkUtil.sendMessage(address, "playOk");
                        break;
                    case "stop":
                        clientList.remove(address);
                        listener.onStop(address);
                        break;
                    case "server?":
                        NetworkUtil.sendMessage(address, "serverYes");
                        break;
                    default:
                        log("unknown message: " + msg);
                        break;
                }
            }
        });
    }

    public void stop() {
        network.stop();

        for (InetAddress clientAddr: clientList) {
            try {
                if (clientAddr.isReachable(500)) {
                    NetworkUtil.sendMessage(clientAddr, "notServe");
                }
            } catch (IOException e) {
                log("check reachable failed: " + clientAddr.getHostAddress());
                e.printStackTrace();
            }
        }
    }

    public List<InetAddress> getClientList() {
        return clientList;
    }

    private static void log(String logMsg) {
        Log.d("Server", logMsg);
    }

    public interface EventListener {
        void onPlay(InetAddress address);
        void onStop(InetAddress address);
    }
}
