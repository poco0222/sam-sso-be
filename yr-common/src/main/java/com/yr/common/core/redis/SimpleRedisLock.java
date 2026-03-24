package com.yr.common.core.redis;

import com.yr.common.utils.uuid.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {

    // 业务名称
    private String name;
    private StringRedisTemplate stringRedisTemplate;

    // 通过构造方法将name和stringRedisTemplate传入  构造方法的好处是什么：构造方法本身不能创建一个对象，真正创建对象的java平台
    // 构造方法只是java平台在创建出一个对象之后去默认调用的方法
    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX = "lock:";
    //加一个uuid 防止误删操作
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId + "", timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        //获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        //获取锁中的标识
        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        //判断标识是否一致
        if (threadId.equals(id)){
            //通过del删除锁  释放锁
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }

    }
}
