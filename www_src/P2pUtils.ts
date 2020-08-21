/// <reference path="./libp2p.d.ts" />

const { Buffer } = require('buffer');
const Libp2p = require('libp2p');
const TCP = require('libp2p-tcp');
const Mplex = require('libp2p-mplex');
const SECIO = require('libp2p-secio');
const Gossipsub = require('libp2p-gossipsub');
import { createWifiPeer } from './wifiP2pBroadcast';

const BROADCAST_TOPIC = Symbol('bftchain-broadcast-topic');
const BLOCKCHAIN_SYNC_TOPIC = Symbol('bftchain-blockchain-sync-topic');

export class P2pUtils {

    ownServerPeer: Libp2pType;
    ownClientPeer: Libp2pType;

    //创建服务节点
    createServerPeer() {
        const serverPeer: Promise<Libp2pType> = createWifiPeer();
        serverPeer.then(value => {
            this.ownServerPeer = value;
        });
    }

    //加入到p2p网络中
    joinP2pNet(serverNode: Libp2pType) {
        const peer : Promise<Libp2pType> = createWifiPeer();

        peer.then(async value => {
            this.ownClientPeer = value;
            await this._addPeerStore(serverNode);
            await this._broadcastEventsSubscribe();
        });
    }

    publish(){
        this.ownServerPeer.pubsub.publish(BROADCAST_TOPIC, Buffer.from('bird bird bird bird!'));
    }

    //节点存储
    async _addPeerStore(serverNode: Libp2pType) {
        this.ownClientPeer.peerStore.addressBook.set(serverNode.peerId, serverNode.multiaddrs);
        await this.ownClientPeer.dial(serverNode.peerId);
    }

    //广播事件订阅
    async _broadcastEventsSubscribe() {
        await this.ownClientPeer.pubsub.subscribe(BROADCAST_TOPIC, (msg) => {
            console.log(`received: ${msg.data.toString()}`)
        });
    }

    //区块同步订阅
    async _blockchainSyncSubscribe() {
        await this.ownClientPeer.pubsub.subscribe(BLOCKCHAIN_SYNC_TOPIC, (msg) => {
            console.log(`received: ${msg.data.toString()}`)
        });
    }
}
