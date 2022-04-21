package com.anhui.fabricbaascommon.bean;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

@NoArgsConstructor
@Data
public class MSPEnv {
    private String mspId;
    private File mspConfigPath;

    public MSPEnv(String mspId, File mspConfigPath) {
        assert mspConfigPath.isDirectory();
        this.mspId = mspId;
        this.mspConfigPath = mspConfigPath;
    }
}
