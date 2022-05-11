package com.anhui.fabricbaascommon.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "网络中联盟相关的请求")
public class NetworkConsortiumRequest extends BaseNetworkRequest {
    @NotNull
    @Pattern(regexp = ParamPattern.CONSORTIUM_NAME_REGEX, message = ParamPattern.CONSORTIUM_NAME_MSG)
    @ApiModelProperty(value = "联盟名称（可将网络与联盟视为等价关系）", required = true)
    private String consortiumName;
}