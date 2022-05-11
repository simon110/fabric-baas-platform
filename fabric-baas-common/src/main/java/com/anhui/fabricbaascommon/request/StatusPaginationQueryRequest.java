package com.anhui.fabricbaascommon.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "状态相关的分页查询请求")
public class StatusPaginationQueryRequest extends PaginationQueryRequest {
    @NotNull
    @ApiModelProperty(value = "申请的状态（-1表示已拒绝、0表示未处理、1表示已通过）", required = true)
    @Range(min = -1, max = 1)
    private Integer status;
}