package com.anhui.fabricbaasorg.response;

import com.anhui.fabricbaasorg.entity.OrdererEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "所有Orderer查询结果")
public class OrdererQueryResult {
    @ApiModelProperty(value = "当前端管理的所有Orderer")
    private List<OrdererEntity> orderers;
}
