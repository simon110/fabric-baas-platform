package com.anhui.fabricbaasttp.request;

import com.anhui.fabricbaascommon.request.PaginationQueryRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Range;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "条件查询注册信息请求")
public class RegistrationQueryRequest extends PaginationQueryRequest {
    @ApiModelProperty(value = "注册申请的状态（-1表示已拒绝、0表示未处理、1表示已通过）", required = true)
    @Range(min = -1, max = 1)
    private int status;
}

