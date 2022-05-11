package com.anhui.fabricbaascommon.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "加入网络请求查询")
public class ParticipationQueryRequest extends NetworkPaginationQueryRequest {
    @NotNull
    @ApiModelProperty(value = "注册申请的状态（-1表示已拒绝、0表示未处理、1表示已通过）", required = true)
    @Range(min = -1, max = 1)
    private Integer status;
}
