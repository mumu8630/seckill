package com.bmw.seckill.controller;

import com.alibaba.fastjson.JSON;
import com.bmw.seckill.common.entity.CommonWebUser;
import com.bmw.seckill.security.WebUserUtil;
import com.bmw.seckill.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO 类描述
 *
 * @author mumu
 * @date 2023/7/10 15:20
 */
@RestController
@RequestMapping("/demo")
@Slf4j
public class DemoController {
    @Autowired
    UserService userService;
    @GetMapping("/test")
    public void checkUserToken(){
        CommonWebUser loginUser = WebUserUtil.getLoginUser();
        System.out.println("==================登录成功====================");
        String json = JSON.toJSONString(loginUser);
        System.out.println(json);
    }
}
