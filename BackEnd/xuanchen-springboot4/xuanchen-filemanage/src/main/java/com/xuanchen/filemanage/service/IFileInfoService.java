package com.xuanchen.filemanage.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xuanchen.filemanage.entity.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
/**
 * Service接口-->文件元数据（文件中心）
 *
 * @author XuanChen
 * @date 2026-10-25
 */
public interface IFileInfoService extends IService<FileInfo> {

    /**
     * 通用上传：校验（白名单+魔数+大小）→ 生成 objectKey → 存储端落盘 → 元数据入库
     *
     * @param file       上传文件
     * @param bizType    业务类型（common/avatar/photo/convert...，服务端会做白名单校验）
     * @param username   上传人登录名
     * @return 入库后的元数据（含 id）
     */
    FileInfo upload(MultipartFile file, String bizType, String username);

    /**
     * 通用上传（带文件夹归属）：与 {@link #upload(MultipartFile, String, String)} 同一套校验，
     * 额外校验 folderId 归属有效（空=根目录）
     */
    FileInfo upload(MultipartFile file, String bizType, String username, String folderId);

    /**
     * 字节内容存储：与 upload 同一套校验（白名单+魔数+大小）与 objectKey 规则，
     * 供服务端内部产生文件的场景复用（如 PDF 转 Word 的结果落库）
     *
     * @param bytes          文件内容
     * @param originalName   原始文件名
     * @param bizType        业务类型
     * @param username       归属人登录名
     * @return 入库后的元数据（含 id）
     */
    FileInfo store(byte[] bytes, String originalName, String bizType, String username);

    /**
     * 字节内容存储（带文件夹归属）：与 {@link #store(byte[], String, String, String)} 同一套校验，
     * 额外校验 folderId 归属有效（空=根目录）
     */
    FileInfo store(byte[] bytes, String originalName, String bizType, String username, String folderId);

    /**
     * 我的文件分页（按创建时间倒序）
     *
     * @param pageNo   页码（1 起）
     * @param pageSize 页大小
     * @param keyword  原始文件名模糊筛选，可空
     * @param ext      扩展名精确筛选，可空
     * @param folderId 文件夹筛选：null=全部（含根目录），"0"=仅根目录，其他=指定文件夹
     * @param username 当前登录名
     */
    Page<FileInfo> pageMy(long pageNo, long pageSize, String keyword, String ext, String folderId, String username);

    /**
     * 签发预览/下载地址（校验归属：上传人本人或 admin）
     *
     * @param id         文件元数据 id
     * @param inline     true=内联预览
     * @param username   当前登录名
     * @param isAdmin    当前用户是否 admin
     * @return 访问 URL
     */
    String getUrl(String id, boolean inline, String username, boolean isAdmin);

    /**
     * 读取文本类文件内容（文本在线预览用）。
     * 直接以预签名 URL 内联打开时浏览器不认 charset（text/plain 无编码声明）会乱码，
     * 故由服务端读出字节后按 BOM → UTF-8 严格校验 → GB18030 依次探测编码并解码返回。
     *
     * @param id       文件元数据 id
     * @param charset  指定编码（空=自动探测），用于用户手动切换
     * @param maxBytes 单次最多读取的字节数，超出按字符边界截断
     * @param username 当前登录名
     * @param isAdmin  当前用户是否 admin
     * @return {content=文本内容, charset=实际编码, truncated=是否截断, fileSize=文件总字节数}
     */
    Map<String, Object> textContent(String id, String charset, int maxBytes, String username, boolean isAdmin);

    /**
     * 删除（本人或 admin）：进回收站——逻辑删元数据并记录 delete_time，
     * 存储对象保留，由回收站恢复 / 彻底删除 / 超期定时清理负责移除
     */
    void delete(String id, String username, boolean isAdmin);

    /**
     * 回收站分页（仅本人；admin 传 username=null 查看全部）
     */
    Page<FileInfo> pageRecycle(long pageNo, long pageSize, String keyword, String username);

    /**
     * 从回收站恢复（本人或 admin）；原文件夹已删除时回落到根目录
     */
    void restore(String id, String username, boolean isAdmin);

    /**
     * 彻底删除（本人或 admin）：物理删元数据；
     * 仅当该存储对象已无其他引用（秒传去重可能多记录共享）时才删除对象
     */
    void purge(String id, String username, boolean isAdmin);

    /**
     * 秒传：凭文件名+MD5 尝试直接建元数据（不传文件内容）。
     * 命中条件：同一用户、同 MD5、同目标桶存在有效记录。未命中返回 null，前端转常规上传。
     */
    FileInfo instantUpload(String originalFilename, String md5, String bizType, String username, String folderId);

    /**
     * 定时清理回收站：彻底删除 delete_time 早于保留期的记录（引用计数为 0 时删对象）
     *
     * @return 本次清理的记录数
     */
    int cleanExpiredRecycle(int keepDays);
}
