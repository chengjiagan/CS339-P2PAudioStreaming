package com.waterlemongan.audiostreaming;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import java.net.InetAddress;
import java.util.List;

public class AudioActivity extends AppCompatActivity {
    private InetAddress serverAddr;
    private ActivityHandler handler;
    private ListView listView;
    private Client client = null;
    private Server server = null;

    public static final String TAG = "AudioActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new ActivityHandler(this);

        startClientNoServer();
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
            case R.id.app_mode_switch:
                if (item.isChecked()) {
                    stopClient();
                    startServer();
                } else {
                    stopServer();
                    startClientNoServer();
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void stopClient() {
        if (client != null) {
            client.stop();
            client = null;
        }
    }

    private void startClient(InetAddress address) {
        setContentView(R.layout.activity_audio_client);

        client = new Client(address, new Client.EventListener() {
            @Override
            public void onServerDisconnect() {
                stopClient();
                sendMessage("toast\nServer disconnect: " + client.getServerName());
                sendMessage("startClientNoServer");
            }

            @Override
            public void onPlayOk() {
                startPlay(client.getServerAddr());
            }
        });

        TextView deviceAddrText = findViewById(R.id.deviceAddr);
        TextView deviceNameText = findViewById(R.id.deviceName);
        deviceAddrText.setText(client.getServerAddress());
        deviceNameText.setText(client.getServerName());

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button) v;
                String text = button.getText().toString();
                if (text.equals("play")) {
                    button.setText("stop");
                    client.sendPlay();
                } else if (text.equals("stop")) {
                    button.setText("play");
                    stopMusic();
                }
            }
        });
    }

    private void startClientNoServer() {
        serverAddr = null;
        setContentView(R.layout.activity_audio);

        Client.searchServer(new Client.SearchListener() {
            @Override
            public void onServerNotFound() {
                sendMessage("toast\nServer not found");
            }

            @Override
            public void onServerFound(InetAddress address) {
                serverAddr = address;
                sendMessage("startClient");
            }
        });
    }

    private void startServer() {
        server = new Server(new Server.EventListener() {
            @Override
            public void onPlay(InetAddress address) {
                sendMessage("deviceListChange");
                receiveMusic(address);
            }

            @Override
            public void onStop(InetAddress address) {
                stopMusic();
            }
        });

        setContentView(R.layout.activity_audio_server);
        listView = findViewById(R.id.audioListView);
        listView.setAdapter(new AudioDeviceListAdapter(
                this, R.layout.row_device, server.getClientList()));
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

private void startPlay(InetAddress address) {
        //TODO
    }

    private void stopMusic() {
        client.sendStop();
        //TODO
    }

    private void receiveMusic(InetAddress address) {
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

    private class AudioDeviceListAdapter extends ArrayAdapter<InetAddress> {

        private List<InetAddress> items;

        public AudioDeviceListAdapter(Context context, int textViewResourceId,
                                      List<InetAddress> objects) {
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
            InetAddress device = items.get(position);
            if (device != null) {
                TextView top = (TextView) v.findViewById(R.id.deviceName);
                TextView bottom = (TextView) v.findViewById(R.id.deviceAddress);

                if (top != null) {
                    top.setText(device.getHostName());
                }
                if (bottom != null) {
                    bottom.setText(device.getHostAddress());
                }
            }
            return v;
        }
    }
}
