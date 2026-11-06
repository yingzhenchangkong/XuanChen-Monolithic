package com.xuanchen.generator.generator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.builder.CustomFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.xuanchen.common.exception.BusinessException;
import com.xuanchen.generator.gendatabase.entity.GenDatasource;
import com.xuanchen.generator.gendatabase.service.GenDatasourceResolver;
import com.xuanchen.generator.gentable.entity.GenTable;
import com.xuanchen.generator.gentable.entity.GenTableColumn;
import com.xuanchen.generator.gentable.entity.GenTableSub;
import com.xuanchen.generator.gentable.service.IGenTableColumnService;
import com.xuanchen.generator.gentable.service.IGenTableService;
import com.xuanchen.generator.gentable.service.IGenTableSubService;
import com.xuanchen.generator.loggenerator.service.ILogGeneratorService;
import com.xuanchen.generator.generator.model.GenFieldView;
import com.xuanchen.generator.generator.model.GenPreviewFile;
import com.xuanchen.generator.generator.security.GeneratorSafetyValidator;
import com.xuanchen.generator.generator.service.ICodeGeneratorService;
import com.xuanchen.generator.generator.template.GenTemplate;
import com.xuanchen.generator.utils.DBUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service接口实现类-->代码生成器
 * <p>
 * 基于 MyBatis-Plus FastAutoGenerator + Freemarker：
 * <ol>
 *     <li>主表一次生成：后端 6 件（Entity/Mapper/MapperXML/Service/ServiceImpl/Controller，Controller
 *     模板按字段配置拼装查询条件）+ 前端 2 件（Index.vue/Operation.vue，按字段配置渲染查询区/表格列/表单）
 *     + menu.sql 菜单权限 SQL；</li>
 *     <li>配置了子表时，子表再单独一次生成（后端 6 件，Controller 含 /listByMainId），
 *     主表 Operation.vue 内含子表维护区块；</li>
 *     <li>字段界面配置优先取 gen_table_column（保存的查询/编辑/列表勾选），未配置时按物理元数据推断默认值。</li>
 * </ol>
 * 输出布局：默认「输出目录/com/xuanchen/... + 输出目录/vue/...」；配置了后端/前端相对路径后
 * 可直接写到仓库的 src/main/java、src/views 之下；menu.sql / ddl.sql 统一落在「输出目录/sql/模块/实体」，
 * 不再混进前端 vue 目录。
 * 注意：MP 3.5.x 的 injectionConfig.customMap 不会进入模板数据模型，
 * 自定义数据必须经 beforeOutputFile 钩子注入；自定义文件名默认拼实体名，需用 formatNameFunction 修正。
 *
 * @author XuanChen
 * @date 2026-02-16
 */
@Service
public class CodeGeneratorServiceImpl implements ICodeGeneratorService {

    /** 审计字段：不进查询/编辑/列表/导出 */
    private static final Set<String> AUDIT_COLUMNS = Set.of(
            "create_by", "create_time", "update_by", "update_time", "del_flag");

    /** 选择类编辑控件（下拉/单选/多选/树选择）需要从字典加载选项 */
    private static final Set<String> SELECT_LIKE_CONTROLS = Set.of("Select", "Radio", "Checkbox", "TreeSelect");

    /** 字段归属 */
    private static final String SCOPE_MAIN = "main";
    private static final String SCOPE_SUB = "sub";

    /** 生成方式（写入生成记录） */
    private static final String TYPE_DISK = "DISK";
    private static final String TYPE_ZIP = "ZIP";
    private static final String TYPE_PREVIEW = "PREVIEW";

    /** 预览上限：文件数与单文件字符数，避免超大响应 */
    private static final int PREVIEW_MAX_FILES = 300;
    private static final long PREVIEW_MAX_BYTES = 2L * 1024 * 1024;
    private static final int PREVIEW_MAX_CHARS = 512 * 1024;

    @Resource
    private GeneratorSafetyValidator validator;
    @Resource
    private IGenTableService genTableService;
    @Resource
    private GenDatasourceResolver genDatasourceResolver;
    @Resource
    private com.xuanchen.generator.utils.DBUtil dbUtil;
    @Resource
    private IGenTableColumnService genTableColumnService;
    @Resource
    private IGenTableSubService genTableSubService;
    @Resource
    private ILogGeneratorService logGeneratorService;

    @Override
    public List<String> codeGeneratorByTableId(String id) {
        GenTable table = null;
        try {
            GenContext ctx = prepareContext(id);
            table = ctx.genTable;
            // 落盘前再次强制校验：保存入口的校验可被绕过（历史脏数据/直接写库），落盘前必须兜底
            Path outputRoot = validator.validateOutputDir(table.getOutputDir());
            layout(ctx, outputRoot);
            Map<Path, FileSnapshot> before = snapshotDirs(ctx);
            doGenerate(ctx, validator.isFileOverrideAllowed());
            List<String> created = collectWritten(ctx, before);
            if (created.isEmpty()) {
                throw new BusinessException(500, "未生成任何文件：目标目录中同名文件已存在且未开启覆盖"
                        + "（xuanchen.generator.allow-file-override），请更换输出目录或开启覆盖后重试");
            }
            logGeneratorService.record(table, TYPE_DISK, ctx.template.key(), created, true, "落盘生成成功");
            return created;
        } catch (RuntimeException e) {
            logGeneratorService.record(table == null ? stub(id) : table, TYPE_DISK, "crud",
                    List.of(), false, e.getMessage());
            throw e;
        }
    }

