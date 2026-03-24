package com.yr.web.domain;

public class SystemInfo {
    private RedisInfo redisInfo;
    private DatabaseInfo databaseInfo;


    public RedisInfo getRedisInfo() {
        return redisInfo;
    }

    public void setRedisInfo(RedisInfo redisInfo) {
        this.redisInfo = redisInfo;
    }

    public DatabaseInfo getDatabaseInfo() {
        return databaseInfo;
    }

    public void setDatabaseInfo(DatabaseInfo databaseInfo) {
        this.databaseInfo = databaseInfo;
    }
}
