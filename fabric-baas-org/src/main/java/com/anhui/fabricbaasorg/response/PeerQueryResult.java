package com.anhui.fabricbaasorg.response;


import com.anhui.fabricbaasorg.entity.PeerEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "所有Peer查询结果")
public class PeerQueryResult {
    @ApiModelProperty(value = "当前端管理的所有Peer")
    private List<PeerEntity> peers;
}
