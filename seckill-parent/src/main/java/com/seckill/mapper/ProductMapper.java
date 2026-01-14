package com.seckill.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.entity.Product;
import org.apache.ibatis.annotations.Param;

/**
 * 商品Mapper接口
 * 使用HBase存储，不再继承BaseMapper
 */
public interface ProductMapper {
    
    /**
     * 插入商品
     */
    int insert(Product product);
    
    /**
     * 根据ID更新商品
     */
    int updateById(Product product);
    
    /**
     * 根据ID删除商品（逻辑删除）
     */
    int deleteById(Long id);
    
    /**
     * 根据ID查询商品
     */
    Product selectById(Long id);
    
    /**
     * 分页查询商品（支持商品名称模糊查询）
     */
    IPage<Product> selectProductPage(Page<Product> page, @Param("productName") String productName);
}