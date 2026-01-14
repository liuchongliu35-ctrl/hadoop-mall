package com.seckill.mapper.impl;

import com.seckill.entity.User;
import com.seckill.mapper.UserMapper;
import com.seckill.util.HBaseUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.filter.SubstringComparator;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
public class UserMapperImpl implements UserMapper {

    // --- 表名与列族常量 ---
    private static final String TABLE_NAME = "user_profile";

    // 列族
    private static final String CF_BASE = "cf_base";
    private static final String CF_ACCOUNT = "cf_account";

    // cf_base 列
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";
    private static final String COL_NICKNAME = "nickname";
    private static final String COL_PHONE = "phone";
    private static final String COL_STATUS = "status";
    private static final String COL_DELETED = "deleted";
    private static final String COL_CREATE_TIME = "create_time";

    // cf_account 列
    private static final String COL_ROLE = "role";
    private static final String COL_LEVEL = "level";
    private static final String COL_POINTS = "points";
    private static final String COL_BALANCE = "balance";
    private static final String COL_LOGIN_COUNT = "login_count";
    private static final String COL_LAST_LOGIN = "last_login";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private HBaseUtil hBaseUtil;

    /**
     * 生成 RowKey
     */
    private String rowKey(Long userId) {
        return String.valueOf(userId);
    }

    @Override
    public int saveOrUpdate(User user) {
        if (user == null || user.getId() == null) {
            log.warn("User对象为空或ID缺失，无法保存");
            return 0;
        }

        try {
            // 构建 Put 对象
            Put put = new Put(Bytes.toBytes(rowKey(user.getId())));

            // 1. 填充基础信息 (cf_base)
            putStr(put, CF_BASE, COL_USERNAME, user.getUsername());
            putStr(put, CF_BASE, COL_PASSWORD, user.getPassword());
            putStr(put, CF_BASE, COL_NICKNAME, user.getNickname());
            putStr(put, CF_BASE, COL_PHONE, user.getPhone());
            putInt(put, CF_BASE, COL_STATUS, user.getStatus());
            // 默认为0
            putInt(put, CF_BASE, COL_DELETED, user.getDeleted() != null ? user.getDeleted() : 0);
            putTime(put, CF_BASE, COL_CREATE_TIME, user.getCreateTime());

            // 2. 填充账户信息 (cf_account)
            putInt(put, CF_ACCOUNT, COL_ROLE, user.getRole());
            putInt(put, CF_ACCOUNT, COL_LEVEL, user.getLevel());
            putLong(put, CF_ACCOUNT, COL_POINTS, user.getPoints());
            putBg(put, CF_ACCOUNT, COL_BALANCE, user.getBalance());
            putLong(put, CF_ACCOUNT, COL_LOGIN_COUNT, user.getLoginCount());
            putTime(put, CF_ACCOUNT, COL_LAST_LOGIN, user.getLastLogin());

            // 调用 HBaseUtil 的 putBatch
            hBaseUtil.putBatch(TABLE_NAME, Collections.singletonList(put));
            return 1;
        } catch (Exception e) {
            log.error("保存用户到HBase失败, userId={}", user.getId(), e);
            throw new RuntimeException("HBase操作失败");
        }
    }

    @Override
    public User selectById(Long id) {
        if (id == null) return null;
        try {
            // 调用 HBaseUtil 的 get
            Result result = hBaseUtil.get(TABLE_NAME, rowKey(id));
            if (result == null || result.isEmpty()) {
                return null;
            }

            // 检查逻辑删除
            Integer deleted = getInt(result, CF_BASE, COL_DELETED);
            if (deleted != null && deleted == 1) {
                return null;
            }
            return convertToUser(result, id);
        } catch (IOException e) {
            log.error("查询用户失败, userId={}", id, e);
            return null;
        }
    }

