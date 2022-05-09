package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.request.NetworkConsortiumRequest;
import com.anhui.fabricbaasorg.entity.OrdererEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "创建网络请求")
public class NetworkCreateRequest extends NetworkConsortiumRequest {
    @Valid
    @NotEmpty
    @ApiModelProperty(value = "预计提供Orderer节点配置", required = true)
    private List<OrdererEntity> orderers;
}
