package com.bmw.seckill.service.impl;

import com.bmw.seckill.common.base.BaseResponse;
import com.bmw.seckill.common.exception.ErrorMessage;
import com.bmw.seckill.dao.SeckillOrderDao;
import com.bmw.seckill.dao.SeckillProductsDao;
import com.bmw.seckill.dao.SeckillUserDao;
import com.bmw.seckill.model.SeckillOrder;
import com.bmw.seckill.model.SeckillProducts;
import com.bmw.seckill.model.SeckillUser;
import com.bmw.seckill.model.http.SeckillReq;
import com.bmw.seckill.model.http.SeckillReqV2;
import com.bmw.seckill.service.ISeckillService;
import com.bmw.seckill.util.Constant;
import com.bmw.seckill.util.DecrCacheStockUtil;
import com.bmw.seckill.util.DistrubuteLimit;
import com.bmw.seckill.util.RedisUtil;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * TODO 类描述
 *
 * @author mumu
 * @date 2023/7/14 10:21
 */
@Service
@Slf4j
public class SeckillServiceImpl implements ISeckillService {

    @Autowired
    private SeckillOrderDao sOrderDao;
    @Autowired
    private SeckillProductsDao sProductsDao;
    @Autowired
    private SeckillUserDao sUserDao;
    @Autowired
    private DecrCacheStockUtil decrCacheStockUtil;
    @Autowired
    private Redisson redisson;
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    DistrubuteLimit distrubuteLimit;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse sOrder(SeckillReq req) {
       log.info("===【开始调用原下单接口】===");
    //   参数校验
        log.info("===【校验用户信息及商品信息】===");
        BaseResponse paramValidRes = validateParam(req.getProductId(),req.getUserId());
        if (paramValidRes.getCode() != 0){
        //    商品信息有误 返回其报错信息
            return paramValidRes;
        }
        log.info("===【校验参数是否合法】[通过]===");
        Long productId = req.getProductId();
        Long userId = req.getUserId();
        SeckillProducts product = sProductsDao.selectByPrimaryKey(productId);
        Date date = new Date();
    //    扣减库存
        log.info("===【开始扣减库存】===");
        product.setSaled(product.getSaled()+1);
        sProductsDao.updateByPrimaryKeySelective(product);
        log.info("===【开始扣减库存】[成功]===");
    //    创建订单
        log.info("===【开始创建订单】===");
        SeckillOrder order = new SeckillOrder();
        order.setProductId(productId);
        order.setProductName(product.getName());
        order.setUserId(userId);
        order.setCreateTime(date);
        sOrderDao.insert(order);
        log.info("===【创建订单】[成功]===");
        return BaseResponse.OK(Boolean.TRUE);

    }

