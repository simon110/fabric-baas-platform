package com.anhui.fabricbaasttp.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "organization")
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "组织信息")
public class OrganizationEntity {
    @ApiModelProperty(value = "组织名称", required = true)
    @Id
    private String name;

    @ApiModelProperty(value = "联系邮箱", required = true)
    private String email;

    @ApiModelProperty(value = "服务端地址", required = true)
    private String apiServer;
}

