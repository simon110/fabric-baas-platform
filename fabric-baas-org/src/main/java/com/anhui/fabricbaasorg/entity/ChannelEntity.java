package com.anhui.fabricbaasorg.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "channel")
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "通道信息")
public class ChannelEntity {
    @ApiModelProperty(value = "通道名称")
    @Id
    private String name;

    @ApiModelProperty(value = "所属网络名称")
    private String networkName;
}