package com.sky.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.sky.entity.DishFlavor;
import com.sky.exception.BaseException;
import com.sky.mapper.DishFlavorMapper;
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
        // 创建一个查询条件的副本，并设置用户ID
        ShoppingCart queryCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, queryCart);
        Long userId = BaseContext.getCurrentId();
        queryCart.setUserId(userId);

        // 根据完整的条件（包括口味）查询购物车
        List<ShoppingCart> list = shoppingMapper.list(queryCart);

        //  判断是更新还是插入
        if (list != null && !list.isEmpty()) {
            // 3.1 已存在：直接更新数量
            ShoppingCart existingCartItem = list.get(0);
            existingCartItem.setNumber(existingCartItem.getNumber() + 1);
            shoppingMapper.updateNumber(existingCartItem);
        } else {
            // 不存在：创建新的购物车项并插入
            // 创建一个全新的、干净的实体对象用于插入，避免状态污染
            ShoppingCart newCartItem = new ShoppingCart();
            BeanUtils.copyProperties(shoppingCartDTO, newCartItem);
            newCartItem.setUserId(userId);
            newCartItem.setNumber(1); // 初始数量为1
            newCartItem.setCreateTime(LocalDateTime.now());

            // 判断是菜品还是套餐，并填充额外信息
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
                // 是菜品
                Dish dish = dishMapper.getByid(dishId);
                if (dish != null) {
                    newCartItem.setName(dish.getName());
                    newCartItem.setImage(dish.getImage());
                    newCartItem.setAmount(dish.getPrice());
                } else {
                    throw new BaseException("添加购物车的菜品不存在");
                }
            } else {
                // 是套餐
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getBysetmeal(setmealId);
                if (setmeal != null) {
                    newCartItem.setName(setmeal.getName());
                    newCartItem.setImage(setmeal.getImage());
                    newCartItem.setAmount(setmeal.getPrice());
                } else {
                    throw new BaseException("添加购物车的套餐不存在");
                }
            }

            // 插入到数据库
            shoppingMapper.insert(newCartItem);
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
