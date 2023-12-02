package com.admin4j.signature.core;

import com.admin4j.signature.core.properties.SignatureProperties;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 签名的默认实现
 *
 * @author zhougang
 * @since 2023/10/11 13:39
 */
public class DefaultSignature extends AbstractSignature {

    public DefaultSignature(StringRedisTemplate stringRedisTemplate, SignatureProperties signatureProperties) {
        super(stringRedisTemplate, signatureProperties);
    }
}
