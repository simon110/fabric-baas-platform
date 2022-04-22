package com.anhui.fabricbaascommon.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 参考资料：
 * https://en.wikipedia.org/wiki/Certificate_signing_request
 */
@Data
@ApiModel("CA服务配置")
public class CAConfig {
    @ApiModelProperty(value = "CA名称（例如WikimediaFoundationCA，对应CA配置ca.name）", required = true)
    private String caName;

    @ApiModelProperty(value = "CSR证书颁发者名称（例如wikipedia.org，对应CA配置csr.cn）", required = true)
    private String csrCommonName;

    @ApiModelProperty(value = "CSR证书所属组织名称（例如WikimediaFoundationInc，对应CA配置csr.names.O）", required = true)
    private String csrOrganizationName;

    @ApiModelProperty(value = "CSR证书所属部门名称（例如IT，对应CA配置csr.names.OU）", required = false)
    private String csrOrganizationUnit;

    @ApiModelProperty(value = "CSR国家代码（例如CN、US，对应CA配置csr.names.C）", required = true)
    private String csrCountryCode;

    @ApiModelProperty(value = "CSR州名或省名（例如Guangxi、Guangdong，对应CA配置csr.names.ST）", required = true)
    private String csrStateOrProvince;

    @ApiModelProperty(value = "CSR城市名称（例如Guilin、Guangzhou，对应CA配置csr.names.L）", required = true)
    private String csrLocality;

    @ApiModelProperty(value = "CSR证书域名（例如localhost,wikipedia.org，对应CA配置csr.hosts）", required = true)
    private List<String> csrHosts;
}
