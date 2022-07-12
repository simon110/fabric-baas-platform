package com.anhui.fabricbaascommon.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(value = "分页查询结果基类")
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> implements Serializable {
    @ApiModelProperty(value = "总页数", required = true)
    private int totalPages;

    @ApiModelProperty(value = "数据项列表", required = true)
    private List<T> items;

    public PageResult(Page<T> page) {
        this.items = page.getContent();
        this.totalPages = page.getTotalPages();
    }
}