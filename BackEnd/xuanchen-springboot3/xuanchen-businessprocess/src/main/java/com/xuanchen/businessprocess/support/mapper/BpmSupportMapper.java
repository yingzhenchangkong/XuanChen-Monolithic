package com.xuanchen.businessprocess.support.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * Mapper-->工作流支撑查询（跨 system 表的只读联查）
 *
 * <p>注意模块边界：仅做只读 SELECT，不修改 system 数据。</p>
 *
 * @author XuanChen
 * @date 2026-10-23
 */
public interface BpmSupportMapper {

    /**
     * 按角色编码查启用账号的登录名
     */
    @Select("SELECT DISTINCT a.user_name FROM sys_account a "
            + "JOIN sys_account_role ar ON ar.account_id = a.id "
            + "JOIN sys_role r ON r.id = ar.role_id "
            + "WHERE r.role_code = #{roleCode} AND a.del_flag = 0 AND a.status = 1")
    List<String> listUserNamesByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 按登录名集合查 映射 userName -> nickName
     */
    @Select("<script>SELECT user_name AS userName, nick_name AS nickName FROM sys_account "
            + "WHERE del_flag = 0 AND user_name IN "
            + "<foreach collection='userNames' item='u' open='(' separator=',' close=')'>#{u}</foreach>"
            + "</script>")
    List<Map<String, Object>> mapNickNames(@Param("userNames") List<String> userNames);

    /**
     * 启用账号下拉（userName + nickName，发起人自选审批人用，登录级）
     */
    @Select("SELECT user_name AS userName, nick_name AS nickName FROM sys_account "
            + "WHERE del_flag = 0 AND status = 1 ORDER BY user_name")
    List<Map<String, Object>> listEnabledAccounts();

    /**
     * 校验账号存在且启用（转办/委托目标校验用）
     */
    @Select("SELECT COUNT(*) FROM sys_account WHERE del_flag = 0 AND status = 1 AND user_name = #{userName}")
    int countEnabledAccount(@Param("userName") String userName);

    /**
     * 查部门负责人（md_dept.leader_user，存登录名；部门不存在/已删除返回 null）
     */
    @Select("SELECT leader_user FROM md_dept WHERE dept_code = #{deptCode} AND del_flag = 0 LIMIT 1")
    String findDeptLeaderUser(@Param("deptCode") String deptCode);

    /**
     * 全量启用部门（dept_code / parent_dept_code / leader_user），引擎内存中沿部门树上溯找负责人
     */
    @Select("SELECT dept_code AS deptCode, parent_dept_code AS parentDeptCode, leader_user AS leaderUser "
            + "FROM md_dept WHERE del_flag = 0")
    List<Map<String, Object>> listAllDepts();

    /**
     * 登录账号（经绑定人员）所属部门编码集合（md_person_dept.dept_id 存 dept_code；未绑定人员返回空）
     */
    @Select("SELECT pd.dept_id FROM sys_account a JOIN md_person_dept pd ON pd.person_id = a.person_id "
            + "WHERE a.user_name = #{userName} AND a.del_flag = 0")
    List<String> listPersonDeptCodes(@Param("userName") String userName);
}
