package com.sky.controller.admin;

import com.sky.constant.CacheConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 菜品管理控制器
 */
@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 新增菜品
     */
    @ApiOperation("新增菜品")
    @PostMapping

    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        //清空所有菜品的缓存数据
        String key = CacheConstant.DISH_LIST_PREFIX + dishDTO.getCategoryId();
        cleanCache(key);
        return Result.success();
    }
    @GetMapping("/page")
    @ApiOperation("分页查询菜品")
    public  Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("分页查询菜品：{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }
    /**
     * 菜品批量删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("菜品批量删除")
    public Result delete( @RequestParam List<Long> ids){
        log.info("删除菜品：{}", ids);
        dishService.deleteBatch(ids);
        //删除缓存数据
        //可能会影响多个key，需要根据分类id删除所有缓存数据 所有以dish_开头的key
        cleanCache(CacheConstant.DISH_LIST_PREFIX + "*");
        return Result.success();
    }
    @GetMapping("/{id}")
    @ApiOperation("根据菜品id查询菜品详情")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据菜品id查询菜品详情：{}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }
    /**
     * 菜品起售停售
     * @param status
     * @param id
     */
    @PutMapping("/status/{status}")
    @ApiOperation("菜品起售停售")
    public Result updateStatus(@PathVariable Integer status,@RequestParam Long id){
        log.info("菜品起售停售：{}", id);
        dishService.startOrStop(status, id);
        //删除缓存数据
        cleanCache(CacheConstant.DISH_LIST_PREFIX + "*");
        return Result.success();
    }
    /**
     * 清理缓存数据
     * @param pattern
     */
    private void cleanCache(String pattern){
        //删除缓存数据
        Set keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
    /**
     * 根据分类id查询菜品选项
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品选项")
    public Result<List<DishVO>> list( Long categoryId){
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        List<DishVO> list = dishService.listWithFlavor(dish);
        return Result.success(list);
    }
    /**
     * 修改菜品
     */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品：{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);
        //删除缓存数据
        cleanCache(CacheConstant.DISH_LIST_PREFIX + "*");
        return Result.success();
    }
}
