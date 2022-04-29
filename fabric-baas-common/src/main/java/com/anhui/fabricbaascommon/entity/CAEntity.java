package com.anhui.fabricbaascommon.entity;


import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Pattern;

@Document(collection = "ca")
@ApiModel("CA基本信息实体")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CAEntity {
    @Pattern(regexp = ParamPattern.ORGANIZATION_NAME_REGEX, message = ParamPattern.ORGANIZATION_NAME_MSG)
    @ApiModelProperty(value = "组织名称", required = true)
    @Id
    private String organizationName;

    @Pattern(regexp = ParamPattern.HOST_REGEX, message = ParamPattern.HOST_MSG)
    @ApiModelProperty(value = "组织域名", required = true)
    private String domain;

    @Pattern(regexp = ParamPattern.COUNTRY_CODE_REGEX, message = ParamPattern.COUNTRY_CODE_MSG)
    @ApiModelProperty(value = "组织国家代码（例如CN、US）", required = true)
    private String countryCode;

    @Pattern(regexp = ParamPattern.STATE_OR_PROVINCE_REGEX, message = ParamPattern.STATE_OR_PROVINCE_MSG)
    @ApiModelProperty(value = "组织所在省份，例如Guangxi", required = true)
    private String stateOrProvince;

    @Pattern(regexp = ParamPattern.LOCALITY_REGEX, message = ParamPattern.LOCALITY_MSG)
    @ApiModelProperty(value = "组织所在城市，例如Guilin", required = true)
    private String locality;
}