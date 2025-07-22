package com.sky.controller.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
public class SetmealControlller {

    @Autowired
    private SetmealService setmealService;

    /**
     * 
     * @param setmealPageQueryDTO
     * @return PageResult
     */

    @GetMapping("/page")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("分页查询：{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.page(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 
     * @param setmealVO
     * @return
     */
    @PostMapping
    public Result save(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐：{}", setmealDTO);
        setmealService.save(setmealDTO);
        return Result.success();
    }

    /**
     * 
     * @param ids
     * @return
     */

    @DeleteMapping
    public Result Delete(@RequestParam List<Long> ids) {
        log.info("根据id批量删除套餐：{}", ids);
        setmealService.delete(ids);
        return Result.success();
    }

    /**
     * 
     * @param id
     * @return
     */

    @GetMapping("/{id}")
    public Result<SetmealVO> getByid(@PathVariable Long id) {
        log.info("根据id查询套餐：{}", id);
        SetmealVO setmealVO = setmealService.getByid(id);
        return Result.success(setmealVO);
    }

    /**
     * 
     * @param setmealVO
     * @return
     */

    @PutMapping
    public Result update(@RequestBody SetmealVO setmealVO) {
        log.info("修改套餐：{}", setmealVO);
        setmealService.update(setmealVO);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    public Result Status(@PathVariable Integer status, Long id) {
        log.info("修改套餐状态:{},{}", status, id);
        setmealService.Status(status, id);
        return Result.success();
    }

}
