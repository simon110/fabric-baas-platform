package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import com.anhui.fabricbaascommon.request.PaginationQueryRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "证书查询请求")
public class CertQueryRequest extends PaginationQueryRequest {
    @Pattern(regexp = ParamPattern.CA_USERTYPE_REGEX, message = ParamPattern.CA_USERTYPE_MSG)
    @ApiModelProperty(value = "证书类型", required = true)
    private String certType;
}
