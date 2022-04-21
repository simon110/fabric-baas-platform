package com.anhui.fabricbaasweb.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(value = "分页查询结果基类")
@AllArgsConstructor
@NoArgsConstructor
public class PaginationQueryResult<T> implements Serializable {
    @ApiModelProperty(value = "总页数", required = true)
    private int totalPages;

    @ApiModelProperty(value = "数据项列表", required = true)
    private List<T> items;
}