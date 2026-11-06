/** 代码生成-表配置 */
export interface GenTable {
  id?: string;                     // 主键
  databaseId?: string;             // 数据库id
  tableName?: string;              // 表名
  ifCreateTable?: boolean;         // 是否创建表（true：目标库无此表，生成前按字段配置建表）
  tableComment?: string;           // 表注释
  parentMenuId?: string;           // 上级菜单（menu.sql 挂载位置）
  subTableName?: string;           // 子表名（旧版单子表字段，保留兼容）
  subTableFkName?: string;         // 子表外键名（旧版单子表字段，保留兼容）
  subTables?: GenTableSub[];       // 子表配置（支持一个主表挂多个子表）
  template?: string;               // 生成模板（single=单表 / one2many=一对多；历史 crud/backend/one2one 按一对多处理）
  outputDir?: string;              // 输出目录
  backendRelPath?: string;         // 后端输出相对路径（相对输出目录）
  frontendRelPath?: string;        // 前端输出相对路径（相对输出目录）
  packageName?: string;            // 包名
  moduleName?: string;             // 模块名
  [key: string]: unknown;
}

/** 代码生成-子表配置：一个主表可挂多张子表 */
export interface GenTableSub {
  id?: string;                     // 主键
  tableId?: string;                // 主表配置id
  subTableName?: string;           // 子表物理表名
  subFkName?: string;              // 子表关联主表的外键列
  subComment?: string;             // 子表标题（留空取物理表注释）
  orderNo?: number;                // 排序码（决定页面展示顺序）
  columns?: GenTableColumn[];      // 该子表的字段配置
  [key: string]: unknown;
}

/** 代码生成-表字段配置 */
export interface GenTableColumn {
  id?: string;                     // 主键
  tableId?: string;                // 表id
  columnScope?: 'main' | 'sub' | string; // 字段归属
  subId?: string;                  // 所属子表配置id（空=主表字段）
  columnName?: string;             // 字段 名称
  columnComment?: string;          // 字段 注释
  columnType?: string;             // 字段 类型
  columnLength?: string;           // 字段 长度
  columnScale?: string;            // 字段 小数位长度
  columnDefault?: string;          // 字段 默认值
  columnIsPk?: boolean;            // 字段 是否主键
  columnIsNullable?: boolean;      // 字段 是否允许为NULL
  uiIsRequired?: boolean;          // 界面 是否必填字段
  uiIsEdit?: boolean;              // 界面 是否编辑字段
  uiIsList?: boolean;              // 界面 是否列表字段
  uiIsQuery?: boolean;             // 界面 是否查询字段
  uiQueryType?: string;            // 界面 查询方式
  uiEditType?: string;             // 界面 编辑方式
  uiDictCode?: string;             // 界面 字典编码
  [key: string]: unknown;
}

/** 代码生成-项目预设：把数据源 + 输出路径 + 包名/模块名 + 模板打包复用 */
export interface GenProjectProfile {
  id?: string;                     // 主键
  profileName?: string;            // 预设名称
  databaseId?: string;             // 默认数据源ID
  outputDir?: string;              // 输出目录
  backendRelPath?: string;         // 后端相对路径
  frontendRelPath?: string;        // 前端相对路径
  packageName?: string;            // 包名
  moduleName?: string;             // 模块名
  parentMenuId?: string;           // 上级菜单
  template?: string;               // 生成模板（single / one2many）
  orderNo?: number;                // 排序码
  [key: string]: unknown;
}

/** 代码生成-数据源连接信息 */
export interface GenDatabase {
  id?: string;                     // 主键
  connType?: string;               // 连接类型
  host?: string;                   // 主机
  port?: string;                   // 端口
  dbName?: string;                 // 数据库名称
  userName?: string;               // 用户名
  password?: string;               // 密码
  [key: string]: unknown;
}

/** 数据库物理表（listDBTable 返回项） */
export interface DBTable {
  tableName?: string;              // 表名
  tableComment?: string;           // 表注释
  [key: string]: unknown;
}

/** 表配置 + 主表字段 + 多子表一次性保存 */
export interface GenTableSavePayload {
  table: GenTable;
  columns: GenTableColumn[] | null;
  subTables: GenTableSub[] | null;
}

/** 生成预览文件 */
export interface GenPreviewFile {
  path: string;
  content: string;
  truncated?: boolean;
}

/** 生成记录 */
export interface GenGenerateLog {
  id?: string;
  tableId?: string;
  tableName?: string;
  tableComment?: string;
  moduleName?: string;
  packageName?: string;
  generateType?: string;           // DISK / ZIP / PREVIEW
  template?: string;
  fileCount?: number;
  fileList?: string;
  success?: boolean;
  message?: string;
  createBy?: string;
  createTime?: string;
}

/** 目录选择器-子目录 */
export interface DirItem {
  name: string;                    // 目录名
  path: string;                    // 绝对路径
  writable?: boolean;              // 是否可写
}

/** 目录选择器-根目录 */
export interface DirRoot {
  name: string;
  path: string;
}

/** 目录选择器返回结构（/tool/generator/directory/list） */
export interface DirListing {
  path: string;                    // 当前目录（首次为空）
  parent?: string;                 // 上一级，已在根时为空
  writable?: boolean;              // 当前目录是否可写
  restricted?: boolean;            // 是否受白名单限制
  roots: DirRoot[];                // 可跳转的根目录
  defaultPath?: string;            // 建议默认目录（新建配置时预填）
  dirs: DirItem[];                 // 子目录
}
