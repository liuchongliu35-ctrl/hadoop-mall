package com.seckill.migration;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

import java.io.IOException;

/**
 * 快速清空 HBase 订单表数据的工具类
 */
public class ClearOrderDataDemo {

    // === HBase 配置 ===
    private static final String ZK_QUORUM = "192.168.124.100";
    private static final String ZK_PORT = "2181";
    // 要清空的表名 (根据你之前的代码，订单表是 order_history)
    private static final String TABLE_NAME = "order_history";

    public static void main(String[] args) throws IOException {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", ZK_QUORUM);
        conf.set("hbase.zookeeper.property.clientPort", ZK_PORT);

        System.out.println("正在连接 HBase...");

        try (Connection connection = ConnectionFactory.createConnection(conf);
             Admin admin = connection.getAdmin()) {

            TableName tableName = TableName.valueOf(TABLE_NAME);

            // 1. 检查表是否存在
            if (!admin.tableExists(tableName)) {
                System.err.println("错误：表 " + TABLE_NAME + " 不存在，无法清除数据！");
                return;
            }

            System.out.println("准备清空表: " + TABLE_NAME + " ...");

            // 2. 检查表是否处于启用状态，Truncate 前通常需要表是 Disable 状态，
            //    但在 Java API 中，admin.truncateTable 会自动处理，或者我们可以手动处理以确保安全。
            if (admin.isTableEnabled(tableName)) {
                System.out.println("正在禁用表...");
                admin.disableTable(tableName);
            }

            // 3. 执行 Truncate (截断)
            // 参数 preserveSplits = true，表示保留原有的分区信息（如果有做预分区的话）
            System.out.println("正在执行 Truncate (截断/清空) 操作...");
            admin.truncateTable(tableName, true);

            System.out.println("表 " + TABLE_NAME + " 数据已全部清空！");

            // 4. (可选) 验证一下是否真的空了，或者 admin.truncateTable 执行完后表通常会自动 Enable
            if (!admin.isTableEnabled(tableName)) {
                admin.enableTable(tableName);
            }
            System.out.println("表状态已恢复为 Enable。");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("清空数据失败！请检查 HBase 服务状态。");
        }
    }
}
