package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.request.NetworkBasedPaginationQueryRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "加入网络请求查询")
public class ParticipationQueryRequest extends NetworkBasedPaginationQueryRequest {
    @ApiModelProperty(value = "注册申请的状态（-1表示已拒绝、0表示未处理、1表示已通过）", required = false)
    @Range(min = -1, max = 1)
    private int status;
}
