package com.anhui.fabricbaasttp.util;

import com.anhui.fabricbaascommon.bean.Node;
import org.apache.commons.lang.CharUtils;

public class IdentifierGenerator {
    private static String deleteNonAlphaChars(String s) {
        StringBuilder builder = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (CharUtils.isAsciiAlpha(c)) {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public static String generatePeerId(String channelName, Node peer) {
        return String.format("%s-%s-%d",
                channelName.toLowerCase(),
                deleteNonAlphaChars(peer.getHost()),
                peer.getPort()
        );
    }

    public static String generateOrdererId(String networkName, Node orderer) {
        return String.format("%s-%s-%d",
                networkName.toLowerCase(),
                deleteNonAlphaChars(orderer.getHost()),
                orderer.getPort()
        );
    }

    public static String generateChannelId(String networkName, String channelName) {
        return String.format("%s-%s", networkName, channelName);
    }

    public static String generateCertfileId(String networkName, String organizationName) {
        return String.format("%s-%s", networkName, organizationName);
    }
}

