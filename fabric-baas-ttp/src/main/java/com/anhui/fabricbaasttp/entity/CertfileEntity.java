package com.anhui.fabricbaasttp.entity;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "certfile")
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "证书信息")
public class CertfileEntity {
    @Id
    private String caUsername;

    private String caPassword;

    private String caUsertype;
}
