package com.seckill.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * HBase工具类
 * 提供HBase的通用操作方法
 */
@Slf4j
@Component
public class HBaseUtil {

    @Autowired
    private Configuration hbaseConfiguration;

    private Connection connection;

    /**
     * 获取HBase连接（单例模式）
     */
    private Connection getConnection() throws IOException {
        if (connection == null || connection.isClosed()) {
            connection = ConnectionFactory.createConnection(hbaseConfiguration);
        }
        return connection;
    }

    /**
     * 获取表对象
     */
    public Table getTable(String tableName) throws IOException {
        return getConnection().getTable(TableName.valueOf(tableName));
    }

    /**
     * 插入或更新数据
     */
    public void put(String tableName, String rowKey, String columnFamily, String column, String value) throws IOException {
        try (Table table = getTable(tableName)) {
            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(
                    Bytes.toBytes(columnFamily),
                    Bytes.toBytes(column),
                    System.currentTimeMillis(),
                    Bytes.toBytes(value)
            );
            table.put(put);
        }
    }

    /**
     * 批量插入或更新数据
     */
    public void putBatch(String tableName, List<Put> puts) throws IOException {
        if (puts == null || puts.isEmpty()) {
            return;
        }
        try (Table table = getTable(tableName)) {
            table.put(puts);
        }
    }

    /**
     * 根据RowKey获取单行数据
     */
    public Result get(String tableName, String rowKey) throws IOException {
        try (Table table = getTable(tableName)) {
            Get get = new Get(Bytes.toBytes(rowKey));
            return table.get(get);
        }
    }

    /**
     * 根据RowKey和列族获取数据
     */
    public Result get(String tableName, String rowKey, String columnFamily) throws IOException {
        try (Table table = getTable(tableName)) {
            Get get = new Get(Bytes.toBytes(rowKey));
            get.addFamily(Bytes.toBytes(columnFamily));
            return table.get(get);
        }
    }

    /**
     * 根据RowKey和列族、列获取数据
     */
    public String getValue(String tableName, String rowKey, String columnFamily, String column) throws IOException {
        try (Table table = getTable(tableName)) {
            Get get = new Get(Bytes.toBytes(rowKey));
            get.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column));
            Result result = table.get(get);
            if (result.isEmpty()) {
                return null;
            }
            Cell cell = result.listCells().get(0);
            return Bytes.toString(CellUtil.cloneValue(cell));
        }
    }

    /**
     * 删除行
     */
    public void delete(String tableName, String rowKey) throws IOException {
        try (Table table = getTable(tableName)) {
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            table.delete(delete);
        }
    }

    /**
     * 删除列
     */
    public void deleteColumn(String tableName, String rowKey, String columnFamily, String column) throws IOException {
        try (Table table = getTable(tableName)) {
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            delete.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column));
            table.delete(delete);
        }
    }

    /**
     * 扫描表
     */
    public ResultScanner scan(String tableName, Scan scan) throws IOException {
        Table table = getTable(tableName);
        return table.getScanner(scan);
    }

    /**
     * 从Result中获取指定列的值
     */
    public String getValueFromResult(Result result, String columnFamily, String column) {
        byte[] value = result.getValue(Bytes.toBytes(columnFamily), Bytes.toBytes(column));
        return value != null ? Bytes.toString(value) : null;
    }

    /**
     * 从Result中获取所有列的值（转换为Map格式，便于使用）
     */
    public void fillResultToMap(Result result, java.util.Map<String, String> map) {
        for (Cell cell : result.listCells()) {
            String cf = Bytes.toString(CellUtil.cloneFamily(cell));
            String column = Bytes.toString(CellUtil.cloneQualifier(cell));
            String value = Bytes.toString(CellUtil.cloneValue(cell));
            map.put(cf + ":" + column, value);
        }
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (connection != null && !connection.isClosed()) {
            try {
                connection.close();
            } catch (IOException e) {
                log.error("关闭HBase连接失败", e);
            }
        }
    }
}

