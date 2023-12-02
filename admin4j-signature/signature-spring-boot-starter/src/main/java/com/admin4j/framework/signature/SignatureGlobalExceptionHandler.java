package com.admin4j.framework.signature;

import com.admin4j.common.exception.handler.AbstractExceptionHandler;
import com.admin4j.common.pojo.IResponse;
import com.admin4j.common.pojo.ResponseEnum;
import com.admin4j.common.pojo.SimpleResponse;
import com.admin4j.framework.signature.core.exception.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author zhougang
 * @since 2023/11/10 17:58
 */
@RestControllerAdvice
@Slf4j
public class SignatureGlobalExceptionHandler extends AbstractExceptionHandler {

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<IResponse> distributedLockException(SignatureException e) {
        log.error("SignatureException：" + e.getMessage(), e);

        return renderException(e, SimpleResponse.of(ResponseEnum.REQUEST_SIGNATURE_FAILURE.getCode(), e.getMessage()));
    }

}
