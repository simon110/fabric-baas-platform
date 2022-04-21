package com.anhui.fabricbaasweb.response;

import lombok.Getter;

@Getter
public enum ResultCode {
    OK(200),
    ERROR(400);

    private final int value;

    ResultCode(int value) {
        this.value = value;
    }
}
