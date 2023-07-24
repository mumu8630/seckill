package com.bmw.seckill.controller;

import cn.hutool.core.lang.Assert;
import com.bmw.seckill.common.util.bean.CommonQueryBean;
import com.bmw.seckill.model.SeckillProducts;
import com.bmw.seckill.service.ISeckillProductsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;

/**
 * TODO 类描述
 *  商品的controller界面
 * @author mumu
 * @date 2023/7/11 17:15
 */

@Controller
@RequestMapping("/product")
@Slf4j
public class SeckillProductsController {

    @Autowired
    ISeckillProductsService seckillProductsService;

    /**
     * 查询商品列表
     * @param model
     * @param name
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping(value = {"/listPageSeckillProducts",""})
    public String listPageSeckillProducts(Model model,
                                          String name,
                                          @RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum,
                                          @RequestParam(value = "pageSize",defaultValue = "20")Integer pageSize){
        SeckillProducts searchItem = new SeckillProducts();
        if (StringUtils.isNotEmpty(name)){
            searchItem.setName(name);
            model.addAttribute("name",name);
        }
        CommonQueryBean queryBean = new CommonQueryBean();
        queryBean.setPageSize(pageSize);
        queryBean.setStart((pageNum-1)* pageSize);
        List<SeckillProducts> seckillProductsList = seckillProductsService.list4Page(searchItem, queryBean);

        int total = seckillProductsService.count(searchItem);
        int totalPageNum = total/pageSize;
        if (total % pageSize >0){
            //如果未能除尽，则需要另开一页
            totalPageNum++;
        }
        model.addAttribute("total",total);
        model.addAttribute("totalPage",totalPageNum);
        model.addAttribute("list",seckillProductsList);
        model.addAttribute("pageSize",pageSize);
        model.addAttribute("pageNum",pageNum);

        return "product/listPageSeckillProducts";
    }

    /**
     * 新增商品秒杀详情
     * @return
     */
    @RequestMapping("/beforeCreateProduct")
    public String beforeCreateProduct(){
        return "product/beforeCreateProduct";
    }


    /**
     * 新增或更新商品信息
     * @param uniqueId
     * @param name
     * @param startBuyTimeStr
     * @param amount
     * @param desc
     * @return
     * @throws Exception
     */
    @RequestMapping("/doCreateProduct")
    public String doCreateProduct(String uniqueId,
                                  String name,
                                  String startBuyTimeStr,
                                  Integer amount,
                                  String desc
                                  ) throws Exception{
        Assert.notNull(uniqueId,"uniqueId is not null");
        Assert.notNull(name,"name is not null");
        Assert.notNull(startBuyTimeStr,"startBuyTimeStr is not null");
        Assert.notNull(amount,"amount is not null");

        SeckillProducts seckillProducts = new SeckillProducts();
        seckillProducts.setName(name);
        seckillProducts.setStartBuyTime(DateUtils.parseDate(startBuyTimeStr,"yyyy-MM-dd HH:mm:SS"));
        seckillProducts.setCount(amount);
        seckillProducts.setProductPeriodKey(uniqueId);
        if (StringUtils.isNotEmpty(desc)){
            seckillProducts.setProductDesc(desc);
        }
        seckillProductsService.uniqueInsert(seckillProducts);

        return "redirect:listPageSeckillProducts?isSave=yes";

    }

    /**
     * 查看详情
     * @param model
     * @param id
     * @return
     */
    @RequestMapping("/showProductItem")
    public String showProductItem(Model model,
                                  Long id){
        SeckillProducts seckillProducts = seckillProductsService.selectByPrimaryKey(id);
        if(seckillProducts != null){
            model.addAttribute("item",seckillProducts);
        }
        return "product/showProductItem";
    }

    /**
     * 更新上下架状态
     * @param id
     * @param status
     * @return
     */
    @RequestMapping("/updateProductStatus")
    public String updateProductStatus(Long id,
                                      Integer status){
        SeckillProducts seckillProducts = seckillProductsService.selectByPrimaryKey(id);
        seckillProducts.setStatus(status);
        //若商品为上线状态 但 商品已经被删除 则修改回来 变成未删除状态
        if (SeckillProducts.STATUS_IS_ONLINE.equals(status) && SeckillProducts.IS_DEALED.equals(seckillProducts.getIsDeleted())){
            seckillProducts.setIsDeleted(0);
        }
        seckillProducts.setUpdatedTime(new Date());
        seckillProductsService.updateByPrimaryKeySelective(seckillProducts);
        return "redirect:listPageSeckillProducts";
    }

    @RequestMapping("/deleteProduct")
    public String deleteProduct(Long id){
    //    不使用物理删除，改成更新idDeleted字段，变成逻辑update
        SeckillProducts seckillProducts = seckillProductsService.selectByPrimaryKey(id);
        seckillProducts.setIsDeleted(SeckillProducts.IS_DEALED);
        seckillProducts.setUpdatedTime(new Date());
        seckillProductsService.updateByPrimaryKeySelective(seckillProducts);

        return "redirect:listPageSeckillProducts";

    }
}
