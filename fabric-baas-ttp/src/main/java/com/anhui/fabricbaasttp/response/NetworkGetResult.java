package com.anhui.fabricbaasttp.response;

import com.anhui.fabricbaasttp.entity.NetworkEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "网络信息查询结果")
public class NetworkGetResult {
    @ApiModelProperty(value = "网络的具体信息")
    private NetworkEntity network;
}
