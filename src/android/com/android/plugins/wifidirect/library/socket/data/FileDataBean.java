package com.android.plugins.wifidirect.library.socket.data;

import com.xuhao.didi.core.iocore.interfaces.ISendable;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * 文件数据对象
 */
public class FileDataBean implements ISendable {

    private String content = "";

    public FileDataBean(String fileName, String filePath, String base64Code){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("cmd", 201);//文件类型
            jsonObject.put("fileName", fileName);//文件名
            jsonObject.put("filePath", filePath);//文件路径
            jsonObject.put("content", base64Code);//文件base64编码
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
