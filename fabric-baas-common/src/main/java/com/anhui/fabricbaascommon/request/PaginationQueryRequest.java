package com.anhui.fabricbaascommon.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@ApiModel(value = "分页查询请求基类")
public class PaginationQueryRequest {
    @NotNull
    @ApiModelProperty(value = "分页查询的页号（从1开始计算）", required = true)
    @Min(1)
    private int page;

    @NotNull
    @ApiModelProperty(value = "分页查询的页面大小", required = true)
    @Min(1)
    private int pageSize;
}

