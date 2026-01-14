package com.seckill.config;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * HBase配置类
 */
@org.springframework.context.annotation.Configuration
public class HBaseConfig {

    @Value("${hbase.zookeeper.quorum}")
    private String zookeeperQuorum;

    @Value("${hbase.zookeeper.port}")
    private String zookeeperPort;

    @Value("${hbase.rpc.timeout}")
    private int rpcTimeout;

    @Value("${hbase.client.operation.timeout}")
    private int operationTimeout;

    @Value("${hbase.client.scanner.timeout.period}")
    private int scannerTimeout;

    @Value("${hbase.client.retries.number}")
    private int retriesNumber;

    @Value("${hbase.zookeeper.session.timeout}")
    private int sessionTimeout;

    /**
     * 创建HBase配置对象
     */
    @Bean
    public Configuration hbaseConfiguration() {
        Configuration conf = HBaseConfiguration.create();
        
        // 设置ZooKeeper地址
        conf.set("hbase.zookeeper.quorum", zookeeperQuorum);
        conf.set("hbase.zookeeper.property.clientPort", zookeeperPort);
        
        // 设置超时和重试参数
        conf.setInt("hbase.rpc.timeout", rpcTimeout);
        conf.setInt("hbase.client.operation.timeout", operationTimeout);
        conf.setInt("hbase.client.scanner.timeout.period", scannerTimeout);
        conf.setInt("hbase.client.retries.number", retriesNumber);
        conf.setInt("zookeeper.session.timeout", sessionTimeout);
        
        // Windows环境配置
        System.setProperty("HADOOP_USER_NAME", "root");
        
        return conf;
    }
}

