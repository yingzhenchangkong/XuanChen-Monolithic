package com.xuanchen.filemanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.filemanage.entity.FileInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper-->文件元数据
 * 注意：FileInfo.delFlag 带 @TableLogic，MP 自动追加 del_flag=0 过滤；
 * 回收站/引用计数等必须看到已删行的场景一律走本类的自定义 SQL。
 *
 * @author XuanChen
 * @date 2026-10-25
 */
@Repository
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    /**
     * 按主键查询（不过滤逻辑删除，供回收站恢复/彻底删除使用）
     */
    @Select("SELECT * FROM file_info WHERE id = #{id}")
    FileInfo selectByIdIgnoreDeleted(@Param("id") String id);

    /**
     * 回收站分页（仅逻辑删记录，按删除时间倒序；username 为空时查全部——admin 视角）
     */
    @Select("<script>SELECT * FROM file_info WHERE del_flag = 1"
            + "<if test='username != null and username != \"\"'> AND username = #{username}</if>"
            + "<if test='keyword != null and keyword != \"\"'> AND original_name LIKE CONCAT('%', #{keyword}, '%')</if>"
            + " ORDER BY delete_time DESC</script>")
    IPage<FileInfo> pageRecycle(Page<FileInfo> page, @Param("username") String username,
                                @Param("keyword") String keyword);

    /**
     * 回收站超期记录（定时清理批次，每批最多 200 条）
     */
    @Select("SELECT * FROM file_info WHERE del_flag = 1 AND delete_time IS NOT NULL"
            + " AND delete_time &lt; #{deadline} LIMIT 200")
    List<FileInfo> listRecycleExpired(@Param("deadline") LocalDateTime deadline);

    /**
     * 物理删除（彻底删除，不受逻辑删影响）
     */
    @Delete("DELETE FROM file_info WHERE id = #{id}")
    int deleteByIdPhysical(@Param("id") String id);

    /**
     * 回收站恢复（必须自定义 SQL：lambdaUpdate 会被 @TableLogic 追加 del_flag=0
     * 条件而永远匹配不到回收站中的行）。folderId 传 null 表示回落根目录。
     */
    @Update("UPDATE file_info SET del_flag = 0, delete_time = NULL, folder_id = #{folderId},"
            + " update_time = NOW() WHERE id = #{id}")
    int restoreById(@Param("id") String id, @Param("folderId") String folderId);

    /**
     * 同一存储对象的其余引用数（含回收站中的记录，任何 del_flag）。
     * 秒传去重后多个元数据可能指向同一 objectKey，仅当引用数为 0 才允许删存储对象。
     */
    @Select("SELECT COUNT(*) FROM file_info WHERE bucket = #{bucket} AND object_key = #{objectKey}"
            + " AND id != #{excludeId}")
    long countReferrer(@Param("bucket") String bucket, @Param("objectKey") String objectKey,
                       @Param("excludeId") String excludeId);

    /**
     * 用户有效文件总字节数（配额校验/用量统计）
     */
    @Select("SELECT COALESCE(SUM(file_size), 0) FROM file_info WHERE username = #{username} AND del_flag = 0")
    long sumUsedBytes(@Param("username") String username);
}
