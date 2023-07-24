package com.bmw.seckill.service.impl;

import com.bmw.seckill.dao.SeckillUserDao;
import com.bmw.seckill.model.SeckillUser;
import com.bmw.seckill.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * TODO 类描述
 *
 * @author mumu
 * @date 2023/7/11 8:19
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    SeckillUserDao userDao;
    @Override
    public SeckillUser findByPhone(String phone) {
        return userDao.selectByPhone(phone);
    }


}
