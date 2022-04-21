package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.bean.Node;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "Orderer TLS证书查询请求")
public class NetworkQueryOrdererTlsCertRequest {
    @ApiModelProperty(value = "网络名称", required = true)
    private String networkName;

    @ApiModelProperty(value = "节点地址", required = true)
    private Node orderer;
}
