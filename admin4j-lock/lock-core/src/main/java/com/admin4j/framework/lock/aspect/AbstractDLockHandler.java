package com.admin4j.framework.lock.aspect;


import com.admin4j.common.service.ILoginTenantInfoService;
import com.admin4j.common.service.ILoginUserInfoService;
import com.admin4j.framework.lock.LockExecutor;
import com.admin4j.framework.lock.LockInfo;
import com.admin4j.framework.lock.annotation.DistributedLock;
import com.admin4j.framework.lock.exception.DistributedLockException;
import com.admin4j.framework.lock.key.DLockKeyGenerator;
import com.admin4j.framework.lock.key.SimpleKeyGenerator;
import com.admin4j.framework.lock.util.DistributedLockUtil;
import com.admin4j.spring.util.SpelUtil;
import com.admin4j.spring.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

/**
 * 分布式锁解析器
 * <a href="https://github.com/redisson/redisson/wiki/8.-%E5%88%86%E5%B8%83%E5%BC%8F%E9%94%81%E5%92%8C%E5%90%8C%E6%AD%A5%E5%99%A8">...</a>
 *
 * @author andanyang
 * @since 2020/12/22 11:06
 */
@Slf4j
public abstract class AbstractDLockHandler {

    private static DLockKeyGenerator LOCK_KEY_GENERATOR;

    /**
     * 切面环绕通知
     *
     * @param joinPoint ProceedingJoinPoint
     * @param lockInfo  锁信息
     * @return Object
     */
    public Object around(ProceedingJoinPoint joinPoint, LockInfo lockInfo) throws Throwable {

        LockExecutor lockExecutor = DistributedLockUtil.getLockExecutor(lockInfo);
        lockExecutor.initSetLockInstance(lockInfo);

        boolean tryLock = true;
        try {

            // 获取超时时间并获取锁
            if (!lockInfo.isTryLock()) {
                lockExecutor.lock(lockInfo);
            } else {
                tryLock = lockExecutor.tryLock(lockInfo);
                if (!tryLock) {
                    lockFailure();
                }
            }

            return joinPoint.proceed();
        } finally {
            if (tryLock) lockExecutor.unlock(lockInfo);
            // log.debug("释放Redis分布式锁[成功]，解锁完成，结束业务逻辑...");
        }
    }

    /**
     * 抢锁失败，抛出异常
     */
    protected void lockFailure() {
        throw new DistributedLockException("failed to acquire lock");
    }

    protected String generateKeyByKeyGenerator(ProceedingJoinPoint joinPoint, String keyGenerator) {
        // 得到被切面修饰的方法的参数列表
        Object[] args = joinPoint.getArgs();
        // 得到被代理的方法
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        DLockKeyGenerator dLockKeyGenerator = StringUtils.isEmpty(keyGenerator) ? defaultDLockKeyGenerator() : SpringUtils.getBean(DLockKeyGenerator.class);
        return dLockKeyGenerator.generate(joinPoint.getTarget(), method, args).toString();
    }

    protected String getDistributedLockKey(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {

        // 得到被切面修饰的方法的参数列表
        Object[] args = joinPoint.getArgs();
        // 得到被代理的方法
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        StringBuilder distributedLockKey = new StringBuilder(distributedLock.prefix());

        String key = StringUtils.defaultIfEmpty(distributedLock.key(), distributedLock.value());
        if (StringUtils.isEmpty(key)) {
            // 按照 KeyGenerator 生成key
            key = generateKeyByKeyGenerator(joinPoint, distributedLock.keyGenerator());
            distributedLockKey.append(key);
        } else {
            // 按照 key 生成key
            String parseElKey = SpelUtil.parse(joinPoint.getThis(), key, method, args);

            if (StringUtils.isBlank(parseElKey)) {
                log.error("DistributedLockKey is null Signature: {}", joinPoint.getSignature());
                throw new DistributedLockException("DistributedLockKey is null");
            }

            distributedLockKey.append(parseElKey);
        }

        // 开启用户模式
        if (distributedLock.user()) {
            ILoginUserInfoService loginUserService = SpringUtils.getBean(ILoginUserInfoService.class);
            Assert.notNull(loginUserService, "ILoginUserInfoService must implement");
            distributedLockKey.append(":U").append(loginUserService.getUserId());
        }

        // 开启租户
        if (distributedLock.tenant()) {
            ILoginTenantInfoService loginTenantInfoService = SpringUtils.getBean(ILoginTenantInfoService.class);
            Assert.notNull(loginTenantInfoService, "ILoginTenantInfoService must implement");
            Assert.notNull(loginTenantInfoService.getTenantId(), "Tenant not null");
            distributedLockKey.append(":T").append(loginTenantInfoService.getTenantId());
        }

        return distributedLockKey.toString();
    }

    protected DLockKeyGenerator defaultDLockKeyGenerator() {

        if (LOCK_KEY_GENERATOR == null) {
            LOCK_KEY_GENERATOR = new SimpleKeyGenerator();
        }
        return LOCK_KEY_GENERATOR;
    }
}
