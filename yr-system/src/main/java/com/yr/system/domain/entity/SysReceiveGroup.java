package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yr.common.mybatisplus.entity.PkModelEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *
 * </p>
 *
 * @author carl
 * @since 2022-01-04
 */
@TableName("sys_receive_group")
public class SysReceiveGroup extends PkModelEntity {
    public static final String ID = "id";
    public static final String RE_CODE = "re_code";
    public static final String RE_MODE = "re_mode";
    /**
     * 接收编码
     */
    @NotBlank(message = "接收编码不能为空")
    @Size(min = 0, max = 50, message = "参数名称不能超过50个字符")
    private String reCode;
    /**
     * 接收模式
     */
    @NotBlank(message = "接收模式不能为空")
    @Size(min = 0, max = 50, message = "参数名称不能超过50个字符")
    private String reMode;
    /**
     * 分组集合
     */
    @TableField(exist = false)
    private List<SysReceiveGroupObject> groupObjectList;

    public List<SysReceiveGroupObject> getGroupObjectList() {
        return groupObjectList;
    }

    public void setGroupObjectList(List<SysReceiveGroupObject> groupObjectList) {
        this.groupObjectList = groupObjectList;
    }

    public String getReCode() {
        return reCode;
    }

    public void setReCode(String reCode) {
        this.reCode = reCode;
    }

    public String getReMode() {
        return reMode;
    }

    public void setReMode(String reMode) {
        this.reMode = reMode;
    }


    @JsonIgnore
    @Override
    public Map<String, Object> getParams() {
        return super.getParams();
    }
}
