package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.request.NetworkConsortiumRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "当前用户创建网络请求")
public class NetworkCreateRequest extends NetworkConsortiumRequest {
    @Valid
    @NotEmpty
    @ApiModelProperty(value = "预计提供Orderer节点地址", required = true)
    private List<Node> orderers;
}
