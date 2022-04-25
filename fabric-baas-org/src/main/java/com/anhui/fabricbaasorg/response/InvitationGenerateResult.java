package com.anhui.fabricbaasorg.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "生成加入通道邀请结果")
public class InvitationGenerateResult {
    @ApiModelProperty(value = "加密信息（用以后续交易签名前的验证）")
    private String invitation;
}
