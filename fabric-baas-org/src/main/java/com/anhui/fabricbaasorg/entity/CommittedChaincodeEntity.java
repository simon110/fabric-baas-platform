package com.anhui.fabricbaasorg.entity;

import com.anhui.fabricbaascommon.bean.CommittedChaincode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "committedchaincode")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "已生效的链码")
public class CommittedChaincodeEntity extends CommittedChaincode {
    @ApiModelProperty(value = "链码所在的Peer", required = true)
    private String peerName;
}
