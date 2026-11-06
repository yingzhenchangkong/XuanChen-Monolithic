/** 生成器日志列表行 */
export interface LogGeneratorRecord {
  id: string;
  tableName?: string;
  tableComment?: string;
  generateType?: string;
  success: boolean;
  fileCount?: number;
  createBy?: string;
  createTime?: string;
}
