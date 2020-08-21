package com.android.plugins.wifidirect.library;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.android.plugins.wifidirect.library.socket.client.WifiSocketClient;
import com.android.plugins.wifidirect.library.socket.server.WifiSocketServer;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wengtaotao on 2020/6/3.
 */

/**
 * 节点状态
 */
class NodeState {
    private int state;
    public static final int DISCONNECTED = 1;//未连接
    public static final int INITIATED = 2;//已启动
    public static final int CONNECTING = 3;//连接中
    public static final int CONNECTED = 4;//已连接
    public static final int DISCONNECTING = 5;//连接断开中

    public NodeState(int s) {
        this.state = s;
    }

    public int get() {
        return this.state;
    }

    public void set(int s) {
        this.state = s;
    }
}

/**
 * Wifi Direct节点
 */
public class WifiDirectNode implements WifiP2pManager.ConnectionInfoListener,
        WifiP2pManager.GroupInfoListener, WifiP2pManager.PeerListListener {

    private final static String TAG = "WifiDirectNode";

    private static final long periodicInterval = 15000;

    private Context context;
    private Handler handler;
    private ServiceData serviceData;

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;

    private Runnable periodicDiscovery = null;
    private Runnable periodicFind = null;
    private Runnable requestConnectionInfo = null;

    private boolean isEnabled = true;

    private WifiP2pDevice device;
    private WifiP2pConfig peerConfig = null;
    private boolean pendingConnect = false;//是否挂起连接
    private WifiP2pDevice groupOwner;
    private List<WifiP2pDevice> peers;
    private List<WifiP2pDevice> services;

    private NodeState nodeState = new NodeState(NodeState.DISCONNECTED);

//    private WifiSocketClient wifiSocketClient = new WifiSocketClient();
//    private WifiSocketServer wifiSocketServer = new WifiSocketServer();
    private WifiSocketClient wifiSocketClient = null;
    private WifiSocketServer wifiSocketServer = null;

    private InternalCallback connectionCallback = new InternalCallback();
    private DiscoveringCallback discoveringCallback;//搜索设备成功的回调接口
    private CommonCallback commonCallback;//通用回调接口
//    private ReceivedFileCallback receivedFileCallback;//接收文件成功的回调接口
//    private ConnectionCloseCallback connectionCloseCallback;//连接断开成功的回调接口

    private WifiP2pManager.DnsSdTxtRecordListener txtRecordListener;

    private WifiP2pDnsSdServiceInfo serviceInfo = null;

    public interface ConnectionCallback {

        void onConnect();

        void onConnectError(String message);
    }

    private class InternalCallback implements ConnectionCallback {

        private ConnectionCallback callback;

        @Override
        public void onConnect() {
            if (callback != null) {
                callback.onConnect();
                callback = null;
            }
        }

        @Override
        public void onConnectError(String message) {
            if (callback != null) {
                callback.onConnectError(message);
                callback = null;
            }
        }

        public void attach(ConnectionCallback callback) {
            this.callback = callback;
        }
    }

    /**
     * 搜索设备成功后回调接口
     */
    public interface DiscoveringCallback {

        void onDevicesUpdate(List<WifiP2pDevice> updates);
    }

    /**
     * 通用的回调接口
     */
    public interface CommonCallback {

        void onSuccess(String type, JSONObject args);
    }

//    /**
//     * 连接断开成功后回调接口
//     */
//    public interface ConnectionCloseCallback {
//
//        void connectionCloseSuccess(String deviceAddress);
//    }

    /**
     * 初始化WIFI直连节点
     * @param context
     */
    public WifiDirectNode(final Context context/*, ServiceData serviceData*/) {
        this.context = context;
//        this.serviceData = new ServiceData();

        handler = new Handler(context.getMainLooper());

        services = new ArrayList<WifiP2pDevice>();
        peers = new ArrayList<WifiP2pDevice>();

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context, context.getMainLooper(), null);
        receiver = new WiFiDirectNodeReceiver(this);

//        Map record = new HashMap();
//        record.put("listenport", "111");
//        record.put("buddyname", "John Doe" + (int) (Math.random() * 1000));
//        record.put("available", "visible");
//
//        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("_test","_presence",record);
        startup();

    }

    private void startup() {
        periodicDiscovery = new Runnable() {
            public void run() {
                if (!isEnabled) {
                    return;
                }
                Log.i(TAG, "开始搜索服务设备22222");
                wifiP2pManager.discoverServices(channel,
                        new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                            }

                            public void onFailure(int reasonCode) {
                                stopDiscovering();
                            }
                        });
                handler.postDelayed(periodicDiscovery, periodicInterval);
            }
        };

