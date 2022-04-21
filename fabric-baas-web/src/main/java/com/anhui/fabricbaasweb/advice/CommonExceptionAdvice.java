package com.anhui.fabricbaasweb.advice;

import com.anhui.fabricbaasweb.bean.CommonResponse;
import com.anhui.fabricbaasweb.bean.ResultCode;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class CommonExceptionAdvice {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public CommonResponse response(Exception e) {
        // 处理所有Service抛出的异常
        // 可用instanceof来对异常类型进行判断
        return new CommonResponse(ResultCode.ERROR.getValue(), e.getMessage(), null);
    }
}