    @Override
    public void downloadZipByTableId(String id, HttpServletResponse response) {
        GenTable table = null;
        Path tempDir = null;
        try {
            GenContext ctx = prepareContext(id);
            table = ctx.genTable;
            // 临时目录必须建在白名单根目录内：系统临时目录不在白名单，落盘前的 containment 断言会直接拒绝
            tempDir = validator.createTempGenerationRoot();
            layout(ctx, tempDir);
            doGenerate(ctx, true);
            List<String> files = collectWritten(ctx, Map.of());
            writeZip(tempDir, response, table.getTableName() + "-code.zip");
            logGeneratorService.record(table, TYPE_ZIP, ctx.template.key(), files, true, "打包下载成功");
        } catch (IOException e) {
            logGeneratorService.record(table == null ? stub(id) : table, TYPE_ZIP, "crud",
                    List.of(), false, e.getMessage());
            throw new BusinessException(500, "打包下载失败：" + e.getMessage());
        } catch (RuntimeException e) {
            logGeneratorService.record(table == null ? stub(id) : table, TYPE_ZIP, "crud",
                    List.of(), false, e.getMessage());
            throw e;
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Override
    public List<GenPreviewFile> previewByTableId(String id) {
        GenTable table = null;
        Path tempDir = null;
        try {
            GenContext ctx = prepareContext(id);
            table = ctx.genTable;
            // 预览不写用户目录，但仍先校验其配置合法性（配置本身非法就不该预览出"看起来能生成"的假象）
            validator.validateOutputDir(table.getOutputDir());
            tempDir = validator.createTempGenerationRoot();
            layout(ctx, tempDir);
            doGenerate(ctx, true);
            List<GenPreviewFile> files = readPreviewFiles(tempDir);
            logGeneratorService.record(table, TYPE_PREVIEW, ctx.template.key(),
                    files.stream().map(GenPreviewFile::getPath).toList(), true,
                    "生成预览成功，共 " + files.size() + " 个文件");
            return files;
        } catch (IOException e) {
            logGeneratorService.record(table == null ? stub(id) : table, TYPE_PREVIEW, "crud",
                    List.of(), false, e.getMessage());
            throw new BusinessException(500, "生成预览失败：" + e.getMessage());
        } catch (RuntimeException e) {
            logGeneratorService.record(table == null ? stub(id) : table, TYPE_PREVIEW, "crud",
                    List.of(), false, e.getMessage());
            throw e;
        } finally {
            deleteRecursively(tempDir);
        }
    }

    // ---------------------------------------------------------------- 上下文组装

    /**
     * 生成上下文：gen_table + sys_datasource + 字段视图 + 模板数据模型，一次性组装并完成全部校验。
     */
    private GenContext prepareContext(String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException(400, "缺少表配置 id");
        }
        GenTable genTable = genTableService.getById(id);
        if (genTable == null) {
            throw new BusinessException(400, "表配置不存在，请先保存代码生成配置");
        }
        if (genTable.getDatabaseId() == null || genTable.getDatabaseId().isBlank()) {
            throw new BusinessException(400, "未选择数据源，无法生成");
        }
        // 按 sys_datasource.id 解析统一数据源，内部校验存在性、启用状态与方言支持
        GenDatasource genDatabase = genDatasourceResolver.resolve(genTable.getDatabaseId());
        if (genTable.getTableName() == null || genTable.getTableName().isBlank()) {
            throw new BusinessException(400, "未配置数据库表名，无法生成");
        }
        if (genTable.getModuleName() == null || genTable.getModuleName().isBlank()) {
            throw new BusinessException(400, "未配置模块名，无法生成");
        }
        if (genTable.getPackageName() == null || genTable.getPackageName().isBlank()) {
            throw new BusinessException(400, "未配置包名，无法生成");
        }
        // 模块名/包名参与落盘路径拼接（两类生成方式都一样），必须先做标识符校验
        validator.validateIdentifiers(genTable.getModuleName(), genTable.getPackageName());
        validator.validateTableName(genTable.getTableName());
        // 子表配置统一从 gen_table_sub 读取；历史单子表（只有 gen_table.sub_table_name）由 service 回落构造
        List<GenTableSub> subConfigs = genTableSubService.listByTable(genTable);
        // 模板先行解析：单表模板直接忽略子表配置（避免产出无人维护的主子代码）
        GenTemplate genTemplate = GenTemplate.of(genTable.getTemplate());
        if (genTemplate == GenTemplate.SINGLE) {
            subConfigs = List.of();
        }
        Set<String> subNames = new HashSet<>();
        for (GenTableSub sub : subConfigs) {
            validator.validateDbIdentifier(sub.getSubTableName(), "子表名");
            validator.validateDbIdentifier(sub.getSubFkName(), "子表外键");
            if (sub.getSubTableName().equalsIgnoreCase(genTable.getTableName())) {
                throw new BusinessException(400, "子表名不能与主表相同：" + sub.getSubTableName());
            }
            if (!subNames.add(sub.getSubTableName().toLowerCase(Locale.ROOT))) {
                throw new BusinessException(400, "子表「" + sub.getSubTableName() + "」重复配置");
            }
        }
        validator.validateRelativeOutputPath(genTable.getBackendRelPath(), "后端输出相对路径");
        validator.validateRelativeOutputPath(genTable.getFrontendRelPath(), "前端输出相对路径");

        GenContext ctx = new GenContext();
        ctx.genTable = genTable;
        ctx.genDatabase = genDatabase;
        ctx.template = genTemplate;
        ctx.subConfigs = subConfigs;
        ctx.hasSub = !subConfigs.isEmpty();
        ctx.entityName = toEntityName(genTable.getTableName());
        ctx.entityPath = decapitalize(ctx.entityName);
        ctx.modulePath = toPath(genTable.getModuleName());
        ctx.packagePath = toPath(genTable.getPackageName());

        // 表必须真实存在于所选数据源：MP 匹配不到表时不会报错、只会生成 0 个文件，
        // 这里提前拦截，避免用户拿到"成功"却只落了一个 menu.sql
        // 字段配置：优先取已保存的界面配置，未配置时回退为按物理元数据推断默认值
        List<GenTableColumn> columns = genTableColumnService.list(new QueryWrapper<GenTableColumn>()
                .eq("table_id", id).eq("column_scope", SCOPE_MAIN).orderByAsc("order_no"));

        // 「是否创建表 = 是」：目标库中还没有这张表，先按字段配置建表（IF NOT EXISTS，可重复执行），
        // 否则紧接着读取元数据会因"表不存在"失败
        if (isTrue(genTable.getIfCreateTable())) {
            ctx.ddlSql = buildCreateTable(genDatabase, genTable, columns);
            List<Map<String, Object>> existing = dbUtil.getTableInfo(genDatabase, genTable.getTableName());
            if (existing.isEmpty()) {
                // PostgreSQL 的 COMMENT ON 需要多条语句依次执行
                executeDdl(genDatabase, ctx.ddlSql);
            } else {
                // 表已存在（重复生成/已手工建过）：不再执行建表，但必须保证配置字段与真实列一致，
                // 否则生成的实体/页面与表结构不符（运行期才炸），这里提前报清楚差在哪
                checkColumnConsistency(genTable.getTableName(), existing, columns);
            }
        }

        ctx.dbColumns = requireTableColumns(genDatabase, genTable.getTableName());
        ctx.fields = columns.isEmpty()
                ? buildFieldsFromMeta(ctx.dbColumns)
                : buildFieldsFromConfig(columns);
        ctx.model = buildModel(ctx);
        return ctx;
    }

    /**
     * 按方言生成建表语句（MySQL 单条；PostgreSQL 含 COMMENT ON，多条）。
     */
    private String buildCreateTable(GenDatasource db, GenTable table, List<GenTableColumn> columns) {
        List<String> sqls = DBUtil.dialectOf(db).buildCreateTable(table, columns);
        return String.join(";\n", sqls);
    }

    /**
     * 执行建表语句；DDL 已按方言生成，按 ";\n" 拆分后逐条执行。
     */
    private void executeDdl(GenDatasource db, String ddl) {
        String[] sqls = ddl.split(";\\s*\\n");
        dbUtil.executeDdl(db, sqls);
    }

    /**
     * 计算输出布局：
     * <ul>
     *     <li>后端根：outputDir/backendRelPath（为空则 outputDir）；</li>
     *     <li>前端根：outputDir/frontendRelPath（为空则 outputDir/vue）。</li>
     * </ul>
     */
    private void layout(GenContext ctx, Path outputRoot) {
        Path backendRoot = validator.resolveUnderRoot(outputRoot, ctx.genTable.getBackendRelPath());
        Path frontendRoot = isBlank(ctx.genTable.getFrontendRelPath())
                ? validator.resolveUnderRoot(outputRoot, "vue")
                : validator.resolveUnderRoot(outputRoot, ctx.genTable.getFrontendRelPath());
        ctx.outputRoot = outputRoot.toAbsolutePath().normalize();
        ctx.backendRoot = backendRoot;
        ctx.frontendRoot = frontendRoot;
        ctx.backendPackageDir = backendRoot.resolve("com/xuanchen")
                .resolve(ctx.modulePath).resolve(ctx.packagePath).normalize();
        ctx.frontendEntityDir = frontendRoot.resolve(ctx.modulePath).resolve(ctx.entityPath).normalize();
        // mapper xml 的落盘路径为手工拼接，单独对白名单做一次 containment 确认
        validator.assertWithinRoots(ctx.backendPackageDir);
        validator.assertWithinRoots(ctx.frontendEntityDir);
    }

    /**
     * 「创建表」模式下表已存在时，校验字段配置与真实列名集合是否一致。
     * <p>
     * 不一致只报错不自动 ALTER：改表结构是高风险操作，交给用户决定（把开关改成「否」按真实表生成，
     * 或先手工调整表结构）。
     */
    private void checkColumnConsistency(String tableName, List<Map<String, Object>> existing,
                                        List<GenTableColumn> configured) {
        Set<String> actualNames = new LinkedHashSet<>();
        for (Map<String, Object> meta : existing) {
            actualNames.add(String.valueOf(meta.get("columnName")).toLowerCase(Locale.ROOT));
        }
        Set<String> configNames = new LinkedHashSet<>();
        for (GenTableColumn column : configured) {
            configNames.add(column.getColumnName().toLowerCase(Locale.ROOT));
        }
        Set<String> onlyInDb = new LinkedHashSet<>(actualNames);
        onlyInDb.removeAll(configNames);
        Set<String> onlyInConfig = new LinkedHashSet<>(configNames);
        onlyInConfig.removeAll(actualNames);
        if (onlyInDb.isEmpty() && onlyInConfig.isEmpty()) {
            return;
        }
        throw new BusinessException(400, "表 " + tableName + " 已存在，且与字段配置不一致"
                + (onlyInDb.isEmpty() ? "" : "；表中多出字段：" + String.join("、", onlyInDb))
                + (onlyInConfig.isEmpty() ? "" : "；配置多出字段：" + String.join("、", onlyInConfig))
                + "。请把「是否创建表」改为「否」后按真实表结构生成，或先手工调整表结构");
    }

    /**
     * 读取表字段元数据，同时充当"表是否存在"的校验：读不到任何列即视为表不存在。
     */
    private List<Map<String, Object>> requireTableColumns(GenDatasource db, String tableName) {
        List<Map<String, Object>> columns = dbUtil.getTableInfo(db, tableName);
        if (columns.isEmpty()) {
            throw new BusinessException(400, "数据源「" + db.getConnName() + "」（库 " + db.getDbName()
                    + "）中不存在表 " + tableName + "，请检查表名与数据源是否匹配");
        }
        return columns;
    }

    /**
     * 模板数据模型：字段列表经 beforeOutputFile 注入 MP 模板数据（customMap 在 3.5.x 不进根作用域）。
     */
    private Map<String, Object> buildModel(GenContext ctx) {
        GenTable genTable = ctx.genTable;
        Map<String, Object> model = new HashMap<>();
        model.put("fields", ctx.fields);
        model.put("queryFields", filter(ctx.fields, GenFieldView::getIsQuery));
        model.put("editFields", filter(ctx.fields, GenFieldView::getIsEdit));
        model.put("listFields", filter(ctx.fields, GenFieldView::getIsList));
        model.put("selectFields", filter(ctx.fields,
                f -> isTrue(f.getIsEdit()) && SELECT_LIKE_CONTROLS.contains(f.getEditType())));
        model.put("entityName", ctx.entityName);
        model.put("entityPath", ctx.entityPath);
        // Controller 模板里的实体变量名（如 sysDict），模板自身无法从 entity 推导
        model.put("entityVar", ctx.entityPath);
        model.put("tableComment", genTable.getTableComment() == null || genTable.getTableComment().isBlank()
                ? genTable.getTableName() : genTable.getTableComment());
        model.put("moduleName", genTable.getModuleName());
        model.put("packageName", genTable.getPackageName());
        // 前后端 API 前缀：Controller @RequestMapping("/包名/实体路径")，前端 url 直接复用
        model.put("urlBase", "/" + genTable.getPackageName() + "/" + ctx.entityPath);
        model.put("permsBase", genTable.getPackageName() + ":" + ctx.entityPath);
        // 菜单 component 路径（前端 views 下的相对路径）
        model.put("vueDir", ctx.modulePath + "/" + ctx.entityPath);
        model.put("parentMenuId", isBlank(genTable.getParentMenuId()) ? "0" : genTable.getParentMenuId());
        model.put("template", ctx.template.key());
        model.put("hasCreateTime", ctx.fields.stream()
                .anyMatch(f -> "create_time".equals(f.getColumnName())));
        model.put("hasSub", ctx.hasSub);
        model.put("isSubTable", false);

        // 多子表：每张子表产出一份「页面渲染用」模型 + 一份「子表后端生成用」模型。
        // 子表模型必须在 model.put("subTables") 之前复制，避免子表 Controller 继承整份子表列表。
        List<Map<String, Object>> subTables = new ArrayList<>();
        List<Map<String, Object>> subModels = new ArrayList<>();
        for (int index = 0; index < ctx.subConfigs.size(); index++) {
            GenTableSub sub = ctx.subConfigs.get(index);
            String subTableName = sub.getSubTableName();
            String subEntityName = toEntityName(subTableName);
            String subEntityPath = decapitalize(subEntityName);
            String subUrlBase = "/" + genTable.getPackageName() + "/" + subEntityPath;
            String subTableComment = isBlank(sub.getSubComment())
                    ? lookupTableComment(ctx.genDatabase, subTableName) : sub.getSubComment();
            // 子表字段优先取已保存的界面配置（gen_table_column.sub_id），未配置时回退为物理元数据推断
            List<GenTableColumn> subConfigured = sub.getColumns() == null ? List.of() : sub.getColumns();
            List<GenFieldView> subFields = subConfigured.isEmpty()
                    ? buildFieldsFromMeta(requireTableColumns(ctx.genDatabase, subTableName))
                    : buildFieldsFromConfig(subConfigured);
            List<GenFieldView> subEditFields = filter(subFields, GenFieldView::getIsEdit);
            List<GenFieldView> subListFields = filter(subFields, GenFieldView::getIsList);
            List<GenFieldView> subSelectFields = filter(subFields,
                    f -> isTrue(f.getIsEdit()) && SELECT_LIKE_CONTROLS.contains(f.getEditType()));
            boolean subHasCreateTime = subFields.stream()
                    .anyMatch(f -> "create_time".equals(f.getColumnName()));

            Map<String, Object> subTable = new HashMap<>();
            subTable.put("index", index);
            subTable.put("tableName", subTableName);
            subTable.put("tableComment", subTableComment);
            subTable.put("entityName", subEntityName);
            subTable.put("entityPath", subEntityPath);
            subTable.put("fkColumn", sub.getSubFkName());
            subTable.put("fkProperty", toCamel(sub.getSubFkName()));
            subTable.put("urlBase", subUrlBase);
            subTable.put("fields", subFields);
            subTable.put("editFields", subEditFields);
            subTable.put("listFields", subListFields);
            subTable.put("selectFields", subSelectFields);
            subTable.put("hasCreateTime", subHasCreateTime);
            subTables.add(subTable);

            // 子表后端会单独再跑一次生成器：controller 模板里的 entityVar/queryFields/urlBase
            // 必须换成子表自己的，否则子表 Controller 会拼出主表的查询条件（字段不存在，编译不过）
            Map<String, Object> subModel = new HashMap<>(model);
            subModel.put("fields", subFields);
            subModel.put("queryFields", filter(subFields, GenFieldView::getIsQuery));
            subModel.put("editFields", subEditFields);
            subModel.put("listFields", subListFields);
            subModel.put("selectFields", subSelectFields);
            subModel.put("entityName", subEntityName);
            subModel.put("entityPath", subEntityPath);
            subModel.put("entityVar", subEntityPath);
            subModel.put("urlBase", subUrlBase);
            subModel.put("tableComment", subTableComment);
            subModel.put("hasCreateTime", subHasCreateTime);
            // 子表 Controller 只额外渲染 /listByMainId，不能继承主表的子表列表（否则会重复渲染）
            subModel.put("hasSub", false);
            subModel.put("subTables", List.of());
            subModel.put("isSubTable", true);
            subModel.put("subFkColumn", sub.getSubFkName());
            subModels.add(subModel);
        }
        model.put("subTables", subTables);
        ctx.subModels = subModels;
        return model;
    }

    // ---------------------------------------------------------------- 生成执行

    /**
     * 执行生成：主表一次（按模板决定是否含 vue 与 menu.sql），随后每张子表各一次（仅后端，
     * Controller 模板内按 isSubTable 渲染 /listByMainId）。
     */
    private void doGenerate(GenContext ctx, boolean allowOverride) {
        GenTable genTable = ctx.genTable;
        // 模板已固定为完整 CRUD：主表恒定产出前端页面与菜单 SQL（子表仅产出后端 6 件）
        runGenerator(ctx, genTable.getTableName(), allowOverride, true, ctx.model);
        for (int i = 0; i < ctx.subModels.size(); i++) {
            runGenerator(ctx, ctx.subConfigs.get(i).getSubTableName(), allowOverride, false, ctx.subModels.get(i));
        }

        // SQL 统一放「输出目录/sql/模块/实体」，不跟前端页面混在一起
        Path sqlDir = ctx.outputRoot.resolve("sql").resolve(ctx.entityPath).normalize();
        validator.assertWithinRoots(sqlDir);

        // 菜单 SQL 随 sql 目录一起交付（与 zip 打包内容一致）。
        // 注意：MP 产出的 Java/Vue 文件在未开启覆盖且同名存在时会被静默跳过，menu.sql 若仍无条件
        // 覆盖并计入"本次生成"，就会出现"接口成功但只更新了 sql"的假成功；这里必须与 MP 行为一致——
        // 已存在且不允许覆盖时跳过且不计入，让 collectWritten 拿到空集合触发明确报错。
        {
            Path menuSql = sqlDir.resolve("menu.sql");
            if (allowOverride || !Files.exists(menuSql)) {
                writeMenuSql(ctx.model, sqlDir);
                ctx.extraFiles.add(menuSql);
            }
        }
        // 建表语句一并落盘留存（本次已自动执行过，方便审计/在其他环境重放），覆盖策略与 menu.sql 一致
        if (ctx.ddlSql != null) {
            Path ddlFile = sqlDir.resolve("ddl.sql");
            if (allowOverride || !Files.exists(ddlFile)) {
                ctx.extraFiles.add(writeDdlSql(ctx.ddlSql, sqlDir));
            }
        }
    }

    private void runGenerator(GenContext ctx, String tableName, boolean allowOverride,
                              boolean withVue, Map<String, Object> model) {
        GenTable genTable = ctx.genTable;
        String mapperXmlDir = ctx.backendPackageDir.resolve("mapper").resolve("xml").toString();

        FastAutoGenerator.create(
                        dbUtil.buildUrl(ctx.genDatabase),
                        ctx.genDatabase.getUserName(), ctx.genDatabase.getPassword())
                .globalConfig(builder -> builder
                        .author("XuanChen")
                        .outputDir(ctx.backendRoot.toString())
                        .commentDate("yyyy-MM-dd")
                        .disableOpenDir())
                .packageConfig(builder -> builder
                        .parent("com.xuanchen." + genTable.getModuleName())
                        .moduleName(genTable.getPackageName())
                        .pathInfo(Map.of(OutputFile.xml, mapperXmlDir)))
                .strategyConfig(builder -> {
                    builder.addInclude(tableName);
                    var entityBuilder = builder.entityBuilder().enableLombok();
                    var controllerBuilder = builder.controllerBuilder().enableRestStyle();
                    var serviceBuilder = builder.serviceBuilder();
                    var mapperBuilder = builder.mapperBuilder();
                    if (allowOverride) {
                        entityBuilder.enableFileOverride();
                        controllerBuilder.enableFileOverride();
                        serviceBuilder.enableFileOverride();
                        mapperBuilder.enableFileOverride();
                    }
                })
                .injectionConfig(builder -> {
                    // MP 3.5.x：customMap 不进模板根作用域，必须用该钩子把自定义数据合入 objectMap
                    builder.beforeOutputFile((table, objectMap) -> objectMap.putAll(model));
                    if (withVue) {
                        builder.customFile(vueCustomFile("templates/Index.vue.ftl",
                                ctx.frontendEntityDir, "Index", allowOverride));
                        builder.customFile(vueCustomFile("templates/Modal.vue.ftl",
                                ctx.frontendEntityDir.resolve("modal"), "Operation", allowOverride));
                        // 一对多（字典管理式）：额外产出 SubList.vue，挂在主表列表页下方维护子表明细。
                        // hasSub 只在「一对多 + 已配置子表」时为 true（单表模板在 prepareContext 已清空子表配置）
                        if (ctx.hasSub) {
                            builder.customFile(vueCustomFile("templates/SubList.vue.ftl",
                                    ctx.frontendEntityDir, "SubList", allowOverride));
                        }
                    }
                })
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }

    private CustomFile vueCustomFile(String templatePath, Path dir, String baseName, boolean allowOverride) {
        CustomFile.Builder builder = new CustomFile.Builder()
                .templatePath(templatePath)
                // MP 输出名 = formatNameFunction 结果 + fileName，二者都会拼接：
                // formatNameFunction 固定为 "Index"/"Operation"，fileName 只承担扩展名，最终得到 Index.vue/Operation.vue
                .formatNameFunction(table -> baseName)
                .fileName(".vue")
                .filePath(dir.toString());
        if (allowOverride) {
            builder.enableFileOverride();
        }
        return builder.build();
    }

    // ---------------------------------------------------------------- 字段视图构建

    /**
     * 按已保存的 gen_table_column 构建字段视图（列名做标识符校验）。
     */
    private List<GenFieldView> buildFieldsFromConfig(List<GenTableColumn> columns) {
        List<GenFieldView> fields = new ArrayList<>();
        for (GenTableColumn column : columns) {
            validator.validateDbIdentifier(column.getColumnName(), "字段名");
            GenFieldView view = new GenFieldView();
            view.setColumnName(column.getColumnName());
            view.setProperty(toCamel(column.getColumnName()));
            view.setComment(column.getColumnComment() == null || column.getColumnComment().isBlank()
                    ? column.getColumnName() : column.getColumnComment());
            String javaType = toJavaType(column.getColumnType());
            view.setJavaType(javaType);
            view.setDictCode(column.getUiDictCode());

            boolean pk = isTrue(column.getColumnIsPk());
            boolean audit = AUDIT_COLUMNS.contains(column.getColumnName());
            String defaultControl = defaultControl(javaType);
            String editType = column.getUiEditType() == null || column.getUiEditType().isBlank()
                    ? defaultControl : column.getUiEditType();
            String queryType = column.getUiQueryType() == null || column.getUiQueryType().isBlank()
                    ? defaultControl : column.getUiQueryType();

            view.setIsQuery(!audit && isTrue(column.getUiIsQuery()));
            view.setQueryType(queryType);
            view.setQueryOp("Input".equals(queryType) ? "like" : "eq");
            view.setIsEdit(!audit && !pk && isTrue(column.getUiIsEdit()));
            view.setIsRequired(!audit && !pk && isTrue(column.getUiIsRequired()));
            view.setEditType(editType);
            view.setIsList(!audit && isTrue(column.getUiIsList()));
            fields.add(view);
        }
        return fields;
    }

    /**
     * 未保存界面配置时按物理表元数据推断默认值：
     * 普通字段默认进查询/编辑/列表，主键与审计字段只保留在实体中。
     */
    private List<GenFieldView> buildFieldsFromMeta(List<Map<String, Object>> metaList) {
        List<GenFieldView> fields = new ArrayList<>();
        for (Map<String, Object> meta : metaList) {
            String columnName = String.valueOf(meta.get("columnName"));
            GenFieldView view = new GenFieldView();
            view.setColumnName(columnName);
            view.setProperty(toCamel(columnName));
            String comment = String.valueOf(meta.get("columnComment"));
            view.setComment(comment == null || comment.isBlank() || "null".equals(comment)
                    ? columnName : comment);
            String javaType = toJavaType(String.valueOf(meta.get("columnType")));
            view.setJavaType(javaType);
            view.setDictCode(null);

            boolean pk = Boolean.TRUE.equals(meta.get("columnIsPk"));
            boolean audit = AUDIT_COLUMNS.contains(columnName);
            String control = defaultControl(javaType);
            view.setIsQuery(!audit && !pk);
            view.setQueryType(control);
            view.setQueryOp("Input".equals(control) ? "like" : "eq");
            view.setIsEdit(!audit && !pk);
            view.setIsRequired(!audit && !pk && !Boolean.TRUE.equals(meta.get("columnIsNullable")));
            view.setEditType(control);
            view.setIsList(!audit);
            fields.add(view);
        }
        return fields;
    }

    /**
     * DB 类型 --> Java 类型简名（tinyint 一律 Integer，布尔语义在模板里用 Switch 控件的
     * checkedValue 1/0 兼容）。
     */
    private String toJavaType(String dbType) {
        if (dbType == null) {
            return "String";
        }
        return switch (dbType.toLowerCase(Locale.ROOT)) {
            case "tinyint", "smallint", "mediumint", "int", "integer" -> "Integer";
            case "bigint" -> "Long";
            case "decimal", "numeric" -> "BigDecimal";
            case "float" -> "Float";
            case "double" -> "Double";
            case "bit", "bool", "boolean" -> "Boolean";
            case "date" -> "LocalDate";
            case "datetime", "timestamp" -> "LocalDateTime";
            case "time" -> "LocalTime";
            default -> "String";
        };
    }

    /**
     * 未配置界面控件时按 Java 类型推断默认控件。
     */
    private String defaultControl(String javaType) {
        return switch (javaType) {
            case "Integer", "Long", "BigDecimal", "Float", "Double" -> "InputNumber";
            case "Boolean" -> "Switch";
            case "LocalDate", "LocalDateTime" -> "DatePicker";
            case "LocalTime" -> "TimePicker";
            default -> "Input";
        };
    }

    private String lookupTableComment(GenDatasource db, String tableName) {
        return dbUtil.getTableList(db).stream()
                .filter(t -> tableName.equals(String.valueOf(t.get("tableName"))))
                .map(t -> String.valueOf(t.get("tableComment")))
                .filter(c -> c != null && !c.isBlank() && !"null".equals(c))
                .findFirst().orElse(tableName);
    }

    // ---------------------------------------------------------------- 菜单 SQL / 预览 / zip / 工具

    /**
     * 用独立 Freemarker Configuration 渲染 menu.sql.ftl（与 MP 生成互不干扰）。
     */
    private void writeMenuSql(Map<String, Object> model, Path dir) {
        try {
            Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
            configuration.setClassForTemplateLoading(CodeGeneratorServiceImpl.class, "/templates");
            configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
            Template template = configuration.getTemplate("menu.sql.ftl");
            Files.createDirectories(dir);
            try (Writer writer = Files.newBufferedWriter(dir.resolve("menu.sql"), StandardCharsets.UTF_8)) {
                template.process(model, writer);
            }
        } catch (Exception e) {
            throw new BusinessException(500, "菜单 SQL 生成失败：" + e.getMessage());
        }
    }

    /**
     * 递归打包目录为 zip 并流式写到响应。
     */
    private void writeZip(Path dir, HttpServletResponse response, String fileName) throws IOException {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
            List<Path> files;
            try (Stream<Path> stream = Files.walk(dir)) {
                files = stream.filter(Files::isRegularFile).sorted().toList();
            }
            for (Path file : files) {
                String entryName = dir.relativize(file).toString().replace('\\', '/');
                zos.putNextEntry(new ZipEntry(entryName));
                Files.copy(file, zos);
                zos.closeEntry();
            }
        }
    }