//        periodicFind = new Runnable() {
//            public void run() {
//                if (!isEnabled) {
//                    return;
//                }
//
//                wifiP2pManager.discoverPeers(channel, null
////                        new WifiP2pManager.ActionListener() {
////                    @Override
////                    public void onSuccess() {
////                        Log.d(TAG, "discover success！");
////                    }
////
////                    @Override
////                    public void onFailure(int reasonCode) {
////                        Log.d(TAG, "discover failure！");
////                    }
////                }
//                );
//                handler.postDelayed(periodicFind, periodicInterval);
//            }
//        };

        requestConnectionInfo = new Runnable() {
            public void run() {
                if (!isEnabled) {
                    return;
                }

                wifiP2pManager.requestConnectionInfo(channel, WifiDirectNode.this);
            }
        };

        context.registerReceiver(receiver, intentFilter);
    }

    private void setupDnsSdResponsor() {
        Log.i(TAG, "安装DNS");
        WifiP2pManager.DnsSdServiceResponseListener serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String serviceNameAndTP,
                    WifiP2pDevice sourceDevice) {
                Log.d(TAG, "搜索到设备2222: " + instanceName + " " + serviceNameAndTP);
            }
        };

//        WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
//            @Override
//            public void onDnsSdTxtRecordAvailable(String serviceFullDomainName,
//                    Map<String, String> record, WifiP2pDevice device) {
//                Log.i(TAG, "搜索到设备：" + device.toString());
//                if (!services.isEmpty()) {
//                    for (WifiP2pDevice found : services) {
//                        if (found.deviceName.equals(device.deviceName)) {
//                            return;
//                        }
//                    }
//                }
//
////                String serviceType = serviceData.getFullDomainName();
////                if (serviceFullDomainName != null && serviceFullDomainName.equals(serviceType)) {
//                    services.add(device);
//                Log.i(TAG, "设备信息：" + services.toString());
////                }
//            }
//        };

        wifiP2pManager.setDnsSdResponseListeners(channel, serviceListener, txtRecordListener);
    }

    /**
     * 开始搜索设备
     */
    public void startDiscovering() {
        if (wifiP2pManager == null) return;
        services.clear();
        setupDnsSdResponsor();
        doDiscoverServices(true);
//        doFindPeers(true);
    }

    private void doDiscoverServices(boolean start) {
        Log.i(TAG, "开始搜索服务设备");
        handler.removeCallbacks(periodicDiscovery);

        wifiP2pManager.clearServiceRequests(channel, null);

//        String serviceType = serviceData.getFullDomainName();
        WifiP2pDnsSdServiceRequest serviceRequest;
//        if (serviceData.getName() != null && !TextUtils.isEmpty(serviceType)) {
//            serviceRequest = WifiP2pDnsSdServiceRequest
//                    .newInstance(serviceData.getName(), serviceType);
//        } else if (!TextUtils.isEmpty(serviceType)) {
//            serviceRequest = WifiP2pDnsSdServiceRequest.newInstance(serviceType);
//        } else {
            serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
//        }
        wifiP2pManager.addServiceRequest(channel, serviceRequest,
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onFailure(int errorCode) {
                    }
                });
        if(serviceInfo == null){
            Map record = new HashMap();
            record.put("listenport", "111");
            record.put("buddyname", "John Doe" + (int) (Math.random() * 1000));
            record.put("available", "visible");

            serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("_test","_presence",record);
        }
        if (start) {
            wifiP2pManager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "onSuccessonSuccess");
                }

                @Override
                public void onFailure(int reason) {
                    Log.i(TAG, "onFailureonFailure");
                }
            });
            periodicDiscovery.run();
        }else{
            wifiP2pManager.removeLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "onSuccess2onSuccess2");
                }

                @Override
                public void onFailure(int reason) {
                    Log.i(TAG, "onFailure2onFailure2");
                }
            });
        }
    }

    private void doFindPeers(boolean start) {
        handler.removeCallbacks(periodicFind);
        wifiP2pManager.stopPeerDiscovery(channel, null);
        if (start) {
            periodicFind.run();
//            wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
//                @Override
//                public void onSuccess() {
//                    Log.d(TAG, "discover success！");
//                }
//
//                @Override
//                public void onFailure(int reasonCode) {
//                    Log.d(TAG, "discover failure！");
//                }
//            });
        }
    }

    /**
     * 停止搜索设备
     */
    public void stopDiscovering() {
        if (wifiP2pManager == null) return;
        services.clear();
        Log.i(TAG, "停止搜索设备3333");
         doDiscoverServices(false);
//        doFindPeers(false);
    }

    /**
     * 获取设备列表
     * @param intent
     */
    public void requestPeers(Intent intent) {
        if (wifiP2pManager == null) return;
//        this.wifiP2pManager.requestPeers(channel, this);
        WifiP2pDeviceList mPeers = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
        this.peers.clear();
        this.peers.addAll(mPeers.getDeviceList());
        if (discoveringCallback != null) {
            discoveringCallback.onDevicesUpdate(new ArrayList<WifiP2pDevice>(this.peers));
        }
    }


    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        if (wifiP2pManager == null) return;
        this.peers.clear();
        this.peers.addAll(peers.getDeviceList());

        if (discoveringCallback != null) {
            discoveringCallback.onDevicesUpdate(new ArrayList<WifiP2pDevice>(this.peers));
        }
    }

    /**
     * 重写GroupInfoListener的方法，获取组对象，并设置组所有者
     * @param group
     */
    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        synchronized (nodeState) {
            if (nodeState.get() != NodeState.CONNECTED)
                return;
        }

        groupOwner = group.getOwner();
    }

    /**
     * 连接设备
     * @param deviceAddress
     * @param groupOwnerIntent
     * @param cb
     */
    public void connect(final String deviceAddress, final int groupOwnerIntent,
            final ConnectionCallback cb) {
        Log.i(TAG, "开始建立连接1111" + deviceAddress);
        if (wifiP2pManager == null) {
            if (cb != null) cb.onConnectError("Shutdown!");
            return;
        }

        handler.removeCallbacks(requestConnectionInfo);
        handler.removeCallbacks(periodicFind);

        if (!isEnabled) {
            if (cb != null) cb.onConnectError("Wifi direct is not enabled.");
            return;
        }

        if (deviceAddress == null || deviceAddress.isEmpty()) {
            if (cb != null) cb.onConnectError("Device address empty.");
            return;
        }

        if (nodeState.get() != NodeState.DISCONNECTED) {
            if(nodeState.get() != NodeState.CONNECTED ){
                if (cb != null) cb.onConnectError(nodeState.get() + "Already connected or in progress.");
                return;
            }
        }

        connectionCallback.attach(cb);

        peerConfig = new WifiP2pConfig();
        peerConfig.deviceAddress = deviceAddress;
        peerConfig.groupOwnerIntent = groupOwnerIntent;
        peerConfig.wps.setup = WpsInfo.PBC;

        if (nodeState.get() == NodeState.CONNECTED && groupOwner != null) {
            if (groupOwner.deviceAddress.equals(deviceAddress)) {
                wifiP2pManager.requestGroupInfo(channel, this);
                connectionCallback.onConnect();
            } else {
                pendingConnect = true;
            }
        } else {
            initiateConnect();
        }
    }

