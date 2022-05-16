package com.anhui.fabricbaascommon.bean;

import lombok.Data;

@Data
public class ChannelInfo {
    private String channelName;
    private String currentBlockHash;
    private String previousBlockHash;
    private int height;
}
