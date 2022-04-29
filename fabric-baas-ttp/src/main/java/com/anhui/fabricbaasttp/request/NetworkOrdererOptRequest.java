package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.request.BaseNetworkRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "Network Orderer操作请求")
public class NetworkOrdererOptRequest extends BaseNetworkRequest {
    @NotNull
    @ApiModelProperty(value = "Orderer节点的信息", required = true)
    private Node orderer;
}
