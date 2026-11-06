package com.xuanchen.common.excel;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.support.ExcelTypeEnum;
import org.apache.fesod.sheet.write.metadata.WriteSheet;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

/**
 * Excel 全量导出工具：主键游标分批查询 + 单个 ExcelWriter 分批追加写入，
 * 避免 service.list() 一次性把全表加载进内存导致 OOM。
 * <p>
 * 基于 Apache Fesod（{@code org.apache.fesod.sheet}）的 FesodSheet 门面。
 *
 * @author XuanChen
 * @date 2026-10-01
 */
public final class FesodSheetExportUtil {
    /**
     * 每批查询/写入条数
     */
    public static final int DEFAULT_BATCH_SIZE = 2000;

    private FesodSheetExportUtil() {
    }

    /**
     * 游标分批拉取数据
     *
     * @param <T> 导出实体类型
     */
    @FunctionalInterface
    public interface PageFetcher<T> {
        /**
         * @param lastId    上一批最后一条记录的主键，首批为 null
         * @param batchSize 每批条数
         * @return 按主键升序排列的一批数据
         */
        List<T> fetch(String lastId, int batchSize);
    }

    /**
     * 流式导出 xlsx：同一 Workbook/Sheet 分批写入，调用方只需提供游标查询逻辑。
     *
     * @param response    HTTP 响应
     * @param fileTitle   下载文件名与 Sheet 名（不含扩展名）
     * @param headClass   表头实体类
     * @param idGetter    实体主键读取器（如 SysAccount::getId）
     * @param pageFetcher 游标分页查询
     */
    public static <T> void export(HttpServletResponse response, String fileTitle, Class<T> headClass,
                                  Function<T, String> idGetter, PageFetcher<T> pageFetcher) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(fileTitle, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

        // ExcelWriter 基于 SXSSF，行写满后刷盘，不在内存保留全部行；try-with-resources 保证 finish/close
        try (ExcelWriter writer = FesodSheet.write(response.getOutputStream(), headClass)
                .excelType(ExcelTypeEnum.XLSX)
                .build()) {
            WriteSheet sheet = FesodSheet.writerSheet(0, fileTitle).build();
            String lastId = null;
            while (true) {
                List<T> batch = pageFetcher.fetch(lastId, DEFAULT_BATCH_SIZE);
                if (batch == null || batch.isEmpty()) {
                    break;
                }
                writer.write(batch, sheet);
                if (batch.size() < DEFAULT_BATCH_SIZE) {
                    break;
                }
                lastId = idGetter.apply(batch.get(batch.size() - 1));
            }
        }
    }

    /**
     * 构造游标查询用的分页对象（固定取第 1 页、不执行 count），
     * 配合 QueryWrapper.gt("id", lastId) 使用，无深分页 offset 放大问题。
     */
    public static <T> IPage<T> cursorPage(int batchSize) {
        return new Page<>(1, batchSize, false);
    }
}
