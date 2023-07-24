package com.bmw.seckill.service.impl;

import cn.hutool.core.lang.Assert;
import com.bmw.seckill.common.util.bean.CommonQueryBean;
import com.bmw.seckill.dao.SeckillProductsDao;
import com.bmw.seckill.model.SeckillProducts;
import com.bmw.seckill.service.ISeckillProductsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * TODO 类描述
 *
 * @author mumu
 * @date 2023/7/11 17:18
 */
@Service
@Slf4j
public class SeckillProductsServiceImpl implements ISeckillProductsService {
    @Autowired
    SeckillProductsDao seckillProductsDao;
    @Override
    public List<SeckillProducts> list4Page(SeckillProducts seckillProducts, CommonQueryBean queryBean) {
        return seckillProductsDao.list4Page(seckillProducts,queryBean);
    }

    @Override
    public int count(SeckillProducts seckillProducts) {
        return seckillProductsDao.count(seckillProducts);
    }

    @Override
    public Long uniqueInsert(SeckillProducts record) {
        try {
            record.setCreateTime(new Date());
            record.setIsDeleted(0);
            record.setStatus(SeckillProducts.STATUS_IS_OFFLINE);

            //通过唯一索引查找
            SeckillProducts existItem = findByProductPeriodKey(record.getProductPeriodKey());
            if (existItem != null){
                //若查找到数据 返回其id
                return existItem.getId();
            }else {
                //否则将数据 添加
                seckillProductsDao.insert(record);
            }

        }catch (Exception e){
            if (e.getMessage().indexOf("Duplicate entry") >= 0){
                SeckillProducts existItem = findByProductPeriodKey(record.getProductPeriodKey());
                return existItem.getId();
            }else {
                log.error(e.getMessage(),e);
                throw new RuntimeException(e.getMessage());
            }
        }
        return null;
    }

    /**
     * 主键查询
     * @param id
     * @return
     */
    @Override
    public SeckillProducts selectByPrimaryKey(Long id) {
        return seckillProductsDao.selectByPrimaryKey(id);
    }

    @Override
    public int updateByPrimaryKeySelective(SeckillProducts record) {
        return seckillProductsDao.updateByPrimaryKeySelective(record);
    }

    @Override
    public SeckillProducts findByProductPeriodKey(String productPeriodKey) {
        Assert.isTrue(!StringUtils.isEmpty(productPeriodKey));

        SeckillProducts item = new SeckillProducts();
        item.setProductPeriodKey(productPeriodKey);

        List<SeckillProducts> list = seckillProductsDao.list(item);
        if (CollectionUtils.isEmpty(list)){
            return null;
        }else {
            return  list.get(0);
        }
    }

    @Override
    public int insert(SeckillProducts record) {
        return seckillProductsDao.insert(record);
    }

    @Override
    public List<SeckillProducts> list(SeckillProducts record) {
        return seckillProductsDao.list(record);
    }


}
