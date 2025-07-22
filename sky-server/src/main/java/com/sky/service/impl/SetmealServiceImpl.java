package com.sky.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;

@Service
public class SetmealServiceImpl implements SetmealService{

    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> setmealVOs = setmealMapper.page(setmealPageQueryDTO);
        return new PageResult<>(setmealVOs.getTotal(), setmealVOs.getResult());
    }

    @Override
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);
        Long id = setmeal.getId();
        List<SetmealDish> setmealDishs = setmealDTO.getSetmealDishes();
        if (setmealDishs != null && !setmealDishs.isEmpty()) {
            setmealDishs.forEach(item -> {
                item.setSetmealId(id);
            });
        }
        setmealMapper.insertDish(setmealDishs);
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        setmealMapper.deleteSetmealDishId(ids);
        setmealMapper.deleteSetmeaId(ids);
    }

    @Override
    public SetmealVO getByid(Long id) {
       // 1. 查询套餐基本信息
        SetmealVO setmealVO = setmealMapper.getByid(id);
        if (setmealVO != null) {
            // 2. 查询套餐包含的菜品
            List<SetmealDish> setmealDishes = setmealMapper.getBySetmealId(id);
            // 3. 将菜品列表设置到 VO 中
            setmealVO.setSetmealDishes(setmealDishes);
        }
        return setmealVO;
    }

    @Override
    @Transactional
    public void update(SetmealVO setmealVO) {
        setmealVO.setUpdateTime(LocalDateTime.now());
        setmealMapper.update(setmealVO);
        List<Long> ids = new ArrayList<>();
        ids.add(setmealVO.getId());
        setmealMapper.deleteSetmealDishId(ids);
        List<SetmealDish> setmealDishs = setmealVO.getSetmealDishes();
        if (setmealDishs != null && !setmealDishs.isEmpty()) {
            setmealDishs.forEach(item -> {
                item.setSetmealId(setmealVO.getId());
            });
        }
        setmealMapper.insertDish(setmealDishs);
    }

    @Override
    public void Status(Integer status, Long id) {
        // TODO Auto-generated method stub
        Setmeal setmeal = new Setmeal();
        setmeal.setId(id);
        setmeal.setStatus(status);
        setmealMapper.updateStatus(setmeal);
    }
}
