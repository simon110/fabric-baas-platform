package com.anhui.fabricbaascommon.entity;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Document(collection = "certfile")
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "证书信息")
public class CertfileEntity {
    @NotNull
    @Pattern(regexp = ParamPattern.CA_USERNAME_REGEX, message = ParamPattern.CA_USERNAME_MSG)
    @ApiModelProperty(value = "CA服务账号", required = true)
    @Id
    private String caUsername;

    @NotNull
    @Pattern(regexp = ParamPattern.CA_PASSWORD_REGEX, message = ParamPattern.CA_PASSWORD_MSG)
    @ApiModelProperty(value = "CA服务密码", required = true)
    private String caPassword;

    @NotNull
    @Pattern(regexp = ParamPattern.CA_USERTYPE_REGEX, message = ParamPattern.CA_USERTYPE_MSG)
    @ApiModelProperty(value = "证书类型（包括orderer、peer、admin、client）", required = true)
    private String caUsertype;
}
