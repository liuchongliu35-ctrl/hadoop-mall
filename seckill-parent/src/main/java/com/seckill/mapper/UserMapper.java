package com.seckill.mapper;

import com.seckill.entity.User;
import java.util.List;


public interface UserMapper {

    /**
     * 保存或更新用户到 HBase
     * RowKey: userId
     */
    int saveOrUpdate(User user);
    /**
     * 根据用户ID查询
     */
    User selectById(Long id);
    /**
     * 根据用户名查询 (注意：HBase非RowKey查询效率较低，需走Filter)
     */
    User selectByUsername(String username);
    /**
     * 分页查询用户列表
     * @param username 用户名（模糊查询）
     * @param limit 条数
     * @param offset 偏移量
     */
    List<User> selectUserList(String username, int limit, int offset);

    /**
     * 获取满足条件的总记录数 (用于分页计算)
     * 注意：HBase 统计总数性能极差，通常建议用 Redis 计数，这里做简易实现
     */
    long countUser(String username);
    /**
     * 逻辑删除
     */
    int deleteById(Long id);
}