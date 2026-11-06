package com.xuanchen.online.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanchen.filemanage.entity.FileCollaborator;
import com.xuanchen.filemanage.entity.FileInfo;
import com.xuanchen.filemanage.service.IFileCollaboratorService;
import com.xuanchen.filemanage.service.IFileInfoService;
import com.xuanchen.online.entity.OnlineDocVO;
import com.xuanchen.online.service.IOnlineDocService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service接口实现类-->在线协同文档（Word/Excel/PPT）
 * <p>内容唯一权威副本是文件中心 file_info 指向的存储对象（docx/xlsx/pptx 原生格式），
 * 本服务只负责：POI 生成空白文档、列表聚合协作角色、重命名/删除/协作者管理。</p>
 *
 * @author XuanChen
 * @date 2026-10-28
 */
@Slf4j
@Service
public class OnlineDocServiceImpl implements IOnlineDocService {

    /** 文档类型 → 扩展名（与 ONLYOFFICE documentType 映射一致：word/cell/slide） */
    private static final Map<String, String> DOC_TYPE_EXT = Map.of(
            "word", "docx", "cell", "xlsx", "slide", "pptx");

    private static final Set<String> DOC_TYPES = DOC_TYPE_EXT.keySet();

    @Autowired
    private IFileInfoService fileInfoService;
    @Autowired
    private IFileCollaboratorService collaboratorService;

    @Override
    public FileInfo create(String docType, String docName, String username) {
        if (docType == null || !DOC_TYPES.contains(docType)) {
            throw new IllegalArgumentException("文档类型非法（word/cell/slide）！");
        }
        String name = docName == null ? "" : docName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("请输入文档名称！");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("文档名称过长（最多 100 字）！");
        }
        String ext = DOC_TYPE_EXT.get(docType);
        byte[] bytes = buildBlank(docType);
        //文件中心统一入库：白名单+魔数+配额校验、桶/objectKey 规则、元数据落库（biz_type='online'）
        return fileInfoService.store(bytes, name + "." + ext, "online", username, null);
    }

    @Override
    public Page<OnlineDocVO> page(long pageNo, long pageSize, String keyword, String username, boolean isAdmin) {
        //我是协作者的文件 id（列表较小时直接 in 查询）
        List<String> collabFileIds = collaboratorService.lambdaQuery()
                .eq(FileCollaborator::getUsername, username)
                .list().stream().map(FileCollaborator::getFileId).toList();
        var query = fileInfoService.lambdaQuery().eq(FileInfo::getBizType, "online");
        if (!isAdmin) {
            //本人创建 OR 我是协作者
            query.and(w -> {
                w.eq(FileInfo::getUsername, username);
                if (!collabFileIds.isEmpty()) {
                    w.or().in(FileInfo::getId, collabFileIds);
                }
            });
        }
        if (keyword != null && !keyword.isBlank()) {
            query.like(FileInfo::getOriginalName, keyword);
        }
        Page<FileInfo> page = query.orderByDesc(FileInfo::getUpdateTime).page(new Page<>(pageNo, pageSize));
        //聚合协作角色
        Map<String, Integer> myPerms = new HashMap<>();
        if (!isAdmin) {
            collaboratorService.lambdaQuery().eq(FileCollaborator::getUsername, username).list()
                    .forEach(c -> myPerms.put(c.getFileId(), c.getPermission()));
        }
        Page<OnlineDocVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(info -> {
            OnlineDocVO vo = new OnlineDocVO();
            BeanUtils.copyProperties(info, vo);
            Integer perm = myPerms.get(info.getId());
            if (perm != null) {
                vo.setRole("collaborator");
                vo.setPermission(perm);
            } else {
                vo.setRole("owner");
            }
            return vo;
        }).toList());
        return result;
    }

    @Override
    public void rename(String fileId, String docName, String username, boolean isAdmin) {
        FileInfo file = requireOwner(fileId, username, isAdmin);
        String name = docName == null ? "" : docName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("请输入文档名称！");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("文档名称过长（最多 100 字）！");
        }
        //仅改名不动扩展名；不刷新 update_time（避免改动协同中的 document key）
        String originalName = name + "." + file.getExt();
        FileInfo update = new FileInfo();
        update.setId(file.getId());
        update.setOriginalName(originalName);
        fileInfoService.updateById(update);
    }

    @Override
    public void remove(String fileId, String username, boolean isAdmin) {
        //文件中心回收站逻辑（内部校验属主/admin），成功后清理协作者记录
        fileInfoService.delete(fileId, username, isAdmin);
        collaboratorService.lambdaUpdate().eq(FileCollaborator::getFileId, fileId).remove();
    }

    @Override
    public List<FileCollaborator> collaborators(String fileId, String username, boolean isAdmin) {
        return collaboratorService.listByFile(fileId, username, isAdmin);
    }

    @Override
    public void addCollaborator(String fileId, String targetUser, Integer permission, String username,
                                boolean isAdmin) {
        collaboratorService.add(fileId, targetUser, permission, username, isAdmin);
    }

    @Override
    public void removeCollaborator(String fileId, String targetUser, String username, boolean isAdmin) {
        collaboratorService.remove(fileId, targetUser, username, isAdmin);
    }

    /**
     * 属主校验（重命名/删除仅属主或 admin）
     */
    private FileInfo requireOwner(String fileId, String username, boolean isAdmin) {
        FileInfo file = fileInfoService.getById(fileId);
        if (file == null) {
            throw new IllegalArgumentException("文件不存在或已删除！");
        }
        if (!isAdmin && !username.equals(file.getUsername())) {
            throw new IllegalArgumentException("仅文件属主可执行此操作！");
        }
        return file;
    }

    /**
     * POI 生成空白文档字节（各十几行，无需打包模板文件）
     */
    private byte[] buildBlank(String docType) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            switch (docType) {
                case "word" -> {
                    try (XWPFDocument doc = new XWPFDocument()) {
                        doc.createParagraph();
                        doc.write(out);
                    }
                }
                case "cell" -> {
                    try (XSSFWorkbook wb = new XSSFWorkbook()) {
                        wb.createSheet("Sheet1");
                        wb.write(out);
                    }
                }
                case "slide" -> {
                    try (XMLSlideShow ppt = new XMLSlideShow()) {
                        ppt.createSlide();
                        ppt.write(out);
                    }
                }
                default -> throw new IllegalArgumentException("文档类型非法！");
            }
            return out.toByteArray();
        } catch (Exception e) {
            log.error("生成空白文档失败 docType={}", docType, e);
            throw new IllegalStateException("生成空白文档失败，请重试！");
        }
    }
}
