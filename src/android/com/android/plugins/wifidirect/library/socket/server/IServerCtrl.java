package com.android.plugins.wifidirect.library.socket.server;

import com.xuhao.didi.core.iocore.interfaces.ISendable;

public interface IServerCtrl {

    /**
     * 发送数据给客户端
     * @param uniqueTag
     * @param sendable
     */
    void sendToClient(String uniqueTag, ISendable sendable);

//    /**
//     * 发送文件数据给客户端
//     * @param uniqueTag
//     * @param sendable
//     */
//    void sendFileToClient(String uniqueTag, ISendable sendable);

    /**
     * 重启服务端
     * @param port
     */
    void restart(int port);

}
