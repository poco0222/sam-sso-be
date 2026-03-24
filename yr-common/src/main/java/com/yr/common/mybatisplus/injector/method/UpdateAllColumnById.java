package com.yr.common.mybatisplus.injector.method;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import java.util.Objects;
import static java.util.stream.Collectors.joining;

/**
 * @author 1050696985@qq.com
 * @version V1.0
 * @Date 2021-8-10 9:05
 * @description @{link https://blog.csdn.net/u013645689/article/details/118385383}
 * 与updateById相比较，差别在getAllSqlSet的i.getSqlSet(true, newPrefix)，即：不进行<if>判断包裹set
 * 自动填充：自动填充字段，即使实体类为null，最终也会填充上去
 * 逻辑删除：set字段中已经过滤掉了逻辑删除字段（前提是开启逻辑删除功能），但是where条件依然有
 * 乐观锁：更新时实体的@Version字段没值，乐观锁不起作用，如果有值，那么会进行版本控制
 */

public class UpdateAllColumnById extends AbstractMethod {

    private static final long serialVersionUID = -5618003380745561518L;

    private static final String SQL = "<script>\nUPDATE %s %s WHERE %s=#{%s} %s\n</script>";
    private static final String METHOD = "updateAllColumnById";

    /**
     * 注入自定义 MappedStatement
     *
     * @param mapperClass mapper 接口
     * @param modelClass  mapper 泛型
     * @param tableInfo   数据库表反射信息
     * @return MappedStatement
     */
    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // 从Mybatis-Plus3.4.0版本的UpdateById.class借鉴
        String sql = UpdateAllColumnById.SQL;
        // 如果配置了@Version和逻辑删除，additional会变成条件：<if test="et != null and et['version'] != null"> AND version=#{MP_OPTLOCK_VERSION_ORIGINAL}</if> AND del_flag=0
        // 如果未配置，则是空字符串
        String additional = this.optlockVersion(tableInfo) + tableInfo.getLogicDeleteSql(true, true);
        // 根据sql格式化模板格式化sql，主要是sqlSet进行设置set的sql语句，不包含sql
        sql = String.format(sql, tableInfo.getTableName(),
                this.sqlSet(tableInfo.isWithLogicDelete(), false, tableInfo, false, ENTITY, ENTITY_DOT),
                tableInfo.getKeyColumn(), ENTITY_DOT + tableInfo.getKeyProperty(), additional);
        // 配置执行内容，用于生成MapperdStatement
        SqlSource sqlSource = this.languageDriver.createSqlSource(this.configuration, sql, modelClass);
        return this.addUpdateMappedStatement(mapperClass, modelClass, UpdateAllColumnById.METHOD, sqlSource);
    }

    /**
     * SQL 更新 set 语句
     *
     * @param logic  是否逻辑删除注入器
     * @param ew     是否存在 UpdateWrapper 条件
     * @param table  表信息
     * @param alias  别名
     * @param prefix 前缀
     * @return sql
     */
    @Override
    protected String sqlSet(boolean logic, boolean ew, TableInfo table, boolean judgeAliasNull, String alias, String prefix) {
        // 设置所有的set内容，但是没用set标签
        String sqlScript = this.getAllSqlSet(logic, prefix, table);
        if (judgeAliasNull) {
            sqlScript = SqlScriptUtils.convertIf(sqlScript, String.format("%s != null", alias), true);
        }

        if (ew) {
            sqlScript += NEWLINE;
            sqlScript += SqlScriptUtils.convertIf(SqlScriptUtils.unSafeParam(U_WRAPPER_SQL_SET), String.format("%s != null and %s != null", WRAPPER, U_WRAPPER_SQL_SET), false);
        }
        // 增加set标签
        sqlScript = SqlScriptUtils.convertSet(sqlScript);
        return sqlScript;
    }

    /**
     * 获取所有的 sql set 片段
     *
     * @param ignoreLogicDelFiled 是否过滤掉逻辑删除字段
     * @param prefix              前缀
     * @param tableInfo           表信息
     * @return sql 脚本片段
     */
    private String getAllSqlSet(boolean ignoreLogicDelFiled, final String prefix, final TableInfo tableInfo) {
        final String newPrefix = prefix == null ? EMPTY : prefix;
        return tableInfo.getFieldList().stream()
                .filter(i -> {
                    // 是否跳过逻辑删除字段
                    if (ignoreLogicDelFiled) {
                        return !(tableInfo.isWithLogicDelete() && i.isLogicDelete());
                    }
                    return true;
                }).map(i -> i.getSqlSet(true, newPrefix)).filter(Objects::nonNull).collect(joining(NEWLINE));
    }

}
