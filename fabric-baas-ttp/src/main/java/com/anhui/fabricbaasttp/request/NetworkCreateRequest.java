package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.constant.ParamPattern;
import com.anhui.fabricbaascommon.request.BaseNetworkRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "当前用户创建网络请求")
public class NetworkCreateRequest extends BaseNetworkRequest {
    @Pattern(regexp = ParamPattern.CONSORTIUM_NAME_REGEX, message = ParamPattern.CONSORTIUM_NAME_MSG)
    @ApiModelProperty(value = "联盟名称（可将网络与联盟视为等价关系）", required = true)
    private String consortiumName;

    @Valid
    @NotEmpty
    @ApiModelProperty(value = "预计提供Orderer节点地址", required = true)
    private List<Node> orderers;
}