    /**
     * 悲观锁下单，要求有事务注解 @Transactional
     * @param req
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse pOrder(SeckillReq req) {
        log.info("===[开始调用秒杀接口(悲观锁)]===");
        //校验用户信息、商品信息、库存信息
        log.info("===[校验用户信息、商品信息、库存信息]===");
        BaseResponse paramValidRes = validateParamPessimistic(req.getProductId(), req.getUserId());
        if (paramValidRes.getCode() != 0) {
            return paramValidRes;
        }

        //此处判断是否重复下单 ，通过数据库联合索引判断
        log.info("===[校验][通过]===");
        List<SeckillOrder> repList = sOrderDao.listRepeatOrdersForUpdate(req.getUserId(), req.getProductId());
        if (repList.size() > 0) {
            log.error("===[该用户重复下单！]===");
            return BaseResponse.error(ErrorMessage.REPEAT_ORDER_ERROR);
        }
        log.info("===[校验 用户是否重复下单][通过校验]===");

        Long userId = req.getUserId();
        Long productId = req.getProductId();
        SeckillProducts product = sProductsDao.selectByPrimaryKey(productId);
        // 下单逻辑
        log.info("===[开始下单逻辑]===");
        Date date = new Date();
        // 扣减库存
        product.setSaled(product.getSaled() + 1);
        sProductsDao.updateByPrimaryKeySelective(product);
        // 创建订单
        SeckillOrder order = new SeckillOrder();
        order.setProductId(productId);
        order.setProductName(product.getName());
        order.setUserId(userId);
        order.setCreateTime(date);
        sOrderDao.insert(order);
        return BaseResponse.OK(Boolean.TRUE);
    }

    /**
     * 乐观锁下单
     * @param req
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse oOrder(SeckillReq req) throws Exception {
        log.info("===[开始调用下单接口~（乐观锁）]===");
        //校验用户信息、商品信息、库存信息
        log.info("===[校验用户信息、商品信息、库存信息]===");
        BaseResponse paramValidRes = validateParam(req.getProductId(), req.getUserId());
        if (paramValidRes.getCode() != 0) {
            return paramValidRes;
        }
        log.info("===[校验][通过]===");
        //下单（乐观锁）
        return createOptimisticOrder(req.getProductId(), req.getUserId());
    }

    private BaseResponse createOptimisticOrder(Long productId, Long userId) throws Exception {
        log.info("===[下单逻辑Starting]===");
        // 创建订单
        SeckillProducts products = sProductsDao.selectByPrimaryKey(productId);
        Date date = new Date();
        SeckillOrder order = new SeckillOrder();
        order.setProductId(productId);
        order.setProductName(products.getName());
        order.setUserId(userId);
        order.setCreateTime(date);
        sOrderDao.insert(order);
        log.info("===[创建订单成功]===");
        //扣减库存
        int res = sProductsDao.updateStockByOptimistic(productId);
        if (res == 0) {
            log.error("===[秒杀失败，抛出异常，执行回滚逻辑！]===");
            throw new Exception("库存不足");
//          return BaseResponse.error(ErrorMessage.SECKILL_FAILED);
        }
        log.info("===[扣减库存成功!]===");
        try {
            addOrderedUserCache(productId, userId);
        } catch (Exception e) {
            log.error("===[记录已购用户缓存时发生异常！]===", e);
        }
        return BaseResponse.OK(Boolean.TRUE);
    }

    //普通校验逻辑
    private BaseResponse validateParam(Long productId, Long userId) {
        SeckillProducts product = sProductsDao.selectByPrimaryKey(productId);
        if (product == null) {
            log.error("===[产品不存在！]===");
            return BaseResponse.error(ErrorMessage.SYS_ERROR);
        }
        if (product.getStartBuyTime().getTime() > System.currentTimeMillis()) {
            log.error("===[秒杀还未开始！]===");
            return BaseResponse.error(ErrorMessage.SECKILL_NOT_START);
        }
        if (product.getSaled() >= product.getCount()) {
            log.error("===[库存不足！]===");
            return BaseResponse.error(ErrorMessage.STOCK_NOT_ENOUGH);
        }
        if (hasOrderedUserCache(productId, userId)) {
            log.error("===[用户重复下单！]===");
            return BaseResponse.error(ErrorMessage.REPEAT_ORDER_ERROR);
        }
        return BaseResponse.OK(Boolean.TRUE);
    }
    public void addOrderedUserCache(Long productId, Long userId) {
        String key = String.format(Constant.redisKey.SECKILL_ORDERED_USER, productId);
        redisUtil.sSet(key, userId);
        log.info("===[已将已购用户放入缓存！]===");
    }
    public Boolean hasOrderedUserCache(Long productId, Long userId) {
        String key = String.format(Constant.redisKey.SECKILL_ORDERED_USER, productId);
        return redisUtil.sHasKey(key, userId);
    }

    // 悲观锁实现的校验逻辑
    private BaseResponse validateParamPessimistic(Long productId, Long userId) {
        //悲观锁，利用selectForUpdate方法锁定记录，并获得最新的SeckillProducts记录
        SeckillProducts product = sProductsDao.selectForUpdate(productId);
        if (product == null) {
            log.error("===[产品不存在！]===");
            return BaseResponse.error(ErrorMessage.SYS_ERROR);
        }
        if (product.getStartBuyTime().getTime() > System.currentTimeMillis()) {
            log.error("===[秒杀还未开始！]===");
            return BaseResponse.error(ErrorMessage.SECKILL_NOT_START);
        }
        if (product.getSaled() >= product.getCount()) {
            log.error("===[库存不足！]===");
            return BaseResponse.error(ErrorMessage.STOCK_NOT_ENOUGH);
        }
        return BaseResponse.OK;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse cOrder(SeckillReq req) throws Exception {
        log.info("===[开始调用下单接口~（避免超卖——Redis）]===");
        long res = 0;
        try {
            //校验用户信息、商品信息、库存信息
            log.info("===[校验用户信息、商品信息、库存信息]===");
            BaseResponse paramValidRes = validateParam(req.getProductId(), req.getUserId());
            if (paramValidRes.getCode() != 0) {
                return paramValidRes;
            }
            log.info("===[校验][通过]===");
            Long productId = req.getProductId();
            Long userId = req.getUserId();
            //redis + lua
            res = decrCacheStockUtil.decrStock(req.getProductId());
            if (res == 2) {
                // 扣减完的库存只要大于等于0，就说明扣减成功
                // 开始数据库扣减库存逻辑
                sProductsDao.decrStock(productId);
                // 创建订单
                SeckillProducts product = sProductsDao.selectByPrimaryKey(productId);
                Date date = new Date();
                SeckillOrder order = new SeckillOrder();
                order.setProductId(productId);
                order.setProductName(product.getName());
                order.setUserId(userId);
                order.setCreateTime(date);
                sOrderDao.insert(order);
                return BaseResponse.OK(Boolean.TRUE);
            } else {
                log.error("===[缓存扣减库存不足！]===");
                return BaseResponse.error(ErrorMessage.STOCK_NOT_ENOUGH);
            }
        } catch (Exception e) {
            log.error("===[异常！]===", e);
            if (res == 2) {
                decrCacheStockUtil.addStock(req.getProductId());
            }
            throw new Exception("异常！");
        }
    }

    /**
     * redisson解决
     * @param req
     * @return
     */
    @Override
    public BaseResponse redissonOrder(SeckillReq req) {
        log.info("===[开始调用下单接口（redission）~]===");
        String lockKey = String.format(Constant.redisKey.SECKILL_DISTRIBUTED_LOCK, req.getProductId());
        RLock lock = redisson.getLock(lockKey);
        try {
            //将锁过期时间设置为30s，定时任务每隔10秒执行续锁操作
            lock.lock();
            //** 另一种写法 **
            //先拿锁，在设置超时时间，看门狗就不会自动续期，锁到达过期时间后，就释放了
            //lock.lock(30, TimeUnit.SECONDS);
            //参数校验
            log.info("===[校验用户信息及商品信息]===");
            BaseResponse paramValidRes = validateParam(req.getProductId(), req.getUserId());
            if (paramValidRes.getCode() != 0) {
                return paramValidRes;
            }
            log.info("===[校验参数是否合法][通过]===");
            Long productId = req.getProductId();
            Long userId = req.getUserId();
            SeckillProducts product = sProductsDao.selectByPrimaryKey(productId);
            Date date = new Date();
            // 扣减库存
            log.info("===[开始扣减库存]===");
            product.setSaled(product.getSaled() + 1);
            sProductsDao.updateByPrimaryKeySelective(product);
            log.info("===[扣减库存][成功]===");
            // 创建订单
            log.info("===[开始创建订单]===");
            SeckillOrder order = new SeckillOrder();
            order.setProductId(productId);
            order.setProductName(product.getName());
            order.setUserId(userId);
            order.setCreateTime(date);
            sOrderDao.insert(order);
            log.info("===[创建订单][成功]===");
            return BaseResponse.OK;
        } catch (Exception e) {
            log.error("===[异常！]===", e);
            return BaseResponse.error(ErrorMessage.SYS_ERROR);
        } finally {
            lock.unlock(); // 释放redis分布式锁
        }
    }

