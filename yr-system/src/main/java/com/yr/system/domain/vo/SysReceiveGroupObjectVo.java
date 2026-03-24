package com.yr.system.domain.vo;

import com.yr.system.domain.entity.SysReceiveGroup;

/**
 * <p>
 * description
 * </p>
 *
 * @author carl 2022-01-14 15:01
 * @version V1.0
 */
public class SysReceiveGroupObjectVo extends SysReceiveGroup {
    /**
     * 接收人分组对象
     */
    private SysReceiveGroup sysReceiveGroup;

    public SysReceiveGroupObjectVo(SysReceiveGroup sysReceiveGroup) {
        this.sysReceiveGroup = sysReceiveGroup;
    }

    public SysReceiveGroupObjectVo() {
    }

    public SysReceiveGroup getSysReceiveGroup() {
        return sysReceiveGroup;
    }

    public void setSysReceiveGroup(SysReceiveGroup sysReceiveGroup) {
        this.sysReceiveGroup = sysReceiveGroup;
    }
}
