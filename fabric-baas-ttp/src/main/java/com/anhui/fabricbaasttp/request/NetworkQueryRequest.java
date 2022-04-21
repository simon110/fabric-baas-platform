package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.request.PaginationQueryRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "查询指定名称网络信息请求")
public class NetworkQueryRequest extends PaginationQueryRequest {
    @NotNull
    @ApiModelProperty(value = "网络名称关键词", required = true)
    private String networkNameKeyword;

    @NotNull
    @ApiModelProperty(value = "组织名称关键词", required = true)
    private String organizationNameKeyword;
}
