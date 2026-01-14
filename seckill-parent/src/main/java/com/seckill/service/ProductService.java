package com.seckill.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.common.PageQuery;
import com.seckill.dto.ProductDTO;
import com.seckill.entity.Product;
import com.seckill.vo.ProductCategoryVO;
import com.seckill.vo.ProductVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    
    void saveProduct(ProductDTO productDTO);
    
    void updateProduct(Long id, ProductDTO productDTO);
    
    void deleteProduct(Long id);
    
    ProductVO getProductById(Long id);
    
    IPage<ProductVO> getProductList(PageQuery pageQuery, String productName);
    
    List<ProductCategoryVO> getCategoryList();
    
    void saveCategory(com.seckill.dto.ProductCategoryDTO categoryDTO);
    
    String uploadImage(MultipartFile file);
}