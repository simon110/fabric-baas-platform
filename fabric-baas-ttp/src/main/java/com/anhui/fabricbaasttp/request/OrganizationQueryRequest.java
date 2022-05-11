package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.request.PaginationQueryRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "条件查询组织信息请求")
public class OrganizationQueryRequest extends PaginationQueryRequest {
    @ApiModelProperty(value = "组织名称关键词", required = true)
    private String organizationNameKeyword;
}