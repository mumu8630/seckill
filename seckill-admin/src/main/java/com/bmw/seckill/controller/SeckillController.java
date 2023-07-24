package com.bmw.seckill.controller;

import com.bmw.seckill.common.base.BaseRequest;
import com.bmw.seckill.common.base.BaseResponse;
import com.bmw.seckill.common.entity.CommonWebUser;
import com.bmw.seckill.common.exception.ErrorMessage;
import com.bmw.seckill.model.http.SeckillReq;
import com.bmw.seckill.model.http.SeckillReqV2;
import com.bmw.seckill.security.WebUserUtil;
import com.bmw.seckill.service.ISeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Objects;

/**
 * TODO 类描述
 *  秒杀功能Controller层
 * @author mumu
 * @date 2023/7/14 10:06
 */
@RestController
@Slf4j
@RequestMapping("/seckill")
public class SeckillController {

    @Autowired
    ISeckillService seckillService;

    @RequestMapping(value = "/simple/order")
    public BaseResponse sOrder(@Valid @RequestBody BaseRequest<SeckillReq> request) {
        // 验证token，获取user信息
        CommonWebUser user = WebUserUtil.getLoginUser();
        if (Objects.isNull(user)) {
            return BaseResponse.error(ErrorMessage.LOGIN_ERROR);
        }
        SeckillReq req = request.getData();
        req.setUserId(user.getId());
        return seckillService.sOrder(req);
    }

    /**
     * 基于synchronized的jvm锁来实现线程安全
     * @param request
     * @return
     */
    @RequestMapping(value = "/synchronized/order")
    public synchronized BaseResponse synchronizedOrder(@Valid @RequestBody BaseRequest<SeckillReq> request) {
        // 验证token，获取user信息
        CommonWebUser user = WebUserUtil.getLoginUser();
        if (Objects.isNull(user)) {
            return BaseResponse.error(ErrorMessage.LOGIN_ERROR);
        }
        SeckillReq req = request.getData();
        req.setUserId(user.getId());
        return seckillService.sOrder(req);
    }

    /**
     * 秒杀下单（避免超卖问题——悲观锁）
     */
    @RequestMapping(value = "/pessimistic/order")
    public BaseResponse pOrder(@Valid @RequestBody BaseRequest<SeckillReq> request) {
        CommonWebUser user = WebUserUtil.getLoginUser();
        if (Objects.isNull(user)) {
            return BaseResponse.error(ErrorMessage.LOGIN_ERROR);
        }
        SeckillReq req = request.getData();
        req.setUserId(user.getId());
        return seckillService.pOrder(req);
    }

    /**
     * 秒杀下单（避免超卖问题——乐观锁）
     */
    @RequestMapping(value = "/optimistic/order")
    public BaseResponse oOrder(@Valid @RequestBody BaseRequest<SeckillReq> request) {
        try {
            CommonWebUser user = WebUserUtil.getLoginUser();
            if (Objects.isNull(user)) {
                return BaseResponse.error(ErrorMessage.LOGIN_ERROR);
            }
            SeckillReq req = request.getData();
            req.setUserId(user.getId());
            return seckillService.oOrder(req);
        } catch (Exception e) {
            log.error("===[秒杀异常！]===", e);
        }
        return BaseResponse.error(ErrorMessage.SYS_ERROR);
    }

    /**
     * 秒杀下单（避免超卖问题——redis缓存库存，保证原子扣减库存）
     */
    @RequestMapping(value = "/cache/order")
    public BaseResponse cOrder(@Valid @RequestBody BaseRequest<SeckillReq> request) {
        try {
            CommonWebUser user = WebUserUtil.getLoginUser();
            if (Objects.isNull(user)) {
                return BaseResponse.error(ErrorMessage.LOGIN_ERROR);
            }
            SeckillReq req = request.getData();
            req.setUserId(user.getId());
            return seckillService.cOrder(req);
        } catch (Exception e) {
            log.error("===秒杀异常！===", e);
        }
        return BaseResponse.error(ErrorMessage.SYS_ERROR);
    }


