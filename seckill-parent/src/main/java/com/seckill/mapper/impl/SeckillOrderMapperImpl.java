package com.seckill.mapper.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.entity.SeckillOrder;
import com.seckill.mapper.SeckillOrderMapper;
import com.seckill.util.HBaseIdGenerator;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * SeckillOrderMapper 的 HBase 实现
 * 表：order_history
 */
@Slf4j
@Repository
@Primary
public class SeckillOrderMapperImpl implements SeckillOrderMapper {

    private static final String TABLE_NAME = "order_history";
    private static final String CF_BASE = "cf_base";
    private static final String CF_ADDRESS = "cf_address";
    private static final String CF_ITEMS = "cf_items";
    private static final String CF_LOGISTICS = "cf_logistics";

    private static final String COL_USER_ID = "user_id";
    private static final String COL_TOTAL_AMOUNT = "total_amount";
    private static final String COL_DISCOUNT_AMOUNT = "discount_amount";
    private static final String COL_ACTUAL_AMOUNT = "actual_amount";
    private static final String COL_STATUS = "status";
    private static final String COL_PAY_METHOD = "pay_method";
    private static final String COL_CREATE_TIME = "create_time";
    private static final String COL_PAY_TIME = "pay_time";
    private static final String COL_DELIVER_TIME = "deliver_time";
    private static final String COL_COMPLETE_TIME = "complete_time";

    private static final String COL_ORDER_NO = "order_no";
    private static final String COL_ACTIVITY_ID = "activity_id";
    private static final String COL_PRODUCT_ID = "product_id";
    private static final String COL_PRODUCT_NAME = "product_name";
    private static final String COL_SECKILL_PRICE = "seckill_price";
    private static final String COL_QUANTITY = "quantity";

    private static final String COL_RECEIVER = "receiver";
    private static final String COL_PHONE = "phone";
    private static final String COL_ADDRESS = "address";
    private static final String COL_POSTCODE = "postcode";

    private static final String COL_ORDER_ITEMS = "order_items";
    private static final String COL_EXPRESS_COMPANY = "express_company";
    private static final String COL_EXPRESS_NO = "express_no";
    private static final String COL_LOGISTICS_INFO = "logistics_info";

    private static final String COL_DELETED = "deleted";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private HBaseUtil hBaseUtil;

    @Autowired
    private HBaseIdGenerator idGenerator;

    private String rowKey(Long id) {
        return String.valueOf(id);
    }

    @Override
    public int insert(SeckillOrder order) {
        try {
            if (order.getId() == null) {
                order.setId(idGenerator.generateId(TABLE_NAME));
            }
            String rk = rowKey(order.getId());
            Put put = new Put(Bytes.toBytes(rk));

            // base
            putStr(CF_BASE, COL_USER_ID, order.getUserId(), put);
            putStr(CF_BASE, COL_ORDER_NO, order.getOrderNo(), put);
            putStr(CF_BASE, COL_ACTIVITY_ID, order.getActivityId(), put);
            putStr(CF_BASE, COL_PRODUCT_ID, order.getProductId(), put);
            putStr(CF_BASE, COL_PRODUCT_NAME, order.getProductName(), put);
            putDec(CF_BASE, COL_SECKILL_PRICE, order.getSeckillPrice(), put);
            putInt(CF_BASE, COL_QUANTITY, order.getQuantity(), put);
            putDec(CF_BASE, COL_TOTAL_AMOUNT, order.getTotalAmount(), put);
            putDec(CF_BASE, COL_DISCOUNT_AMOUNT, order.getDiscountAmount(), put);
            putDec(CF_BASE, COL_ACTUAL_AMOUNT, order.getActualAmount(), put);
            putInt(CF_BASE, COL_STATUS, order.getStatus(), put);
            putStr(CF_BASE, COL_PAY_METHOD, order.getPayMethod(), put);
            putTime(CF_BASE, COL_CREATE_TIME, order.getCreateTime(), put);
            putTime(CF_BASE, COL_PAY_TIME, order.getPayTime(), put);
            putTime(CF_BASE, COL_DELIVER_TIME, order.getDeliverTime(), put);
            putTime(CF_BASE, COL_COMPLETE_TIME, order.getCompleteTime(), put);

            // address
            putStr(CF_ADDRESS, COL_RECEIVER, order.getReceiver(), put);
            putStr(CF_ADDRESS, COL_PHONE, order.getPhone(), put);
            putStr(CF_ADDRESS, COL_ADDRESS, order.getAddress(), put);
            putStr(CF_ADDRESS, COL_POSTCODE, order.getPostcode(), put);

            // items
            putStr(CF_ITEMS, COL_ORDER_ITEMS, order.getOrderItems(), put);

            // logistics
            putStr(CF_LOGISTICS, COL_EXPRESS_COMPANY, order.getExpressCompany(), put);
            putStr(CF_LOGISTICS, COL_EXPRESS_NO, order.getExpressNo(), put);
            putStr(CF_LOGISTICS, COL_LOGISTICS_INFO, order.getLogisticsInfo(), put);

            // deleted
            putInt(CF_BASE, COL_DELETED, order.getDeleted() == null ? 0 : order.getDeleted(), put);

            hBaseUtil.putBatch(TABLE_NAME, List.of(put));
            return 1;
        } catch (Exception e) {
            log.error("插入订单失败", e);
            return 0;
        }
    }

