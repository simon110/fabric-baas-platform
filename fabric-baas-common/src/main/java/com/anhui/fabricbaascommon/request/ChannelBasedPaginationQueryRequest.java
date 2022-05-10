package com.anhui.fabricbaascommon.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "通道相关的查询请求")
public class ChannelBasedPaginationQueryRequest extends PaginationQueryRequest {
    @NotNull
    @Pattern(regexp = ParamPattern.CHANNEL_NAME_REGEX, message = ParamPattern.CHANNEL_NAME_MSG)
    @ApiModelProperty(value = "通道名称", required = true)
    private String channelName;
}
