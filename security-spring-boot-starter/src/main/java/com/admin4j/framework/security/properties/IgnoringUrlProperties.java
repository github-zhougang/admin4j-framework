package com.admin4j.framework.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 根据配置文件 忽略url
 * 注意支持 ant-style 风格语法;如果开头没有模糊匹配，请以 / 开头。
 *
 * @author andanyang
 * @since 2023/3/24 17:00
 */
@Data
@ConfigurationProperties(prefix = "admin4j.security.ignoring")
public class IgnoringUrlProperties {

    /**
     * 包含所有请求类型的路径,不考虑请求方法
     * 注意支持 ant-style 风格语法;如果开头没有模糊匹配，请以 / 开头。
     */
    private String[] uris;
    /**
     * get 请求
     */
    private String[] get;
    /**
     * post 请求
     */
    private String[] post;
    /**
     * put 请求
     */
    private String[] put;
    /**
     * delete 请求
     */
    private String[] delete;
    /**
     * patch 请求
     */
    private String[] patch;
}
