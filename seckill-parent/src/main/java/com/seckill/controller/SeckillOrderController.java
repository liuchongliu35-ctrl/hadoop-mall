package com.seckill.controller;

import com.seckill.annotation.RateLimit;
import com.seckill.common.LimitType;
import com.seckill.common.PageQuery;
import com.seckill.common.PageResult;
import com.seckill.common.Result;
import com.seckill.dto.CartItemAddDTO;
import com.seckill.dto.SeckillOrderDTO;
import com.seckill.service.SeckillOrderService;
import com.seckill.util.JwtUtil;
import com.seckill.vo.SeckillOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 秒杀订单控制器（使用Redisson分布式锁）
 */
@Tag(name = "秒杀订单管理", description = "秒杀订单相关接口")
@Slf4j
@RestController
@RequestMapping("/api/seckill/order")
public class SeckillOrderController {
    
    @Autowired
    private SeckillOrderService orderService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Operation(summary = "秒杀下单", description = "用户参与秒杀活动下单，使用Redisson分布式锁保证并发安全")
    @PostMapping("/create")
        @RateLimit(
            limitType = LimitType.USER_ACTIVITY,
            limit = 5,
            period = 60,
            message = "请求过于频繁，请1分钟后再试"
    )
    public Result<Long> createOrder(@RequestBody @Valid SeckillOrderDTO orderDTO, 
                                    HttpServletRequest request) {
        // 从token中获取用户ID
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        if (!jwtUtil.validateToken(token)) {
            return Result.error("token已失效");
        }
        
        Long userId = jwtUtil.getUserId(token);
        orderDTO.setUserId(userId);
        
        try {
            Long orderId = orderService.createSeckillOrder(orderDTO);
            log.info("用户 {} 秒杀下单成功，订单ID: {}", userId, orderId);
            return Result.success(orderId);
        } catch (Exception e) {
            log.error("秒杀下单失败", e);
            return Result.error(e.getMessage());
        }
    }
//
//    @Operation(summary = "购物车结算", description = "根据购物车商品列表批量创建订单")
//    @PostMapping("/checkout")
//    public Result<Map<String, Object>> checkoutFromCart(@RequestBody List<CartItemAddDTO> items,
//                                                        @RequestAttribute("userId") Long userId) {
//        try {
//            Map<String, Object> result = orderService.createOrdersFromCart(userId, items);
//            return Result.success(result);
//        } catch (Exception e) {
//            log.error("购物车结算失败", e);
//            return Result.error(e.getMessage());
//        }
//    }
    
    @Operation(summary = "取消订单", description = "取消待支付订单，使用分布式锁保证库存回滚的准确性")
    @PostMapping("/cancel/{orderId}")
    public Result<String> cancelOrder(@PathVariable @Parameter(description = "订单ID") Long orderId,
                                      HttpServletRequest request) {
        // 验证用户token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        if (!jwtUtil.validateToken(token)) {
            return Result.error("token已失效");
        }
        
        try {
            orderService.cancelOrder(orderId);
            return Result.success("订单取消成功");
        } catch (Exception e) {
            log.error("取消订单失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @Operation(summary = "支付订单", description = "支付订单，扣减实际库存")
    @PostMapping("/pay/{orderId}")
    public Result<String> payOrder(@PathVariable @Parameter(description = "订单ID") Long orderId,
                                   HttpServletRequest request) {
        // 验证用户token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        if (!jwtUtil.validateToken(token)) {
            return Result.error("token已失效");
        }
        
        try {
            orderService.payOrder(orderId);
            return Result.success("订单支付成功");
        } catch (Exception e) {
            log.error("支付订单失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @Operation(summary = "查询订单详情", description = "根据订单ID查询订单详情")
    @GetMapping("/detail/{orderId}")
    public Result<SeckillOrderVO> getOrderDetail(@PathVariable @Parameter(description = "订单ID") Long orderId) {
        try {
            SeckillOrderVO orderDetail = orderService.getOrderDetail(orderId);
            return Result.success(orderDetail);
        } catch (Exception e) {
            log.error("查询订单详情失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @Operation(summary = "查询用户订单", description = "查询当前用户的订单列表")
    @GetMapping("/user/list")
    public Result<List<SeckillOrderVO>> getUserOrders(@RequestParam(required = false) Integer status,
                                                      HttpServletRequest request) {
        // 验证用户token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        if (!jwtUtil.validateToken(token)) {
            return Result.error("token已失效");
        }
        
        Long userId = jwtUtil.getUserId(token);
        try {
            List<SeckillOrderVO> orders = orderService.getUserOrders(userId, status);
            return Result.success(orders);
        } catch (Exception e) {
            log.error("查询用户订单失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @Operation(summary = "分页查询订单列表", description = "管理员分页查询订单列表")
    @GetMapping("/list")
    public Result<PageResult<SeckillOrderVO>> getOrderList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status) {
        
        PageQuery pageQuery = new PageQuery(pageNum, pageSize);
        try {
            PageResult<SeckillOrderVO> result = orderService.getOrderList(pageQuery, status);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询订单列表失败", e);
            return Result.error(e.getMessage());
        }
    }
}