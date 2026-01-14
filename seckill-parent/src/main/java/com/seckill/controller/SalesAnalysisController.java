package com.seckill.controller;

import com.seckill.common.Result;
import com.seckill.entity.SalesData;
import com.seckill.service.SalesAnalysisService;
import com.seckill.vo.HotProductVO;
import com.seckill.vo.SalesDashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "销售分析")
@RestController
@RequestMapping("/api/sales")
public class SalesAnalysisController {

    @Autowired
    private SalesAnalysisService salesAnalysisService;

    @Operation(summary = "今日销售看板")
    @GetMapping("/dashboard/today")
    public Result<SalesDashboardVO> getTodayDashboard() {
        SalesDashboardVO vo = salesAnalysisService.getTodayDashboard();
        return Result.success(vo);
    }

    @Operation(summary = "今日热门商品排行")
    @GetMapping("/hot/daily")
    public Result<List<HotProductVO>> getTodayHotProducts(
            @Parameter(description = "TOP N，默认10")
            @RequestParam(value = "top", defaultValue = "10") Integer top) {
        List<HotProductVO> list = salesAnalysisService.getTodayHotProducts(top);
        return Result.success(list);
    }

    @Operation(summary = "历史每日销售数据")
    @GetMapping("/history/daily")
    public Result<List<SalesData>> getDailySales(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "productId", required = false) Long productId) {
        List<SalesData> list = salesAnalysisService.getDailySales(startDate, endDate, productId);
        return Result.success(list);
    }
}


