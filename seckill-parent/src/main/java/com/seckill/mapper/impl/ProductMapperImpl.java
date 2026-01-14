package com.seckill.mapper.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.entity.Product;
import com.seckill.mapper.ProductMapper;
import com.seckill.util.HBaseIdGenerator;
import com.seckill.util.HBaseUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ProductMapper的HBase实现类
 */
@Slf4j
@Repository
@Primary
public class ProductMapperImpl implements ProductMapper {

    private static final String TABLE_NAME = "product_info";
    private static final String CF_BASE = "cf_base";
    private static final String CF_DETAIL = "cf_detail";
    private static final String CF_STOCK = "cf_stock";
    private static final String CF_STAT = "cf_stat";
    
    // 列名常量
    private static final String COL_NAME = "name";
    private static final String COL_CATEGORY = "category";
    private static final String COL_BRAND = "brand";
    private static final String COL_PRICE = "price";
    private static final String COL_COST = "cost";
    private static final String COL_STATUS = "status";
    private static final String COL_CREATE_TIME = "create_time";
    private static final String COL_DESCRIPTION = "description";
    private static final String COL_SPEC = "spec";
    private static final String COL_IMAGES = "images";
    private static final String COL_TAGS = "tags";
    private static final String COL_TOTAL_STOCK = "total_stock";
    private static final String COL_WAREHOUSE_STOCK = "warehouse_stock";
    private static final String COL_SAFE_STOCK = "safe_stock";
    private static final String COL_LOCK_STOCK = "lock_stock";
    private static final String COL_VIEW_COUNT = "view_count";
    private static final String COL_SALE_COUNT = "sale_count";
    private static final String COL_COLLECT_COUNT = "collect_count";
    private static final String COL_UPDATE_TIME = "update_time";
    private static final String COL_DELETED = "deleted";

    @Autowired
    private HBaseUtil hBaseUtil;

    @Autowired
    private HBaseIdGenerator idGenerator;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 生成RowKey：使用product_id
     */
    private String getRowKey(Long id) {
        return String.valueOf(id);
    }

