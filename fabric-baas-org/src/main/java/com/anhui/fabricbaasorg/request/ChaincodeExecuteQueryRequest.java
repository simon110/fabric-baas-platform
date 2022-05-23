package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import com.anhui.fabricbaascommon.request.BaseChannelRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "链码操作基本请求")
public class ChaincodeExecuteQueryRequest extends BaseChannelRequest {
    @NotBlank
    @ApiModelProperty(value = "链码名称")
    private String chaincodeName;

    @NotNull
    @ApiModelProperty(value = "请求参数")
    private Map<String, Object> params;

    @Pattern(regexp = ParamPattern.DEPLOYMENT_NAME_REGEX, message = ParamPattern.DEPLOYMENT_NAME_MSG)
    @ApiModelProperty(value = "Peer节点名称", required = true)
    private String peerName;
}
