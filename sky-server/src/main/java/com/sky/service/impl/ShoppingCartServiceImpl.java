package com.sky.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingMapper;
import com.sky.service.ShoppingCartService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingMapper shoppingMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Override
    @Transactional
    public void addShopping(ShoppingCartDTO shoppingCartDTO) {
        // 1. 创建一个用于查询的购物车实体
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        // 2. 查询购物车中是否已存在该商品
        List<ShoppingCart> list = shoppingMapper.list(shoppingCart);

        // 3. 判断查询结果
        if (list != null && !list.isEmpty()) {
            // 3.1 如果已存在，则更新数量（在数据库查出的对象上+1）
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingMapper.updateNumber(cart);
        } else {
            // 3.2 如果不存在，则插入新记录
            // 判断是菜品还是套餐，并设置名称、图片、金额
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
                // 是菜品
                Dish dish = dishMapper.getByid(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            } else {
                // 是套餐
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getBysetmeal(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }

            // 设置初始数量为1和创建时间
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());

            // 插入到数据库
            shoppingMapper.insert(shoppingCart);
        }
    }

    @Override
    public List<ShoppingCart> showShopping() {
        ShoppingCart cartBuilder = ShoppingCart.builder()
                .userId(BaseContext.getCurrentId())
                .build();
        return shoppingMapper.list(cartBuilder);
    }

    @Override
    public void cleanShopping() {
        Long userid = BaseContext.getCurrentId();
        shoppingMapper.cleanShopping(userid);
    }

    @Override
    public void deleteShopping(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        // 2. 查询购物车中是否已存在该商品
        List<ShoppingCart> list = shoppingMapper.list(shoppingCart);
        if (list != null && !list.isEmpty()) {
            ShoppingCart cart = list.get(0);
            Integer number = cart.getNumber();
            if (number > 1) {
                // 2.1.1 如果商品数量大于1，则执行数量减一的操作
                cart.setNumber(number - 1);
                shoppingMapper.updateNumber(cart);
                log.info("商品数量减一，当前数量: {}", cart.getNumber());
            } else {
                // 2.1.2 如果商品数量等于1，则直接删除该条记录
                shoppingMapper.deleteShopping(cart);
                log.info("商品从购物车中移除，ID: {}", cart.getId());
            }
        }
    }

}
