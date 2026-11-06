/** 文件记录（文件中心） */
export interface FileInfoRecord {
  id: string;
  originalName: string;
  ext?: string;
  fileSize?: number;
  fileType?: string;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}
