package com.waterlemongan.audiostreaming;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private ListView listView;
    private List<WifiP2pDevice> deviceList;

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 3456;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.CHANGE_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CHANGE_NETWORK_STATE,
                    Manifest.permission.ACCESS_WIFI_STATE
            }, PERMISSIONS_REQUEST_CODE);
        }

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        listView = findViewById(R.id.listView);
        deviceList = new ArrayList<>();
        listView.setAdapter(new WiFiPeerListAdapter(this, R.layout.row_device, deviceList));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WifiP2pDevice device = deviceList.get(position);
                Log.d(TAG, "connect to device: " + device.deviceName);

                connect(device);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "remove all connections");
        manager.removeGroup(channel, null);
        manager.cancelConnect(channel, null);
        startDiscovery();
    }

    @Override
    protected void onPause() {
        super.onPause();
        manager.stopPeerDiscovery(channel, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "remove all connections");
        manager.removeGroup(channel, null);
        manager.cancelConnect(channel, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_search:
                startDiscovery();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void createP2pNetwork(View view) {
        manager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                nextActivity();
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "create wifi p2p network failed");
                Toast.makeText(MainActivity.this, "create network failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void connect(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                nextActivity();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this, "Connect Failed: " + reason, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void nextActivity() {
        Intent intent = new Intent(MainActivity.this, AudioActivity.class);
        startActivity(intent);
    }

    private void startDiscovery() {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (manager != null) {
                    manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                        @Override
                        public void onPeersAvailable(WifiP2pDeviceList peers) {
                            deviceList.clear();
                            deviceList.addAll(peers.getDeviceList());
                            ((WiFiPeerListAdapter) listView.getAdapter()).notifyDataSetChanged();
                            Log.d(TAG, "peers found: " + deviceList.size());
                        }
                    });
                }
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "discovery failed" + reason);
                Toast.makeText(MainActivity.this, "Discovery Failed: " + reason, Toast.LENGTH_LONG).show();
            }
        });
    }

    private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

        private List<WifiP2pDevice> items;

        public WiFiPeerListAdapter(Context context, int textViewResourceId,
                                   List<WifiP2pDevice> objects) {
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
            WifiP2pDevice device = items.get(position);
            if (device != null) {
                TextView top = (TextView) v.findViewById(R.id.deviceName);
                TextView bottom = (TextView) v.findViewById(R.id.deviceAddress);

                if (top != null) {
                    top.setText(device.deviceName);
                }
                if (bottom != null) {
                    bottom.setText(device.deviceAddress);
                }
            }
            return v;
        }
    }
}
