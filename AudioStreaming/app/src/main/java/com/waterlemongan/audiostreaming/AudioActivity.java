package com.waterlemongan.audiostreaming;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.waterlemongan.audiostreaming.utils.AudioUtils;

import java.io.File;
import java.net.InetAddress;
import java.util.List;

public class AudioActivity extends AppCompatActivity {
    private InetAddress serverAddr;
    private ActivityHandler handler;
    private ListView listView;
    private Client client = null;
    private Server server = null;
    private boolean isServer = false;
    private boolean isPlay = false;

    private AudioUtils audioUtils;

    public static final String TAG = "AudioActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new ActivityHandler(this);

        client = new Client();
        server = new Server();
        startClientNoServer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopClient();
        stopServer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_audio, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_switch:
                isServer = !isServer;
                if (isServer) {
                    Log.d(TAG, "server");
                    item.setTitle("Server");
                    stopClient();
                    startServer();
                } else {
                    Log.d(TAG, "client");
                    item.setTitle("Client");
                    stopServer();
                    startClientNoServer();
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void stopClient() {
        client.stop();
    }

    private void startClient(InetAddress address) {
        setContentView(R.layout.activity_audio_client);

        client.start(address, new Client.EventListener() {
            @Override
            public void onServerDisconnect() {
                stopClient();
                sendMessage("toast\nServer disconnect: " + client.getServerName());
                sendMessage("startClientNoServer");
            }

            @Override
            public void onPlayOk() {
                clientPlayMusic(client.getServerAddr());
            }
        });

        TextView deviceAddrText = findViewById(R.id.deviceAddr);
        TextView deviceNameText = findViewById(R.id.deviceName);
        deviceAddrText.setText(client.getServerAddress());
//        deviceNameText.setText(client.getServerName());

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button) v;
                isPlay = !isPlay;
                if (isPlay) {
                    button.setText("stop");
                    client.sendPlay();
                } else {
                    button.setText("play");
                    clientStopMusic();
                }
            }
        });
    }

    private void clientPlayMusic(InetAddress address) {

        audioUtils = new AudioUtils();
        File file = new File(Environment.getExternalStorageDirectory(), "Secret_Base.mp3");
        audioUtils.startClient(10050, 10051, address.getHostAddress(), false, file.getPath());
    }

    private void clientStopMusic() {
        client.sendStop();
        audioUtils.stopClient();
    }

    private void startClientNoServer() {
        serverAddr = null;
        setContentView(R.layout.activity_audio);

        client.searchServer(new Client.SearchListener() {
            @Override
            public void onServerNotFound() {
                sendMessage("toast\nServer not found");
            }

            @Override
            public void onServerFound(InetAddress address) {
                serverAddr = address;
                Log.d(TAG, "connect server: " + address.getHostAddress());
                sendMessage("startClient");
            }
        });
    }

    private void startServer() {
        setContentView(R.layout.activity_audio_server);

        server.start(new Server.EventListener() {
            @Override
            public void onNewDevice(InetAddress address) {
                Log.d(TAG, "device connect: " + address);
                sendMessage("deviceListChange");
            }

            @Override
            public void onDeviceDisconnect(InetAddress address) {
                Log.d(TAG, "device disconnect: " + address);
                sendMessage("deviceListChange");
            }

            @Override
            public void onPlay(InetAddress address) {
                Log.d(TAG, address.getHostAddress() + " say play");
                sendMessage("deviceListChange");
                serverPlayMusic(address);
            }

            @Override
            public void onStop(InetAddress address) {
                Log.d(TAG, address.getHostAddress() + " say stop");
                sendMessage("deviceListChange");
                serverStopMusic(address);
            }
        });

        setContentView(R.layout.activity_audio_server);
        listView = findViewById(R.id.audioListView);
        listView.setAdapter(new AudioDeviceListAdapter(
                this, R.layout.row_device, server.getClientList()));
    }

    private void stopServer() {
        server.stop();
    }

    private void serverStopMusic(InetAddress address) {
        Log.d(TAG, "stop music");
        //TODO
    }

    private void serverPlayMusic(InetAddress address) {
        Log.d(TAG, "receive music");
        //TODO
    }

    private void sendMessage(String msg) {
        Message message = Message.obtain();
        message.obj = msg;
        handler.sendMessage(message);
    }

    private void updateListView() {
        ((AudioDeviceListAdapter) listView.getAdapter()).notifyDataSetChanged();
    }

    private static class ActivityHandler extends Handler {
        private AudioActivity activity;

        ActivityHandler(AudioActivity activity) {
            this.activity = activity;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            String action = (String) msg.obj;
            if (action.equals("startClient")) {
                activity.startClient(activity.serverAddr);
            } else if (action.startsWith("toast")) {
                Toast.makeText(activity, action.split("\n")[1], Toast.LENGTH_LONG).show();
            } else if (action.equals("startClientNoServer")) {
                activity.startClientNoServer();
            } else if (action.equals("deviceListChange")) {
                activity.updateListView();
            }
        }
    }

    private class AudioDeviceListAdapter extends ArrayAdapter<Client> {

        private List<Client> items;

        public AudioDeviceListAdapter(Context context, int textViewResourceId,
                                      List<Client> objects) {
            super(context, textViewResourceId, objects);
            items = objects;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_device, null);
            }
            InetAddress device = items.get(position).getSelfAddr();
            if (device != null) {
                TextView top = (TextView) v.findViewById(R.id.deviceName);
                TextView bottom = (TextView) v.findViewById(R.id.deviceAddress);

                if (top != null) {
                    top.setText("Device " + position);
                }
                if (bottom != null) {
                    bottom.setText(device.getHostAddress());
                }
            }
            return v;
        }
    }
}
