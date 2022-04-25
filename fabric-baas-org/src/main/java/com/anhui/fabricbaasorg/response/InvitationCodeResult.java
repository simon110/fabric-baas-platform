package com.anhui.fabricbaasorg.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "邀请码结果")
public class InvitationCodeResult {
    @ApiModelProperty("邀请码（BASE64编码形式的二进制数据）")
    private String invitationCode;
}
