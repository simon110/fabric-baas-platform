package com.anhui.fabricbaasorg.entity;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ttp")
@ApiModel("在TTP端的组织信息")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TTPEntity {
    @ApiModelProperty(value = "登录可信端的组织名称", required = true)
    @Id
    private String organizationName;

    @ApiModelProperty(value = "登录可信端的组织密码", required = true)
    private String password;

    @ApiModelProperty(value = "可信端的服务地址", required = true)
    private String apiServer;
}
