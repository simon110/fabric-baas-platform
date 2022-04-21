package com.anhui.fabricbaascommon.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("邀请信息")
public class Invitation {
    @ApiModelProperty("邀请组织名称")
    private String invitorOrgName;

    @ApiModelProperty("收邀组织名称")
    private String inviteeOrgName;

    @ApiModelProperty("相关通道名称")
    private String channelName;

    @ApiModelProperty("邀请的时间戳")
    private Long timestamp;
}

