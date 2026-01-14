package com.seckill.migration;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

public class SelectAllTest {
    // === HBase 配置 ===
    private static final String ZK_QUORUM = "192.168.124.100";
    private static final String ZK_PORT = "2181";
    public static void main(String[] args) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", ZK_QUORUM);
        conf.set("hbase.zookeeper.property.clientPort", ZK_PORT);
        System.out.println("正在连接 HBase...");
        try (Connection conn = ConnectionFactory.createConnection(conf)) {
            System.out.println("连接成功，开始抽样扫描...");

            // 1. 扫描秒杀活动表
            scanAndPrint(conn, "seckill_activity", 3);

            // 2. 扫描订单历史表
            scanAndPrint(conn, "order_history", 3);

            // 3. 扫描用户表
            scanAndPrint(conn, "user_profile", 3);
        }
    }
    /**
     * 通用扫描方法
     */
    private static void scanAndPrint(Connection conn, String tableNameStr, int limit) {
        System.out.println("\n========================================================");
        System.out.println("正在扫描表: [" + tableNameStr + "]");
        System.out.println("========================================================");
        try (Table table = conn.getTable(TableName.valueOf(tableNameStr))) {
            Scan scan = new Scan();
            // 如果数据量极大，建议设置 scan.setLimit(limit) (HBase 2.0+ 支持)
            // 这里为了兼容性，在循环中手动 break

            try (ResultScanner scanner = table.getScanner(scan)) {
                int count = 0;
                for (Result result : scanner) {
                    count++;
                    String rowKey = Bytes.toString(result.getRow());
                    System.out.println("RowKey: " + rowKey);
                    // 根据表名调用不同的打印逻辑
                    if ("seckill_activity".equals(tableNameStr)) {
                        printActivity(result);
                    } else if ("order_history".equals(tableNameStr)) {
                        printOrder(result);
                    } else if ("user_profile".equals(tableNameStr)) {
                        printUser(result);
                    }
                    System.out.println("----------------------------------------");

                    if (count >= limit) {
                        System.out.println(">> 已达到显示上限 (" + limit + "条)，停止扫描当前表。");
                        break;
                    }
                }
                if (count == 0) {
                    System.out.println("(该表中没有数据)");
                }
            }
        } catch (Exception e) {
            System.err.println("扫描表 " + tableNameStr + " 失败: " + e.getMessage());
        }
    }
    // --- 1. 打印秒杀活动数据 ---
    private static void printActivity(Result r) {
        System.out.println("  [基本信息]");
        System.out.println("    - 活动名称: " + getValue(r, "cf_base", "activity_name"));
        System.out.println("    - 商品ID:   " + getValue(r, "cf_base", "product_id"));
        System.out.println("    - 秒杀价格: " + getValue(r, "cf_base", "seckill_price"));
        System.out.println("    - 库存:     " + getValue(r, "cf_base", "seckill_stock"));
        System.out.println("  [时间范围]");
        System.out.println("    - 开始: " + getValue(r, "cf_base", "start_time"));
        System.out.println("    - 结束: " + getValue(r, "cf_base", "end_time"));
    }
    // --- 2. 打印订单数据 ---
    private static void printOrder(Result r) {
        System.out.println("  [订单概况]");
        System.out.println("    - 订单号: " + getValue(r, "cf_base", "order_no"));
        System.out.println("    - 用户ID: " + getValue(r, "cf_base", "user_id"));
        System.out.println("    - 总金额: " + getValue(r, "cf_base", "total_amount"));
        System.out.println("    - 状态:   " + getValue(r, "cf_base", "status"));
        System.out.println("    - 时间:   " + getValue(r, "cf_base", "create_time"));
        System.out.println("  [商品详情 (JSON)]");
        // 这里对应迁移代码中的 putStringRaw(put, "cf_items", "item_1", itemJson);
        System.out.println("    - Item: " + getValue(r, "cf_items", "item_1"));
    }
    // --- 3. 打印用户数据 ---
    private static void printUser(Result r) {
        System.out.println("  [用户信息]");
        System.out.println("    - 用户名: " + getValue(r, "cf_base", "username"));
        System.out.println("    - 手机号: " + getValue(r, "cf_base", "phone"));
        System.out.println("    - 邮箱:   " + getValue(r, "cf_base", "email"));
        System.out.println("  [账户等级]");
        // 迁移代码中: putString(put, "cf_account", "level", rs.getInt("role"));
        System.out.println("    - Level: " + getValue(r, "cf_account", "level"));
    }
    // --- 工具方法 ---
    private static String getValue(Result result, String family, String qualifier) {
        byte[] valBytes = result.getValue(Bytes.toBytes(family), Bytes.toBytes(qualifier));
        return valBytes == null ? "null" : Bytes.toString(valBytes);
    }
}
