package com.anhui.fabricbaascommon.bean;

import lombok.Data;

@Data
public class ChaincodeApproval {
    private String organizationName;
    private boolean isApproved;
}