    @Override
    public int updateById(SeckillOrder order) {
        try {
            SeckillOrder exist = selectById(order.getId());
            if (exist == null) {
                return 0;
            }
            // 用put覆盖
            return insert(order);
        } catch (Exception e) {
            log.error("更新订单失败", e);
            return 0;
        }
    }

    @Override
    public int deleteById(Long id) {
        try {
            Put put = new Put(Bytes.toBytes(rowKey(id)));
            putInt(CF_BASE, COL_DELETED, 1, put);
            hBaseUtil.putBatch(TABLE_NAME, List.of(put));
            return 1;
        } catch (Exception e) {
            log.error("删除订单失败", e);
            return 0;
        }
    }

    @Override
    public SeckillOrder selectById(Long id) {
        try {
            Result result = hBaseUtil.get(TABLE_NAME, rowKey(id));
            if (result == null || result.isEmpty()) {
                return null;
            }
            return convert(result, id);
        } catch (Exception e) {
            log.error("查询订单失败", e);
            return null;
        }
    }

    @Override
    public IPage<SeckillOrder> selectOrderPage(Page<SeckillOrder> page, Integer status) {
        List<SeckillOrder> all = scanOrders(status, null);
        return buildPage(page, all);
    }

    @Override
    public List<SeckillOrder> selectListByUser(Long userId, Integer status) {
        return scanOrders(status, userId);
    }

    private List<SeckillOrder> scanOrders(Integer status, Long userId) {
        List<SeckillOrder> list = new ArrayList<>();
        try (ResultScanner scanner = hBaseUtil.scan(TABLE_NAME, new Scan())) {
            for (Result r : scanner) {
                Long id = Long.parseLong(Bytes.toString(r.getRow()));
                SeckillOrder order = convert(r, id);
                if (order == null) continue;
                if (order.getDeleted() != null && order.getDeleted() == 1) continue;
                if (status != null && !status.equals(order.getStatus())) continue;
                if (userId != null && !userId.equals(order.getUserId())) continue;
                list.add(order);
            }
        } catch (Exception e) {
            log.error("扫描订单失败", e);
        }
        // sort createTime desc
        list.sort((a, b) -> {
            if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
            if (a.getCreateTime() == null) return 1;
            if (b.getCreateTime() == null) return -1;
            return b.getCreateTime().compareTo(a.getCreateTime());
        });
        return list;
    }

    private Page<SeckillOrder> buildPage(Page<SeckillOrder> page, List<SeckillOrder> all) {
        int total = all.size();
        int start = (int) ((page.getCurrent() - 1) * page.getSize());
        int end = Math.min(start + (int) page.getSize(), total);
        List<SeckillOrder> sub = start < total ? all.subList(start, end) : new ArrayList<>();
        Page<SeckillOrder> p = new Page<>(page.getCurrent(), page.getSize(), total);
        p.setRecords(sub);
        return p;
    }

