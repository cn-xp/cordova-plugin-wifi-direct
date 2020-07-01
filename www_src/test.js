/// <reference path="./wifi_direct.d.ts" />
var wifiDirect = new WifiDirect();
wifiDirect.startDiscovering();
wifiDirect.on("aaa", function (bb) { console.log(bb); });
var param = { address: '11', deviceName: '22', primaryType: '33', status: 1 };
wifiDirect.connect(param);
