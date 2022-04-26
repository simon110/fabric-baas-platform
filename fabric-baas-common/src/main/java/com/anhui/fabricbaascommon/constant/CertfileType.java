package com.anhui.fabricbaascommon.constant;

public class CertfileType {
    public static final String ADMIN = "admin";
    public static final String CLIENT = "client";
    public static final String ORDERER = "orderer";
    public static final String PEER = "peer";
    public static final String[] ALL = {ADMIN, CLIENT, ORDERER, PEER};

    public static boolean exists(String type) {
        return ADMIN.equals(type) ||
                CLIENT.equals(type) ||
                ORDERER.equals(type) ||
                PEER.equals(type);
    }
}