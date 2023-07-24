package com.bmw.seckill.service;

import com.bmw.seckill.common.entity.CommonWebUser;
import com.bmw.seckill.model.SeckillUser;

import java.util.List;

/**
 * TODO 类描述
 *
 * @author mumu
 * @date 2023/7/11 8:19
 */
public interface UserService {
    SeckillUser findByPhone(String phone);

}
