package com.anhui.fabricbaasttp.util;

public class IdentifierGenerator {
    public static String peerIdentifier(String channelName, int peerNo) {
        return String.format("%s-Peer%d", channelName, peerNo);
    }

    public static String channelIdentifier(String networkName, String channelName) {
        return String.format("%s-%s", networkName, channelName);
    }

    public static String ordererIdentifier(String networkName, int ordererNo) {
        return String.format("%s-Orderer%d", networkName, ordererNo);
    }

    public static String orgCertIdentifier(String networkName, String orgName) {
        return String.format("%s-%s", networkName, orgName);
    }
}

