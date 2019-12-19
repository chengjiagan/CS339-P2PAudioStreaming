package rtpuse;


import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

class Server {
    private NetworkUtil network;
    private List<Client> clientList;
    private EventListener listener;
    private Client.AliveListener aliveListener = new Client.AliveListener() {
        @Override
        public void timeout(InetAddress address) {
            for (Client c: clientList) {
                if (c.getSelfAddr().equals(address)) {
                    clientList.remove(c);
                }
            }
            listener.onDeviceDisconnect(address);
        }
    };

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
                    Client clientObj = new Client(address);
                    clientObj.checkAlive(aliveListener);
                    clientList.add(clientObj);
                    listener.onNewDevice(address);
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
                        index = indexOf(address);
                        if (index != -1) {
                            clientList.get(index).setPlaying(true);
                        }
                        NetworkUtil.sendMessage(address, "playOk");
                        break;
                    case "stop":
                        index = indexOf(address);
                        if (index != -1) {
                            clientList.get(index).setPlaying(false);
                        }
                        listener.onStop(address);
                        break;
                    case "server?":
                        index = indexOf(address);
                        if (index != -1) {
                            clientList.get(index).cancleTimer();
                            clientList.get(index).checkAlive(aliveListener);
                        }
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

    private int indexOf(InetAddress address) {
        for (int i = 0; i < clientList.size(); i++) {
            if (clientList.get(i).getSelfAddr().equals(address)) {
                return i;
            }
        }
        return -1;
    }

    private static void log(String logMsg) {
        System.out.println("Server"+logMsg);
    }

    public interface EventListener {
        void onNewDevice(InetAddress address);
        void onDeviceDisconnect(InetAddress address);
        void onPlay(InetAddress address);
        void onStop(InetAddress address);
    }
}
