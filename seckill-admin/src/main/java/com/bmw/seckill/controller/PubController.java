package com.bmw.seckill.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@RequestMapping("/pub")
public class PubController {

    @RequestMapping("/login")
    public String beforelogin(){

        return "pub/beforeLogin";
    }
}