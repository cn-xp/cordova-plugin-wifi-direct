package com.android.plugins.wifidirect.library;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

/**
 * Created by wengtaotao on 2020/6/3.
 */
public class WiFiDirectNodeReceiver extends BroadcastReceiver {

    private static final String TAG = "WiFiDirectNodeReceiver";

    private WifiDirectNode node;

    public WiFiDirectNodeReceiver(WifiDirectNode node) {
        super();
        this.node = node;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (node == null) {
            return;
        }
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {//指示是否启用或禁用WIFI-P2P
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            node.setEnabled(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {//WIFI-P2P连接的状态已更改
//            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
//            if(networkInfo.isConnected()){
                node.onConnectionInfoAvailable((WifiP2pInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO));
            Log.i(TAG, "连接状态变更！");
//            }else {
//
//            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {//此设备的详细信息已更改
            node.setDevice((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {//点对点设备列表已更改
            node.requestPeers(intent);
            Log.i(TAG, "搜索设备成功！");
        }
    }
}
