Android Wifi Direct Cordova Plugin
========

wifi-direct的cordova插件。socket部分使用了OKSocket第三方库来做设备间的双工通信，支持文件传输。

引入插件
--------
在cordova项目下通过命令<cordova plugin add 插件项目的路径>，将插件引入cordova项目中，例如：

```
cordova plugin add https://github.com/wengtaotao/cordova-plugin-wifi-direct.git
```

***支持 Android SDK >= 21***

如何使用
--------
插件对象为：WifiDirect。在app准备就绪后，就可以直接使用这个代码来获取插件对象。例如：
```
onDeviceReady: function() {
    wifiDirectNode = new WifiDirect();     
}
```



## API介绍

###方法

startDiscovering()
---
    开始搜索支持wifi-direct的设备（配合监听事件discovered）。例如：
```
wifiDirectNode.startDiscovering();
```

stopDiscovering()
---
    停止搜索设备（配合监听事件stopDiscovering）。例如：
```
wifiDirectNode.stopDiscovering();
```

connect(peer)
---
    连接设备，参数为设备对象（设备对象从监听事件discovered中得到）（配合监听事件connection）。例如：
```
wifiDirectNode.connect(selectedDevice);
```

disconnect()
---
    断开连接（配合监听事件disconnection）。例如：
```
wifiDirectNode.disconnect();
```

shutdown()
---
    停止并退出（配合监听事件shutdown）。例如：
```
wifiDirectNode.shutdown();
```

createServer ()
---
    创建服务器（配合监听事件createdServer）。例如
```
wifiDirectNode.createServer();
```

sendFileToServer(fileName, dataURL)
---
    发送文件给服务器（配合监听事件sendFile、receivedFile）。例如：
```
wifiDirectNode.sendFileToServer(fileName,dataUrl);
```

sendFileToClient(uniqueTag, fileName, dataURL)
---
    发送文件给客户端（配合监听事件sendFile、receivedFile）。例如：
```
wifiDirectNode.sendFileToClient(uniqueTag,fileName,dataUrl);
```


###事件监听

discovered
---
    搜索设备成功后会触发这个监听事件，回调函数中返回的参数是设备列表。例如：
```
wifiDirectNode.on('discovered', function(result){
     console.log('discovered success.');
     deviceList = result; 
});
```
stopDiscovering
---
	停止搜索设备成功后触发这个监听事件。例如：
```
wifiDirectNode.on('stopDiscovering', function(result){
     console.log('stopDiscovering success.'); 
});
```
connection
---
	连接设备成功后会触发这个监听事件。客户端和服务端的区别在于回调函数返回的结果不同，服务端返回的结果包含客户端的唯一标识uniqueTag。例如：
```
wifiDirectNode.on('connection', function(result){
     if(result.uniqueTag){//服务端才有值         
        uniqueTag = result.uniqueTag;     
        }     
    console.log('connection success.'); 
});
```

disconnection
---
	断开设备成功后会触发这个监听事件。客户端和服务端的区别在于回调函数返回的结果不同，服务端返回的结果包含客户端的唯一标识uniqueTag。例如：
```
wifiDirectNode.on('disconnection', function(result){
    if(result.uniqueTag){//服务端才有值         
        uniqueTag = result.uniqueTag;     
    }     
    console.log('disconnection success.'); 
});
```
receivedFile
---
	接收文件成功后会触发这个监听事件。回调函数返回的结果有文件名（fileName）、文件扩展名（extension）、文件路径（filePath），如果是服务端则多一个客户端唯一标识字段uniqueTag。例如：
```
wifiDirectNode.on('receivedFile', function(result){
    var fileName = result.fileName;     
    var extension = result.extension;
    var filePath = result.filePath;     
    if(result.uniqueTag){         
        uniqueTag = result.uniqueTag;     
    }     
    console.log('receive file success.'); 
});
```
sendFile
---
	发送文件成功后会触发这个监听事件。例如：
```
wifiDirectNode.on('sendFile', function(result){
     console.log('send file success.'); 
});
```
shutdown
---
	停止并退出成功后会触发这个监听事件。例如：
```
wifiDirectNode.on('shutdown', function(result){
     console.log('shutdown success.'); 
});
```
createdServer
---
	创建服务器成功后会触发这个监听事件。例如：
```
wifiDirectNode.on('createdServer', function(result){
     console.log('created server success.'); 
});
```



