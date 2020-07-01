// Type definitions for wifi_direct.js
// Project: [LIBRARY_URL_HERE] 
// Definitions by: [YOUR_NAME_HERE] <[YOUR_URL_HERE]> 
// Definitions: https://github.com/borisyankov/DefinitelyTyped

/**
 * 设备对象类型
 */
declare interface PeerObject {
    address: string,        //设备地址
    deviceName: string,     //设备名称
    status: number          //设备状态（0=已连接，1=已请求，2=连接失败，3=可用的，4=不可用的）
}
/**
 * wifi-direct接口
 */
declare class WifiDirect {
		
	/**
	 * 构造函数
	 */
	constructor();

	/**
     * 注册监听事件
     * @param key       监听事件名
     * @param callback  回调函数
     */
    on(key : string, callback : Function): void;
		
	/**
	 * 开始搜索设备
	 */
	startDiscovering(): void;

	/**
	 * 停止搜索设备
	 */
	stopDiscovering(): void;
		
	/**
	 * 连接设备
	 * @param peer  设备对象
	 */
	connect(peer : PeerObject): void;
		
	/**
	 * 断开连接
	 */
	disconnect(): void;
		
	/**
	 * 停止并退出
	 */
	shutdown(): void;
		
	/**
	 * 创建服务器
	 */
	createServerReceiveFile(): void;
		
	/**
	 * 发送文件给服务器
	 * @param fileName  文件名
	 * @param dataURL   文件的base64编码
	 */
	sendFileToServer(fileName : string, dataURL : string): void;
		
	/**
	 * 发送文件给客户端
	 * @param uniqueTag 客户端标签（唯一标识）
	 * @param fileName  文件名
	 * @param dataURL   文件的base64编码
	 */
	sendFileToClient(uniqueTag : string, fileName : string, dataURL : string): void;
}
