package com.sky.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;

import lombok.extern.slf4j.Slf4j;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Slf4j
public class ShopController {

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 
     * @param status
     * @return
     */

    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status) {
        log.info("设置店铺状态为：{}", status == 1 ? "营业中" : "打样中");
        redisTemplate.opsForValue().set(MessageConstant.KEY, status);
        return Result.success();
    }


    @GetMapping("/status")
    public Result<Integer> getStatus() {
        Integer status = (Integer) redisTemplate.opsForValue().get(MessageConstant.KEY);
        log.info("获得到店铺的状态为:{}", status == 1 ? "营业中" : "打样中");
        return Result.success(status);
    }

}
