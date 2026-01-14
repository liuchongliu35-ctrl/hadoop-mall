package com.seckill.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.seckill.common.PageQuery;
import com.seckill.common.Result;
import com.seckill.dto.ProductCategoryDTO;
import com.seckill.dto.ProductDTO;
import com.seckill.enums.UserRoleEnum;
import com.seckill.service.ProductService;
import com.seckill.util.JwtUtil;
import com.seckill.vo.ProductCategoryVO;
import com.seckill.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping
    public Result<?> saveProduct(@RequestBody ProductDTO productDTO, HttpServletRequest request) {
        checkAdminPermission(request);
        productService.saveProduct(productDTO);
        return Result.success("添加商品成功");
    }

    @PutMapping("/{id}")
    public Result<?> updateProduct(@PathVariable Long id, @RequestBody ProductDTO productDTO, HttpServletRequest request) {
        checkAdminPermission(request);
        productService.updateProduct(id, productDTO);
        return Result.success("更新商品成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteProduct(@PathVariable Long id, HttpServletRequest request) {
        checkAdminPermission(request);
        productService.deleteProduct(id);
        return Result.success("删除商品成功");
    }

    @GetMapping("/{id}")
    public Result<ProductVO> getProductById(@PathVariable Long id) {
        ProductVO productVO = productService.getProductById(id);
        return Result.success(productVO);
    }

    @GetMapping("/list")
    public Result<IPage<ProductVO>> getProductList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String productName) {
        
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPageNum(pageNum);
        pageQuery.setPageSize(pageSize);
        
        IPage<ProductVO> result = productService.getProductList(pageQuery, productName);
        return Result.success(result);
    }

    @PostMapping("/category")
    public Result<?> saveCategory(@RequestBody ProductCategoryDTO categoryDTO, HttpServletRequest request) {
        checkAdminPermission(request);
        productService.saveCategory(categoryDTO);
        return Result.success("添加分类成功");
    }

    @GetMapping("/category/list")
    public Result<List<ProductCategoryVO>> getCategoryList() {
        List<ProductCategoryVO> result = productService.getCategoryList();
        return Result.success(result);
    }

    @PostMapping("/upload")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        checkAdminPermission(request);
        String imageUrl = productService.uploadImage(file);
        return Result.success(imageUrl);
    }

    private void checkAdminPermission(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("未登录");
        }
        
        Integer role = jwtUtil.getRole(token);
        if (!role.equals(UserRoleEnum.ADMIN.getCode())) {
            throw new RuntimeException("无权限访问");
        }
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}