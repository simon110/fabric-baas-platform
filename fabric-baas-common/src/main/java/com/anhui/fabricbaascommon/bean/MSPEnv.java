package com.anhui.fabricbaascommon.bean;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

@NoArgsConstructor
@Data
public class MSPEnv {
    private String mspId;
    private File mspConfig;

    public MSPEnv(String mspId, File mspConfig) {
        assert mspConfig.isDirectory();
        this.mspId = mspId;
        this.mspConfig = mspConfig;
    }
}
