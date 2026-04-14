package com.xuanchen.auth.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.auth.auth.entity.Auth;
import org.springframework.stereotype.Repository;

/**
 * Mapper接口-->认证、授权
 *
 * @author XuanChen
 * @date 2025-03-13
 */
@Repository
public interface AuthMapper extends BaseMapper<Auth> {
}
