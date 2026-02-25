export interface GenDatabase {
  id: string;                     // 主键
  connType: string | undefined;   // 连接类型
  connName: string;               // 连接名称
  host: string;                   // 主机地址
  port: string;                   // 端口
  dbName: string;                 // 数据库名称
  userName: string;               // 用户名
  password: string;               // 密码
  orderNo: number | undefined;    // 排序码
  status: boolean;                // 状态
}