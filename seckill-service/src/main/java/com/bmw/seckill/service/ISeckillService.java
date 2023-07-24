package com.bmw.seckill.service;

import com.bmw.seckill.common.base.BaseResponse;
import com.bmw.seckill.model.http.SeckillReq;
import com.bmw.seckill.model.http.SeckillReqV2;


/**
 * TODO 类描述
 *
 * @author mumu
 * @date 2023/7/14 10:21
 */
public interface ISeckillService {

    /**
     * 用户下单，扣减库存 增加售卖数量
     * @param req
     * @return
     */
    BaseResponse sOrder(SeckillReq req);

    /**
     * 悲观锁下单
     * @param req
     * @return
     */
    BaseResponse pOrder(SeckillReq req);


    /**
     * 乐观锁下单
     * @param req
     * @return
     */
    BaseResponse oOrder(SeckillReq req) throws Exception;

    BaseResponse cOrder(SeckillReq req) throws Exception;

    BaseResponse redissonOrder(SeckillReq req);

    BaseResponse orderV1(SeckillReq req) throws Exception;

    BaseResponse createOrder(SeckillReq req);

    BaseResponse orderV2(SeckillReq req) throws Exception;

    BaseResponse orderV4 (SeckillReqV2 req) throws Exception;

    BaseResponse<String> getVerifyHash(SeckillReq req);
}
