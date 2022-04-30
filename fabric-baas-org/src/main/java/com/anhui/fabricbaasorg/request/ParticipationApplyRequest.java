package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.request.BaseNetworkRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "加入网络请求")
public class ParticipationApplyRequest extends BaseNetworkRequest {

    @NotEmpty
    @ApiModelProperty(value = "预计提供Orderer节点端口", required = true)
    private List<Integer> ordererPorts;

    @NotBlank
    @ApiModelProperty(value = "申请描述信息", required = true)
    private String description;
}

