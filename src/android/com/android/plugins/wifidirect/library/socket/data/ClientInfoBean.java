package com.android.plugins.wifidirect.library.socket.data;

import com.android.plugins.wifidirect.library.socket.server.IServerCtrl;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClient;

public class ClientInfoBean {
    private long mCreateTime;

    private long mHandShakeTime;

    private IClient mIClient;

    private long mLastActionTime;

//    private boolean isAdministrator;

    private IServerCtrl mIServerCtrl;

    public IServerCtrl getmIServerCtrl() {
        return mIServerCtrl;
    }

    public ClientInfoBean(IClient IClient, IServerCtrl ctrl) {
        mIClient = IClient;
        mIServerCtrl = ctrl;
    }

    public long getCreateTime() {
        return mCreateTime;
    }

    public void setCreateTime(long createTime) {
        mCreateTime = createTime;
    }

    public long getHandShakeTime() {
        return mHandShakeTime;
    }

    public void setHandShakeTime(long handShakeTime) {
        mHandShakeTime = handShakeTime;
    }

//    public boolean isAdministrator() {
//        return isAdministrator;
//    }
//
//    public void setAdministrator(boolean administrator) {
//        isAdministrator = administrator;
//    }

    public IClient getIClient() {
        return mIClient;
    }

    public long getLastActionTime() {
        return mLastActionTime;
    }

    public void setLastActionTime(long lastActionTime) {
        mLastActionTime = lastActionTime;
    }
}
