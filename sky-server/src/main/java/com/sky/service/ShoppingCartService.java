package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {

    void addShopping(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> showShopping();

    void cleanShopping();

    void deleteShopping(ShoppingCartDTO shoppingCartDTO);
}
