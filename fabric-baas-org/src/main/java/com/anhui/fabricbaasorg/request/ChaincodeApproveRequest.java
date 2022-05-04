package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaasorg.entity.ApprovedChaincodeEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "链码投票请求")
public class ChaincodeApproveRequest extends ApprovedChaincodeEntity {

}
