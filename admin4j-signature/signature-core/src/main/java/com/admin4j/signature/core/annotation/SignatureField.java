package com.admin4j.signature.core.annotation;

import java.lang.annotation.*;

/**
 * 签名算法实现 => 指定哪些字段需要进行签名
 */
@Documented
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SignatureField {

    /**
     * 字段：自定义name，对应前端传入的字段名
     *
     * @return
     */
    String filedName() default "";

    /**
     * 签名顺序
     *
     * @return
     */
    int order() default Integer.MIN_VALUE;

}
