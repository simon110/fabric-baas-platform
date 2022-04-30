package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.constant.ParamRange;
import com.anhui.fabricbaascommon.request.BaseChannelRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Range;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "Orderer添加请求")
public class ChannelAddOrdererRequest extends BaseChannelRequest {

    @Range(min = ParamRange.MIN_KUBERNETES_PORT, max = ParamRange.MAX_KUBERNETES_PORT, message = ParamRange.KUBERNETES_PORT_MSG)
    @ApiModelProperty(value = "Orderer节点的端口（必须已经添加到网络）", required = true)
    private int ordererPort;
}