    /**
     * 令牌通实现限流
     *
     */
    // Guava令牌桶：每秒放行5个请求
    RateLimiter rateLimiter = RateLimiter.create(5);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse orderV1(SeckillReq req) throws Exception{
        log.info("===[开始调用下单接口（应用限流）]===");
        /**
         *  增加应用限流
         *  阻塞式 & 非阻塞式
         */
        log.info("===[开始经过限流程序]===");
        //  阻塞式获取令牌
        log.info("===[令牌桶限流:等待时间{}]===", rateLimiter.acquire());
        //  非阻塞式获取令牌
        //if (!rateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
        //    log.error("你被限流了！直接返回失败！");
        //    return BaseResponse.error(ErrorMessage.SECKILL_RATE_LIMIT_ERROR);
        //}
        log.info("===[限流程序][通过]===");
        //校验用户信息、商品信息、库存信息
        log.info("===[校验用户信息、商品信息、库存信息]===");
        BaseResponse paramValidRes = validateParam(req.getProductId(), req.getUserId());
        if (paramValidRes.getCode() != 0) {
            return paramValidRes;
        }
        log.info("===[校验][通过]===");
        //下单（乐观锁）
        return createOptimisticOrder(req.getProductId(), req.getUserId());
    }

    /**
     * redis+lua分布式限流
     * @param req
     * @return
     */
    @Override
    public BaseResponse createOrder(SeckillReq req) {
        Long productId = req.getProductId();
        Long userId = req.getUserId();
        // 创建订单
        SeckillProducts products = sProductsDao.selectByPrimaryKey(productId);
        Date date = new Date();
        SeckillOrder order = new SeckillOrder();
        order.setProductId(productId);
        order.setProductName(products.getName());
        order.setUserId(userId);
        order.setCreateTime(date);
        sOrderDao.insert(order);
        log.info("===[下单逻辑][创建订单成功]===");
        //扣减库存
        sProductsDao.decrStock(productId);
        return BaseResponse.OK;
    }