    /**
     * 插入商品
     */
    @Override
    public int insert(Product product) {
        try {
            // 如果ID为空，自动生成
            if (product.getId() == null) {
                product.setId(idGenerator.generateProductId());
            }
//            todo Hbase任何操作钱都需要先确定RowKey，然后将行键转化为字符串，再将行键交给Put
//            todo put是写入数据的载体
            String rowKey = getRowKey(product.getId());
            Put put = new Put(Bytes.toBytes(rowKey));

            // cf_base列族
            if (product.getProductName() != null) {
//                todo addColumn是在填充单元格
                put.addColumn(Bytes.toBytes(CF_BASE), Bytes.toBytes(COL_NAME), 
                        System.currentTimeMillis(), Bytes.toBytes(product.getProductName()));
            }
            if (product.getCategoryId() != null) {
                put.addColumn(Bytes.toBytes(CF_BASE), Bytes.toBytes(COL_CATEGORY), 
                        System.currentTimeMillis(), Bytes.toBytes(String.valueOf(product.getCategoryId())));
            }
            if (product.getBrand() != null) {
                put.addColumn(Bytes.toBytes(CF_BASE), Bytes.toBytes(COL_BRAND), 
                        System.currentTimeMillis(), Bytes.toBytes(product.getBrand()));
            }
            if (product.getPrice() != null) {
                put.addColumn(Bytes.toBytes(CF_BASE), Bytes.toBytes(COL_PRICE), 
                        System.currentTimeMillis(), Bytes.toBytes(product.getPrice().toString()));
            }
            if (product.getCost() != null) {
                put.addColumn(Bytes.toBytes(CF_BASE), Bytes.toBytes(COL_COST), 
                        System.currentTimeMillis(), Bytes.toBytes(product.getCost().toString()));
            }
            if (product.getStatus() != null) {
                put.addColumn(Bytes.toBytes(CF_BASE), Bytes.toBytes(COL_STATUS), 
                        System.currentTimeMillis(), Bytes.toBytes(String.valueOf(product.getStatus())));
            }
            if (product.getCreateTime() != null) {
                put.addColumn(Bytes.toBytes(CF_BASE), Bytes.toBytes(COL_CREATE_TIME), 
                        System.currentTimeMillis(), Bytes.toBytes(product.getCreateTime().format(dateTimeFormatter)));
            }

            // cf_detail列族
            if (product.getProductDesc() != null) {
                put.addColumn(Bytes.toBytes(CF_DETAIL), Bytes.toBytes(COL_DESCRIPTION), 
                        System.currentTimeMillis(), Bytes.toBytes(product.getProductDesc()));
            }
            if (product.getSpec() != null) {
                put.addColumn(Bytes.toBytes(CF_DETAIL), Bytes.toBytes(COL_SPEC), 
                        System.currentTimeMillis(), Bytes.toBytes(product.getSpec()));
            }
            if (product.getImages() != null) {
                put.addColumn(Bytes.toBytes(CF_DETAIL), Bytes.toBytes(COL_IMAGES), 
                        System.currentTimeMillis(), Bytes.toBytes(product.getImages()));
            }
            if (product.getTags() != null) {
                put.addColumn(Bytes.toBytes(CF_DETAIL), Bytes.toBytes(COL_TAGS), 
                        System.currentTimeMillis(), Bytes.toBytes(product.getTags()));
            }

            // cf_stock列族
            if (product.getStock() != null) {
                put.addColumn(Bytes.toBytes(CF_STOCK), Bytes.toBytes(COL_TOTAL_STOCK), 
                        System.currentTimeMillis(), Bytes.toBytes(String.valueOf(product.getStock())));
            }
            if (product.getWarehouseStock() != null) {
                put.addColumn(Bytes.toBytes(CF_STOCK), Bytes.toBytes(COL_WAREHOUSE_STOCK), 
                        System.currentTimeMillis(), Bytes.toBytes(product.getWarehouseStock()));
            }
            if (product.getSafeStock() != null) {
                put.addColumn(Bytes.toBytes(CF_STOCK), Bytes.toBytes(COL_SAFE_STOCK), 
                        System.currentTimeMillis(), Bytes.toBytes(String.valueOf(product.getSafeStock())));
            }
            if (product.getLockStock() != null) {
                put.addColumn(Bytes.toBytes(CF_STOCK), Bytes.toBytes(COL_LOCK_STOCK), 
                        System.currentTimeMillis(), Bytes.toBytes(String.valueOf(product.getLockStock())));
            }

            // cf_stat列族
            if (product.getViewCount() != null) {
                put.addColumn(Bytes.toBytes(CF_STAT), Bytes.toBytes(COL_VIEW_COUNT), 
                        System.currentTimeMillis(), Bytes.toBytes(String.valueOf(product.getViewCount())));
            }
            if (product.getSaleCount() != null) {
                put.addColumn(Bytes.toBytes(CF_STAT), Bytes.toBytes(COL_SALE_COUNT), 
                        System.currentTimeMillis(), Bytes.toBytes(String.valueOf(product.getSaleCount())));
            }
            if (product.getCollectCount() != null) {
                put.addColumn(Bytes.toBytes(CF_STAT), Bytes.toBytes(COL_COLLECT_COUNT), 
                        System.currentTimeMillis(), Bytes.toBytes(String.valueOf(product.getCollectCount())));
            }
            if (product.getUpdateTime() != null) {
                put.addColumn(Bytes.toBytes(CF_STAT), Bytes.toBytes(COL_UPDATE_TIME), 
                        System.currentTimeMillis(), Bytes.toBytes(product.getUpdateTime().format(dateTimeFormatter)));
            }
            
            // 逻辑删除标记（存储在cf_base中）
            int deleted = product.getDeleted() != null ? product.getDeleted() : 0;
            put.addColumn(Bytes.toBytes(CF_BASE), Bytes.toBytes(COL_DELETED), 
                    System.currentTimeMillis(), Bytes.toBytes(String.valueOf(deleted)));

            hBaseUtil.putBatch(TABLE_NAME, List.of(put));
            return 1;
        } catch (Exception e) {
            log.error("插入商品失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 根据ID更新商品
     */
    @Override
    public int updateById(Product product) {
        try {
            // 先查询是否存在
            Product existProduct = selectById(product.getId());
            if (existProduct == null) {
                return 0;
            }

            // todo 使用insert方法更新，后续追加一个时间戳即可
            return insert(product);
        } catch (Exception e) {
            log.error("更新商品失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 根据ID删除商品
     */
    @Override
    public int deleteById(Long id) {
        try {
            String rowKey = getRowKey(id);
            Put put = new Put(Bytes.toBytes(rowKey));
            // todo 往cf_base:deleted这一列写入 "1"，表示删除
            put.addColumn(Bytes.toBytes(CF_BASE), Bytes.toBytes(COL_DELETED), 
                    System.currentTimeMillis(), Bytes.toBytes("1"));
            hBaseUtil.putBatch(TABLE_NAME, List.of(put));
            return 1;
        } catch (Exception e) {
            log.error("删除商品失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 根据ID查询商品
     */
    @Override
    public Product selectById(Long id) {
        try {
            String rowKey = getRowKey(id);
//            todo 点查：根据表名和行键查询一个商品的数据
            Result result = hBaseUtil.get(TABLE_NAME, rowKey);
            
            if (result.isEmpty()) {
                return null;
            }

            return convertResultToProduct(result, id);
        } catch (Exception e) {
            log.error("查询商品失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 分页查询商品
     */
    @Override
    public IPage<Product> selectProductPage(Page<Product> page, String productName) {
        try {
//            todo Scan是Hbase扫描全表查询多条数据的方式
            Scan scan = new Scan();
            scan.setCaching(1000); // 设置缓存
            
            List<Product> allProducts = new ArrayList<>();
            ResultScanner scanner = hBaseUtil.scan(TABLE_NAME, scan);
            
            for (Result result : scanner) {
                // 获取rowKey（即product_id）
                String rowKey = Bytes.toString(result.getRow());
                Long id = Long.parseLong(rowKey);
//                todo 将id对应的商品拿出来
                Product product = convertResultToProduct(result, id);
                boolean isNotDeleted = product != null &&
                        (product.getDeleted() == null || product.getDeleted() == 0);
                // 过滤已删除的商品
                if (isNotDeleted) {
                    // 名称模糊匹配
                    boolean nameMatch = (productName == null || productName.isEmpty()) ||
                            (product.getProductName() != null && product.getProductName().contains(productName));
//                  满足条件就放入结果list中
                    if (nameMatch) {
                        allProducts.add(product);
                    }
                }
            }
            scanner.close();

            // 按创建时间倒序排序
            allProducts.sort((p1, p2) -> {
                if (p1.getCreateTime() == null && p2.getCreateTime() == null) return 0;
                if (p1.getCreateTime() == null) return 1;
                if (p2.getCreateTime() == null) return -1;
                return p2.getCreateTime().compareTo(p1.getCreateTime());
            });

            // 分页处理
            int total = allProducts.size();
            int start = (int) ((page.getCurrent() - 1) * page.getSize());
            int end = Math.min(start + (int) page.getSize(), total);
            
            List<Product> pageList = start < total ? allProducts.subList(start, end) : new ArrayList<>();
            
            // 构建分页结果
            Page<Product> resultPage = new Page<>(page.getCurrent(), page.getSize(), total);
            resultPage.setRecords(pageList);
            
            return resultPage;
        } catch (Exception e) {
            log.error("分页查询商品失败: {}", e.getMessage(), e);
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }
    }

    /**
     * 将HBase Result转换为Product对象
     */
    private Product convertResultToProduct(Result result, Long id) {
        try {
            Product product = new Product();
            product.setId(id);

            // 从Result中读取各列的值
//            todo 将指定列族的指定单元格的数据拿出来
            String name = hBaseUtil.getValueFromResult(result, CF_BASE, COL_NAME);
            String category = hBaseUtil.getValueFromResult(result, CF_BASE, COL_CATEGORY);
            String brand = hBaseUtil.getValueFromResult(result, CF_BASE, COL_BRAND);
            String price = hBaseUtil.getValueFromResult(result, CF_BASE, COL_PRICE);
            String cost = hBaseUtil.getValueFromResult(result, CF_BASE, COL_COST);
            String status = hBaseUtil.getValueFromResult(result, CF_BASE, COL_STATUS);
            String createTime = hBaseUtil.getValueFromResult(result, CF_BASE, COL_CREATE_TIME);
            String deleted = hBaseUtil.getValueFromResult(result, CF_BASE, COL_DELETED);

            String description = hBaseUtil.getValueFromResult(result, CF_DETAIL, COL_DESCRIPTION);
            String spec = hBaseUtil.getValueFromResult(result, CF_DETAIL, COL_SPEC);
            String images = hBaseUtil.getValueFromResult(result, CF_DETAIL, COL_IMAGES);
            String tags = hBaseUtil.getValueFromResult(result, CF_DETAIL, COL_TAGS);

            String totalStock = hBaseUtil.getValueFromResult(result, CF_STOCK, COL_TOTAL_STOCK);
            String warehouseStock = hBaseUtil.getValueFromResult(result, CF_STOCK, COL_WAREHOUSE_STOCK);
            String safeStock = hBaseUtil.getValueFromResult(result, CF_STOCK, COL_SAFE_STOCK);
            String lockStock = hBaseUtil.getValueFromResult(result, CF_STOCK, COL_LOCK_STOCK);

            String viewCount = hBaseUtil.getValueFromResult(result, CF_STAT, COL_VIEW_COUNT);
            String saleCount = hBaseUtil.getValueFromResult(result, CF_STAT, COL_SALE_COUNT);
            String collectCount = hBaseUtil.getValueFromResult(result, CF_STAT, COL_COLLECT_COUNT);
            String updateTime = hBaseUtil.getValueFromResult(result, CF_STAT, COL_UPDATE_TIME);

            // todo 构造product对象
            product.setProductName(name);
            if (category != null) product.setCategoryId(Long.parseLong(category));
            product.setBrand(brand);
            if (price != null) product.setPrice(new BigDecimal(price));
            if (cost != null) product.setCost(new BigDecimal(cost));
            if (status != null) product.setStatus(Integer.parseInt(status));
            if (createTime != null) {
                product.setCreateTime(LocalDateTime.parse(createTime, dateTimeFormatter));
            }
            if (deleted != null) product.setDeleted(Integer.parseInt(deleted));

            product.setProductDesc(description);
            product.setSpec(spec);
            product.setImages(images);
            if (images != null && !images.isEmpty()) {
                // 假设多张图用逗号分隔，取第一张
                String mainImage = images.split(",")[0];
                product.setImgUrl(mainImage);
            }
            product.setTags(tags);

            if (totalStock != null) product.setStock(Integer.parseInt(totalStock));
            product.setWarehouseStock(warehouseStock);
            if (safeStock != null) product.setSafeStock(Integer.parseInt(safeStock));
            if (lockStock != null) product.setLockStock(Integer.parseInt(lockStock));

            if (viewCount != null) product.setViewCount(Long.parseLong(viewCount));
            if (saleCount != null) product.setSaleCount(Long.parseLong(saleCount));
            if (collectCount != null) product.setCollectCount(Long.parseLong(collectCount));
            if (updateTime != null) {
                product.setUpdateTime(LocalDateTime.parse(updateTime, dateTimeFormatter));
            }

            return product;
        } catch (Exception e) {
            log.error("转换Product对象失败: {}", e.getMessage(), e);
            return null;
        }
    }
}