//    public void requestConnectionInfo() {
//        if (wifiP2pManager == null) return;
//        wifiP2pManager.requestConnectionInfo(channel, this);
//    }

    /**
     * 启动Wifi Direct连接
     */
    private void initiateConnect() {
        pendingConnect = false;
        if (peerConfig == null)
            return;
        nodeState.set(NodeState.INITIATED);
        wifiP2pManager.connect(channel, peerConfig,
                new WifiP2pManager.ActionListener() {
                    public void onSuccess() {
                        if (nodeState.get() == NodeState.INITIATED) {
                            nodeState.set(NodeState.CONNECTING);
                        }
                        Log.i(TAG, "connect success!!");
                        handler.postDelayed(requestConnectionInfo, 30000);
                    }

                    public void onFailure(int reasonCode) {
                        nodeState.set(NodeState.DISCONNECTED);
                        peerConfig = null;

                        connectionCallback.onConnectError("connect failed: " + reasonCode);
                    }
                });
    }

    /**
     * 重写ConnectionInfoListener的方法，获取组连接信息，并设置节点状态
     * @param info
     */
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        if (wifiP2pManager == null) return;

        handler.removeCallbacks(requestConnectionInfo);
        if (!info.groupFormed) {
            groupOwner = null;
        }else if(info.isGroupOwner){
//            wifiSocketServer.startServerSocket();//创建serverSocket
//            Intent intent = new Intent(MyApplication.getAppContext(),WifiServerService.class);
//            new WifiServerService().startService(intent);
        }else{
            String serverIp = info.groupOwnerAddress.getHostAddress();
            if(wifiSocketClient == null){
                Log.i(TAG, "客户端不存在，进行初始化");
                wifiSocketClient = new WifiSocketClient(serverIp, commonCallback);
                Log.i(TAG, "初始化成功22222");
            }else {
                Log.i(TAG, "客户端已经存在：" + wifiSocketClient);
                wifiSocketClient.connect();
                Log.i(TAG, "连接成功111");
            }

//            socketClient.connect();
//            Log.i(TAG, "客户端连接成功2");
        }

        switch (nodeState.get()) {
            case NodeState.INITIATED:
            case NodeState.CONNECTING:
                if (info.groupFormed) {//已生成组对象
                    nodeState.set(NodeState.CONNECTED);
                    wifiP2pManager.requestGroupInfo(channel, this);

                    connectionCallback.onConnect();
                } else {//未生成组对象，取消连接
                    wifiP2pManager.cancelConnect(channel, null);

                    nodeState.set(NodeState.DISCONNECTED);
                    peerConfig = null;

                    connectionCallback.onConnectError("Timeout");
                }
                break;
            case NodeState.CONNECTED:
                if (!info.groupFormed) {//未生成组对象，修改节点状态为已断开连接
                    nodeState.set(NodeState.DISCONNECTED);
                    peerConfig = null;
                }
                break;
            case NodeState.DISCONNECTING:
            case NodeState.DISCONNECTED:
                if (info.groupFormed) {
                    nodeState.set(NodeState.CONNECTED);
                    wifiP2pManager.requestGroupInfo(channel, this);

                    if (pendingConnect && peerConfig != null) {
                        peerConfig = null;
                    }

                    pendingConnect = false;
                } else {
                    if (!pendingConnect && peerConfig != null) {
                        peerConfig = null;
                    }
                    nodeState.set(NodeState.DISCONNECTED);

                    if (pendingConnect) {
                        initiateConnect();
                    }
                }
                break;
        }
    }

    /**
     * 关闭连接
     */
    public void disconnect() {
        if (wifiP2pManager == null) return;

        connectionCallback.onConnectError("Disconnect!");
        if(wifiSocketClient != null){
            wifiSocketClient.disconnect();
        }else if(wifiSocketServer != null){
            wifiSocketServer.disconnect();
        }

        if (!isEnabled) return;

        switch (nodeState.get()) {
            case NodeState.INITIATED:
            case NodeState.CONNECTING:
                handler.removeCallbacks(requestConnectionInfo);

                nodeState.set(NodeState.DISCONNECTING);
                wifiP2pManager.cancelConnect(channel,
                        new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                                nodeState.set(NodeState.DISCONNECTED);
                                peerConfig = null;
                            }

                            public void onFailure(int reasonCode) {
                                nodeState.set(NodeState.DISCONNECTED);
                                peerConfig = null;
                            }
                        });
                break;
            case NodeState.CONNECTED:
                nodeState.set(NodeState.DISCONNECTING);
                wifiP2pManager.removeGroup(channel,
                        new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                                nodeState.set(NodeState.DISCONNECTED);
                                peerConfig = null;
                            }

                            public void onFailure(int reasonCode) {
                                nodeState.set(NodeState.DISCONNECTED);
                                peerConfig = null;
                            }
                        });
                break;
        }
    }

    /**
     * 停止运行
     */
    public void shutdown() {
        if (wifiP2pManager == null) return;
        WifiP2pManager.ActionListener noop = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
            }
        };
        stopDiscovering();
        disconnect();
        handler.removeCallbacks(requestConnectionInfo);
        wifiP2pManager.clearLocalServices(channel, noop);
        wifiP2pManager.stopPeerDiscovery(channel, noop);
        if(wifiSocketClient != null){
            wifiSocketClient.disconnect();
            wifiSocketClient = null;
        }else if(wifiSocketServer != null){
            wifiSocketServer.disconnect();
        }

        if (receiver != null) {
            context.unregisterReceiver(receiver);
        }

        peers.clear();
        services.clear();

        device = null;
        wifiP2pManager = null;
        channel = null;
        context = null;
        discoveringCallback = null;
        connectionCallback = null;
        commonCallback = null;
    }

    public WifiP2pDevice getDevice() {
        return this.device;
    }

    public void setDevice(WifiP2pDevice thisDevice) {
        this.device = thisDevice;
    }

    public void setDiscoveringCallback(DiscoveringCallback callback) {
        discoveringCallback = callback;
    }

    public void setCommonCallback(CommonCallback commonCallback) {
        this.commonCallback = commonCallback;
    }

    public void setTxtRecordListener(WifiP2pManager.DnsSdTxtRecordListener txtRecordListener) {
        this.txtRecordListener = txtRecordListener;
    }

    //    public void setConnectionCloseCallback(ConnectionCloseCallback callback) {
