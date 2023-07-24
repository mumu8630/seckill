package com.bmw.seckill.controller;

import com.alibaba.fastjson.JSON;
import com.bmw.seckill.common.base.BaseRequest;
import com.bmw.seckill.common.base.BaseResponse;
import com.bmw.seckill.common.entity.CommonWebUser;
import com.bmw.seckill.common.exception.ErrorMessage;
import com.bmw.seckill.model.SeckillUser;
import com.bmw.seckill.model.http.UserReq;
import com.bmw.seckill.model.http.UserResp;
import com.bmw.seckill.service.UserService;
import com.bmw.seckill.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;


/**
 * TODO 类描述
 *
 * @author mumu
 * @date 2023/7/11 8:16
 */

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
   private final String USER_PHONE_CODE_BEFORE="u:p:c:b";
   @Autowired
    private UserService userService;
   @Autowired
    private RedisUtil redisUtil;


    /**
     * 验证码发送
     * @param req
     * @return
     */
   @PostMapping(value = "/getPhoneSmsCode")
    public BaseResponse<Boolean> getPhoneSmsCode(@Valid @RequestBody BaseRequest<UserReq.BaseUserInfo> req){
       String phone = req.getData().getPhone();
       SeckillUser seckillUser = userService.findByPhone(phone);
       //判断用户是否存在
       //调用接口发送验证码，验证码存储在redis中
       if (seckillUser != null) {
           //短信验证码，默认为6位
           //String randomCode = String.valueOf(new Random().nextInt(10000000));
//           设置redis键 值 时间
           String randomCode = "123456";
           redisUtil.set(USER_PHONE_CODE_BEFORE + phone,randomCode,60*30);
           return BaseResponse.OK(true);
       }
       else {
           return BaseResponse.error(ErrorMessage.USERNULL_ERROR,false);
       }
   }

    /**
     * 验证码验证
     * @param req
     * @return
     * @throws Exception
     */
   @PostMapping("/userPhoneLogin")
    public BaseResponse userPhoneLogin(@Valid @RequestBody BaseRequest<UserReq.LoginUserInfo> req) throws Exception{
       UserReq.LoginUserInfo loginUserInfo = req.getData();
       //从redis获取 键名为 u:p:c:b手机号的值
       Object existObj = redisUtil.get(USER_PHONE_CODE_BEFORE + loginUserInfo.getPhone());
       if (existObj == null || !existObj.toString().equals(loginUserInfo.getSmsCode())){
//           没有验证码 或 对应验证码不同
           return BaseResponse.error(ErrorMessage.SMSCODE_ERROR);
       }else {
           //验证码如果正确，在缓存中删除验证码
           redisUtil.del(USER_PHONE_CODE_BEFORE+loginUserInfo.getPhone());
           SeckillUser seckillUser = userService.findByPhone(loginUserInfo.getPhone());
           CommonWebUser commonWebUser = new CommonWebUser();
           BeanUtils.copyProperties(seckillUser,commonWebUser);
           String token = UUID.randomUUID().toString().replaceAll("-","");
           //设置token超时时间为一个月 存入redis
           redisUtil.set(token, JSON.toJSONString(commonWebUser),60*60*24*30);
           UserResp.BaseUserResp resp = new UserResp.BaseUserResp();
           resp.setToken(token);
           return BaseResponse.OK(resp);
       }
   }


}
