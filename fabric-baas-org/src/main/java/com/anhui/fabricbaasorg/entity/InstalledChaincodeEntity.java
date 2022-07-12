package com.anhui.fabricbaasorg.entity;

import com.anhui.fabricbaascommon.bean.InstalledChaincode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Document(collection = "installedchaincode")
@AllArgsConstructor
@NoArgsConstructor
@Data
@ApiModel(value = "已安装的链码信息")
public class InstalledChaincodeEntity extends InstalledChaincode {
    @Id
    @JsonIgnore
    private ObjectId id;

    @ApiModelProperty(value = "链码所在的Peer", required = true)
    private String peerName;
}
