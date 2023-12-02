package com.admin4j.framework.xxl.props;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author andanyang
 * @since 2023/6/25 15:02
 */
@Data
//@ConfigurationProperties(prefix = "xxl.job")
public class XxlJobProperties {

    public static final String PREFIX = "xxl.job";

    /**
     * 是否启用。默认启用
     */
    @Value("${xxl.job.enable}")
    private boolean enable = false;

    /**
     * 调度中心部署根地址 [选填]：如调度中心集群部署存在多个地址则用逗号分隔。执行器将会使用该地址进行"执行器心跳注册"和"任务结果回调"；为空则关闭自动注册；
     */
    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    /**
     * 执行器通讯TOKEN [选填]：非空时启用；
     */
    @Value("${xxl.job.access-token}")
    private String accessToken;

    /**
     * 执行器AppName [选填]：执行器心跳注册分组依据；为空则关闭自动注册
     */
    @Value("${xxl.job.executor.app-name:${spring.application.name}}")
    private String appName;

    /**
     * 执行器注册 [选填]：优先使用该配置作为注册地址，为空时使用内嵌服务 ”IP:PORT“ 作为注册地址。从而更灵活的支持容器类型执行器动态IP和动态映射端口问题。
     */
    @Value("${xxl.job.executor.address:}")
    private String address;

    /**
     * 执行器IP [选填]：默认为空表示自动获取IP，多网卡时可手动设置指定IP，该IP不会绑定Host仅作为通讯实用；地址信息用于 "执行器注册" 和 "调度中心请求并触发任务"；
     */
    @Value("${xxl.job.executor.ip:}")
    private String ip;

    /**
     * 执行器端口号 [选填]：小于等于0则自动获取；默认端口为9999，单机部署多个执行器时，注意要配置不同执行器端口；
     */
    @Value("${xxl.job.executor.port:0}")
    private int port;

    /**
     * 执行器运行日志文件存储磁盘路径 [选填] ：需要对该路径拥有读写权限；为空则使用默认路径；
     */
    @Value("${xxl.job.executor.log-path:}")
    private String logPath;

    /**
     * 执行器日志文件保存天数 [选填] ： 过期日志自动清理, 限制值大于等于3时生效; 否则, 如-1, 关闭自动清理功能；
     */
    @Value("${xxl.job.executor.log-retention-days:30}")
    private int logRetentionDays;
}
