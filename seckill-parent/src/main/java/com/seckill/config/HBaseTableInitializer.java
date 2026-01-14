package com.seckill.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * HBase表初始化类
 * 在项目启动时自动创建所需的HBase表
 */
@Slf4j
@Component
@Order(1) // 设置执行顺序，确保在其他组件之前执行
public class HBaseTableInitializer implements CommandLineRunner {

    @Autowired
    private org.apache.hadoop.conf.Configuration hbaseConfiguration;

    /**
     * 项目启动时执行
     */
    public void run(String... args) throws Exception {
        log.info("=== 开始初始化HBase表 ===");
        
        Connection connection = null;
        Admin admin = null;
        
        try {
            // 创建连接
            connection = ConnectionFactory.createConnection(hbaseConfiguration);
            admin = connection.getAdmin();
            
            // 测试连接
            if (!testConnection(admin)) {
                log.error("HBase连接测试失败，跳过表创建");
                return;
            }
            
            // 创建商品信息表
            createProductInfoTable(admin);
            
            // 创建订单历史表
            createOrderHistoryTable(admin);
            
            // 创建用户档案表
            createUserProfileTable(admin);
            
            // 创建销售数据表
            createSalesDataTable(admin);
            
            // 创建购物车表
            createCartDataTable(admin);

            // 创建秒杀活动表
            createSeckillActivityTable(admin);
            
            // 创建ID生成器表
            createIdGeneratorTable(admin);
            
            log.info("=== HBase表初始化完成 ===");
            
        } catch (Exception e) {
            log.error("HBase表初始化失败: {}", e.getMessage(), e);
        } finally {
            // 关闭连接
            if (admin != null) {
                try {
                    admin.close();
                } catch (Exception e) {
                    log.error("关闭Admin连接失败", e);
                }
            }
            if (connection != null && !connection.isClosed()) {
                try {
                    connection.close();
                } catch (Exception e) {
                    log.error("关闭Connection连接失败", e);
                }
            }
        }
    }

    /**
     * 测试HBase连接
     */
    private boolean testConnection(Admin admin) {
        try {
            // 使用listTableNames()方法测试连接，避免使用废弃的getClusterStatus()
            admin.listTableNames();
            log.info("HBase连接成功！");
            return true;
        } catch (Exception e) {
            log.error("HBase连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建商品信息表 (product_info)
     * 列族: cf_base, cf_detail, cf_stock, cf_stat
     */
    private void createProductInfoTable(Admin admin) throws Exception {
        String tableName = "product_info";
        String[] columnFamilies = {"cf_base", "cf_detail", "cf_stock", "cf_stat"};
        createTable(admin, tableName, columnFamilies);
    }

    /**
     * 创建订单历史表 (order_history)
     * 列族: cf_base, cf_address, cf_items, cf_logistics
     */
    private void createOrderHistoryTable(Admin admin) throws Exception {
        String tableName = "order_history";
        String[] columnFamilies = {"cf_base", "cf_address", "cf_items", "cf_logistics"};
        createTable(admin, tableName, columnFamilies);
    }

    /**
     * 创建用户档案表 (user_profile)
     * 列族: cf_base, cf_account, cf_address, cf_behavior
     */
    private void createUserProfileTable(Admin admin) throws Exception {
        String tableName = "user_profile";
        String[] columnFamilies = {"cf_base", "cf_account", "cf_address", "cf_behavior"};
        createTable(admin, tableName, columnFamilies);
    }

    /**
     * 创建销售数据表 (sales_data)
     * 列族: cf_daily, cf_hourly, cf_region
     */
    private void createSalesDataTable(Admin admin) throws Exception {
        String tableName = "sales_data";
        String[] columnFamilies = {"cf_daily", "cf_hourly", "cf_region"};
        createTable(admin, tableName, columnFamilies);
    }

    /**
     * 创建购物车表 (cart_data)
     * 列族: cf_items
     */
    private void createCartDataTable(Admin admin) throws Exception {
        String tableName = "cart_data";
        String[] columnFamilies = {"cf_items"};
        createTable(admin, tableName, columnFamilies);
    }

    /**
     * 创建秒杀活动表 (t_seckill_activity)
     * 列族: cf_base
     */
    private void createSeckillActivityTable(Admin admin) throws Exception {
        String tableName = "seckill_activity";
        String[] columnFamilies = {"cf_base"};
        createTable(admin, tableName, columnFamilies);
    }

    /**
     * 创建ID生成器表 (id_generator)
     * 列族: cf_id
     */
    private void createIdGeneratorTable(Admin admin) throws Exception {
        String tableName = "id_generator";
        String[] columnFamilies = {"cf_id"};
        createTable(admin, tableName, columnFamilies);
    }

    /**
     * 创建表的通用方法
     */
    private void createTable(Admin admin, String tableName, String[] columnFamilies) throws Exception {
        TableName tbName = TableName.valueOf(tableName);
        
        // 检查表是否存在
        if (admin.tableExists(tbName)) {
            log.info("表 {} 已存在，跳过创建", tableName);
            return;
        }
        
        // 创建表描述器
        TableDescriptorBuilder tableBuilder = TableDescriptorBuilder.newBuilder(tbName);
        
        // 添加列族
        for (String columnFamily : columnFamilies) {
            ColumnFamilyDescriptor cfDesc = ColumnFamilyDescriptorBuilder
                    .newBuilder(Bytes.toBytes(columnFamily))
                    .setMaxVersions(3) // 保留3个版本
                    .setMinVersions(1)
                    .setTimeToLive(2147483647) // 长期保留（约68年）
                    .build();
            tableBuilder.setColumnFamily(cfDesc);
        }
        
        // 创建表
        admin.createTable(tableBuilder.build());
        log.info("表 {} 创建成功！列族: {}", tableName, Arrays.toString(columnFamilies));
    }
}

