package com.anhui.fabricbaasttp.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "网络内的组织查询结果")
public class NetworkQueryOrganizationResult {
    @ApiModelProperty(value = "网络中所有的组织名")
    private List<String> organizationNames;
}