    @Override
    public User selectByUsername(String username) {
        if (!StringUtils.hasText(username)) return null;

        // 构造 Filter 扫描器
        Scan scan = new Scan();
        SingleColumnValueFilter filter = new SingleColumnValueFilter(
                Bytes.toBytes(CF_BASE),
                Bytes.toBytes(COL_USERNAME),
                CompareFilter.CompareOp.EQUAL,
                new SubstringComparator(username)
        );
        filter.setFilterIfMissing(true);
        scan.setFilter(filter);

        // 使用 HBaseUtil 的 scan 方法
        try (ResultScanner scanner = hBaseUtil.scan(TABLE_NAME, scan)) {
            for (Result result : scanner) {
                // 检查逻辑删除
                Integer deleted = getInt(result, CF_BASE, COL_DELETED);
                if (deleted == null || deleted == 0) {
                    Long userId = Long.parseLong(Bytes.toString(result.getRow()));
                    return convertToUser(result, userId);
                }
            }
        } catch (IOException e) {
            log.error("根据用户名查询HBase失败: {}", username, e);
        }
        return null;
    }

    @Override
    public List<User> selectUserList(String username, int limit, int offset) {
        List<User> userList = new ArrayList<>();
        Scan scan = new Scan();

        // 如果有用户名筛选
        if (StringUtils.hasText(username)) {
            SingleColumnValueFilter filter = new SingleColumnValueFilter(
                    Bytes.toBytes(CF_BASE),
                    Bytes.toBytes(COL_USERNAME),
                    CompareFilter.CompareOp.EQUAL,
                    new SubstringComparator(username)
            );
            filter.setFilterIfMissing(true);
            scan.setFilter(filter);
        }

        // 使用 HBaseUtil 的 scan 方法
        try (ResultScanner scanner = hBaseUtil.scan(TABLE_NAME, scan)) {
            int currentIdx = 0; // 当前遍历的有效行索引
            int collected = 0;  // 已收集的数量

            for (Result result : scanner) {
                // 1. 过滤逻辑删除
                Integer deleted = getInt(result, CF_BASE, COL_DELETED);
                if (deleted != null && deleted == 1) continue;

                // 2. 模拟分页 (Skip)
                if (currentIdx < offset) {
                    currentIdx++;
                    continue;
                }

                // 3. 收集数据 (Limit)
                Long userId = Long.parseLong(Bytes.toString(result.getRow()));
                userList.add(convertToUser(result, userId));
                collected++;
                currentIdx++;

                if (collected >= limit) break;
            }
        } catch (IOException e) {
            log.error("分页扫描用户失败", e);
        }
        return userList;
    }

    @Override
    public long countUser(String username) {
        long total = 0;
        Scan scan = new Scan();

        if (StringUtils.hasText(username)) {
            SingleColumnValueFilter filter = new SingleColumnValueFilter(
                    Bytes.toBytes(CF_BASE),
                    Bytes.toBytes(COL_USERNAME),
                    CompareFilter.CompareOp.EQUAL,
                    new SubstringComparator(username)
            );
            // 如果该行没有username列，跳过
            filter.setFilterIfMissing(true);
            scan.setFilter(filter);
        }
        try (ResultScanner scanner = hBaseUtil.scan(TABLE_NAME, scan)) {
            for (Result r : scanner) {
                // 只要能扫到结果，就计数
                total++;
            }
        } catch (IOException e) {
            log.error("统计用户数量失败", e);
            return 0;
        }
        return total;
    }
    @Override
    public int deleteById(Long id) {
        if (id == null) return 0;
        try {
            // 逻辑删除
            Put put = new Put(Bytes.toBytes(rowKey(id)));
            putInt(put, CF_BASE, COL_DELETED, 1);

            hBaseUtil.putBatch(TABLE_NAME, Collections.singletonList(put));
            return 1;
        } catch (IOException e) {
            log.error("删除用户失败", e);
            return 0;
        }
    }