    /**
     * 读取预览文件：只读生成根目录内的文本文件，超出上限的截断返回。
     */
    private List<GenPreviewFile> readPreviewFiles(Path root) throws IOException {
        List<Path> files;
        try (Stream<Path> stream = Files.walk(root)) {
            files = stream.filter(Files::isRegularFile).sorted().limit(PREVIEW_MAX_FILES).toList();
        }
        List<GenPreviewFile> result = new ArrayList<>();
        for (Path file : files) {
            GenPreviewFile preview = new GenPreviewFile();
            preview.setPath(root.relativize(file).toString().replace('\\', '/'));
            long size = Files.size(file);
            if (size > PREVIEW_MAX_BYTES) {
                preview.setContent("（文件过大，已跳过预览：" + size + " 字节）");
                preview.setTruncated(true);
            } else {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                if (content.length() > PREVIEW_MAX_CHARS) {
                    content = content.substring(0, PREVIEW_MAX_CHARS);
                    preview.setTruncated(true);
                }
                preview.setContent(content);
            }
            result.add(preview);
        }
        return result;
    }

    /**
     * 生成前快照：只扫描本次可能写出的两个目录，避免输出目录指向仓库根时全盘遍历。
     * 值为 (mtime, size)，用于区分"本次新增"与"本次覆盖更新"。
     */
    private Map<Path, FileSnapshot> snapshotDirs(GenContext ctx) {
        Map<Path, FileSnapshot> files = new HashMap<>();
        files.putAll(listGeneratedFiles(ctx.backendPackageDir));
        files.putAll(listGeneratedFiles(ctx.frontendEntityDir));
        return files;
    }

