package com.seckill.migration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public class DeleteTest {
    // === HBase 配置 ===
    private static final String ZK_QUORUM = "192.168.124.100";
    private static final String ZK_PORT = "2181";
    private static final String TABLE_NAME = "user_profile";

    // 列族和列名
    private static final byte[] CF_BASE = Bytes.toBytes("cf_base");
    private static final byte[] COL_USERNAME = Bytes.toBytes("username");
    public static void main(String[] args) throws IOException {
        // 1. 初始化 HBase 连接
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", ZK_QUORUM);
        conf.set("hbase.zookeeper.property.clientPort", ZK_PORT);
        System.out.println("正在连接 HBase...");

        try (Connection conn = ConnectionFactory.createConnection(conf);
             Table table = conn.getTable(TableName.valueOf(TABLE_NAME))) {
            System.out.println("连接成功，开始扫描并清理用户...");
            // 2. 准备扫描器
            Scan scan = new Scan();
            // 优化：只查询 username 列，减少网络传输
            scan.addColumn(CF_BASE, COL_USERNAME);
            List<Delete> deleteList = new ArrayList<>();
            int keepCount = 0;
            int scanCount = 0;
            // 3. 遍历数据
            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    scanCount++;

                    // 获取 RowKey
                    byte[] rowKey = result.getRow();

                    // 获取用户名
                    byte[] usernameBytes = result.getValue(CF_BASE, COL_USERNAME);
                    String username = usernameBytes != null ? Bytes.toString(usernameBytes) : "";
                    // 4. 判断逻辑
                    if ("liuchong".equals(username)) {
                        System.out.println(">>> 保留用户: " + username + " (RowKey: " + Bytes.toString(rowKey) + ")");
                        keepCount++;
                    } else {
                        // 加入删除队列
                        Delete delete = new Delete(rowKey);
                        deleteList.add(delete);
                        System.out.println("--- 标记删除: " + (username.isEmpty() ? "无用户名" : username) + " (RowKey: " + Bytes.toString(rowKey) + ")");
                    }
                }
            }
            // 5. 执行批量删除
            if (!deleteList.isEmpty()) {
                System.out.println("正在执行删除操作，共 " + deleteList.size() + " 条记录...");
                table.delete(deleteList);
                System.out.println("删除完成！");
            } else {
                System.out.println("没有需要删除的用户。");
            }
            System.out.println("========================================");
            System.out.println("扫描总数: " + scanCount);
            System.out.println("保留数量: " + keepCount);
            System.out.println("删除数量: " + deleteList.size());
        }
    }
}
