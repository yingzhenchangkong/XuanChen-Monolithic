/** 代码生成-数据源 */
export interface GenDatabase {
  id?: string;                     // 主键
  connType?: string;               // 连接类型
  connName?: string;               // 连接名称
  host?: string;                   // 主机地址
  port?: string;                   // 端口
  dbName?: string;                 // 数据库名称
  userName?: string;               // 用户名
  password?: string;               // 密码
  orderNo?: number;                // 排序码
  status?: number;                 // 状态（1启用，0停用）
  [key: string]: unknown;
}
