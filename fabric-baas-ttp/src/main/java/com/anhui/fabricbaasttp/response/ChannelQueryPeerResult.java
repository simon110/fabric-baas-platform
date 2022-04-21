package com.anhui.fabricbaasttp.response;

import com.anhui.fabricbaascommon.bean.Node;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "通道内的Peer查询结果")
public class ChannelQueryPeerResult {
    private List<Node> peers;
}
