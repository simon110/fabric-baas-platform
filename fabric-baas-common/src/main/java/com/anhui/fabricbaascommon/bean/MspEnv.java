package com.anhui.fabricbaascommon.bean;

import cn.hutool.core.lang.Assert;
import com.anhui.fabricbaascommon.exception.CertfileException;
import com.anhui.fabricbaascommon.fabric.CertfileUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

@NoArgsConstructor
@Data
public class MspEnv {
    private String mspId;
    private File mspConfig;

    public MspEnv(String mspId, File mspConfig) {
        Assert.isTrue(mspConfig.isDirectory());
        this.mspId = mspId;
        this.mspConfig = mspConfig;
    }

    public void assertMspCert() throws CertfileException {
        if (!CertfileUtils.checkMSPDir(mspConfig)) {
            throw new CertfileException("非法的MSP证书目录：" + mspConfig);
        }
    }
}
