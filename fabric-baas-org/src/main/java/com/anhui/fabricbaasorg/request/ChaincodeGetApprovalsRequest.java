package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.bean.ApprovedChaincode;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "链码投票情况查询请求")
public class ChaincodeGetApprovalsRequest extends ApprovedChaincode {
}
