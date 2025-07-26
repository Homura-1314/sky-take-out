package com.sky.service;

import java.util.List;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

public interface DishService {

    void save(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

	void delete(List<Long> ids);

    DishVO getByid(Long id);

    void updata(DishDTO dishDTO);

    List<Dish> listByid(Long categoryid);


    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);

    void startOrStop(Integer status, Long id);
}
