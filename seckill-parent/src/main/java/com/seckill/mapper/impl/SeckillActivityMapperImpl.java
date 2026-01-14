package com.seckill.mapper.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.entity.SeckillActivity;
import com.seckill.mapper.SeckillActivityMapper;
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
 * 秒杀活动 HBase 实现
 * 表：seckill_activity
 */
@Slf4j
@Repository
@Primary
public class SeckillActivityMapperImpl implements SeckillActivityMapper {

    private static final String TABLE_NAME = "seckill_activity";
    private static final String CF_BASE = "cf_base";
    private static final String COL_NAME = "activity_name";
    private static final String COL_PRODUCT_ID = "product_id";
    private static final String COL_SECKILL_PRICE = "seckill_price";
    private static final String COL_SECKILL_STOCK = "seckill_stock";
    private static final String COL_START_TIME = "start_time";
    private static final String COL_END_TIME = "end_time";
    private static final String COL_STATUS = "status";
    private static final String COL_CREATE_TIME = "create_time";
    private static final String COL_UPDATE_TIME = "update_time";
    private static final String COL_DELETED = "deleted";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private HBaseUtil hBaseUtil;

    @Autowired
    private HBaseIdGenerator idGenerator;

    private String rk(Long id) {
        return String.valueOf(id);
    }

    @Override
    public int insert(SeckillActivity activity) {
        try {
            if (activity.getId() == null) {
                activity.setId(idGenerator.generateId(TABLE_NAME));
            }
            Put put = new Put(Bytes.toBytes(rk(activity.getId())));
            putStr(COL_NAME, activity.getActivityName(), put);
            putLong(COL_PRODUCT_ID, activity.getProductId(), put);
            putDec(COL_SECKILL_PRICE, activity.getSeckillPrice(), put);
            putInt(COL_SECKILL_STOCK, activity.getSeckillStock(), put);
            putTime(COL_START_TIME, activity.getStartTime(), put);
            putTime(COL_END_TIME, activity.getEndTime(), put);
            putInt(COL_STATUS, activity.getStatus(), put);
            putTime(COL_CREATE_TIME, activity.getCreateTime(), put);
            putTime(COL_UPDATE_TIME, activity.getUpdateTime(), put);
            putInt(COL_DELETED, activity.getDeleted() == null ? 0 : activity.getDeleted(), put);
            hBaseUtil.putBatch(TABLE_NAME, List.of(put));
            return 1;
        } catch (Exception e) {
            log.error("插入活动失败", e);
            return 0;
        }
    }

    @Override
    public int updateById(SeckillActivity activity) {
        SeckillActivity exist = selectById(activity.getId());
        if (exist == null) return 0;
        return insert(activity);
    }

    @Override
    public int deleteById(Long id) {
        try {
            Put put = new Put(Bytes.toBytes(rk(id)));
            putInt(COL_DELETED, 1, put);
            hBaseUtil.putBatch(TABLE_NAME, List.of(put));
            return 1;
        } catch (Exception e) {
            log.error("删除活动失败", e);
            return 0;
        }
    }

    @Override
    public SeckillActivity selectById(Long id) {
        try {
            Result result = hBaseUtil.get(TABLE_NAME, rk(id));
            if (result == null || result.isEmpty()) return null;
            return convert(result, id);
        } catch (Exception e) {
            log.error("查询活动失败", e);
            return null;
        }
    }

    @Override
    public IPage<SeckillActivity> selectActivityPage(Page<SeckillActivity> page, Integer status) {
        List<SeckillActivity> all = scan(status, false);
        return page(all, page);
    }

    @Override
    public List<SeckillActivity> selectActiveActivities() {
        return scan(null, true);
    }

    @Override
    public List<SeckillActivity> selectAll() {
        return scan(null, false);
    }

