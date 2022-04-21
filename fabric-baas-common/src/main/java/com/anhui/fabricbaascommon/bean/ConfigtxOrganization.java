package com.anhui.fabricbaascommon.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfigtxOrganization {
    private String name;
    private String id;
    private String mspDir;
}

