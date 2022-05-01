package com.anhui.fabricbaasorg.entity;


import com.anhui.fabricbaascommon.constant.ParamPattern;
import com.anhui.fabricbaascommon.constant.ParamRange;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Document(collection = "orderer")
@AllArgsConstructor
@NoArgsConstructor
@Data
@ApiModel(value = "Orderer节点信息")
public class OrdererEntity {
    @Id
    @ApiModelProperty(value = "Orderer名称", required = true)
    @Pattern(regexp = ParamPattern.DEPLOYMENT_NAME_REGEX, message = ParamPattern.DEPLOYMENT_NAME_MSG)
    private String name;

    @ApiModelProperty(value = "物理节点名称", required = true)
    @NotBlank
    private String kubeNodeName;

    @ApiModelProperty(value = "主端口（对应7050）", required = true)
    @Range(min = ParamRange.MIN_KUBERNETES_PORT, max = ParamRange.MAX_KUBERNETES_PORT, message = ParamRange.KUBERNETES_PORT_MSG)
    private Integer kubeNodePort;
}
