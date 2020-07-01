package com.android.plugins.wifidirect.library.socket.server.callback;

import android.util.Log;
import com.android.plugins.wifidirect.library.WifiDirectNode;
import com.android.plugins.wifidirect.library.socket.data.MsgDataBean;
import com.android.plugins.wifidirect.library.socket.server.IServerCtrl;
import com.android.plugins.wifidirect.library.socket.server.WatchdogThread;
import com.android.plugins.wifidirect.library.socket.data.ClientInfoBean;
import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.socket.client.sdk.OkSocket;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerReceiver implements IServerActionListener, IServerCtrl {

    private final String TAG = "ServerReceiver";

    private volatile IServerManager mIServerManager;

    private volatile int mPort;

    private ConcurrentHashMap<String, ClientInfoBean> mClientInfoBeanList = new ConcurrentHashMap<>();

    private WatchdogThread mWatchdogThread = null;

    private WifiDirectNode.CommonCallback commonCallback;


    public ServerReceiver(int port, WifiDirectNode.CommonCallback callback) {
        mPort = port;
        commonCallback = callback;
    }

    public void setServerManager(IServerManager serverManager) {
        mIServerManager = serverManager;
    }

    public IServerManager getmIServerManager() {
        return mIServerManager;
    }

    /**
     * 服务端启动的回调函数
     * @param serverPort
     */
    @Override
    public void onServerListening(int serverPort) {
        mClientInfoBeanList.clear();
        if (mWatchdogThread != null && !mWatchdogThread.isShutdown()) {
            mWatchdogThread.shutdown();
        }
        Log.i(TAG, "服务器启动完成.正在监听端口:" + serverPort);
        mWatchdogThread = new WatchdogThread(mClientInfoBeanList);
        mWatchdogThread.start();
    }

    /**
     * 客户端建立连接的回调函数
     * @param client
     * @param serverPort
     * @param clientPool
     */
    @Override
    public void onClientConnected(IClient client, int serverPort, IClientPool clientPool) {
        ClientInfoBean bean = new ClientInfoBean(client, ServerReceiver.this);
        bean.setCreateTime(System.currentTimeMillis());
        client.addIOCallback(new ClientIOCallback(bean, commonCallback));

        synchronized (mClientInfoBeanList) {
            mClientInfoBeanList.put(client.getUniqueTag(), bean);
        }
        Log.i(TAG, client.getUniqueTag() + " 上线,客户端数量:" + getClientNum());
//        sendToAdmin(new MsgWriteBean(client.getHostIp() + " 主机已上线, 客户端数量" + getClientNum()));
        if(commonCallback != null) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("uniqueTag", client.getUniqueTag());
                commonCallback.onSuccess("connection", jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 客户端断开连接的回调函数
     * @param client
     * @param serverPort
     * @param clientPool
     */
    @Override
    public void onClientDisconnected(IClient client, int serverPort, IClientPool clientPool) {
        client.removeAllIOCallback();
        synchronized (mClientInfoBeanList) {
            mClientInfoBeanList.remove(client.getUniqueTag());
        }
        Log.i(TAG, client.getUniqueTag() + " 下线,客户端数量:" + getClientNum());
//        sendToAdmin(new MsgWriteBean(client.getHostIp() + " 主机已下线, 客户端数量" + getClientNum()));
        if(commonCallback != null){
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("uniqueTag", client.getUniqueTag());
                commonCallback.onSuccess("disconnection", jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 服务端即将关闭的回调函数
     * @param serverPort
     * @param shutdown
     * @param clientPool
     * @param throwable
     */
    @Override
    public void onServerWillBeShutdown(int serverPort, IServerShutdown shutdown, IClientPool clientPool, Throwable throwable) {
        clientPool.sendToAll(new MsgDataBean("服务器即将关闭"));
        if (throwable == null) {
            Log.i(TAG, "服务器即将关闭,没有异常");
        } else {
            Log.i(TAG, "服务器即将关闭,异常信息:" + throwable.getMessage());
            throwable.printStackTrace();
        }
        mWatchdogThread.shutdown();
        shutdown.shutdown();
    }

    /**
     * 服务端已经关闭的回调函数
     * @param serverPort
     */
    @Override
    public void onServerAlreadyShutdown(int serverPort) {
        mClientInfoBeanList.clear();
        OkSocket.server(serverPort).unRegisterReceiver(this);
        Log.i(TAG, "服务器已经关闭");
    }

    public int getClientNum() {
        synchronized (mClientInfoBeanList) {
//            int num = 0;
//            Map<String, ClientInfoBean> map = new HashMap<>(mClientInfoBeanList);
//            Iterator<String> it = map.keySet().iterator();
//            while (it.hasNext()) {
//                String key = it.next();
//                ClientInfoBean clientInfoBean = map.get(key);
//                if (!clientInfoBean.isAdministrator()) {
//                num++;
//                }
//            }
            return mClientInfoBeanList.size();
        }
    }


    @Override
    public void sendToClient(String uniqueTag, ISendable sendable) {
        synchronized (mClientInfoBeanList) {
            Log.i(TAG, "服务端发送文件给客户端：" + uniqueTag + ", 客户端数量：" + mClientInfoBeanList);
            Map<String, ClientInfoBean> map = new HashMap<>(mClientInfoBeanList);
            Iterator<String> iterator = map.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                ClientInfoBean clientInfoBean = map.get(key);
                if (uniqueTag != null && uniqueTag.equals(clientInfoBean.getIClient().getUniqueTag())) {
                    Log.i(TAG, "匹配到客户端：" + uniqueTag);
                    clientInfoBean.getIClient().send(sendable);
                    Log.i(TAG, "服务端发送文件给客户端成功：" + uniqueTag);
                }
            }
        }
    }

    @Override
    public void restart(final int port) {
        synchronized (this) {
            Log.i(TAG, "服务器重启中...");
            mIServerManager.shutdown();

            new Thread(() -> {
                Log.i(TAG, "5秒后启动服务器...");
                while (true) {
                    try {
                        Thread.sleep(5000);
                        break;
                    } catch (InterruptedException e) {
                    }
                }
                mIServerManager = OkSocket.server(port).registerReceiver(ServerReceiver.this);
                mPort = port;
                mIServerManager.listen();
            }).start();
        }
    }

}