    // --- 实体转换 ---

    private User convertToUser(Result r, Long userId) {
        User user = new User();
        user.setId(userId);
        // --- 基础信息 (保持不变) ---
        user.setUsername(getStr(r, CF_BASE, COL_USERNAME));
        user.setPassword(getStr(r, CF_BASE, COL_PASSWORD));
        user.setNickname(getStr(r, CF_BASE, COL_NICKNAME));
        user.setPhone(getStr(r, CF_BASE, COL_PHONE));
        user.setStatus(getInt(r, CF_BASE, COL_STATUS));
        user.setDeleted(getInt(r, CF_BASE, COL_DELETED));
        user.setCreateTime(getTime(r, CF_BASE, COL_CREATE_TIME));
        // --- 账户信息 (关键修改部分) ---

        // 1. 分别读取 HBase 中的 role 和 level 列
        Integer hbaseRole = getInt(r, CF_ACCOUNT, COL_ROLE);
        Integer hbaseLevel = getInt(r, CF_ACCOUNT, COL_LEVEL);
        // 2. 【兼容逻辑】：如果 role 列是空的，说明是迁移过来的老数据，
        if (hbaseRole == null && hbaseLevel != null) {
            hbaseRole = hbaseLevel;
        }
        // 3. 【兜底逻辑】：如果还是 null，默认给 0 (普通用户)，防止 Service 层空指针报错
        if (hbaseRole == null) {
            hbaseRole = 0;
        }
        user.setRole(hbaseRole);
        user.setLevel(hbaseLevel); // level 字段照常赋值

        user.setPoints(getLong(r, CF_ACCOUNT, COL_POINTS));
        user.setBalance(getBg(r, CF_ACCOUNT, COL_BALANCE));
        user.setLoginCount(getLong(r, CF_ACCOUNT, COL_LOGIN_COUNT));
        user.setLastLogin(getTime(r, CF_ACCOUNT, COL_LAST_LOGIN));
        return user;
    }

    // --- 写入 Helper (操作 Put 对象) ---

    private void putStr(Put put, String cf, String col, String val) {
        if (val != null) {
            put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(col), Bytes.toBytes(val));
        }
    }

    private void putInt(Put put, String cf, String col, Integer val) {
        if (val != null) {
            put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(col), Bytes.toBytes(String.valueOf(val)));
        }
    }

    private void putLong(Put put, String cf, String col, Long val) {
        if (val != null) {
            put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(col), Bytes.toBytes(String.valueOf(val)));
        }
    }

    private void putBg(Put put, String cf, String col, BigDecimal val) {
        if (val != null) {
            put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(col), Bytes.toBytes(val.toString()));
        }
    }

    private void putTime(Put put, String cf, String col, LocalDateTime time) {
        if (time != null) {
            put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(col), Bytes.toBytes(time.format(DATE_TIME_FORMATTER)));
        }
    }

    // --- 读取 Helper (使用 HBaseUtil 解析 Result) ---

    private String getStr(Result r, String cf, String col) {
        return hBaseUtil.getValueFromResult(r, cf, col);
    }

    private Integer getInt(Result r, String cf, String col) {
        String v = getStr(r, cf, col);
        return StringUtils.hasText(v) ? Integer.parseInt(v) : null;
    }

    private Long getLong(Result r, String cf, String col) {
        String v = getStr(r, cf, col);
        return StringUtils.hasText(v) ? Long.parseLong(v) : 0L;
    }

    private BigDecimal getBg(Result r, String cf, String col) {
        String v = getStr(r, cf, col);
        return StringUtils.hasText(v) ? new BigDecimal(v) : BigDecimal.ZERO;
    }

    private LocalDateTime getTime(Result r, String cf, String col) {
        String v = getStr(r, cf, col);
        return StringUtils.hasText(v) ? LocalDateTime.parse(v, DATE_TIME_FORMATTER) : null;
    }
}