    /**
     * 本次真正写出的文件：生成后对比快照，新增 + 覆盖更新（mtime/size 变化）都算写入；
     * 未开启覆盖时被 MP 跳过的同名文件 mtime 不变，自然不会出现在清单里。
     * menu.sql/ddl.sql 等非 MP 产出文件固定计入（zip/预览模式下快照为空、全部算新增）。
     */
    private List<String> collectWritten(GenContext ctx, Map<Path, FileSnapshot> before) {
        Set<Path> written = new LinkedHashSet<>();
        for (Map.Entry<Path, FileSnapshot> entry : snapshotDirs(ctx).entrySet()) {
            if (!entry.getValue().equals(before.get(entry.getKey()))) {
                written.add(entry.getKey());
            }
        }
        for (Path file : ctx.extraFiles) {
            if (Files.isRegularFile(file)) {
                written.add(file);
            }
        }
        List<String> result = new ArrayList<>();
        for (Path file : written) {
            result.add(ctx.outputRoot.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/'));
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    /**
     * 列出目录下现有的所有文件及其 (mtime, size)（用于生成前后对比）。目录不存在时返回空集合。
     */
    private Map<Path, FileSnapshot> listGeneratedFiles(Path root) {
        if (root == null || !Files.isDirectory(root)) {
            return Map.of();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile).collect(java.util.stream.Collectors.toMap(
                    p -> p, p -> {
                        try {
                            return new FileSnapshot(Files.getLastModifiedTime(p).toMillis(), Files.size(p));
                        } catch (IOException e) {
                            // 读取属性失败时给一个必不相等的哨兵值，宁可多报不漏报
                            return new FileSnapshot(-1L, -1L);
                        }
                    }));
        } catch (IOException e) {
            // 遍历失败不影响生成，只是拿不到文件清单
            return Map.of();
        }
    }

    /** 文件快照：修改时间 + 大小，两者都不变即认为本次未触碰该文件 */
    private record FileSnapshot(long lastModified, long size) {
    }

    private void deleteRecursively(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException ignored) {
                    // 临时目录清理失败不影响响应，留给系统临时目录清理机制
                }
            });
        } catch (IOException ignored) {
            // 同上
        }
    }

    private List<GenFieldView> filter(List<GenFieldView> fields, Predicate<GenFieldView> predicate) {
        return fields.stream().filter(predicate).toList();
    }

    /** 下划线命名 --> 小驼峰（user_name -> userName） */
    private String toCamel(String name) {
        String[] parts = name.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            }
        }
        return sb.toString();
    }

    /** 下划线命名 --> 大驼峰实体名（sys_user_x -> SysUserX） */
    private String toEntityName(String tableName) {
        StringBuilder sb = new StringBuilder();
        for (String part : tableName.toLowerCase(Locale.ROOT).split("_")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return sb.toString();
    }

    /** 包名 --> 相对路径（system.user -> system/user） */
    private String toPath(String packageName) {
        return packageName.replace('.', '/');
    }

    /**
     * sb3 侧 gen_table_column 的勾选字段是 Integer（0/1），统一按"等于 1 即 true"判断，
     * 同时兼容 Boolean，避免与 sb4 产生行为差异。
     */
    private boolean isTrue(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() == 1;
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 落盘建表语句（已自动执行过，留存用于审计与在其他环境重放）。
     */
    private Path writeDdlSql(String ddl, Path dir) {
        try {
            Files.createDirectories(dir);
            String content = "-- 代码生成器按字段配置自动创建的表结构（生成时已执行）\n" + ddl + ";\n";
            Path file = dir.resolve("ddl.sql");
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            throw new BusinessException(500, "建表语句落盘失败：" + e.getMessage());
        }
    }

    private String decapitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * 日志记录用的占位表配置（prepareContext 阶段失败时只有 id）。
     */
    private GenTable stub(String id) {
        GenTable table = new GenTable();
        table.setId(id);
        return table;
    }

    /**
     * 生成上下文（单次生成内有效）。
     */
    private static class GenContext {
        GenTable genTable;
        GenDatasource genDatabase;
        GenTemplate template;
        boolean hasSub;
        String entityName;
        String entityPath;
        String modulePath;
        String packagePath;
        List<Map<String, Object>> dbColumns;
        List<GenFieldView> fields;
        Map<String, Object> model;
        /** 本次自动执行的建表语句（未开启「创建表」时为 null） */
        String ddlSql;
        /** 子表配置（按 order_no 排好序） */
        List<GenTableSub> subConfigs = new ArrayList<>();
        /** 每张子表各一份 Controller 生成模型，与 subConfigs 下标一一对应 */
        List<Map<String, Object>> subModels = new ArrayList<>();
        /** 输出布局（layout 阶段计算） */
        Path outputRoot;
        Path backendRoot;
        Path frontendRoot;
        Path backendPackageDir;
        Path frontendEntityDir;
        /** menu.sql / ddl.sql 等非 MP 产出文件 */
        final List<Path> extraFiles = new ArrayList<>();
    }
}
