package com.yr.system.domain.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;


/**
 * <p>
 * MP分页超类
 * </p>
 *
 * @author carl
 * @since 2022-01-04
 */
public class PageVo<T> extends Page<T> {

    /**
     * 当前页码
     */
    private Integer page = 1;
    /**
     * 每页数量
     */
    private Integer rows = 10;
    /**
     * 排序规则：升序=asc，降序=desc
     */
    private String order;
    /**
     * 排序字段名称
     */
    private String sort;

    public PageVo() {

    }

    @Override
    public boolean hasNext() {
        return super.hasNext();
    }

    /**
     * 设置排序规则
     *
     * @param order 升序=asc，降序=desc
     * @return
     */
    public PageVo<T> order(String order) {
        setOrder(order);
        return this;
    }

    /**
     * 设置排序字段
     *
     * @param orderBy 字段名称
     * @return
     */
    public PageVo<T> orderBy(String orderBy) {
        setSort(orderBy);
        return this;
    }

    public Integer getPage() {
        return page;
    }

    /**
     * 重写设置page方法
     *
     * @param page
     */
    public void setPage(Integer page) {
        this.page = page;
        this.setCurrent(page);
    }

    public Integer getRows() {
        return rows;
    }

    /**
     * 重写设置rows方法
     *
     * @param rows
     */
    public void setRows(Integer rows) {
        this.rows = rows;
        this.setSize(rows);
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
