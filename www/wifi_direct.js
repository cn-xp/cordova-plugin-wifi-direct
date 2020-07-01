'use strict';
var exec = require('cordova/exec');
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
var EventCode;
(function (EventCode) {
    EventCode["DISCOVERED"] = "discovered";
    EventCode["STOP_DISCOVERING"] = "stopDiscovering";
    EventCode["CONNECTION"] = "connection";
    EventCode["DIS_CONNECTION"] = "disconnection";
    EventCode["RECEIVED_FILE"] = "receivedFile";
    EventCode["SEND_FILE"] = "sendFile";
    EventCode["SHUT_DOWN"] = "shutdown";
    EventCode["CREATED_SERVER"] = "createdServer";
})(EventCode || (EventCode = {}));
//所有监听的事件名列表
var EVENT_LIST = ['discovered', 'stopDiscovering', 'connection', 'disconnection', 'sendFile', 'shutdown', 'receivedFile', 'createdServer'];
//监听事件触发的方法集
var functionListMap = {};
/**
 * 触发监听事件的方法
 * @param key       监听事件名
 * @param params    事件的参数
 */
function emit(key) {
    var params = [];
    for (var _i = 1; _i < arguments.length; _i++) {
        params[_i - 1] = arguments[_i];
    }
    if (functionListMap.hasOwnProperty(key)) {
        var functionList = functionListMap[key];
        functionList.forEach(function (func) { return func.apply(void 0, params); });
    }
}
/**
 * cordova.exec执行失败的方法
 * @param e         异常
 */
function _error(e) {
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
    if (result.operationType == 'connection') { //连接服务器成功
        emit(EventCode.CONNECTION, result);
    }
    else if (result.operationType == 'receivedFile') { //接收文件成功
        emit(EventCode.RECEIVED_FILE, result);
    }
    else if (result.operationType == 'disconnection') { //连接断开
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
    if (result.operationType == 'createdServer') { //创建服务器成功
        emit(EventCode.CREATED_SERVER, result);
    }
    else if (result.operationType == 'connection') { //客户端连接成功
        emit(EventCode.CONNECTION, result);
    }
    else if (result.operationType == 'receivedFile') { //接收文件成功
        emit(EventCode.RECEIVED_FILE, result);
    }
    else if (result.operationType == 'disconnection') { //客户端关闭
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
var WifiDirect = /** @class */ (function () {
    function WifiDirect() {
        exec(null, _error, 'WifiDirect', 'getInstance', []);
    }
    /**
     * 注册监听事件
     * @param key       监听事件名
     * @param callback  回调函数
     */
    WifiDirect.prototype.on = function (key, callback) {
        if (!EVENT_LIST.includes(key)) {
            console.error('the event listener [' + key + '] is not occured in the list');
            return;
        }
        if (typeof callback !== 'function') {
            console.error('the callback must be a function');
            return;
        }
        if (functionListMap.hasOwnProperty(key)) {
            var functionList = functionListMap[key];
            functionList.push(callback);
            functionListMap[key] = functionList;
        }
        else {
            var functionList = [];
            functionList.push(callback);
            functionListMap[key] = functionList;
        }
    };
    /**
     * 开始搜索设备
     */
    WifiDirect.prototype.startDiscovering = function () {
        return exec(_successDiscovered, _error, 'WifiDirect', 'startDiscovering', []);
    };
    /**
     * 停止搜索设备
     */
    WifiDirect.prototype.stopDiscovering = function () {
        return exec(_successStopDiscovering, _error, 'WifiDirect', 'stopDiscovering', []);
    };
    /**
     * 连接设备
     * @param peer  设备对象
     */
    WifiDirect.prototype.connect = function (peer) {
        return exec(_successConnection, _error, 'WifiDirect', 'connect', [peer]);
    };
    /**
     * 断开连接
     */
    WifiDirect.prototype.disconnect = function () {
        return exec(_successDisConnection, _error, 'WifiDirect', 'disconnect', []);
    };
    /**
     * 停止并退出
     */
    WifiDirect.prototype.shutdown = function () {
        return exec(_successShutdown, _error, 'WifiDirect', 'shutdown', []);
    };
    /**
     * 创建服务器
     */
    WifiDirect.prototype.createServer = function () {
        return exec(_successCreateServer, _error, 'WifiDirect', 'createServer', []);
    };
    /**
     * 发送文件给服务器
     * @param fileName  文件名
     * @param dataURL   文件的base64编码
     */
    WifiDirect.prototype.sendFileToServer = function (fileName, dataURL) {
        return exec(_successSendFile, _error, 'WifiDirect', 'sendFileToServer', [fileName, dataURL]);
    };
    /**
     * 发送文件给客户端
     * @param uniqueTag 客户端标签（唯一标识）
     * @param fileName  文件名
     * @param dataURL   文件的base64编码
     */
    WifiDirect.prototype.sendFileToClient = function (uniqueTag, fileName, dataURL) {
        return exec(_successSendFile, _error, 'WifiDirect', 'sendFileToClient', [uniqueTag, fileName, dataURL]);
    };
    return WifiDirect;
}());
module.exports = WifiDirect;
