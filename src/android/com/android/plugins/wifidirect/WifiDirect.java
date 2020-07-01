package com.android.plugins.wifidirect;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import com.android.plugins.wifidirect.library.WifiDirectNode;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by JasonYang on 2017/9/20.
 */
public class WifiDirect extends CordovaPlugin {

    private static final String TAG = WifiDirect.class.getSimpleName();

    private static final String FIELD_DEVICE_NAME = "deviceName";
    private static final String FIELD_ADDRESS = "address";
//    private static final String FIELD_PRIMARY_TYPE = "primaryType";
//    private static final String FIELD_SECONDARY_TYPE = "secondaryType";
    private static final String FIELD_STATUS = "status";

    private static final String FIELD_CLIENT_UNIQUETAG = "uniqueTag";//客户端标签
    private static final String FIELD_FILE_NAME = "fileName";//文件名
    private static final String FIELD_EXTENSION = "extension";//文件扩展名
    private static final String FIELD_FILE_PATH = "filePath";//文件路径

    private static final String FIELD_OPERATION_TYPE = "operationType";//操作类型

    private WifiDirectNode node;

    public WifiDirect() {
    }

    @Override
    public void onDestroy() {
        shutdown();
    }

    @Override
    public boolean execute(String actionAsString, JSONArray args, CallbackContext callbackContext) {
        Action action;
        try {
            action = Action.valueOf(actionAsString);
        } catch (IllegalArgumentException e) {
            // shouldn't ever happen
            Log.e(TAG, "unexpected error", e);
            return false;
        }

        try {
            return executeAndPossiblyThrow(action, args, callbackContext);
        } catch (JSONException e) {
            Log.e(TAG, "unexpected error", e);
            return false;
        }
    }

