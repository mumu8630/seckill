package com.bmw.seckill.service;

import com.bmw.seckill.common.util.bean.CommonQueryBean;
import com.bmw.seckill.model.SeckillProducts;

import java.util.List;

/**
 * TODO 类描述
 *
 * @author mumu
 * @date 2023/7/11 17:17
 */
public interface ISeckillProductsService {

    /**
     * 分页
     * @param seckillProducts
     * @param queryBean
     * @return
     */
    List<SeckillProducts> list4Page(SeckillProducts seckillProducts, CommonQueryBean queryBean);

    /**
     * count查询
     * @param seckillProducts
     * @return
     */
    int count(SeckillProducts seckillProducts);

    /**
     * 唯一索引保证新增的数据唯一.
     */
    Long uniqueInsert(SeckillProducts record);

    /**
     * 通过主键查找
     * @param id
     * @return
     */
    SeckillProducts selectByPrimaryKey(Long id);

    /**
     * 通过主键更新
     * @param record
     * @return
     */
    int updateByPrimaryKeySelective(SeckillProducts record);

    /**
     * 通过商品id查找
     * @param productPeriodKey
     * @return
     */
    SeckillProducts findByProductPeriodKey(String productPeriodKey);

    /**
     * 增加商品
     * @param record
     * @return
     */
    int insert(SeckillProducts record);

    /**
     * list查找
     * @param record
     * @return
     */
    List<SeckillProducts> list(SeckillProducts record);
}
