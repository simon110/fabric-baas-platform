package com.anhui.fabricbaasttp.util;

public class IdentifierGenerator {
    public static String ofPeer(String channelName, int peerNo) {
        return String.format("%s-Peer%d", channelName, peerNo);
    }

    public static String ofOrderer(String networkName, int ordererNo) {
        return String.format("%s-Orderer%d", networkName, ordererNo);
    }

    public static String ofChannel(String networkName, String channelName) {
        return String.format("%s-%s", networkName, channelName);
    }

    public static String ofCertfile(String networkName, String organizationName) {
        return String.format("%s-%s", networkName, organizationName);
    }
}

