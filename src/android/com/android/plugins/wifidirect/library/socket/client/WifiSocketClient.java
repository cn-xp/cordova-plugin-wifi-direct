package com.android.plugins.wifidirect.library.socket.client;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import com.android.plugins.wifidirect.library.WifiDirectNode;
import com.android.plugins.wifidirect.library.socket.data.*;
import com.android.plugins.wifidirect.library.utils.CommonUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xuhao.didi.core.iocore.interfaces.IPulseSendable;
import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.core.pojo.OriginalData;
import com.xuhao.didi.core.utils.SLog;
import com.xuhao.didi.socket.client.impl.client.action.ActionDispatcher;
import com.xuhao.didi.socket.client.impl.exceptions.UnConnectException;
import com.xuhao.didi.socket.client.sdk.OkSocket;
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;
import com.xuhao.didi.socket.client.sdk.client.connection.NoneReconnect;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * socket客户端
 */
public class WifiSocketClient {

    private final String TAG = "WifiSocketClient";

    private WifiDirectNode.CommonCallback commonCallback;

    private ConnectionInfo mInfo;
    private IConnectionManager mManager;
    private OkSocketOptions mOkOptions;

    private final int MAX_RECONNECTION_TIME = 3;//最大重连次数
    private int reconnectionTime = 0;//重连次数

    private String serverIp;//服务端IP
    private int DEFAULT_PORT = 3876;//服务端端口

    public WifiSocketClient(String serverIp, WifiDirectNode.CommonCallback callback) {
        this.serverIp = serverIp;
        commonCallback = callback;
        initManager();
    }

    private SocketActionAdapter adapter = new SocketActionAdapter() {

        /**
         * Socket连接成功回调
         * @param info
         * @param action
         */
        @Override
        public void onSocketConnectionSuccess(ConnectionInfo info, String action) {
            Log.i(TAG, "连接成功(Connecting Successful)");
            mManager.send(new HandShakeBean());//发送握手信息
            mManager.getPulseManager().setPulseSendable(new PulseBean());//心跳检测
            reconnectionTime = 0;
        }

        /**
         * Socket连接状态由连接->断开回调
         * @param info
         * @param action
         * @param e
         */
        @Override
        public void onSocketDisconnection(ConnectionInfo info, String action, Exception e) {
            if (e != null) {
                Log.e(TAG, "异常断开(Disconnected with exception):" + e.getMessage());
                e.printStackTrace();
            } else {
                Log.e(TAG, "正常断开(Disconnect Manually)");
            }
            if(commonCallback != null){
                commonCallback.onSuccess("disconnection", null);
            }
            reconnectionTime = 0;
        }

        /**
         * Socket连接失败回调
         * @param info
         * @param action
         * @param e
         */
        @Override
        public void onSocketConnectionFailed(ConnectionInfo info, String action, Exception e) {
            Log.e(TAG, "连接失败(Connecting Failed)" + e.getMessage());
            Log.e(TAG, "异常信息为：" + e.toString());
            e.printStackTrace();
            if(reconnectionTime < MAX_RECONNECTION_TIME){
                Log.i(TAG, "进行第" + reconnectionTime + 1 + "次重连");
                ConnectionInfo redirectInfo = new ConnectionInfo(serverIp, DEFAULT_PORT);
                redirectInfo.setBackupInfo(mInfo.getBackupInfo());
                mManager.switchConnectionInfo(redirectInfo);
                mManager.connect();
                reconnectionTime ++;
            } else {
                Log.i(TAG, "三次重连失败");
                reconnectionTime = 0;
            }
        }

        @Override
        public void onSocketIOThreadStart(String action) {
            super.onSocketIOThreadStart(action);
        }

        @Override
        public void onSocketIOThreadShutdown(String action, Exception e) {
            super.onSocketIOThreadShutdown(action, e);
        }

        /**
         * Socket从服务器读取到字节回调
         * @param info
         * @param action
         * @param data
         */
        @Override
        public void onSocketReadResponse(ConnectionInfo info, String action, OriginalData data) {
            String str = new String(data.getBodyBytes(), Charset.forName("utf-8"));
            try {
                JsonObject jsonObject = new JsonParser().parse(str).getAsJsonObject();
                int cmd = jsonObject.get("cmd").getAsInt();
                switch (cmd) {//登陆成功
                    case 54: {
                        String handshake = jsonObject.get("handshake").getAsString();
                        Log.i(TAG, "握手成功! 握手信息(Handshake Success):" + handshake + ". 开始心跳(Start Heartbeat)..");
                        break;
                    }
                    case 14: {
                        Log.i(TAG, "收到心跳,喂狗成功(Heartbeat Received,Feed the Dog)");
                        mManager.getPulseManager().feed();
                        break;
                    }
                    case 100: {
                        String content = jsonObject.get("content").getAsString();
                        Log.i(TAG, content);
                        break;
                    }
                    case 200: {
                        mManager.send(new CallbackBean("客户端已接收到文本信息。"));
                        break;
                    }
                    case 201: {
                        Log.i(TAG, "文件接收开始");
                        String fileName = jsonObject.get("fileName").getAsString();
                        String content = jsonObject.get("content").getAsString();
                        byte[] buffer = Base64.decode(content, Base64.DEFAULT);//将文件base64转字节数组
                        String folderPath = Environment.getExternalStorageDirectory().getPath() + "/wifiDirect/" + CommonUtils.getCurrentDate();
                        File file = new File(folderPath);
                        if (!file.exists()) {
                            file.mkdirs();
                            Log.i(TAG, "创建文件夹成功");
                        }
                        String newPath = CommonUtils.getSavePath(folderPath, fileName);
                        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(newPath)));
                        dos.write(buffer, 0, buffer.length);
                        dos.close();
                        Log.i(TAG, "文件接收完成，存储路径为：" + newPath);
                        if (commonCallback != null) {//告知前端已接收完文件
                            JSONObject args = new JSONObject();
                            args.put("filePath", newPath);
                            commonCallback.onSuccess("receivedFile", args);
                        }
                        mManager.send(new CallbackBean("客户端已接收到文件。"));
                        break;
                    }
                    default: {
                        Log.i(TAG, str);
                        break;
                    }
                }
//            } else if (cmd == 57) {//切换,重定向.(暂时无法演示,如有疑问请咨询github)
//                String ip = jsonObject.get("data").getAsString().split(":")[0];
//                int port = Integer.parseInt(jsonObject.get("data").getAsString().split(":")[1]);
//                ConnectionInfo redirectInfo = new ConnectionInfo(ip, port);
//                redirectInfo.setBackupInfo(mInfo.getBackupInfo());
//                mManager.getReconnectionManager().addIgnoreException(RedirectException.class);
//                mManager.disconnect(new RedirectException(redirectInfo));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Socket写给服务器字节后回调
         * @param info
         * @param action
         * @param data
         */
        @Override
        public void onSocketWriteResponse(ConnectionInfo info, String action, ISendable data) {
            byte[] bytes = data.parse();
            bytes = Arrays.copyOfRange(bytes, 4, bytes.length);
            String str = new String(bytes, Charset.forName("utf-8"));
            JsonObject jsonObject = new JsonParser().parse(str).getAsJsonObject();
            int cmd = jsonObject.get("cmd").getAsInt();
            switch (cmd) {
                case 54: {
                    String handshake = jsonObject.get("handshake").getAsString();
                    Log.i(TAG, "发送握手数据(Handshake Sending):" + handshake);
                    mManager.getPulseManager().pulse();
                    break;
                }
                default:
                    Log.i(TAG, str);
            }
        }

        /**
         * 发送心跳后的回调
         * @param info
         * @param data
         */
        @Override
        public void onPulseSend(ConnectionInfo info, IPulseSendable data) {
            byte[] bytes = data.parse();
            bytes = Arrays.copyOfRange(bytes, 4, bytes.length);
            String str = new String(bytes, Charset.forName("utf-8"));
            JsonObject jsonObject = new JsonParser().parse(str).getAsJsonObject();
            int cmd = jsonObject.get("cmd").getAsInt();
            if (cmd == 14) {
                Log.i(TAG, "发送心跳包(Heartbeat Sending)");
            }
        }
    };

