package com.sky.controller.user;

import com.sky.constant.CacheConstant;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品选项
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品选项")
    public Result<List<DishVO>> list(Long categoryId) {
        //构造redis中的key 规则:dish_分类id
        String key = CacheConstant.DISH_LIST_PREFIX + categoryId;
        //查询Redis中是否存在菜品数据
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
        if (list != null) {
            return Result.success(list);
        }
        //如果存在 直接返回 无需查询数据库
        //如果不存在 需要查询数据库，将数据缓存到Redis中
       Dish dish = new Dish();
       dish.setStatus(StatusConstant.ENABLE);
       dish.setCategoryId(categoryId);
       list = dishService.listWithFlavor(dish);

       //将查询到的菜品数据缓存到Redis中
       redisTemplate.opsForValue().set(key, list, 30, TimeUnit.MINUTES);
       return Result.success(list);
    }


}
