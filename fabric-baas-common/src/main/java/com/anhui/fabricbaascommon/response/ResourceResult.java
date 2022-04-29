package com.anhui.fabricbaascommon.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "资源查询结果")
public class ResourceResult {
    @ApiModelProperty(value = "资源下载路径")
    private String downloadUrl;
}