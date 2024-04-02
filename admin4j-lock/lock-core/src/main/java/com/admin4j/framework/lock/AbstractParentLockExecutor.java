package com.admin4j.framework.lock;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author andanyang
 * @since 2024/1/16 17:04
 */
@Slf4j
public abstract class AbstractParentLockExecutor<T> implements LockExecutor<T> {

    @Getter
    @Setter
    protected LockExecutor<?> parent;

    @Override
    public void initSetLockInstance(LockInfo lockInfo) {

        if (parent != null) {
            parent.initSetLockInstance(lockInfo);
            lockInfo.setParentLockInstance(lockInfo.getLockInstance());
            lockInfo.setLockInstance(null);
        }
        if (lockInfo.getLockInstance() != null) {
            return;
        }
        T lockInstanceSelf = getLockInstanceSelf(lockInfo);
        lockInfo.setLockInstance(lockInstanceSelf);
    }

    protected abstract T getLockInstanceSelf(LockInfo lockInfo);

    @Override
    public void lock(LockInfo lockInfo) {

        if (parent != null) {

            Object lockInstance = lockInfo.getLockInstance();
            lockInfo.setLockInstance(lockInfo.getParentLockInstance());
            parent.lock(lockInfo);
            lockInfo.setLockInstance(lockInstance);
        }

        lockSelf(lockInfo);
    }

    /**
     * 实现自身的锁
     *
     * @param lockInfo
     */
    protected abstract void lockSelf(LockInfo lockInfo);

    @Override
    public boolean tryLock(LockInfo lockInfo) {

        if (parent != null) {
            // 先获取本地锁
            Object lockInstance = lockInfo.getLockInstance();
            lockInfo.setLockInstance(lockInfo.getParentLockInstance());
            if (!parent.tryLock(lockInfo)) {
                // 未获取本地锁直接返回
                lockInfo.setLockInstance(lockInstance);
                return false;
            }
            lockInfo.setLockInstance(lockInstance);
        }

        boolean lockSelf = tryLockSelf(lockInfo);
        if (!lockSelf) {
            // 获取锁失败，先释放 parent 锁
            Object lockInstance = lockInfo.getLockInstance();
            lockInfo.setLockInstance(lockInfo.getParentLockInstance());
            parent.unlock(lockInfo);
            lockInfo.setLockInstance(lockInstance);
        }
        return lockSelf;
    }

    protected abstract boolean tryLockSelf(LockInfo lockInfo);

    @Override
    public void unlock(LockInfo lockInfo) {

        if (parent != null) {

            Object lockInstance = lockInfo.getLockInstance();
            try {

                lockInfo.setLockInstance(lockInfo.getParentLockInstance());
                // 先解锁本地锁
                parent.unlock(lockInfo);
            } catch (Exception e) {
                log.error("unlock parent lock failed: {}", e.getMessage(), e);
                // 解锁失败 设置锁无效
                if (lockInfo.getLockInstance() instanceof InvalidLockInstance) {
                    ((InvalidLockInstance) lockInfo).invalid();
                }
            } finally {
                lockInfo.setLockInstance(lockInstance);
            }
        }

        unlockSelf(lockInfo);
    }

    protected abstract void unlockSelf(LockInfo lockInfo);
}
