
const Libp2p = require('libp2p');
const TCP = require('libp2p-tcp');
const Mplex = require('libp2p-mplex');
const SECIO = require('libp2p-secio');
const Gossipsub = require('libp2p-gossipsub');


export async function createWifiPeer() {
    const node = await Libp2p.create({
        addresses: {
            listen: ['/ip4/0.0.0.0/tcp/0']
        },
        modules: {
            transport: [TCP],
            streamMuxer: [Mplex],
            connEncryption: [SECIO],
            pubsub: Gossipsub
        }
    });

    await node.start();
    return node;
}
