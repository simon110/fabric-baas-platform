package com.anhui.fabricbaasttp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "participation")
@CompoundIndexes({
        // def为属性到索引排序方向的映射，其中1代表正序、-1代表逆序
        @CompoundIndex(name = "uni_idx", def = "{'networkName':1,'organizationName':1}", unique = false)
})
@Data
@ApiModel(value = "加入网络申请信息")
public class ParticipationEntity {
    @Id
    @JsonIgnore
    private ObjectId id;

    @ApiModelProperty(value = "网络名称")
    private String networkName;

    @ApiModelProperty(value = "申请加入的组织名称")
    private String organizationName;

    @Indexed
    @ApiModelProperty(value = "申请处理状态", required = true)
    private int status;

    @ApiModelProperty(value = "申请描述信息", required = true)
    private String description;

    @ApiModelProperty(value = "申请时间戳", required = true)
    private long timestamp;

    @ApiModelProperty(value = "网络中同意此申请的组织名称", required = true)
    private List<String> approvals;
}

