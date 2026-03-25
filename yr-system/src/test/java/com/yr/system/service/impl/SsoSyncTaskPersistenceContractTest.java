/**
 * @file 锁定身份中心同步任务持久化与详情字段契约
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yr.common.core.domain.entity.SsoClient;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定一期 sync-task 持久化与详情返回的基础契约。
 */
class SsoSyncTaskPersistenceContractTest {

    /**
     * 验证 SSO 持久化实体声明了稳定的表名与主键元数据。
     */
    @Test
    void shouldDeclareStableTableMetadataForSsoPersistenceEntities() throws NoSuchFieldException {
        assertTableMetadata(SsoClient.class, "sso_client", "clientId");
        assertTableMetadata(SsoSyncTask.class, "sso_sync_task", "taskId");
        assertTableMetadata(SsoSyncTaskItem.class, "sso_sync_task_item", "itemId");
    }

    /**
     * 验证同步任务详情实体显式暴露 item 明细与统计字段，供 console 使用。
     */
    @Test
    void shouldExposeTaskDetailFieldsForSyncConsole() {
        List<String> fieldNames = Arrays.stream(SsoSyncTask.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertThat(fieldNames).contains(
                "itemList",
                "totalItemCount",
                "successItemCount",
                "failedItemCount"
        );
    }

    /**
     * 验证同步任务条目会持久化 msgKey，并预留 MQ 视图字段给 console 详情使用。
     *
     * @throws NoSuchFieldException 当字段不存在时抛出
     */
    @Test
    void shouldPersistMsgKeyAndExposeMessageLogViewOnSyncTaskItem() throws NoSuchFieldException {
        Field msgKeyField = SsoSyncTaskItem.class.getDeclaredField("msgKey");
        Field messageLogField = SsoSyncTaskItem.class.getDeclaredField("messageLog");
        TableField tableField = msgKeyField.getAnnotation(TableField.class);

        assertThat(tableField == null || tableField.exist())
                .withFailMessage("SsoSyncTaskItem.msgKey 不应再声明为非持久化字段")
                .isTrue();
        assertThat(messageLogField.getType().getSimpleName()).isEqualTo("SsoSyncTaskMessageLogView");
    }

    /**
     * 统一断言表名与主键注解，避免后续误删导致运行时持久化失效。
     *
     * @param entityClass 实体类型
     * @param expectedTableName 预期表名
     * @param primaryKeyFieldName 主键字段名
     * @throws NoSuchFieldException 当主键字段不存在时抛出
     */
    private void assertTableMetadata(Class<?> entityClass,
                                     String expectedTableName,
                                     String primaryKeyFieldName) throws NoSuchFieldException {
        TableName tableName = entityClass.getAnnotation(TableName.class);
        Field primaryKeyField = entityClass.getDeclaredField(primaryKeyFieldName);

        assertThat(tableName)
                .withFailMessage("%s 缺少 @TableName 注解", entityClass.getSimpleName())
                .isNotNull();
        assertThat(tableName.value()).isEqualTo(expectedTableName);
        assertThat(primaryKeyField.getAnnotation(TableId.class))
                .withFailMessage("%s.%s 缺少 @TableId 注解", entityClass.getSimpleName(), primaryKeyFieldName)
                .isNotNull();
    }
}
