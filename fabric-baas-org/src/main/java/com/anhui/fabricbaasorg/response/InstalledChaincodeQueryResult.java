package com.anhui.fabricbaasorg.response;

import com.anhui.fabricbaasorg.entity.InstalledChaincodeEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "查询所有已安装的链码结果")
public class InstalledChaincodeQueryResult {
    @ApiModelProperty(value = "所有已安装的链码")
    private List<InstalledChaincodeEntity> installedChaincodes;
}
