package com.anhui.fabricbaasttp.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "registration")
@ApiModel(value = "组织注册信息")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationEntity {
    @Id
    @JsonIgnore
    private ObjectId id;

    @ApiModelProperty(value = "组织名称", required = true)
    @Indexed
    private String organizationName;

    @ApiModelProperty(value = "联系邮箱", required = true)
    private String email;

    @ApiModelProperty(value = "组织服务端地址", required = true)
    private String apiServer;

    @ApiModelProperty(value = "登录密码", required = true)
    @JsonIgnore
    private String password;

    @ApiModelProperty(value = "描述信息", required = true)
    private String description;

    @Indexed
    @ApiModelProperty(value = "处理状态", required = true)
    private int status;

    @ApiModelProperty(value = "提交时间", required = true)
    private long timestamp;
}

