package com.anhui.fabricbaasttp.request;


import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

@Data
@ApiModel(value = "当前用户提交加入网络申请请求")
public class ParticipationApplyRequest {
    @Pattern(regexp = ParamPattern.NETWORK_NAME_REGEX, message = ParamPattern.NETWORK_NAME_MSG)
    @ApiModelProperty(value = "网络名称", required = true)
    private String networkName;

    @NotNull
    @ApiModelProperty(value = "预计提供Orderer节点地址", required = true)
    private List<Node> orderers;

    @NotBlank
    @ApiModelProperty(value = "申请描述信息", required = true)
    private String description;
}
