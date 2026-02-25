export interface GenTable {
  id: string;                     // 主键
  tableName: string;              // 表名
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
  columnName: string;             // 列名
  columnComment: string;          // 列注释
  isRequired: boolean;            // 是否必填字段
  isEdit: boolean;                // 是否编辑字段
  isList: boolean;                // 是否列表字段
  isQuery: boolean;               // 是否查询字段
  queryType: string;              // 查询类型
}