    /**
     * 秒杀下单（避免超卖问题——redission）
     */
    @RequestMapping(value = "/redission/order")
    public BaseResponse rOrder(@Valid @RequestBody BaseRequest<SeckillReq> request) {
        CommonWebUser user = WebUserUtil.getLoginUser();
        if (Objects.isNull(user)) {
            return BaseResponse.error(ErrorMessage.LOGIN_ERROR);
        }
        SeckillReq req = request.getData();
        req.setUserId(user.getId());
        return seckillService.redissonOrder(req);
    }

    /**
     * 令牌桶限流后乐观锁下单
     */
    @RequestMapping(value = "/v1/order")
    public BaseResponse orderV1(@Valid @RequestBody BaseRequest<SeckillReq> request) {
        try {
            CommonWebUser user = WebUserUtil.getLoginUser();
            if (Objects.isNull(user)) {
                return BaseResponse.error(ErrorMessage.LOGIN_ERROR);
            }
            SeckillReq req = request.getData();
            req.setUserId(user.getId());
            return seckillService.orderV1(req);
        } catch (Exception e) {
            log.error("===秒杀发生异常！===", e);
        }
        return BaseResponse.error(ErrorMessage.SECKILL_FAILED);
    }

    /**
     * 分布式 实现限流
     * @param request
     * @return
     */
    @RequestMapping(value = "/v2/order")
    public BaseResponse orderV2(@Valid @RequestBody BaseRequest<SeckillReq> request) {
        try {
            CommonWebUser user = WebUserUtil.getLoginUser();
            if (Objects.isNull(user)) {
                return BaseResponse.error(ErrorMessage.LOGIN_ERROR);
            }
            SeckillReq req = request.getData();
            req.setUserId(user.getId());
            return seckillService.orderV2(req);
        } catch (Exception e) {
            log.error("===秒杀发生异常！===", e);
        }
        return BaseResponse.error(ErrorMessage.SECKILL_FAILED);
    }

    /**
     * nginx+lua秒杀设计 + 下单扣库存接口
     */
    @RequestMapping(value = "/createOrder")
    public BaseResponse createOrder(@Valid @RequestBody BaseRequest<SeckillReq> request) {
        try {
            CommonWebUser user = WebUserUtil.getLoginUser();
            if (Objects.isNull(user)) {
                return BaseResponse.error(ErrorMessage.LOGIN_ERROR);
            }
            SeckillReq req = request.getData();
            req.setUserId(user.getId());
            return seckillService.createOrder(req);
        } catch (Exception e) {
            log.error("===秒杀发生异常！===", e);
        }
        return BaseResponse.error(ErrorMessage.SECKILL_FAILED);
    }
    /**
     * 秒杀url隐藏——（秒杀url隐藏——前置验证码接口）
     */
    @RequestMapping(value = "/verifyHash")
    public BaseResponse<String> getVerifyHash(@Valid @RequestBody BaseRequest<SeckillReq> request) {
        CommonWebUser user = WebUserUtil.getLoginUser();
        if (Objects.isNull(user)) {
            return BaseResponse.error(ErrorMessage.LOGIN_ERROR);
        }
        SeckillReq req = request.getData();
        req.setUserId(user.getId());
        return seckillService.getVerifyHash(req);
    }

    /**
     * 单频次限制
     * @param request
     * @return
     */
    @RequestMapping(value = "/v4/order")
    public BaseResponse orderV4(@Valid @RequestBody BaseRequest<SeckillReqV2> request) {
        try {
            CommonWebUser user = WebUserUtil.getLoginUser();
            if (Objects.isNull(user)) {
                return BaseResponse.error(ErrorMessage.LOGIN_ERROR);
            }
            SeckillReqV2 req = request.getData();
            req.setUserId(user.getId());
            return seckillService.orderV4(req);
        } catch (Exception e) {
            log.error("===秒杀发生异常！===", e);
        }
        return BaseResponse.error(ErrorMessage.SECKILL_FAILED);
    }
}
