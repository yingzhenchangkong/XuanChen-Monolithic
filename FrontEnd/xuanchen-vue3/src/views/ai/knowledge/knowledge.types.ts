/** 知识库记录（对应后端 ai_kb） */
export interface AiKbRecord {
  id: string;
  kbName: string;
  kbDesc?: string;
  /** 入库时的向量化模型（换模型需重新解析全部文档） */
  embedModel?: string;
  chunkSize?: number;
  chunkOverlap?: number;
  /** 1启用 0停用 */
  status: number;
  createBy?: string;
  createTime?: string;
  updateTime?: string;
}

/** 知识库文档记录（对应后端 ai_document） */
export interface AiDocRecord {
  id: string;
  kbId: string;
  fileName: string;
  fileId?: string;
  fileType?: string;
  fileSize?: number;
  /** pending 待解析 / parsing 解析中 / ready 就绪 / failed 失败 */
  status: string;
  chunkCount?: number;
  errorMsg?: string;
  createTime?: string;
}

/** 检索命中块 */
export interface AiSearchHit {
  content: string;
  score: number;
  kbId?: string;
  docId?: string;
  fileName?: string;
}

/** 文件中心文件元数据（选择器列表行） */
export interface FileCenterRecord {
  id: string;
  originalName: string;
  ext?: string;
  fileSize?: number;
  createTime?: string;
}
