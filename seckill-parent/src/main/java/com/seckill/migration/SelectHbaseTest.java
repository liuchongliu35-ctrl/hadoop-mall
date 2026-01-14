package com.seckill.migration;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

public class SelectHbaseTest {
    // === HBase 配置 ===
    private static final String ZK_QUORUM = "192.168.124.100";
    private static final String ZK_PORT = "2181";
    private static final String TABLE_NAME = "product_info";
    public static void main(String[] args) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", ZK_QUORUM);
        conf.set("hbase.zookeeper.property.clientPort", ZK_PORT);
        System.out.println("正在连接 HBase (" + ZK_QUORUM + ":" + ZK_PORT + ")...");
        try (Connection conn = ConnectionFactory.createConnection(conf);
             Table table = conn.getTable(TableName.valueOf(TABLE_NAME))) {
            System.out.println("连接成功，开始扫描表: " + TABLE_NAME);
            System.out.println("----------------------------------------------------------------------------------");
            Scan scan = new Scan();
            try (ResultScanner scanner = table.getScanner(scan)) {
                int count = 0;
                for (Result result : scanner) {
                    count++;
                    printRow(result);
                }
                System.out.println("----------------------------------------------------------------------------------");
                System.out.println("查询完成，共找到 " + count + " 条数据。");
            }
        }
    }
    private static void printRow(Result result) {
        String rowKey = Bytes.toString(result.getRow());
        // 读取基本信息
        String name = getValue(result, "cf_base", "name");
        String price = getValue(result, "cf_base", "price");

        // 读取详细信息 (注意这里读取了 images)
        String desc = getValue(result, "cf_detail", "description");
        // 对应迁移代码中的 putString(put, "cf_detail", "images", rs.getString("img_url"));
        String images = getValue(result, "cf_detail", "images");
        System.out.println("RowKey: " + rowKey);
        System.out.println("  - 名称: " + name);
        System.out.println("  - 价格: " + price);
        // 打印图片链接
        System.out.println("  - 图片: " + images);

        // 截断过长的描述方便查看
        String shortDesc = (desc != null && desc.length() > 20) ? desc.substring(0, 20) + "..." : desc;
        System.out.println("  - 描述: " + shortDesc);
        System.out.println("");
    }
    private static String getValue(Result result, String family, String qualifier) {
        byte[] valBytes = result.getValue(Bytes.toBytes(family), Bytes.toBytes(qualifier));
        return valBytes == null ? "null" : Bytes.toString(valBytes);
    }
}
