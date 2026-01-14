package com.seckill.mapper.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.seckill.entity.CartData;
import com.seckill.mapper.CartMapper;
import com.seckill.util.HBaseUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

@Slf4j
@Repository
public class CartMapperImpl implements CartMapper {

//    购物车对应的Hbase表
    private static final String TABLE_NAME = "cart_data";
//    cf_base来存储元数据
    private static final String CF_BASE = "cf_base";
//    cf_items专门存商品
    private static final String CF_ITEMS = "cf_items";

    // cf_base的属性
    private static final String COL_UPDATE_TIME = "update_time";
    private static final String COL_DELETED = "deleted";
    private static final String PRODUCT_COL_PREFIX = "product_";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Autowired
    private HBaseUtil hBaseUtil;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 使用用户ID作为RowKey，转成字符串
     */
    private String rowKey(Long userId) {
        return String.valueOf(userId);
    }

    @Override
    public int saveOrUpdate(CartData cartData) {
        if(cartData == null|| cartData.getId()==null){
            log.warn("CartData 或 UserId 为空，无法保存到HBase");
            return 0;
        }

        try {
//        创建行键
            String rk=rowKey(cartData.getUserId());
//          todo 使用put记录下你对这个表的所有操作，最后将所有操作提交作用于表
            Put put = new Put(Bytes.toBytes(rk));
//        更新元数据(cf_base)
            LocalDateTime updateTime = cartData.getUpdateTime() != null ? cartData.getUpdateTime() : LocalDateTime.now();
            putTime(CF_BASE,COL_UPDATE_TIME,updateTime,put);
            putInt(CF_BASE, COL_DELETED, 0, put);
//        更新商品项
            if(cartData.getCartItems()!=null&&!cartData.getCartItems().isEmpty()){
                Map<String, Object> itemMap = (Map<String, Object>) objectMapper.readValue(cartData.getCartItems(), new TypeReference<Object>() {});
                for(Map.Entry<String, Object> entry : itemMap.entrySet()){
                    String productId = entry.getKey();
                    String itemJson = objectMapper.writeValueAsString(entry.getValue());
//                    使用 "product_" 作为前缀，与元数据列区分开
                    String colName = PRODUCT_COL_PREFIX + productId;
                    putStr(CF_ITEMS,colName,itemJson,put);
                }
            }
//            todo 将所有的操作提交
            hBaseUtil.putBatch(TABLE_NAME, List.of(put));
            return 1;
        } catch (Exception e) {
            log.error("保存或更新购物车到HBase失败, userId={}", cartData.getUserId(), e);
            return 0;
        }
    }


//  根据用户Id获取购物车商品
    @Override
    public CartData findByUserId(Long userId) {
        if (userId == null) return null;
        try {
            Result result = hBaseUtil.get(TABLE_NAME, rowKey(userId));
            if (result == null || result.isEmpty()) {
                return null;
            }
            // 检查是否被逻辑删除
            Integer deleted = getInt(result, CF_BASE, COL_DELETED);
            if (deleted != null && deleted == 1) {
                return null; // 已被删除
            }
            return convertToCartData(result, userId);
        } catch (IOException e) {
            log.error("从HBase查询购物车失败, userId={}", userId, e);
            return null;
        }
    }



    @Override
    public int deleteByUserId(Long userId) {
        if (userId == null) return 0;
        try {
//        逻辑删除，将deleted标记置为1
            Put put = new Put(Bytes.toBytes(rowKey(userId)));
            putInt(CF_BASE, COL_DELETED, 1, put);
            putTime(CF_BASE,COL_UPDATE_TIME,LocalDateTime.now(),put);
            hBaseUtil.putBatch(TABLE_NAME,List.of(put));
            return 1;
        } catch (IOException e) {
            log.error("从HBase逻辑删除购物车失败, userId={}", userId, e);
            return 0;
        }
    }

//    todo 处理从Hbase获取的字符串数据
    private CartData convertToCartData(Result r, Long userId) {
        try {
            CartData cartData = new CartData();
            cartData.setUserId(userId);
            cartData.setUpdateTime(getTime(r,CF_BASE,COL_UPDATE_TIME));
            cartData.setDeleted(getInt(r,CF_BASE, COL_DELETED));
            Map<String, Object> itemsMap = new HashMap<>();
//        todo 遍历cf_Item列族下的所有列
            NavigableMap<byte[], byte[]> familyMap = r.getFamilyMap(Bytes.toBytes(CF_ITEMS));
            if(familyMap!=null){
                for(Map.Entry<byte[], byte[]> entry : familyMap.entrySet()){
    //               获取列名
                    String colName = Bytes.toString(entry.getKey());
    //                获取行键下的数据
                    if(colName.startsWith(PRODUCT_COL_PREFIX)){
    //                     因为String colName = PRODUCT_COL_PREFIX + productId;
                        String productId = colName.substring(PRODUCT_COL_PREFIX.length());
                        String itemJson = Bytes.toString(entry.getValue());
                        Object itemObject = objectMapper.readValue(itemJson, Object.class);
                        itemsMap.put(productId,itemObject);
                    }
                }
            }
            if(!itemsMap.isEmpty()){
                cartData.setCartItems(objectMapper.writeValueAsString(itemsMap));
            }
            return cartData;
        } catch (JsonProcessingException e) {
            log.error("转换HBase Result为CartData失败", e);
            return null;
        }
    }

    private void putStr(String cf, String colName, String val, Put put) {
        if(val==null){return;}
        put.addColumn(Bytes.toBytes(cf),Bytes.toBytes(colName),System.currentTimeMillis(),Bytes.toBytes(val));
    }

    private void putTime(String cf, String col, LocalDateTime time, Put put) {
        if (time == null) return;
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(col), System.currentTimeMillis(), Bytes.toBytes(time.format(DATE_TIME_FORMATTER)));
    }
    private void putInt(String cf, String col, Integer val, Put put) {
        if (val == null) return;
        put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(col), System.currentTimeMillis(), Bytes.toBytes(String.valueOf(val)));
    }


    private String getStr(Result r, String cf, String col) {
        return hBaseUtil.getValueFromResult(r, cf, col);
    }
    private Integer getInt(Result r, String cf, String col) {
        String v = getStr(r, cf, col);
        return v == null ? null : Integer.parseInt(v);
    }
    private LocalDateTime getTime(Result r, String cf, String col) {
        String v = getStr(r, cf, col);
        return v == null ? null : LocalDateTime.parse(v, DATE_TIME_FORMATTER);
    }
}
