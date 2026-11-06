package com.xuanchen.filemanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanchen.filemanage.entity.FileFolder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper-->文件中心文件夹
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Repository
public interface FileFolderMapper extends BaseMapper<FileFolder> {

    /**
     * 递归收集某文件夹的全部后代id（含自身；MySQL 8 递归 CTE）。
     * 层级防脏数据成环：仅沿 del_flag=0 的有效节点向下递归。
     */
    @Select("WITH RECURSIVE sub(id) AS ("
            + "SELECT id FROM file_folder WHERE id = #{id} AND username = #{username} AND del_flag = 0 "
            + "UNION ALL "
            + "SELECT f.id FROM file_folder f JOIN sub ON f.parent_id = sub.id "
            + "WHERE f.username = #{username} AND f.del_flag = 0"
            + ") SELECT id FROM sub")
    List<String> listDescendantIds(@Param("id") String id, @Param("username") String username);
}