    private void initManager() {
        final Handler handler = new Handler(Looper.getMainLooper());
        mInfo = new ConnectionInfo(serverIp, DEFAULT_PORT);
        SLog.setIsDebug(true);
        mOkOptions = new OkSocketOptions.Builder()
//                .setReconnectionManager(OkSocketOptions.getDefault().getReconnectionManager())
                .setReconnectionManager(new NoneReconnect())
                .setConnectTimeoutSecond(10)
                .setMaxReadDataMB(10)
                .setCallbackThreadModeToken(new OkSocketOptions.ThreadModeToken() {
                    @Override
                    public void handleCallbackEvent(ActionDispatcher.ActionRunnable runnable) {
                        handler.post(runnable);
                    }
                })
                .build();
        mManager = OkSocket.open(mInfo).option(mOkOptions);
        mManager.registerReceiver(adapter);
        mManager.connect();
        Log.i(TAG, "初始化客户端成功，并成功连接：" + serverIp + "   " + DEFAULT_PORT);
    }

    /**
     * 建立socket连接
     */
    public void connect() {
        if (mManager == null) {
            return;
        }
        if (!mManager.isConnect()) {
            mManager.connect();
            Log.i(TAG, "客户端连接成功");
        } else {
            Log.e(TAG, "socket已连接，不可重复连接！");
        }
    }

    /**
     * 断开socket连接
     */
    public void disconnect() {
        if (mManager != null) {
            mManager.disconnect();
//            mManager.unRegisterReceiver(adapter);
            Log.i(TAG, "客户端断开连接成功");
        }
    }

    /**
     * 发送文件数据到server
     * @param fileName
     * @param data
     */
    public void sendFileData(String fileName, String data) {
        if(mManager != null) {
            FileDataBean fileDataBean = new FileDataBean(fileName, "", data);
            mManager.send(fileDataBean);
            Log.i(TAG, "发送文件数据成功");
        } else {
            Log.e(TAG, "发送文件数据失败");
        }
    }

    /**
     * 发送消息数据到server
     * @param data
     */
    public void sendMsgData(String data) {
        if(mManager != null) {
            MsgDataBean msgDataBean = new MsgDataBean(data);
            mManager.send(msgDataBean);
            Log.i(TAG, "发送消息数据成功");
        } else {
            Log.e(TAG, "发送消息数据失败");
        }
    }

}
