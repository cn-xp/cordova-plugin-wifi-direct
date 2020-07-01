'use strict';
const exec = require('cordova/exec');

/*class WifiDirect {

  private _Instance: WifiDirectNode;

  public getInstance(type, domain, name, port = 32051, props, success, failure) {
    return exec(hash => {
      if (!this._Instance || this._Instance.hash !== hash) {
        this._Instance = new WifiDirectNode(type, domain, name, port, props, hash);
      }
      success(this._Instance);
    }, failure, 'WifiDirect', 'getInstance', [type, domain, name, port, props]);
  }
} */

//监听事件枚举
enum EventCode{
    DISCOVERED = "discovered",//发现设备列表
    STOP_DISCOVERING = "stopDiscovering",//停止搜索设备
    CONNECTION = "connection",//连接设备
    DIS_CONNECTION = "disconnection",//断开连接
    RECEIVED_FILE = "receivedFile",//接收文件成功
    SEND_FILE = "sendFile",//发送文件完成
    SHUT_DOWN = "shutdown",//停止并退出
    CREATED_SERVER = "createdServer",//创建服务器成功
}

//所有监听的事件名列表
var EVENT_LIST: Array<string> = ['discovered', 'stopDiscovering', 'connection', 'disconnection', 'sendFile', 'shutdown', 'receivedFile', 'createdServer'];
//监听事件触发的方法集
var functionListMap = {};

/**
 * 触发监听事件的方法
 * @param key       监听事件名
 * @param params    事件的参数
 */
function emit(key, ...params) {
    if(functionListMap.hasOwnProperty(key)){
        let functionList = functionListMap[key];
        functionList.forEach(func => func(...params));
    }
}

/**
 * cordova.exec执行失败的方法
 * @param e         异常
 */
function _error(e): void {
    console.error('Wifi Direct Error:' + e);
}
/**
 * 搜索设备执行成功的方法
 * @param result 执行结果（设备对象列表）
 */
function _successDiscovered(result) {
    emit(EventCode.DISCOVERED, result);
}

/**
 * 停止搜索设备执行成功的方法
 * @param result 执行结果
 */
function _successStopDiscovering(result) {
    emit(EventCode.STOP_DISCOVERING, result);
}

/**
 * 连接设备执行成功的方法
 * @param result 执行结果
 */
function _successConnection(result) {
    if(result.operationType == 'connection') {//连接服务器成功
        emit(EventCode.CONNECTION, result);
    } else if(result.operationType == 'receivedFile') {//接收文件成功
        emit(EventCode.RECEIVED_FILE, result);
    } else if(result.operationType == 'disconnection') {//连接断开
        emit(EventCode.DIS_CONNECTION, result);
    }
}

/**
 * 断开连接执行成功的方法
 * @param result 执行结果
 */
function _successDisConnection(result) {
    emit(EventCode.DIS_CONNECTION, result);
}

/**
 * 创建服务器执行成功的方法
 * @param result 执行结果
 */
function _successCreateServer(result) {
    if(result.operationType == 'createdServer') {//创建服务器成功
        emit(EventCode.CREATED_SERVER, result);
    } else if(result.operationType == 'connection') {//客户端连接成功
        emit(EventCode.CONNECTION, result);
    } else if(result.operationType == 'receivedFile') {//接收文件成功
        emit(EventCode.RECEIVED_FILE, result);
    } else if(result.operationType == 'disconnection') {//客户端关闭
        emit(EventCode.DIS_CONNECTION, result);
    }
}

/**
 * 发送文件执行成功的方法
 * @param result 执行结果
 */
function _successSendFile(result) {
    emit(EventCode.SEND_FILE, result);
}

/**
 * 停止并退出执行成功的方法
 * @param result 执行结果
 */
function _successShutdown(result) {
    emit(EventCode.SHUT_DOWN, result);
}

//wifi-direct接口
class WifiDirect{

  constructor(){
    exec(null, _error, 'WifiDirect', 'getInstance', []);
  }

	/**
     * 注册监听事件
     * @param key       监听事件名
     * @param callback  回调函数
     */
    on(key, callback): void{
        if(!EVENT_LIST.includes(key)){
            console.error('the event listener [' + key + '] is not occured in the list');
            return;
        }
        if(typeof callback !== 'function'){
            console.error('the callback must be a function');
            return;
        }
        if(functionListMap.hasOwnProperty(key)){
            let functionList = functionListMap[key];
            functionList.push(callback);
            functionListMap[key] = functionList;
        }else{
            let functionList = [];
            functionList.push(callback);
            functionListMap[key] = functionList;
        }
    }

	/**
	 * 开始搜索设备
	 */
    startDiscovering(): void {
        return exec(_successDiscovered, _error, 'WifiDirect', 'startDiscovering', []);
    }

	/**
	 * 停止搜索设备
	 */
    stopDiscovering(): void {
        return exec(_successStopDiscovering, _error, 'WifiDirect', 'stopDiscovering', []);
    }

	/**
	 * 连接设备
	 * @param peer  设备对象
	 */
    connect(peer): void {
        return exec(_successConnection, _error, 'WifiDirect', 'connect', [peer]);
    }

	/**
	 * 断开连接
	 */
    disconnect(): void {
        return exec(_successDisConnection, _error, 'WifiDirect', 'disconnect', []);
    }

	/**
	 * 停止并退出
	 */
    shutdown() {
        return exec(_successShutdown, _error, 'WifiDirect', 'shutdown', []);
    }

	/**
	 * 创建服务器
	 */
    createServer(): void {
        return exec(_successCreateServer, _error, 'WifiDirect', 'createServer', []);
    }

	/**
	 * 发送文件给服务器
	 * @param fileName  文件名
	 * @param dataURL   文件的base64编码
	 */
    sendFileToServer(fileName, dataURL): void {
        return exec(_successSendFile, _error, 'WifiDirect', 'sendFileToServer', [fileName, dataURL]);
    }

    /**
     * 发送文件给客户端
     * @param uniqueTag 客户端标签（唯一标识）
     * @param fileName  文件名
     * @param dataURL   文件的base64编码
     */
    sendFileToClient(uniqueTag, fileName, dataURL): void {
        return exec(_successSendFile, _error, 'WifiDirect', 'sendFileToClient', [uniqueTag, fileName, dataURL]);
    }
}

module.exports = WifiDirect;