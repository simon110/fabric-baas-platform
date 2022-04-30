package com.anhui.fabricbaasorg.entity;


import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Document(collection = "remoteuser")
@ApiModel("在TTP端的组织信息")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RemoteUserEntity {
    @Pattern(regexp = ParamPattern.ORGANIZATION_NAME_REGEX, message = ParamPattern.ORGANIZATION_NAME_MSG)
    @ApiModelProperty(value = "登录可信端的组织名称", required = true)
    @Id
    private String organizationName;

    @Size(min = 8, message = "密码的长度最少为8")
    @ApiModelProperty(value = "登录可信端的组织密码", required = true)
    private String password;

    @Pattern(regexp = ParamPattern.API_SERVER_REGEX, message = ParamPattern.API_SERVER_MSG)
    @ApiModelProperty(value = "可信端的服务地址", required = true)
    private String apiServer;
}
