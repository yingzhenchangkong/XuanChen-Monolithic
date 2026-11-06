package com.xuanchen.masterdata.mdperson.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.masterdata.mdperson.entity.MdPerson;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper接口-->人员信息
 *
 * @author XuanChen
 * @date 2026-10-16
 */
@Repository
public interface MdPersonMapper extends BaseMapper<MdPerson> {
    /**
     * 解除所有账号对指定人员的绑定（账号保留为无人员信息的账号）
     *
     * @param personId 人员ID
     */
    @Update("UPDATE sys_account SET person_id = NULL WHERE person_id = #{personId}")
    void unbindPerson(@Param("personId") String personId);

    /**
     * 删除人员的部门关系
     *
     * @param personId 人员ID
     */
    @Delete("DELETE FROM md_person_dept WHERE person_id = #{personId}")
    void deleteDeptRelation(@Param("personId") String personId);

    /**
     * 删除人员的岗位关系
     *
     * @param personId 人员ID
     */
    @Delete("DELETE FROM md_person_post WHERE person_id = #{personId}")
    void deletePostRelation(@Param("personId") String personId);

    /**
     * 回收站 列表（del_flag = 1；@TableLogic 会自动过滤正常数据，回收站查询走原生 SQL 绕过）
     */
    List<MdPerson> listRecycleBin(@Param("page") Page<MdPerson> page, @Param("person") MdPerson person);

    /**
     * 回收站 彻底删除 人员档案（物理删除，仅限 del_flag = 1 的记录）
     */
    Integer deleteRecycleBin(@Param("personIds") String[] personIds);

    /**
     * 回收站 彻底删除 人员部门关系（仅清理回收站中人员的关系行）
     */
    Integer deleteRecycleBinDept(@Param("personIds") String[] personIds);

    /**
     * 回收站 彻底删除 人员岗位关系（仅清理回收站中人员的关系行）
     */
    Integer deleteRecycleBinPost(@Param("personIds") String[] personIds);

    /**
     * 回收站 还原（del_flag 置回 0）
     */
    Integer revertRecycleBin(@Param("personIds") String[] personIds);
}