    /**
     * 分布式实现限流 redis + lua
     * @param req
     * @return
     * @throws Exception
     */
    @Override
    public BaseResponse orderV2(SeckillReq req) throws Exception {
        log.info("===[开始调用下单接口（应用限流）]===");
        log.info("===[开始经过限流程序]===");
        //分布式限流
        try {
            if (!distrubuteLimit.exec()) {
                log.info("你被分布式锁限流了！直接返回失败！");
                return BaseResponse.error(ErrorMessage.SECKILL_RATE_LIMIT_ERROR);
            }
        } catch (IOException e) {
            log.error("===[分布式限流程序发生异常！]===", e);
            return BaseResponse.error(ErrorMessage.SECKILL_FAILED);
        }
        log.info("===[限流程序][通过]===");
        //校验用户信息、商品信息、库存信息
        log.info("===[校验用户信息、商品信息、库存信息]===");
        BaseResponse paramValidRes = validateParam(req.getProductId(), req.getUserId());
        if (paramValidRes.getCode() != 0) {
            return paramValidRes;
        }
        log.info("===[校验][通过]===");
        //下单（乐观锁）
        return createOptimisticOrder(req.getProductId(), req.getUserId());
    }
    @Override
    public BaseResponse<String> getVerifyHash(SeckillReq req) {
        log.info("===[开始调用获取秒杀验证码接口]===");
        //校验用户信息、商品信息、库存信息
        log.info("===[校验用户信息、商品信息、库存信息]===");
        BaseResponse paramValidRes = validateParam(req.getProductId(), req.getUserId());
        if (paramValidRes.getCode() != 0) {
            return paramValidRes;
        }
        log.info("===[校验][通过]===");

        //生成hash
        String verify = Constant.VALIDATE_CODE_SALT + req.getProductId() + req.getUserId();
        String verifyHash = DigestUtils.md5DigestAsHex(verify.getBytes());
        //将hash和用户商品信息存入redis
        String key = String.format(Constant.redisKey.SECKILL_VALIDATE_CODE, req.getProductId(), req.getUserId());
        redisUtil.set(key, verifyHash, 60);
        return BaseResponse.OK(verifyHash);
    }

