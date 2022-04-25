package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.bean.Certfile;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "证书创建请求")
public class CertGenerateRequest extends Certfile {
}
