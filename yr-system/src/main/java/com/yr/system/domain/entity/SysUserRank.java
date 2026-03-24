package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yr.common.mybatisplus.entity.CustomEntity;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-26 18:24
 * @description
 */

@TableName("sys_user_rank")
public class SysUserRank extends CustomEntity {

    /**
     * 表id，主键
     */
    @TableId
    private Long id;

    /**
     * 用户id，sys_user.user_id
     */
    private Long userId;

    /**
     * 职级id，sys_rank.id
     */
    private Long rankId;

    // getter setter
    //--------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRankId() {
        return rankId;
    }

    public void setRankId(Long rankId) {
        this.rankId = rankId;
    }
}
