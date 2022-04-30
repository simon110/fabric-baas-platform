package com.anhui.fabricbaascommon.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfigtxOrganization {
    private String name;
    private String id;
    private File mspDir;

    public ConfigtxOrganization(String organizationName, File mspDir) {
        this.name = organizationName;
        this.id = organizationName;
        this.mspDir = mspDir;
    }
}