//        connectionCloseCallback = callback;
//    }

    public ServiceData getServiceData() {
        return serviceData;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
        if (!isEnabled) {
            stopDiscovering();
            disconnect();
        }
    }

//    public String getSocketInfo(){
//        return wifiSocketServer.getSocketInfo() + wifiSocketClient.getSocketInfo();
//    }

//    public void sendMessage(){
//        wifiSocketClient.sendMessage("你好啊，服务器！");
//    }

//    public void sendClientClose(){
//        wifiSocketServer.sendClientClose();
//    }

    /**
     * 启动当前设备作为服务端
     */
    public void createServer(){
        WifiP2pManager.ActionListener actionListener = new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if(commonCallback != null){
                    commonCallback.onSuccess("createdServer", null);
                }
            }

            @Override
            public void onFailure(int reason) {

            }
        };
        wifiP2pManager.createGroup(channel, actionListener);
        wifiSocketServer = new WifiSocketServer(commonCallback);
    }

    /**
     * 发送文件给服务端
     * @param fileName
     * @param dataUrl
     */
    public void sendFileToServer(String fileName, String dataUrl) {
        Log.i(TAG, "客户端开始发送文件...");
        wifiSocketClient.sendFileData(fileName, dataUrl);
        Log.i(TAG, "客户端发送文件成功...");
    }

    /**
     * 发送文件给客户端
     * @param uniqueTag
     * @param fileName
     * @param dataUrl
     */
    public void sendFileToClient(String uniqueTag, String fileName, String dataUrl) {
        Log.i(TAG, "服务端开始发送文件...");
        wifiSocketServer.sendFileData(uniqueTag, fileName, dataUrl);
        Log.i(TAG, "服务端发送文件成功...");
    }

    public List<WifiP2pDevice> getServices(){
        return services;
    }
}
