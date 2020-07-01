package com.android.plugins.wifidirect.library.socket.data;


import com.xuhao.didi.core.iocore.interfaces.ISendable;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * 文字消息数据对象
 */
public class MsgDataBean implements ISendable {

    private String content = "";

    public MsgDataBean(String data) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("cmd", 200);//文字消息
            jsonObject.put("content", data);
            content = jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] parse() {
        byte[] body = content.getBytes(Charset.defaultCharset());
        ByteBuffer bb = ByteBuffer.allocate(4 + body.length);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(body.length);
        bb.put(body);
        return bb.array();
    }
}
