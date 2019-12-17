package com.waterlemongan.audiostreaming;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

class Server {
    private NetworkUtil network;
    private List<Client> clientList;
    private EventListener listener;

    public Server() {
        network = new NetworkUtil();
        clientList = new ArrayList<>();
    }

    public void start(EventListener aListener) {
        listener = aListener;

        network.receiveBroadcastMessage(new NetworkUtil.MessageListener() {
            @Override
            public void onReceiveMessage(InetAddress address, String msg) {
                if (msg.equals("client")) {
                    log("new client: " + address.getHostAddress());
                    clientList.add(new Client(address));
                    NetworkUtil.sendMessage(address, "server");
                } else {
                    log("unknown message:" + msg);
                }
            }
        });

        network.receiveMessage(new NetworkUtil.MessageListener() {
            @Override
            public void onReceiveMessage(InetAddress address, String msg) {
                int index;
                switch (msg) {
                    case "play":
                        listener.onPlay(address);
                        index = clientList.indexOf(address);
                        clientList.get(index).setPlaying(true);
                        NetworkUtil.sendMessage(address, "playOk");
                        break;
                    case "stop":
                        index = clientList.indexOf(address);
                        clientList.get(index).setPlaying(true);
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

        for (Client clientObj: clientList) {
            InetAddress clientAddr = clientObj.getSelfAddr();
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

    public List<Client> getClientList() {
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
