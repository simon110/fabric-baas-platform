package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.util.List;

@Data
@ApiModel(value = "创建网络请求")
public class NetworkCreateRequest {
    @Pattern(regexp = ParamPattern.NETWORK_NAME_REGEX, message = ParamPattern.NETWORK_NAME_MSG)
    @ApiModelProperty(value = "网络名称", required = true)
    private String networkName;

    @Pattern(regexp = ParamPattern.CONSORTIUM_NAME_REGEX, message = ParamPattern.CONSORTIUM_NAME_MSG)
    @ApiModelProperty(value = "联盟名称（可将网络与联盟视为等价关系）", required = true)
    private String consortiumName;

    @NotEmpty
    @ApiModelProperty(value = "预计提供Orderer节点端口", required = true)
    private List<Integer> ordererPorts;
}
