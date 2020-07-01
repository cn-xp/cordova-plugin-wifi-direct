package com.android.plugins.wifidirect.library.utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonUtils {

    /**
     * 获取当前时间yyyyMMdd
     * @return
     */
    public static String getCurrentDate(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        Date date = new Date();
        String currentDate = simpleDateFormat.format(date);
        return currentDate;
    }

    /**
     * 获取文件需要保存的路径
     * @param folderPath
     * @param fileName
     * @return
     */
    public static String getSavePath(String folderPath, String fileName){
        int length = fileName.length();//文件名长度
        int index = fileName.lastIndexOf(".");
        String name = fileName.substring(0, index);//文件基础名
        String extension = fileName.substring(index + 1, length);//文件扩展名
        File file;
        String savePath;
        int nameIndex = 0;
        do{
            savePath = folderPath;
            if(nameIndex > 0){//重名则文件名加上(数字)的后缀
                savePath += "/" + name + "(" + nameIndex + ")" + "." + extension;
            }else{
                savePath += "/" + fileName;
            }
            file = new File(savePath);
            nameIndex ++;
        }while (file.exists());
        return savePath;
    }
}
