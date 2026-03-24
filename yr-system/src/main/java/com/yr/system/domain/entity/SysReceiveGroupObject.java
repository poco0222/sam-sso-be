package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yr.common.mybatisplus.entity.PkModelEntity;

/**
 * <p>
 *
 * </p>
 *
 * @author carl
 * @since 2022-01-04
 */
@TableName("sys_receive_group_object")
public class SysReceiveGroupObject extends PkModelEntity {

    public static final String ID = "id";
    public static final String RE_GROUP_ID = "re_group_id";
    public static final String OBJECT_ID = "object_id";
    private Long reGroupId;
    private Long reObjectId;
    @TableField("re_name")
    private String name;

    public Long getReGroupId() {
        return reGroupId;
    }

    public void setReGroupId(Long reGroupId) {
        this.reGroupId = reGroupId;
    }

    public Long getReObjectId() {
        return reObjectId;
    }

    public void setReObjectId(Long reObjectId) {
        this.reObjectId = reObjectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
