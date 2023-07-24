package com.bmw.seckill.service.impl;

import cn.hutool.core.lang.Assert;
import com.bmw.seckill.dao.SeckillAdminDao;
import com.bmw.seckill.model.SeckillAdmin;
import com.bmw.seckill.service.IAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * TODO 类描述
 *
 * @author mumu
 * @date 2023/7/12 9:09
 */
@Service("adminservice")
public class AdminServiceImpl implements IAdminService {
    @Autowired
    SeckillAdminDao seckillAdminDao;
    @Override
    public List<SeckillAdmin> listAdmin() {
        return seckillAdminDao.list(new SeckillAdmin());
    }

    /**
     * 通过用户名查找
     * @param username
     * @return
     */
    @Override
    public SeckillAdmin findByUsername(String username) {
        SeckillAdmin item = new SeckillAdmin();
        item.setLoginName(username);
        List<SeckillAdmin> list = seckillAdminDao.list(item);
        if (!CollectionUtils.isEmpty(list)) {
            Assert.isTrue(list.size() == 1);
            return list.get(0);
        }
        return null;
    }
}
