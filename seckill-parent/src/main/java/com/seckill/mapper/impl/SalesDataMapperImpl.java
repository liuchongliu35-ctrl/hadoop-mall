package com.seckill.mapper.impl;

import com.seckill.entity.SalesData;
import com.seckill.mapper.SalesDataMapper;
import com.seckill.util.HBaseUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 表：sales_data
 * 列族：
 *  - cf_daily: date, product_id, category_id, sale_count, sale_amount
 */
@Slf4j
@Repository
@Primary
public class SalesDataMapperImpl implements SalesDataMapper {

    private static final String TABLE_NAME = "sales_data";
    private static final String CF_DAILY = "cf_daily";
    private static final String COL_DATE = "date";
    private static final String COL_PRODUCT_ID = "product_id";
    private static final String COL_CATEGORY_ID = "category_id";
    private static final String COL_SALE_COUNT = "sale_count";
    private static final String COL_SALE_AMOUNT = "sale_amount";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_DATE;

    @Autowired
    private HBaseUtil hBaseUtil;

    private String rowKey(LocalDate date, Long productId) {
        return date.format(DATE_FMT) + "_" + productId;
    }

//    todo 将成功的交易信息记录到Hbase的销售情况记录表中
    @Override
    public void addDailySale(LocalDate date, Long productId, Long categoryId, long quantity, BigDecimal amount) {
        try {
            String rk = rowKey(date, productId);
            Result result = hBaseUtil.get(TABLE_NAME, rk);
            long newCount = quantity;
            BigDecimal newAmount = amount;
            if (result != null && !result.isEmpty()) {
                String countStr = hBaseUtil.getValueFromResult(result, CF_DAILY, COL_SALE_COUNT);
                String amtStr = hBaseUtil.getValueFromResult(result, CF_DAILY, COL_SALE_AMOUNT);
                if (countStr != null) {
                    newCount += Long.parseLong(countStr);
                }
                if (amtStr != null) {
                    newAmount = newAmount.add(new BigDecimal(amtStr));
                }
            }

            Put put = new Put(Bytes.toBytes(rk));
            put.addColumn(Bytes.toBytes(CF_DAILY), Bytes.toBytes(COL_DATE),
                    System.currentTimeMillis(), Bytes.toBytes(date.format(DATE_FMT)));
            put.addColumn(Bytes.toBytes(CF_DAILY), Bytes.toBytes(COL_PRODUCT_ID),
                    System.currentTimeMillis(), Bytes.toBytes(String.valueOf(productId)));
            if (categoryId != null) {
                put.addColumn(Bytes.toBytes(CF_DAILY), Bytes.toBytes(COL_CATEGORY_ID),
                        System.currentTimeMillis(), Bytes.toBytes(String.valueOf(categoryId)));
            }
            put.addColumn(Bytes.toBytes(CF_DAILY), Bytes.toBytes(COL_SALE_COUNT),
                    System.currentTimeMillis(), Bytes.toBytes(String.valueOf(newCount)));
            put.addColumn(Bytes.toBytes(CF_DAILY), Bytes.toBytes(COL_SALE_AMOUNT),
                    System.currentTimeMillis(), Bytes.toBytes(newAmount.toPlainString()));

            hBaseUtil.putBatch(TABLE_NAME, List.of(put));
        } catch (Exception e) {
            log.error("写入SalesData失败", e);
        }
    }

//    todo 查询每日历史销售记录
    @Override
    public List<SalesData> queryDailySales(LocalDate startDate, LocalDate endDate, Long productId) {
        List<SalesData> list = new ArrayList<>();
        try (ResultScanner scanner = hBaseUtil.scan(TABLE_NAME, new Scan())) {
            for (Result r : scanner) {
                String dateStr = hBaseUtil.getValueFromResult(r, CF_DAILY, COL_DATE);
                String productIdStr = hBaseUtil.getValueFromResult(r, CF_DAILY, COL_PRODUCT_ID);
                if (dateStr == null || productIdStr == null) continue;
                LocalDate date = LocalDate.parse(dateStr, DATE_FMT);
                Long pid = Long.parseLong(productIdStr);
                if (date.isBefore(startDate) || date.isAfter(endDate)) continue;
                if (productId != null && !productId.equals(pid)) continue;

                SalesData data = new SalesData();
                data.setDate(date);
                data.setProductId(pid);
                String catStr = hBaseUtil.getValueFromResult(r, CF_DAILY, COL_CATEGORY_ID);
                if (catStr != null) data.setCategoryId(Long.parseLong(catStr));
                String countStr = hBaseUtil.getValueFromResult(r, CF_DAILY, COL_SALE_COUNT);
                String amtStr = hBaseUtil.getValueFromResult(r, CF_DAILY, COL_SALE_AMOUNT);
                if (countStr != null) data.setSaleCount(Long.parseLong(countStr));
                if (amtStr != null) data.setSaleAmount(new BigDecimal(amtStr));
                data.setCreateTime(LocalDateTime.now());
                data.setUpdateTime(LocalDateTime.now());
                data.setDeleted(0);
                list.add(data);
            }
        } catch (Exception e) {
            log.error("查询SalesData失败", e);
        }
        list.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        return list;
    }
}


