package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.common.BusinessException;
import com.seckill.common.PageQuery;
import com.seckill.dto.ProductCategoryDTO;
import com.seckill.dto.ProductDTO;
import com.seckill.entity.Product;
import com.seckill.entity.ProductCategory;
import com.seckill.enums.ProductStatusEnum;
import com.seckill.mapper.ProductMapper;
import com.seckill.mapper.ProductCategoryMapper;
import com.seckill.service.ProductService;
import com.seckill.util.MinioUtil;
import com.seckill.vo.ProductCategoryVO;
import com.seckill.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;
    
//    @Autowired
//    private ProductCategoryMapper categoryMapper;
    
    @Autowired
    private MinioUtil minioUtil;

    @Override
    public void saveProduct(ProductDTO productDTO) {
        Product product = new Product();
        BeanUtils.copyProperties(productDTO, product);
        // HBase 不自动填充时间，手动写入
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        // 默认统计字段
        product.setViewCount(0L);
        product.setSaleCount(0L);
        product.setCollectCount(0L);
        product.setImgUrl(productDTO.getImgUrl());
        product.setImages(productDTO.getImgUrl());
        int result = productMapper.insert(product);
        if (result <= 0) {
            throw new BusinessException("保存商品失败");
        }
    }

    @Override
    public void updateProduct(Long id, ProductDTO productDTO) {
        Product existProduct = productMapper.selectById(id);
        if (existProduct == null) {
            throw new BusinessException("商品不存在");
        }
        
        Product product = new Product();
        BeanUtils.copyProperties(productDTO, product);
        product.setId(id);
        // 保留原创建时间，更新更新时间
        product.setCreateTime(existProduct.getCreateTime());
        product.setUpdateTime(LocalDateTime.now());
        if(productDTO.getImgUrl()!=null){
            product.setImgUrl(productDTO.getImgUrl());
            product.setImages(productDTO.getImgUrl());
        }
        int result = productMapper.updateById(product);
        if (result <= 0) {
            throw new BusinessException("更新商品失败");
        }
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        int result = productMapper.deleteById(id);
        if (result <= 0) {
            throw new BusinessException("删除商品失败");
        }
    }

    @Override
    public ProductVO getProductById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(product, productVO);
        if(product.getStatus()!=null){
            productVO.setStatusDesc(ProductStatusEnum.getByCode(product.getStatus()).getDesc());
        }else {
            productVO.setStatusDesc(ProductStatusEnum.getByCode(1).getDesc());
        }

        
        // 查询分类名称
//        if (product.getCategoryId() != null) {
//            ProductCategory category = categoryMapper.selectById(product.getCategoryId());
//            if (category != null) {
//                productVO.setCategoryName(category.getCategoryName());
//            }
//        }
        
        return productVO;
    }

    @Override
    public IPage<ProductVO> getProductList(PageQuery pageQuery, String productName) {
        Page<Product> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        IPage<Product> productPage = productMapper.selectProductPage(page, productName);
        
        // 转换为VO
        List<ProductVO> productVOList = productPage.getRecords().stream()
                .map(product -> {
                    ProductVO productVO = new ProductVO();
                    BeanUtils.copyProperties(product, productVO);
                    if(product.getStatus()!=null){
                        productVO.setStatusDesc(ProductStatusEnum.getByCode(product.getStatus()).getDesc());
                    }else {
                        productVO.setStatusDesc(ProductStatusEnum.getByCode(1).getDesc());
                    }
                    productVO.setImgUrl(product.getImages());
                    log.info("这个商品数据如下：{}",product);
                    return productVO;
                })
                .collect(Collectors.toList());
        
        // 转换返回结果
        IPage<ProductVO> result = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        result.setRecords(productVOList);
        
        return result;
    }

    @Override
    public List<ProductCategoryVO> getCategoryList() {
//        List<ProductCategory> categories = categoryMapper.selectList(
//                new LambdaQueryWrapper<ProductCategory>()
//                        .eq(ProductCategory::getDeleted, 0)
//        );
        
//        return categories.stream()
//                .map(category -> {
//                    ProductCategoryVO categoryVO = new ProductCategoryVO();
//                    BeanUtils.copyProperties(category, categoryVO);
//                    return categoryVO;
//                })
//                .collect(Collectors.toList());
        return null;
    }

    @Override
    public void saveCategory(ProductCategoryDTO categoryDTO) {
        ProductCategory category = new ProductCategory();
        BeanUtils.copyProperties(categoryDTO, category);
//        int result = categoryMapper.insert(category);
//        if (result <= 0) {
//            throw new BusinessException("保存分类失败");
//        }
    }

    @Override
    public String uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        
        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只能上传图片文件");
        }
        
        // 检查文件大小（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException("图片文件大小不能超过10MB");
        }
        
        try {
            return minioUtil.uploadFile(file);
        } catch (Exception e) {
            throw new BusinessException("图片上传失败");
        }
    }
}