package com.yr.system.service.impl;

import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.system.domain.entity.SysAttachment;
import com.yr.system.mapper.SysAttachmentMapper;
import com.yr.system.service.ISysAttachmentService;
import org.springframework.stereotype.Service;

/**
 * 附件表(SysAttachment)表服务实现类
 *
 * @author Youngron
 * @since 2021-12-30 10:48:34
 */
@Service
public class SysAttachmentServiceImpl extends CustomServiceImpl<SysAttachmentMapper, SysAttachment> implements ISysAttachmentService {

}