    private List<SeckillActivity> scan(Integer status, boolean onlyActive) {
        List<SeckillActivity> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        try (ResultScanner scanner = hBaseUtil.scan(TABLE_NAME, new Scan())) {
            for (Result r : scanner) {
                Long id = Long.parseLong(Bytes.toString(r.getRow()));
                SeckillActivity a = convert(r, id);
                if (a == null) continue;
                if (a.getDeleted() != null && a.getDeleted() == 1) continue;
                if (status != null && !status.equals(a.getStatus())) continue;
                if (onlyActive) {
                    if (!(a.getStatus() != null && (a.getStatus() == 0 || a.getStatus() == 1))) continue;
                    if (a.getEndTime() != null && !a.getEndTime().isAfter(now)) continue;
                }
                list.add(a);
            }
        } catch (Exception e) {
            log.error("扫描活动失败", e);
        }
        list.sort((x, y) -> {
            if (x.getStartTime() == null && y.getStartTime() == null) return 0;
            if (x.getStartTime() == null) return 1;
            if (y.getStartTime() == null) return -1;
            return x.getStartTime().compareTo(y.getStartTime());
        });
        return list;
    }

    private Page<SeckillActivity> page(List<SeckillActivity> all, Page<SeckillActivity> page) {
        int total = all.size();
        int start = (int) ((page.getCurrent() - 1) * page.getSize());
        int end = Math.min(start + (int) page.getSize(), total);
        List<SeckillActivity> sub = start < total ? all.subList(start, end) : new ArrayList<>();
        Page<SeckillActivity> p = new Page<>(page.getCurrent(), page.getSize(), total);
        p.setRecords(sub);
        return p;
    }

    private SeckillActivity convert(Result r, Long id) {
        try {
            SeckillActivity a = new SeckillActivity();
            a.setId(id);
            a.setActivityName(getStr(r, COL_NAME));
            a.setProductId(getLong(r, COL_PRODUCT_ID));
            a.setSeckillPrice(getDec(r, COL_SECKILL_PRICE));
            a.setSeckillStock(getInt(r, COL_SECKILL_STOCK));
            a.setStartTime(getTime(r, COL_START_TIME));
            a.setEndTime(getTime(r, COL_END_TIME));
            a.setStatus(getInt(r, COL_STATUS));
            a.setCreateTime(getTime(r, COL_CREATE_TIME));
            a.setUpdateTime(getTime(r, COL_UPDATE_TIME));
            a.setDeleted(getInt(r, COL_DELETED));
            return a;
        } catch (Exception e) {
            log.error("转换活动失败", e);
            return null;
        }
    }

    private void putStr(String col, Object val, Put put) {
        if (val == null) return;
        put.addColumn(Bytes.toBytes(CF_BASE), Bytes.toBytes(col), System.currentTimeMillis(), Bytes.toBytes(String.valueOf(val)));
    }

    private void putLong(String col, Long val, Put put) {
        if (val == null) return;
        put.addColumn(Bytes.toBytes(CF_BASE), Bytes.toBytes(col), System.currentTimeMillis(), Bytes.toBytes(String.valueOf(val)));
    }

    private void putInt(String col, Integer val, Put put) {
        if (val == null) return;
        put.addColumn(Bytes.toBytes(CF_BASE), Bytes.toBytes(col), System.currentTimeMillis(), Bytes.toBytes(String.valueOf(val)));
    }

    private void putDec(String col, BigDecimal val, Put put) {
        if (val == null) return;
        put.addColumn(Bytes.toBytes(CF_BASE), Bytes.toBytes(col), System.currentTimeMillis(), Bytes.toBytes(val.toPlainString()));
    }

    private void putTime(String col, LocalDateTime time, Put put) {
        if (time == null) return;
        put.addColumn(Bytes.toBytes(CF_BASE), Bytes.toBytes(col), System.currentTimeMillis(), Bytes.toBytes(time.format(FMT)));
    }

    private String getStr(Result r, String col) {
        return hBaseUtil.getValueFromResult(r, CF_BASE, col);
    }

    private Integer getInt(Result r, String col) {
        String v = getStr(r, col);
        return v == null ? null : Integer.parseInt(v);
    }

    private Long getLong(Result r, String col) {
        String v = getStr(r, col);
        return v == null ? null : Long.parseLong(v);
    }

    private BigDecimal getDec(Result r, String col) {
        String v = getStr(r, col);
        return v == null ? null : new BigDecimal(v);
    }

    private LocalDateTime getTime(Result r, String col) {
        String v = getStr(r, col);
        return v == null ? null : LocalDateTime.parse(v, FMT);
    }
}

