package com.sky.controller.user;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.sky.dto.ShoppingCartDTO;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
@Slf4j
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @PostMapping("/add")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("添加购物车：{}", shoppingCartDTO);
        shoppingCartService.addShopping(shoppingCartDTO);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<ShoppingCart>> list(){
        log.info("查看购物车");
        List<ShoppingCart> shoppingCarts = shoppingCartService.showShopping();
        return Result.success(shoppingCarts);
    }

    @DeleteMapping("/clean")
    public Result clean(){
        log.info("清空购物车");
        shoppingCartService.cleanShopping();
        return Result.success();
    }

    @PostMapping("/sub")
    public Result deleteShopping(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("删除某一个商品：{}", shoppingCartDTO);
        shoppingCartService.deleteShopping(shoppingCartDTO);
        return Result.success();
    }

}
