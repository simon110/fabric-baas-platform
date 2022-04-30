package com.anhui.fabricbaascommon.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfigtxOrderer {
    private String host;
    private int port;
    private File serverTlsCert;
    private File clientTlsCert;

    public String getAddr() {
        return host + ":" + port;
    }

    public ConfigtxOrderer(Node node, File tlsCert) {
        this.host = node.getHost();
        this.port = node.getPort();
        this.serverTlsCert = tlsCert;
        this.clientTlsCert = tlsCert;
    }
}
