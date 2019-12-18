package com.waterlemongan.audiostreaming;

import android.content.Context;
import android.graphics.drawable.Drawable;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.waterlemongan.audiostreaming.utils.AudioUtils;
import com.waterlemongan.audiostreaming.utils.MusicUtils;
import com.waterlemongan.audiostreaming.utils.Song;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class AudioActivity extends AppCompatActivity {
    private InetAddress serverAddr;
    private ActivityHandler handler;
    private ListView listView;
    private ListView musicListView;
    private List<Song> mMusicList;
    private String srcPath;
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
        if (client != null) {
            client.stop();
        }
    }

    private void startClient(InetAddress address) {
        setContentView(R.layout.activity_audio_client);

        //初始化音乐列表
        musicListView = (ListView) findViewById(R.id.musicListView);
        mMusicList = new ArrayList<>();
        mMusicList = MusicUtils.getMusicData(this);
        Song song = mMusicList.get(1);
        srcPath = song.path;
        MusicListAdapter musicListAdapter = new MusicListAdapter(this, R.layout.item_music_listview, mMusicList);
        musicListView.setAdapter(musicListAdapter);
        Button button = findViewById(R.id.button);
        musicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Song music = mMusicList.get(position);
                if (audioUtils != null) {
                    clientStopMusic();
                }
                srcPath = music.path;
                client.sendPlay();
                isPlay = true;
                View v = findViewById(R.id.button);
                Button button = (Button) v;
                button.setText("pause");
            }
        });
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

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button) v;
                isPlay = !isPlay;
                if (isPlay) {
                    button.setText("pause");
                    if (audioUtils == null){
                        client.sendPlay();
                    } else {
                        audioUtils.reversePause();
                    }

                } else {
                    button.setText("play");
                    //clientStopMusic();
                    audioUtils.reversePause();
                }
            }
        });
    }

    private void clientPlayMusic(InetAddress address) {

        audioUtils = new AudioUtils();
        //File file = new File(Environment.getExternalStorageDirectory(), "Secret_Base.mp3");
        audioUtils.startClient(10050, 10051, address.getHostAddress(), false, srcPath);
    }

    private void clientStopMusic() {
        if (audioUtils != null) {
            audioUtils.stopClient();
        }
        client.sendStop();
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

        audioUtils = new AudioUtils();
        audioUtils.startServer(10050, 10051);
    }

    private void stopServer() {
        if (audioUtils != null) {
            audioUtils.stopServer();
        }
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
        private Drawable stop;
        private Drawable play;

        public AudioDeviceListAdapter(Context context, int textViewResourceId,
                                      List<Client> objects) {
            super(context, textViewResourceId, objects);
            items = objects;
            play = context.getDrawable(R.drawable.ic_play);
            stop = context.getDrawable(R.drawable.ic_pause);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_audio, null);
            }
            InetAddress device = items.get(position).getSelfAddr();
            if (device != null) {
                ImageView top = v.findViewById(R.id.audioStatus);
                TextView bottom = v.findViewById(R.id.audioAddress);

                if (top != null) {
                    if (items.get(position).isPlaying()) {
                        top.setImageDrawable(play);
                    } else {
                        top.setImageDrawable(stop);
                    }
                }
                if (bottom != null) {
                    bottom.setText(device.getHostAddress());
                }
            }
            return v;
        }
    }

    private class MusicListAdapter extends ArrayAdapter<Song> {

        private List<Song> musicList;
        private int resourceId;

        public MusicListAdapter(Context context, int textViewResourceId, List<Song> objects) {
            super(context, textViewResourceId, objects);
            resourceId = textViewResourceId;
            musicList = objects;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder holder = null;
            if (view == null) {
                holder = new ViewHolder();
                //引入布局
                view = LayoutInflater.from(getContext()).inflate(resourceId, viewGroup, false);
                //实例化对象
                holder.song = (TextView) view.findViewById(R.id.item_mymusic_song);
                holder.singer = (TextView) view.findViewById(R.id.item_mymusic_singer);
                holder.duration = (TextView) view.findViewById(R.id.item_mymusic_duration);
                holder.position = (TextView) view.findViewById(R.id.item_mymusic_postion);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            //给控件赋值
            holder.song.setText(musicList.get(i).name.toString());
            holder.singer.setText(musicList.get(i).singer.toString());
            //时间需要转换一下
            int duration = musicList.get(i).duration;
            String time = MusicUtils.formatTime(duration);
            holder.duration.setText(time);
            holder.position.setText(i+1+"");

            return view;
        }
        class ViewHolder{
            TextView song;//歌曲名
            TextView singer;//歌手
            TextView duration;//时长
            TextView position;//序号

        }
    }
}