    private boolean executeAndPossiblyThrow(Action action, JSONArray args,
            final CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case getInstance: {
                try {
                    if (node == null) {
//                        final String type = args.optString(0);
//                        final String domain = args.optString(1);
//                        final String name = args.optString(2);
//                        final int port = args.optInt(3);
//                        final JSONObject props = args.optJSONObject(4);

//                        ServiceData serviceData = new ServiceData(type, domain, name, port, props);
                        Context context = this.cordova.getActivity().getApplicationContext();
                        node = new WifiDirectNode(context);
                        callbackContext.success();
                    } else {
                        callbackContext.success();
                    }
                } catch (Exception e) {
                    callbackContext.error("Device does not support Wifi-Direct.");
                    e.printStackTrace();
                }
            }
            break;
            case startDiscovering: {
                if (node != null) {
                    node.setDiscoveringCallback(new WifiDirectNode.DiscoveringCallback() {
                        @Override
                        public void onDevicesUpdate(List<WifiP2pDevice> updates) {
                            JSONArray status = new JSONArray();
                            if (updates != null && !updates.isEmpty()) {
                                for (WifiP2pDevice wifiP2pDevice : updates) {
                                    try {
                                        JSONObject device = new JSONObject();
                                        device.put(FIELD_DEVICE_NAME, wifiP2pDevice.deviceName);
                                        device.put(FIELD_ADDRESS, wifiP2pDevice.deviceAddress);
//                                        device.put(FIELD_PRIMARY_TYPE, wifiP2pDevice.primaryDeviceType);
//                                        device.put(FIELD_SECONDARY_TYPE, wifiP2pDevice.secondaryDeviceType);
                                        device.put(FIELD_STATUS, wifiP2pDevice.status);
                                        status.put(device);
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Error: " + e.getMessage());
                                    }
                                }
                            }

                            PluginResult result = new PluginResult(PluginResult.Status.OK, status);
                            result.setKeepCallback(true);
                            callbackContext.sendPluginResult(result);
                        }
                    });
                    node.startDiscovering();
                } else {
                    callbackContext.error("Wifi Direct has been shut down.");
                }
            }
            break;
            case stopDiscovering: {
                if (node != null) {
                    node.setDiscoveringCallback(null);
                    node.stopDiscovering();
                    callbackContext.success();
                } else {
                    callbackContext.error("Wifi Direct has been shut down.");
                }
            }
            break;
            case connect: {
                final JSONObject device = args.optJSONObject(0);
                if (node != null) {
                    try {
                        String address = device.getString(FIELD_ADDRESS);
                        WifiDirectNode.ConnectionCallback cb = new WifiDirectNode.ConnectionCallback() {
                            @Override
                            public void onConnect() {
                                try {
                                    JSONObject status = new JSONObject();
                                    status.put(FIELD_OPERATION_TYPE, "connection");//操作类型为：连接成功

                                    PluginResult result = new PluginResult(PluginResult.Status.OK, status);
                                    result.setKeepCallback(true);
                                    callbackContext.sendPluginResult(result);
                                } catch (JSONException e) {
                                    Log.e(TAG, "Error: " + e.getMessage());
                                    callbackContext.error("Unexpected Error happened.");
                                }
                            }

                            @Override
                            public void onConnectError(String message) {
                                callbackContext.error("Connection error -> " + message);
                            }
                        };

                        WifiDirectNode.CommonCallback callback = new WifiDirectNode.CommonCallback() {
                            @Override
                            public void onSuccess(String type, JSONObject args) {
                                switch(type) {
                                    case "receivedFile": {//客户端接收文件
                                        try{
                                            String filePath = args.getString("filePath");
                                            int length = filePath.length();
                                            int index = filePath.lastIndexOf(".");
                                            String name = filePath.substring(0, index);//文件名
                                            String extension = filePath.substring(index + 1, length);//文件扩展名
                                            JSONObject status = new JSONObject();
                                            status.put(FIELD_OPERATION_TYPE, "receivedFile");//操作类型为：接收文件
                                            status.put(FIELD_FILE_NAME, name);//文件名
                                            status.put(FIELD_EXTENSION, extension);//扩展名
                                            status.put(FIELD_FILE_PATH, filePath);//文件路径

                                            PluginResult result = new PluginResult(PluginResult.Status.OK, status);
                                            result.setKeepCallback(true);
                                            callbackContext.sendPluginResult(result);
                                        } catch (JSONException e) {
                                            Log.e(TAG, "Error: " + e.getMessage());
                                            callbackContext.error("Unexpected Error happened.");
                                        }
                                        break;
                                    }
                                    case "disconnection": {//客户端断开连接
                                        try{
                                            JSONObject status = new JSONObject();
                                            status.put(FIELD_OPERATION_TYPE, "disconnection");//操作类型为：关闭连接

                                            PluginResult result = new PluginResult(PluginResult.Status.OK, status);
                                            result.setKeepCallback(true);
                                            callbackContext.sendPluginResult(result);
                                        } catch (JSONException e) {
                                            Log.e(TAG, "Error: " + e.getMessage());
                                            callbackContext.error("Unexpected Error happened.");
                                        }
                                        break;
                                    }
                                }
                            }
                        };
                        node.setCommonCallback(callback);
                        node.connect(address, 0, cb);
                    } catch (JSONException e) {
                        callbackContext.error("Unexpected Error happened.");
                    }
                } else {
                    callbackContext.error("Wifi Direct has been shut down.");
                }
            }
            break;
            case disconnect: {
                if (node != null) {
                    node.disconnect();
                    callbackContext.success();
                } else {
                    callbackContext.error("Wifi Direct has been shut down.");
                }
            }
            break;
            case shutdown: {
                if (node != null) {
                    node.shutdown();
//                    node = null;
                }
                callbackContext.success();
            }
            break;
//            case getSocketInfo: {
//                if(node != null){
//                    String info = node.getSocketInfo();
//                    PluginResult result = new PluginResult(PluginResult.Status.OK, info);
//                    result.setKeepCallback(true);
//                    callbackContext.sendPluginResult(result);
//                }else{
//                    callbackContext.error("Wifi Direct has been shut down222.");
//                }
//            }
//            break;
//            case sendMessage: {
//                if(node != null){
//                    node.sendMessage();
//                    callbackContext.success();
//                }else {
//                    callbackContext.error("Wifi Direct has been shut down333.");
//                }
//            }
//            break;
//            case sendClientClose: {
//                if(node != null){
//                    node.sendClientClose();
//                    callbackContext.success();
//                }else {
//                    callbackContext.error("Wifi Direct has been shut down4444.");
//                }
//            }
//            break;
            case createServer: {
                if(node != null){
                    //通用回调接口
                    WifiDirectNode.CommonCallback commonCallback = new WifiDirectNode.CommonCallback() {
                        @Override
                        public void onSuccess(String type, JSONObject args) {
                            switch(type) {
                                case "createdServer": {//创建服务器成功
                                    try {
                                        JSONObject status = new JSONObject();
                                        status.put(FIELD_OPERATION_TYPE, "createdServer");//操作类型为：创建服务器

                                        PluginResult result = new PluginResult(PluginResult.Status.OK, status);
                                        result.setKeepCallback(true);
                                        callbackContext.sendPluginResult(result);
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Error: " + e.getMessage());
                                        callbackContext.error("Unexpected Error happened.");
                                    }
                                    break;
                                }
                                case "connection": {//客户端连接成功
                                    try{
                                        JSONObject status = new JSONObject();
                                        String uniqueTag = args.getString("uniqueTag");
                                        status.put(FIELD_OPERATION_TYPE, "connection");//操作类型为：客户端连接成功
                                        status.put(FIELD_CLIENT_UNIQUETAG, uniqueTag);

                                        PluginResult result = new PluginResult(PluginResult.Status.OK, status);
                                        result.setKeepCallback(true);
                                        callbackContext.sendPluginResult(result);
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Error: " + e.getMessage());
                                        callbackContext.error("Unexpected Error happened.");
                                    }
                                    break;
                                }
                                case "receivedFile": {//服务端接收文件
                                    try{
                                        String uniqueTag = args.getString("uniqueTag");
                                        String filePath = args.getString("filePath");
                                        int length = filePath.length();
                                        int index = filePath.lastIndexOf(".");
                                        String name = filePath.substring(0, index);//文件名
                                        String extension = filePath.substring(index + 1, length);//文件扩展名
                                        JSONObject status = new JSONObject();
                                        status.put(FIELD_OPERATION_TYPE, "receivedFile");//操作类型为：接收文件
                                        status.put(FIELD_CLIENT_UNIQUETAG, uniqueTag);//设备标签
                                        status.put(FIELD_FILE_NAME, name);//文件名
                                        status.put(FIELD_EXTENSION, extension);//扩展名
                                        status.put(FIELD_FILE_PATH, filePath);//文件路径

                                        PluginResult result = new PluginResult(PluginResult.Status.OK, status);
                                        result.setKeepCallback(true);
                                        callbackContext.sendPluginResult(result);
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Error: " + e.getMessage());
                                        callbackContext.error("Unexpected Error happened.");
                                    }
                                    break;
                                }
                                case "disconnection": {//服务端收到客户端关闭
                                    try{
                                        String uniqueTag = args.getString("uniqueTag");
                                        JSONObject status = new JSONObject();
                                        status.put(FIELD_OPERATION_TYPE, "disconnection");//操作类型为：关闭连接
                                        status.put(FIELD_CLIENT_UNIQUETAG, uniqueTag);//设备标签

                                        PluginResult result = new PluginResult(PluginResult.Status.OK, status);
                                        result.setKeepCallback(true);
                                        callbackContext.sendPluginResult(result);
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Error: " + e.getMessage());
                                        callbackContext.error("Unexpected Error happened.");
                                    }
                                    break;
                                }
                            }

                        }
                    };
                    node.setCommonCallback(commonCallback);
                    node.createServer();
                }else {
                    callbackContext.error("Wifi Direct has been shut down5555.");
                }
            }
            break;
            case sendFileToServer: {
                if(node != null){
                    final String fileName = args.optString(0);
                    final String dataUrl = args.optString(1);
                    node.sendFileToServer(fileName,dataUrl);
                    callbackContext.success();
                }else {
                    callbackContext.error("Wifi Direct has been shut down6666.");
                }
            }
            break;
            case sendFileToClient: {
                if(node != null){
                    final String uniqueTag = args.optString(0);
                    final String fileName = args.optString(1);
                    final String dataUrl = args.optString(2);
                    node.sendFileToClient(uniqueTag, fileName, dataUrl);
                    callbackContext.success();
                }else {
                    callbackContext.error("Wifi Direct has been shut down6666.");
                }
            }
            break;
        }
        return true;
    }

    private void shutdown() {
        if (node != null) {
            node.shutdown();
//            node = null;
        }
    }

    private enum Action {
        getInstance,
        startDiscovering,
        stopDiscovering,
        connect,
        disconnect,
        shutdown,
        createServer,
        sendFileToServer,
        sendFileToClient,
    }
}
