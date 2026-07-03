package com.sky.controller.user;

import com.sky.constant.CacheConstant;
import com.sky.constant.StatusConstant;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;


@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "店铺相关接口")
@Slf4j
public class ShopController {
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取营业状态
     * @return
     */
    @ApiOperation("获取营业状态")
    @GetMapping ("/status")
    public Result<Integer> getStatus() {
        //log.info("获取店铺营业状态");
        Integer status = (Integer) redisTemplate.opsForValue().get(CacheConstant.SHOP_STATUS);
        if (status == null) {
            status = StatusConstant.DISABLE;
            redisTemplate.opsForValue().set(CacheConstant.SHOP_STATUS, status);
        }
        //判断当前店铺营业状态是否存在
        log.info("获取店铺营业状态为：{}", status == 1?"营业中" : "打烊中");
        return Result.success(status);
    }
}
