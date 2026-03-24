package com.yr.system.domain.dto;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * description
 * </p>
 *
 * @author carl 2022-01-06 15:51
 * @version V1.0
 */
public class SysGroupObjectDTO implements Serializable {
    /**
     * 模板编码
     */
    private Long reGroupId;

//    /**
//     * 对象ID
//     */
//    private List<Long> objectIds;


    private List<ModeObjectDTO> objectIds;


    private List<Long> ids;

    /**
     * 是否全部删除
     */
    private Boolean delAll = false;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    public boolean isDelAll() {
        return delAll;
    }

    public void setDelAll(boolean delAll) {
        this.delAll = delAll;
    }

    public Long getReGroupId() {
        return reGroupId;
    }

    public void setReGroupId(Long reGroupId) {
        this.reGroupId = reGroupId;
    }

    public List<ModeObjectDTO> getObjectIds() {
        return objectIds;
    }

    public void setObjectIds(List<ModeObjectDTO> objectIds) {
        this.objectIds = objectIds;
    }

    //    public List<Long> getObjectIds() {
//        return objectIds;
//    }
//
//    public void setObjectIds(List<Long> objectIds) {
//        this.objectIds = objectIds;
//    }
//
//    public void setObjectIds(String objectIds) {
//        if (StringUtils.isNotNull(objectIds) && StringUtils.isNotBlank(objectIds)) {
//            this.objectIds = Arrays.asList(objectIds.split(",")).stream().map(Long::new).collect(Collectors.toList());
//        }
//    }

    @Override
    public String toString() {
        return "SysGroupObjectDTO{" +
                "reMode='" + reGroupId + '\'' +
                ", objectId=" + objectIds +
                '}';
    }
}
