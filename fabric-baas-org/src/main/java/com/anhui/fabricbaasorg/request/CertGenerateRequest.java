package com.anhui.fabricbaasorg.request;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "证书创建请求")
public class CertGenerateRequest extends CertfileEntity {
}
