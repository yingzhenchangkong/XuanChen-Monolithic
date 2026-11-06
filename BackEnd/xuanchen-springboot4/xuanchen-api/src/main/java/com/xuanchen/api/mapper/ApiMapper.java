package com.xuanchen.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.api.entity.Api;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

/**
 * Mapper-->数据接口服务-接口定义
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Repository
public interface ApiMapper extends BaseMapper<Api> {

    /**
     * 删除时重命名 api_code（追加 _del_<id>），释放唯一索引 uk_api_code，
     * 使编码可被重新创建（逻辑删行仍占用唯一键，直接重建会撞唯一索引）。
     * 注意：@TableLogic 会追加 del_flag=0 条件匹配不到已删行，故必须用自定义 SQL。
     */
    @Update("UPDATE api SET api_code = CONCAT(api_code, '_del_', id) WHERE id = #{id}")
    int renameCodeOnDelete(String id);
}
