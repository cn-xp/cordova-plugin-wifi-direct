package com.android.plugins.wifidirect.library.socket.server.callback;

import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import com.android.plugins.wifidirect.library.WifiDirectNode;
import com.android.plugins.wifidirect.library.socket.data.*;
import com.android.plugins.wifidirect.library.utils.CommonUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.core.pojo.OriginalData;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClient;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClientIOCallback;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClientPool;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientIOCallback implements IClientIOCallback {

    private final String TAG = "ClientIOCallback";

    private boolean mNeedDisconnect;

    private ClientInfoBean mClientInfoBean;

    WifiDirectNode.CommonCallback commonCallback;

    public ClientIOCallback(ClientInfoBean bean, WifiDirectNode.CommonCallback callback) {
        mClientInfoBean = bean;
        mNeedDisconnect = false;
        commonCallback = callback;
    }

    /**
     * server从客户端读取到字节回调
     * @param originalData
     * @param client
     * @param clientPool
     */
    @Override
    public void onClientRead(OriginalData originalData, IClient client, IClientPool<IClient, String> clientPool) {
        Log.i(TAG, "开始读取到客户端数据");
        String str = new String(originalData.getBodyBytes(), Charset.forName("utf-8"));
        try {
            JsonObject jsonObject = new JsonParser().parse(str).getAsJsonObject();
            int cmd = jsonObject.get("cmd").getAsInt();
            switch (cmd) {
                case 54: {//握手
                    Log.i(TAG, "开始握手");
                    if (mClientInfoBean.getHandShakeTime() != 0) {
                        client.send(new MsgDataBean("Illegal handshake, error message: duplicate handshake"));
                        mNeedDisconnect = true;
                        return;
                    }
//                    String handshake = jsonObject.get("handshake").getAsString();
//                    if ("admin".equals(handshake)) {
//                        mClientInfoBean.setAdministrator(true);
//                    } else {
//                        mClientInfoBean.setAdministrator(false);
//                    }

                    mClientInfoBean.setHandShakeTime(System.currentTimeMillis());
                    client.send(new OriginalWriteBean(originalData));
                    break;
                }
//                case 911: {//重启
//                    if (mClientInfoBean.isAdministrator()) {
//                        int port = jsonObject.get("port").getAsInt();
//                        mClientInfoBean.getmIServerCtrl().restart(port);
//                    } else {
//                        client.send(new MsgWriteBean("Illegal cmd, your not administrator"));
//                        mNeedDisconnect = true;
//                    }
//                    break;
//                }
                case 100: {
                    String content = jsonObject.get("content").getAsString();
                    Log.i(TAG, content);
                    break;
                }
                case 200: {
                    String content = jsonObject.get("content").getAsString();
                    client.send(new CallbackBean(content));
                    break;
                }
                case 201: {
                    Log.i(TAG, "文件接收开始");
                    String fileName = jsonObject.get("fileName").getAsString();
                    String content = jsonObject.get("content").getAsString();
                    byte[] buffer = Base64.decode(content,Base64.DEFAULT);//将文件base64转字节数组
                    String folderPath = Environment.getExternalStorageDirectory().getPath() + "/wifiDirect/" + CommonUtils.getCurrentDate();
                    File file = new File(folderPath);
                    if(!file.exists()){
                        file.mkdirs();
                        Log.i(TAG, "创建文件夹成功");
                    }
                    String newPath =  CommonUtils.getSavePath(folderPath, fileName);
                    DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(newPath)));
                    dos.write(buffer, 0, buffer.length);
                    dos.close();
                    Log.i(TAG, "文件接收完成，存储路径为：" + newPath);
                    if(commonCallback != null){//告知前端已接收完文件
                        JSONObject args = new JSONObject();
                        args.put("uniqueTag", client.getUniqueTag());
                        args.put("filePath", newPath);
                        commonCallback.onSuccess("receivedFile", args);
                    }
                    client.send(new CallbackBean("服务端已接收到文件。"));
                    break;
                }
                default: {
                    client.send(new OriginalWriteBean(originalData));
                    break;
                }
//                case 912: {//踢出客户端
//                    if (mClientInfoBean.isAdministrator()) {
//                        String who = jsonObject.get("who").getAsString();
//                        IClient whoClient = clientPool.findByUniqueTag(who);
//                        if (whoClient != null) {
//                            mClientInfoBean.getAdminCtrl().sendToAdmin(new MsgWriteBean("成功踢出:"+who));
//                            whoClient.disconnect();
//                        }else{
//                            mClientInfoBean.getAdminCtrl().sendToAdmin(new MsgWriteBean("踢出失败:"+who));
//                        }
//                    } else {
//                        client.send(new MsgWriteBean("Illegal cmd, your not administrator"));
//                        mNeedDisconnect = true;
//                    }
//                    break;
//                }
            }
            mClientInfoBean.setLastActionTime(System.currentTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
            if (mClientInfoBean.getHandShakeTime() == 0) {
                client.send(new MsgDataBean("Illegal handshake, you need handshake first"));
                mNeedDisconnect = true;
                return;
            }
        }
//        if (!mClientInfoBean.isAdministrator()) {
//            mClientInfoBean.getmIServerCtrl().sendToClient(client.getUniqueTag(), new MsgWriteBean(client.getUniqueTag() + "@ " + str));
//        }

    }

    /**
     * server写给客户端字节后回调
     * @param sendable
     * @param client
     * @param clientPool
     */
    @Override
    public void onClientWrite(ISendable sendable, IClient client, IClientPool<IClient, String> clientPool) {
        mClientInfoBean.setLastActionTime(System.currentTimeMillis());
        if (mNeedDisconnect) {
            client.disconnect(new Exception("主动断开,因为判定非法"));
            mClientInfoBean.getmIServerCtrl().sendToClient(client.getHostIp(), new MsgDataBean(client.getUniqueTag() + "非法操作,服务器断开他"));
        }
    }

}
