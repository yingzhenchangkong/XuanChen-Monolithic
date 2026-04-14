export interface GenTable {
  id: string;                     // 主键
  databaseId: string | undefined; // 数据库id
  tableName: string | undefined;  // 表名
  tableComment: string;           // 表注释
  subTableName: string;           // 子表名
  subTableFkName: string;         // 子表外键名
  template: string;               // 模板
  outputDir: string;              // 输出目录
  packageName: string;            // 包名
  moduleName: string;             // 模块名
}

export interface GenTableColumn {
  id: string;                     // 主键
  tableId: string;                // 表id
  columnName: string;             // 字段 名称
  columnComment: string;          // 字段 注释
  columnType: string | undefined; // 字段 类型
  columnLength: string;           // 字段 长度
  columnScale: string;            // 字段 小数位长度
  columnDefault: string;          // 字段 默认值
  columnIsPk: boolean;            // 字段 是否主键
  columnIsNullable: boolean;      // 字段 是否允许为NULL
  uiIsRequired: boolean;          // 界面 是否必填字段
  uiIsEdit: boolean;              // 界面 是否编辑字段
  uiIsList: boolean;              // 界面 是否列表字段
  uiIsQuery: boolean;             // 界面 是否查询字段
  uiQueryType: string | undefined;// 界面 查询方式
}

export interface GenDatabase {
  id: string;                     // 主键
  connType: string;               // 连接类型
  host: string;                   // 主机
  port: string;                   // 端口
  dbName: string;                 // 数据库名称
  userName: string;               // 用户名
  password: string;               // 密码
}