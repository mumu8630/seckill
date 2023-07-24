package com.bmw.seckill.service;

import com.bmw.seckill.model.SeckillAdmin;

import java.util.List;

/**
 * TODO 类描述
 *
 * @author mumu
 * @date 2023/7/12 9:08
 */
public interface IAdminService {
    List<SeckillAdmin> listAdmin();

    SeckillAdmin findByUsername(String username);
}
