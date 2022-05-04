package com.anhui.fabricbaasorg.entity;

import com.anhui.fabricbaascommon.bean.ApprovedChaincode;
import com.anhui.fabricbaascommon.constant.ParamPattern;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Document(collection = "approvedchaincode")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "已投票的链码")
public class ApprovedChaincodeEntity extends ApprovedChaincode {
    @Id
    private ObjectId id;

    @Pattern(regexp = ParamPattern.DEPLOYMENT_NAME_REGEX, message = ParamPattern.DEPLOYMENT_NAME_MSG)
    @ApiModelProperty(value = "链码所在的Peer", required = true)
    private String peerName;

    @JsonIgnore
    @ApiModelProperty(value = "链码是否已经生效", required = false)
    private boolean committed = false;

    @NotBlank
    @ApiModelProperty(value = "链码的Package ID")
    private String installedChaincodeIdentifier;
}
