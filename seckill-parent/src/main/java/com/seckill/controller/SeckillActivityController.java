package com.seckill.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.annotation.RateLimit;
import com.seckill.common.LimitType;
import com.seckill.common.PageQuery;
import com.seckill.common.PageResult;
import com.seckill.common.Result;
import com.seckill.dto.SeckillActivityDTO;
import com.seckill.service.SeckillActivityService;
import com.seckill.util.JwtUtil;
import com.seckill.vo.ActivityStatusVo;
import com.seckill.vo.SeckillActivityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@Tag(name = "秒杀活动管理")
@RestController
@RequestMapping("/api/seckill/activity")
@Validated
public class SeckillActivityController {
    
    @Autowired
    private SeckillActivityService activityService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping
    @Operation(summary = "新增秒杀活动")
    public Result<Void> saveActivity(@Valid @RequestBody SeckillActivityDTO activityDTO, 
                                    HttpServletRequest request) {
        // 检查权限 - 只有管理员可以创建活动
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Integer role = jwtUtil.getRole(token);
            if (!Integer.valueOf(1).equals(role)) { // 1-管理员
                return Result.error(403, "无权限操作");
            }
        } else {
            return Result.error(401, "请先登录");
        }
        
        activityService.saveActivity(activityDTO);
        return Result.success();
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "修改秒杀活动")
    public Result<Void> updateActivity(@Parameter(description = "活动ID") @PathVariable Long id,
                                       @Valid @RequestBody SeckillActivityDTO activityDTO,
                                       HttpServletRequest request) {
        // 检查权限 - 只有管理员可以修改活动
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Integer role = jwtUtil.getRole(token);
            if (!Integer.valueOf(1).equals(role)) { // 1-管理员
                return Result.error(403, "无权限操作");
            }
        } else {
            return Result.error(401, "请先登录");
        }
        
        activityService.updateActivity(id, activityDTO);
        return Result.success();
    }
    
    @GetMapping("/list")
    @Operation(summary = "分页查询秒杀活动")
    public Result<PageResult<SeckillActivityVO>> getActivityList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "活动状态") @RequestParam(required = false) Integer status) {
        
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPageNum(pageNum);
        pageQuery.setPageSize(pageSize);
        IPage<SeckillActivityVO> page = activityService.getActivityList(pageQuery, status);
        
        PageResult<SeckillActivityVO> pageResult = PageResult.of(page.getTotal(), page.getRecords());
        
        return Result.success(pageResult);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询秒杀活动详情")
    public Result<SeckillActivityVO> getActivityById(@Parameter(description = "活动ID") @PathVariable Long id) {
        SeckillActivityVO activity = activityService.getActivityById(id);
        return Result.success(activity);
    }
    
    @PutMapping("/status/{id}")
    @Operation(summary = "手动修改活动状态")
    public Result<Void> updateActivityStatus(@Parameter(description = "活动ID") @PathVariable Long id,
                                             @Parameter(description = "状态") @RequestParam Integer status,
                                             HttpServletRequest request) {
        // 检查权限 - 只有管理员可以修改活动状态
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Integer role = jwtUtil.getRole(token);
            if (!Integer.valueOf(1).equals(role)) { // 1-管理员
                return Result.error(403, "无权限操作");
            }
        } else {
            return Result.error(401, "请先登录");
        }
        
        activityService.updateActivityStatus(id, status);
        return Result.success();
    }
    
    @GetMapping("/active")
    @Operation(summary = "获取活跃的秒杀活动列表")
    public Result<List<SeckillActivityVO>> getActiveActivities() {
        List<SeckillActivityVO> activities = activityService.getActiveActivities();
        return Result.success(activities);
    }
}