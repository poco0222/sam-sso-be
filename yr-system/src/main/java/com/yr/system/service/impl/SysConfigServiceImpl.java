/**
 * @file 系统参数服务实现，负责参数查询与缓存维护
 * @author Codex
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.yr.common.annotation.DataSource;
import com.yr.common.constant.Constants;
import com.yr.common.constant.UserConstants;
import com.yr.common.core.redis.RedisCache;
import com.yr.common.core.text.Convert;
import com.yr.common.enums.DataSourceType;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.SysConfig;
import com.yr.system.mapper.SysConfigMapper;
import com.yr.system.service.ISysConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 参数配置 服务层实现
 *
 * @author Youngron
 */
@Service
public class SysConfigServiceImpl implements ISysConfigService {
    private final SysConfigMapper configMapper;

    private final RedisCache redisCache;

    /**
     * 显式声明参数服务依赖，避免字段注入带来的可测试性问题。
     *
     * @param configMapper 参数 Mapper
     * @param redisCache Redis 缓存组件
     */
    public SysConfigServiceImpl(SysConfigMapper configMapper, RedisCache redisCache) {
        this.configMapper = configMapper;
        this.redisCache = redisCache;
    }

    /**
     * 查询参数配置信息
     *
     * @param configId 参数配置ID
     * @return 参数配置信息
     */
    @Override
    @DataSource(DataSourceType.MASTER)
    public SysConfig selectConfigById(Long configId) {
        SysConfig config = new SysConfig();
        config.setConfigId(configId);
        return configMapper.selectConfig(config);
    }

    /**
     * 根据键名查询参数配置信息
     *
     * @param configKey 参数key
     * @return 参数键值
     */
    @Override
    public String selectConfigByKey(String configKey) {
        String configValue = Convert.toStr(redisCache.getCacheObject(getCacheKey(configKey)));
        if (StringUtils.isNotEmpty(configValue)) {
            return configValue;
        }
        SysConfig config = new SysConfig();
        config.setConfigKey(configKey);
        SysConfig retConfig = configMapper.selectConfig(config);
        if (StringUtils.isNotNull(retConfig)) {
            redisCache.setCacheObject(getCacheKey(configKey), retConfig.getConfigValue());
            return retConfig.getConfigValue();
        }
        return StringUtils.EMPTY;
    }

    /**
     * 获取验证码开关
     *
     * @return true开启，false关闭
     */
    @Override
    public boolean selectCaptchaOnOff() {
        String captchaOnOff = selectConfigByKey("sys.account.captchaOnOff");
        if (StringUtils.isEmpty(captchaOnOff)) {
            return true;
        }
        return Convert.toBool(captchaOnOff);
    }

    /**
     * 查询参数配置列表
     *
     * @param config 参数配置信息
     * @return 参数配置集合
     */
    @Override
    public List<SysConfig> selectConfigList(SysConfig config) {
        return configMapper.selectConfigList(config);
    }

    /**
     * 新增参数配置
     *
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public int insertConfig(SysConfig config) {
        int row = configMapper.insertConfig(config);
        if (row > 0) {
            redisCache.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
        }
        return row;
    }

    /**
     * 修改参数配置
     *
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public int updateConfig(SysConfig config) {
        int row = configMapper.updateConfig(config);
        if (row > 0) {
            redisCache.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
        }
        return row;
    }

    /**
     * 批量删除参数信息
     *
     * @param configIds 需要删除的参数ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfigByIds(Long[] configIds) {
        // 先完成整批读取与校验，避免前半段已删、后半段才抛异常的半成功状态。
        List<SysConfig> configsToDelete = new ArrayList<>();
        for (Long configId : configIds) {
            SysConfig config = selectConfigById(configId);
            if (StringUtils.isNull(config)) {
                throw new CustomException(String.format("参数配置不存在，参数ID：%1$s", configId));
            }
            if (StringUtils.equals(UserConstants.YES, config.getConfigType())) {
                throw new CustomException(String.format("内置参数【%1$s】不能删除 ", config.getConfigKey()));
            }
            configsToDelete.add(config);
        }
        for (SysConfig config : configsToDelete) {
            configMapper.deleteConfigById(config.getConfigId());
            redisCache.deleteObject(getCacheKey(config.getConfigKey()));
        }
    }

    /**
     * 加载参数缓存数据
     */
    @Override
    public void loadingConfigCache() {
        warmUpConfigCache();
    }

    /**
     * 启动后批量预热参数缓存，并返回写入的参数数量。
     *
     * @return 预热的参数数量
     */
    @Override
    public int warmUpConfigCache() {
        List<SysConfig> configsList = configMapper.selectConfigList(new SysConfig());
        for (SysConfig config : configsList) {
            redisCache.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
        }
        return configsList.size();
    }

    /**
     * 清空参数缓存数据
     */
    @Override
    public void clearConfigCache() {
        Collection<String> keys = redisCache.keys(Constants.SYS_CONFIG_KEY + "*");
        redisCache.deleteObject(keys);
    }

    /**
     * 重置参数缓存数据
     */
    @Override
    public void resetConfigCache() {
        clearConfigCache();
        loadingConfigCache();
    }

    /**
     * 校验参数键名是否唯一
     *
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public String checkConfigKeyUnique(SysConfig config) {
        Long configId = StringUtils.isNull(config.getConfigId()) ? -1L : config.getConfigId();
        SysConfig info = configMapper.checkConfigKeyUnique(config.getConfigKey());
        if (StringUtils.isNotNull(info) && info.getConfigId().longValue() != configId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 设置cache key
     *
     * @param configKey 参数键
     * @return 缓存键key
     */
    private String getCacheKey(String configKey) {
        return Constants.SYS_CONFIG_KEY + configKey;
    }
}