    private SeckillOrder convert(Result r, Long id) {
        try {
            SeckillOrder o = new SeckillOrder();
            o.setId(id);
            o.setUserId(getLong(r, CF_BASE, COL_USER_ID));
            o.setOrderNo(getStr(r, CF_BASE, COL_ORDER_NO));
            o.setActivityId(getLong(r, CF_BASE, COL_ACTIVITY_ID));
            o.setProductId(getLong(r, CF_BASE, COL_PRODUCT_ID));
            o.setProductName(getStr(r, CF_BASE, COL_PRODUCT_NAME));
            o.setSeckillPrice(getDec(r, CF_BASE, COL_SECKILL_PRICE));
            o.setQuantity(getInt(r, CF_BASE, COL_QUANTITY));
            o.setTotalAmount(getDec(r, CF_BASE, COL_TOTAL_AMOUNT));
            o.setDiscountAmount(getDec(r, CF_BASE, COL_DISCOUNT_AMOUNT));
            o.setActualAmount(getDec(r, CF_BASE, COL_ACTUAL_AMOUNT));
            o.setStatus(getInt(r, CF_BASE, COL_STATUS));
            o.setPayMethod(getStr(r, CF_BASE, COL_PAY_METHOD));
            o.setCreateTime(getTime(r, CF_BASE, COL_CREATE_TIME));
            o.setPayTime(getTime(r, CF_BASE, COL_PAY_TIME));
            o.setDeliverTime(getTime(r, CF_BASE, COL_DELIVER_TIME));
            o.setCompleteTime(getTime(r, CF_BASE, COL_COMPLETE_TIME));

            o.setReceiver(getStr(r, CF_ADDRESS, COL_RECEIVER));
            o.setPhone(getStr(r, CF_ADDRESS, COL_PHONE));
            o.setAddress(getStr(r, CF_ADDRESS, COL_ADDRESS));
            o.setPostcode(getStr(r, CF_ADDRESS, COL_POSTCODE));

            o.setOrderItems(getStr(r, CF_ITEMS, COL_ORDER_ITEMS));

            o.setExpressCompany(getStr(r, CF_LOGISTICS, COL_EXPRESS_COMPANY));
            o.setExpressNo(getStr(r, CF_LOGISTICS, COL_EXPRESS_NO));
            o.setLogisticsInfo(getStr(r, CF_LOGISTICS, COL_LOGISTICS_INFO));

            o.setDeleted(getInt(r, CF_BASE, COL_DELETED));
            return o;
        } catch (Exception e) {
            log.error("转换订单失败", e);
            return null;
        }
    }

    private void putStr(String cf, String col, Object val, Put put) {
        if (val == null) return;
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(col), System.currentTimeMillis(), Bytes.toBytes(String.valueOf(val)));
    }

    private void putInt(String cf, String col, Integer val, Put put) {
        if (val == null) return;
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(col), System.currentTimeMillis(), Bytes.toBytes(String.valueOf(val)));
    }

    private void putDec(String cf, String col, BigDecimal val, Put put) {
        if (val == null) return;
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(col), System.currentTimeMillis(), Bytes.toBytes(val.toPlainString()));
    }

    private void putTime(String cf, String col, LocalDateTime time, Put put) {
        if (time == null) return;
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(col), System.currentTimeMillis(), Bytes.toBytes(time.format(DATE_TIME_FORMATTER)));
    }

    private String getStr(Result r, String cf, String col) {
        return hBaseUtil.getValueFromResult(r, cf, col);
    }

    private Integer getInt(Result r, String cf, String col) {
        String v = getStr(r, cf, col);
        return v == null ? null : Integer.parseInt(v);
    }

    private Long getLong(Result r, String cf, String col) {
        String v = getStr(r, cf, col);
        return v == null ? null : Long.parseLong(v);
    }

    private BigDecimal getDec(Result r, String cf, String col) {
        String v = getStr(r, cf, col);
        return v == null ? null : new BigDecimal(v);
    }

    private LocalDateTime getTime(Result r, String cf, String col) {
        String v = getStr(r, cf, col);
        return v == null ? null : LocalDateTime.parse(v, DATE_TIME_FORMATTER);
    }
}

