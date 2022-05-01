package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaasorg.entity.PeerEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "Peer启动请求")
public class PeerStartRequest extends PeerEntity {
    
}
