package com.android.plugins.wifidirect.library.socket.server;

import android.util.Log;
import com.android.plugins.wifidirect.library.WifiDirectNode;
import com.android.plugins.wifidirect.library.socket.data.FileDataBean;
import com.android.plugins.wifidirect.library.socket.server.callback.ServerReceiver;
import com.xuhao.didi.core.utils.SLog;
import com.xuhao.didi.socket.client.sdk.OkSocket;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IServerManager;

public class WifiSocketServer {

    private final String TAG = "WifiSocketServer";

    private ServerReceiver serverReceiver;
    private int DEFAULT_PORT = 3876;

    public WifiSocketServer(WifiDirectNode.CommonCallback callback) {
        SLog.setIsDebug(true);
        serverReceiver = new ServerReceiver(DEFAULT_PORT, callback);
        IServerManager serverManager = OkSocket.server(DEFAULT_PORT)
                .registerReceiver(serverReceiver);
        serverReceiver.setServerManager(serverManager);
        serverManager.listen();
        Log.i(TAG, "建立服务端成功");
    }

    public void disconnect() {
        serverReceiver.getmIServerManager().shutdown();
    }

    /**
     * 发送文件给客户端
     * @param uniqueTag
     * @param fileName
     * @param data
     */
    public void sendFileData(String uniqueTag, String fileName, String data) {
        FileDataBean fileDataBean = new FileDataBean(fileName, "", data);
        serverReceiver.sendToClient(uniqueTag, fileDataBean);
    }

}
