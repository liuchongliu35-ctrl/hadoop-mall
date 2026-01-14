package com.seckill.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * HBase ID生成器
 * 使用HBase表存储自增ID
 */
@Slf4j
@Component
public class HBaseIdGenerator {

    private static final String ID_TABLE_NAME = "id_generator";
    private static final String ID_CF = "cf_id";
    private static final String ID_COL = "current_id";

    @Autowired
    private org.apache.hadoop.conf.Configuration hbaseConfiguration;

    private Connection connection;

    /**
     * 获取连接
     */
    private Connection getConnection() throws IOException {
        if (connection == null || connection.isClosed()) {
            connection = ConnectionFactory.createConnection(hbaseConfiguration);
        }
        return connection;
    }

    /**
     * 生成下一个ID（线程安全）
     */
    public synchronized Long generateId(String tableName) {
        try (Table table = getConnection().getTable(TableName.valueOf(ID_TABLE_NAME))) {
            String rowKey = tableName;
            
            // 使用原子操作获取并递增ID
            Increment increment = new Increment(Bytes.toBytes(rowKey));
            increment.addColumn(Bytes.toBytes(ID_CF), Bytes.toBytes(ID_COL), 1);
            
            Result result = table.increment(increment);
            byte[] value = result.getValue(Bytes.toBytes(ID_CF), Bytes.toBytes(ID_COL));
            
            if (value != null) {
                return Bytes.toLong(value);
            } else {
                // 如果不存在，初始化为1
                Put put = new Put(Bytes.toBytes(rowKey));
                put.addColumn(Bytes.toBytes(ID_CF), Bytes.toBytes(ID_COL), 
                        System.currentTimeMillis(), Bytes.toBytes(1L));
                table.put(put);
                return 1L;
            }
        } catch (Exception e) {
            log.error("生成ID失败: {}", e.getMessage(), e);
            // 如果HBase操作失败，使用时间戳作为ID（降级方案）
            return System.currentTimeMillis();
        }
    }

    /**
     * 为商品表生成ID
     */
    public Long generateProductId() {
        return generateId("product_info");
    }
}