    @Override
    public BaseResponse orderV4(SeckillReqV2 req) throws Exception{
        log.info("===[开始调用下单接口（乐观锁+分步式限流+url隐藏+单用户频次限制）]===");
        //校验用户信息、商品信息、库存信息
        log.info("===[校验用户信息、商品信息、库存信息]===");
        BaseResponse paramValidRes = validateParamV4(req.getProductId(), req.getUserId(), req.getVerifyCode());
        if (paramValidRes.getCode() != 0) {
            return paramValidRes;
        }
        log.info("===[校验][通过]===");
        /**
         *  增加应用限流
         *  阻塞式 & 非阻塞式
         */
        log.info("===[开始经过限流程序]===");
        //分布式限流
        try {
            if (!distrubuteLimit.exec()) {
                log.info("你被分布式锁限流了！直接返回失败！");
                return BaseResponse.error(ErrorMessage.SECKILL_RATE_LIMIT_ERROR);
            }
        } catch (IOException e) {
            log.error("===[分布式限流程序发生异常！]===", e);
            return BaseResponse.error(ErrorMessage.SECKILL_FAILED);
        }
        log.info("===[限流程序][通过]===");
        //下单（乐观锁）
        return createOptimisticOrder(req.getProductId(), req.getUserId());
    }

    private BaseResponse validateParamV4(Long productId, Long userId, String verifyCode) {
        SeckillProducts product = sProductsDao.selectByPrimaryKey(productId);
        if (product == null) {
            log.error("===[产品不存在！]===");
            return BaseResponse.error(ErrorMessage.SYS_ERROR);
        }
        SeckillUser user = sUserDao.selectByPrimaryKey(userId);
        if (user == null) {
            log.error("===[用户不存在！]===");
            return BaseResponse.error(ErrorMessage.SYS_ERROR);
        }
        if (product.getStartBuyTime().getTime() > System.currentTimeMillis()) {
            log.error("===[秒杀还未开始！]===");
            return BaseResponse.error(ErrorMessage.SECKILL_NOT_START);
        }
        if (product.getSaled() >= product.getCount()) {
            log.error("===[库存不足！]===");
            return BaseResponse.error(ErrorMessage.STOCK_NOT_ENOUGH);
        }
        if (hasOrderedUserCache(productId, userId)) {
            log.error("===[用户重复下单！]===");
            return BaseResponse.error(ErrorMessage.REPEAT_ORDER_ERROR);
        }
        //单用户访问频次限制
        String visitKey = String.format(Constant.redisKey.SECKILL_USER_VISIT, productId, userId);
        long visitCount = redisUtil.incr(visitKey, 1L);
        redisUtil.expire(visitKey, 5);
        if (visitCount > Constant.VISIT_LIMIT) {
            return BaseResponse.error(ErrorMessage.SECKILL_USER_VISIT_LIMIT_ERROR);
        }
        log.info("===[单用户频次限制合法]===");
        //校验验证码
        String key = String.format(Constant.redisKey.SECKILL_VALIDATE_CODE, productId, userId);
        if (!verifyCode.equals(String.valueOf(redisUtil.get(key)))) {
            return BaseResponse.error(ErrorMessage.SECKILL_VALIDATE_ERROR);
        }
        return BaseResponse.OK;
    }
}
