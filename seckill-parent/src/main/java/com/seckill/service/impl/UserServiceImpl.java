package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.common.BusinessException;
import com.seckill.common.PageQuery;
import com.seckill.dto.UserLoginDTO;
import com.seckill.dto.UserRegisterDTO;
import com.seckill.entity.User;
import com.seckill.enums.UserRoleEnum;
import com.seckill.mapper.UserMapper;
import com.seckill.service.UserService;
import com.seckill.util.JwtUtil;
import com.seckill.util.RedisCache;
import com.seckill.vo.LoginVO;
import com.seckill.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private RedisCache redisCache;

    @Override
    public void register(UserRegisterDTO req) {
        // 1. 校验用户名是否已存在 (HBase Scan)
        User existUser = userMapper.selectByUsername(req.getUsername());
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        // 2. 构建新用户对象
        User user = new User();
        BeanUtils.copyProperties(req, user);
        // 3. 生成分布式ID (MyBatis-Plus自带工具，适合做HBase RowKey)
        user.setId(IdWorker.getId());
        // 4. 密码加密
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        // 5. 设置默认值
        user.setStatus(1); // 1: 正常
        user.setDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setRole(0); // 管理员用户
        user.setLevel(1);
        user.setPoints(0L);
        user.setBalance(BigDecimal.ZERO);
        user.setLoginCount(0L);
        // 6. 保存到 HBase
        userMapper.saveOrUpdate(user);
        log.info("用户注册成功: id={}, username={}", user.getId(), user.getUsername());
    }

    @Override
    public LoginVO login(UserLoginDTO loginDTO) {
        // 查询用户
        User user = userMapper.selectByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 3. 校验状态
        if (user.getStatus() != 1) {
            throw new RuntimeException("账号已被冻结");
        }
        // 4. 更新登录信息
        user.setLoginCount(user.getLoginCount() + 1);
        user.setLastLogin(LocalDateTime.now());

        userMapper.saveOrUpdate(user);
        // 生成token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
//        todo 将用户信息存入redis，定时过期时间为30分钟
        redisCache.setCacheObject("login:"+user.getId(),user,30, TimeUnit.MINUTES);
        // 构造返回数据
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
//        log.info("用户身份：{}",user.getRole());
        userVO.setRoleDesc(UserRoleEnum.getByCode(user.getRole()).getDesc());
        loginVO.setUser(userVO);
        
        return loginVO;
    }


    @Override
    public UserVO getCurrentUser(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException("token已失效");
        }
        
        Long userId = jwtUtil.getUserId(token);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setRoleDesc(UserRoleEnum.getByCode(user.getRole()).getDesc());
        
        return userVO;
    }

    @Override
    public List<UserVO> getUserList(PageQuery pageQuery, String username) {
        // 调用 HBase Mapper 的 selectUserList，传入最大值作为 Limit 以获取所有符合条件的数据
        List<User> userList = userMapper.selectUserList(username, Integer.MAX_VALUE, 0);
        log.info("用户信息如下：{}", userList);
        // 转换为 VO
        List<UserVO> users = userList.stream()
                .map(user -> {
                    UserVO userVO = new UserVO();
                    BeanUtils.copyProperties(user, userVO);
                    if (user.getRole() != null) {
                        userVO.setRoleDesc(UserRoleEnum.getByCode(user.getRole()).getDesc());
                    } else {
                        userVO.setRoleDesc(UserRoleEnum.getByCode(0).getDesc());
                    }
                    return userVO;
                })
                .collect(Collectors.toList());
        return users;
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        user.setRole(status);

        // HBase 中使用 saveOrUpdate 进行覆盖写
        int result = userMapper.saveOrUpdate(user);

        if (result <= 0) {
            throw new BusinessException("更新失败");
        }
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

}