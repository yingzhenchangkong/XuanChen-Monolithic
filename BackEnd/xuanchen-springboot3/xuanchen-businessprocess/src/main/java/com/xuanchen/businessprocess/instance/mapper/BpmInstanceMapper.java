package com.xuanchen.businessprocess.instance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.businessprocess.instance.entity.BpmInstance;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Mapper-->审批流程实例
 *
 * @author XuanChen
 * @date 2026-10-23
 */
public interface BpmInstanceMapper extends BaseMapper<BpmInstance> {

    /**
     * 按 id 加行锁读取（FOR UPDATE）——审批推进前调用，
     * 串行化同一实例的并发处理（并行分支下 token 状态必须串行变更），锁随事务提交/回滚释放
     */
    @Select("SELECT * FROM bpm_instance WHERE id = #{id} FOR UPDATE")
    BpmInstance selectByIdForUpdate(@Param("id") String id);
}
