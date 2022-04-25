package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.util.List;

@Data
@ApiModel(value = "加入网络请求")
public class ParticipationApplyRequest {
    @Pattern(regexp = ParamPattern.NETWORK_NAME_REGEX, message = ParamPattern.NETWORK_NAME_MSG)
    @ApiModelProperty(value = "网络名称")
    private String networkName;

    @NotEmpty
    @ApiModelProperty(value = "预计提供Orderer节点端口", required = true)
    private List<Integer> ordererPorts;

    @NotBlank
    @ApiModelProperty(value = "申请描述信息", required = true)
    private String description;
}

