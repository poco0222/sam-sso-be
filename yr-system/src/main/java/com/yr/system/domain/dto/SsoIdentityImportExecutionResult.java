/**
 * @file INIT_IMPORT 执行结果 DTO
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.domain.dto;

import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 承载导入执行后的任务状态、摘要与 item 明细。
 */
@Data
public class SsoIdentityImportExecutionResult {

    /** 执行后的任务状态。 */
    private String status;

    /** 结果摘要。 */
    private String resultSummary;

    /** 本次执行生成的明细。 */
    private List<SsoSyncTaskItem> itemList = new ArrayList<>();

    /** 明细总数。 */
    private long totalItemCount;

    /** 成功明细数。 */
    private long successItemCount;

    /** 失败明细数。 */
    private long failedItemCount;
}
