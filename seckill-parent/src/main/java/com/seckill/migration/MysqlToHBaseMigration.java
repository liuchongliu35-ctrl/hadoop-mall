package com.seckill.migration;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 简单的测试迁移程序：
 * 将原 MySQL seckill 库中的部分表数据导入到 HBase 对应表中。
 *
 * 说明：
 *  - 仅用于一次性迁移测试，运行 main 方法即可。
 *  - 为了简单起见，没有做回滚和幂等控制，重复执行可能覆盖同一行。
 */
public class MysqlToHBaseMigration {

    // === MySQL 配置（与 application.yml / seckill.sql 对应） ===
    private static final String MYSQL_URL =
            "jdbc:mysql://localhost:3306/seckill?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASS = "123mysql";

    // === HBase 配置（与项目 HBaseConfig 保持一致） ===
    private static final String ZK_QUORUM = "192.168.124.100";
    private static final String ZK_PORT = "2181";

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws Exception {
        // 1. 初始化 HBase 连接
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", ZK_QUORUM);
        conf.set("hbase.zookeeper.property.clientPort", ZK_PORT);

        try (Connection hbaseConn = ConnectionFactory.createConnection(conf);
             java.sql.Connection mysqlConn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASS)) {

            System.out.println("MySQL & HBase 连接成功，开始迁移数据...");

            migrateProducts(mysqlConn, hbaseConn);
            migrateSeckillActivities(mysqlConn, hbaseConn);
            migrateSeckillOrders(mysqlConn, hbaseConn);
            migrateUsers(mysqlConn, hbaseConn);

            System.out.println("数据迁移完成！");
        }
    }

    /**
     * t_product -> HBase: product_info
     */
    private static void migrateProducts(java.sql.Connection mysqlConn, Connection hbaseConn) throws Exception {
        String sql = "SELECT id, product_name, product_desc, price, stock, category_id, img_url, status, " +
                "create_time, update_time, deleted FROM t_product";
        try (PreparedStatement ps = mysqlConn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery();
             Table table = hbaseConn.getTable(TableName.valueOf("product_info"))) {

            System.out.println("开始迁移商品表 t_product -> product_info");
            while (rs.next()) {
                long id = rs.getLong("id");
                String rowKey = String.valueOf(id);
                Put put = new Put(Bytes.toBytes(rowKey));

                // cf_base
                putString(put, "cf_base", "name", rs.getString("product_name"));
                putString(put, "cf_base", "category", rs.getString("category_id"));
                putString(put, "cf_base", "price", rs.getBigDecimal("price"));
                putString(put, "cf_base", "status", rs.getInt("status"));
                putString(put, "cf_base", "create_time", toDateTimeStr(rs.getTimestamp("create_time")));

                // cf_detail
                putString(put, "cf_detail", "description", rs.getString("product_desc"));
                putString(put, "cf_detail", "images", rs.getString("img_url")); // 简化：单图片直接存

                // cf_stock
                putString(put, "cf_stock", "total_stock", rs.getInt("stock"));

                // cf_stat
                putString(put, "cf_stat", "update_time", toDateTimeStr(rs.getTimestamp("update_time")));

                table.put(put);
            }
            System.out.println("商品表迁移完成");
        }
    }

    /**
     * t_seckill_activity -> HBase: seckill_activity
     */
    private static void migrateSeckillActivities(java.sql.Connection mysqlConn, Connection hbaseConn) throws Exception {
        String sql = "SELECT id, activity_name, product_id, seckill_price, seckill_stock, start_time, end_time, " +
                "status, create_time, update_time, deleted FROM t_seckill_activity";
        try (PreparedStatement ps = mysqlConn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery();
             Table table = hbaseConn.getTable(TableName.valueOf("seckill_activity"))) {

            System.out.println("开始迁移秒杀活动表 t_seckill_activity -> seckill_activity");
            while (rs.next()) {
                long id = rs.getLong("id");
                String rowKey = String.valueOf(id);
                Put put = new Put(Bytes.toBytes(rowKey));

                putString(put, "cf_base", "activity_name", rs.getString("activity_name"));
                putString(put, "cf_base", "product_id", rs.getLong("product_id"));
                putString(put, "cf_base", "seckill_price", rs.getBigDecimal("seckill_price"));
                putString(put, "cf_base", "seckill_stock", rs.getInt("seckill_stock"));
                putString(put, "cf_base", "status", rs.getInt("status"));
                putString(put, "cf_base", "start_time", toDateTimeStr(rs.getTimestamp("start_time")));
                putString(put, "cf_base", "end_time", toDateTimeStr(rs.getTimestamp("end_time")));
                putString(put, "cf_base", "create_time", toDateTimeStr(rs.getTimestamp("create_time")));
                putString(put, "cf_base", "update_time", toDateTimeStr(rs.getTimestamp("update_time")));
                putString(put, "cf_base", "deleted", rs.getInt("deleted"));

                table.put(put);
            }
            System.out.println("秒杀活动表迁移完成");
        }
    }

    /**
     * seckill_order -> HBase: order_history
     */
    private static void migrateSeckillOrders(java.sql.Connection mysqlConn, Connection hbaseConn) throws Exception {
        String sql = "SELECT id, user_id, activity_id, product_id, product_name, order_no, seckill_price, " +
                "quantity, total_amount, status, pay_time, create_time, update_time, deleted FROM seckill_order";
        try (PreparedStatement ps = mysqlConn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery();
             Table table = hbaseConn.getTable(TableName.valueOf("order_history"))) {

            System.out.println("开始迁移订单表 seckill_order -> order_history");
            while (rs.next()) {
                long id = rs.getLong("id");
                String rowKey = String.valueOf(id);
                Put put = new Put(Bytes.toBytes(rowKey));

                // cf_base
                putString(put, "cf_base", "user_id", rs.getLong("user_id"));
                putString(put, "cf_base", "total_amount", rs.getBigDecimal("total_amount"));
                putString(put, "cf_base", "status", rs.getInt("status"));
                putString(put, "cf_base", "create_time", toDateTimeStr(rs.getTimestamp("create_time")));
                putString(put, "cf_base", "pay_time", toDateTimeStr(rs.getTimestamp("pay_time")));
                putString(put, "cf_base", "update_time", toDateTimeStr(rs.getTimestamp("update_time")));

                // 兼容我们扩展的字段
                putString(put, "cf_base", "order_no", rs.getString("order_no"));
                putString(put, "cf_base", "activity_id", rs.getLong("activity_id"));
                putString(put, "cf_base", "product_id", rs.getLong("product_id"));

                // cf_items（这里简单按单商品订单存一条JSON，可选）
                String itemJson = String.format(
                        "{\"product_id\":%d,\"name\":\"%s\",\"price\":%s,\"quantity\":%d,\"amount\":%s}",
                        rs.getLong("product_id"),
                        rs.getString("product_name"),
                        rs.getBigDecimal("seckill_price"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("total_amount")
                );
                putStringRaw(put, "cf_items", "item_1", itemJson);

                // deleted
                putString(put, "cf_base", "deleted", rs.getInt("deleted"));

                table.put(put);
            }
            System.out.println("订单表迁移完成");
        }
    }

    /**
     * t_user -> HBase: user_profile（仅迁基础信息）
     */
    private static void migrateUsers(java.sql.Connection mysqlConn, Connection hbaseConn) throws Exception {
        String sql = "SELECT id, username, password, phone, email, role, create_time, update_time, deleted FROM t_user";
        try (PreparedStatement ps = mysqlConn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery();
             Table table = hbaseConn.getTable(TableName.valueOf("user_profile"))) {

            System.out.println("开始迁移用户表 t_user -> user_profile");
            while (rs.next()) {
                long id = rs.getLong("id");
                String rowKey = String.valueOf(id);
                Put put = new Put(Bytes.toBytes(rowKey));

                // cf_base
                putString(put, "cf_base", "username", rs.getString("username"));
                putString(put, "cf_base", "phone", rs.getString("phone"));
                putString(put, "cf_base", "email", rs.getString("email"));
                putString(put, "cf_base", "register_time", toDateTimeStr(rs.getTimestamp("create_time")));
                putString(put, "cf_base", "status", 1); // 简化：全部设为正常

                // cf_account
                putString(put, "cf_account", "level", rs.getInt("role")); // 简化：role 当作等级

                // cf_behavior
                putString(put, "cf_behavior", "last_login", toDateTimeStr(rs.getTimestamp("update_time")));

                // deleted
                putString(put, "cf_base", "deleted", rs.getInt("deleted"));

                table.put(put);
            }
            System.out.println("用户表迁移完成");
        }
    }

    // ===== 工具方法 =====

    private static void putString(Put put, String cf, String col, Object val) {
        if (val == null) return;
        put.addColumn(
                Bytes.toBytes(cf),
                Bytes.toBytes(col),
                System.currentTimeMillis(),
                Bytes.toBytes(String.valueOf(val))
        );
    }

    private static void putStringRaw(Put put, String cf, String col, String val) {
        if (val == null) return;
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(col),
                System.currentTimeMillis(), Bytes.toBytes(val));
    }

    private static String toDateTimeStr(java.sql.Timestamp ts) {
        if (ts == null) return null;
        LocalDateTime ldt = ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return ldt.format(DATE_TIME_FMT);
    }
}


