"use strict";
/// <reference path="./libp2p.d.ts" />
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var __generator = (this && this.__generator) || function (thisArg, body) {
    var _ = { label: 0, sent: function() { if (t[0] & 1) throw t[1]; return t[1]; }, trys: [], ops: [] }, f, y, t, g;
    return g = { next: verb(0), "throw": verb(1), "return": verb(2) }, typeof Symbol === "function" && (g[Symbol.iterator] = function() { return this; }), g;
    function verb(n) { return function (v) { return step([n, v]); }; }
    function step(op) {
        if (f) throw new TypeError("Generator is already executing.");
        while (_) try {
            if (f = 1, y && (t = op[0] & 2 ? y["return"] : op[0] ? y["throw"] || ((t = y["return"]) && t.call(y), 0) : y.next) && !(t = t.call(y, op[1])).done) return t;
            if (y = 0, t) op = [op[0] & 2, t.value];
            switch (op[0]) {
                case 0: case 1: t = op; break;
                case 4: _.label++; return { value: op[1], done: false };
                case 5: _.label++; y = op[1]; op = [0]; continue;
                case 7: op = _.ops.pop(); _.trys.pop(); continue;
                default:
                    if (!(t = _.trys, t = t.length > 0 && t[t.length - 1]) && (op[0] === 6 || op[0] === 2)) { _ = 0; continue; }
                    if (op[0] === 3 && (!t || (op[1] > t[0] && op[1] < t[3]))) { _.label = op[1]; break; }
                    if (op[0] === 6 && _.label < t[1]) { _.label = t[1]; t = op; break; }
                    if (t && _.label < t[2]) { _.label = t[2]; _.ops.push(op); break; }
                    if (t[2]) _.ops.pop();
                    _.trys.pop(); continue;
            }
            op = body.call(thisArg, _);
        } catch (e) { op = [6, e]; y = 0; } finally { f = t = 0; }
        if (op[0] & 5) throw op[1]; return { value: op[0] ? op[1] : void 0, done: true };
    }
};
exports.__esModule = true;
exports.P2pUtils = void 0;
var Buffer = require('buffer').Buffer;
var Libp2p = require('libp2p');
var TCP = require('libp2p-tcp');
var Mplex = require('libp2p-mplex');
var SECIO = require('libp2p-secio');
var Gossipsub = require('libp2p-gossipsub');
var wifiP2pBroadcast_1 = require("./wifiP2pBroadcast");
var BROADCAST_TOPIC = Symbol('bftchain-broadcast-topic');
var BLOCKCHAIN_SYNC_TOPIC = Symbol('bftchain-blockchain-sync-topic');
var P2pUtils = /** @class */ (function () {
    function P2pUtils() {
    }
    //创建服务节点
    P2pUtils.prototype.createServerPeer = function () {
        var _this = this;
        var serverPeer = wifiP2pBroadcast_1.createWifiPeer();
        serverPeer.then(function (value) {
            _this.ownServerPeer = value;
        });
    };
    //加入到p2p网络中
    P2pUtils.prototype.joinP2pNet = function (serverNode) {
        var _this = this;
        var peer = wifiP2pBroadcast_1.createWifiPeer();
        peer.then(function (value) { return __awaiter(_this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                switch (_a.label) {
                    case 0:
                        this.ownClientPeer = value;
                        return [4 /*yield*/, this._addPeerStore(serverNode)];
                    case 1:
                        _a.sent();
                        return [4 /*yield*/, this._broadcastEventsSubscribe()];
                    case 2:
                        _a.sent();
                        return [2 /*return*/];
                }
            });
        }); });
    };
    P2pUtils.prototype.publish = function () {
        this.ownServerPeer.pubsub.publish(BROADCAST_TOPIC, Buffer.from('bird bird bird bird!'));
    };
    //节点存储
    P2pUtils.prototype._addPeerStore = function (serverNode) {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                switch (_a.label) {
                    case 0:
                        this.ownClientPeer.peerStore.addressBook.set(serverNode.peerId, serverNode.multiaddrs);
                        return [4 /*yield*/, this.ownClientPeer.dial(serverNode.peerId)];
                    case 1:
                        _a.sent();
                        return [2 /*return*/];
                }
            });
        });
    };
    //广播事件订阅
    P2pUtils.prototype._broadcastEventsSubscribe = function () {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                switch (_a.label) {
                    case 0: return [4 /*yield*/, this.ownClientPeer.pubsub.subscribe(BROADCAST_TOPIC, function (msg) {
                            console.log("received: " + msg.data.toString());
                        })];
                    case 1:
                        _a.sent();
                        return [2 /*return*/];
                }
            });
        });
    };
    //区块同步订阅
    P2pUtils.prototype._blockchainSyncSubscribe = function () {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                switch (_a.label) {
                    case 0: return [4 /*yield*/, this.ownClientPeer.pubsub.subscribe(BLOCKCHAIN_SYNC_TOPIC, function (msg) {
                            console.log("received: " + msg.data.toString());
                        })];
                    case 1:
                        _a.sent();
                        return [2 /*return*/];
                }
            });
        });
    };
    return P2pUtils;
}());
exports.P2pUtils = P2pUtils;
