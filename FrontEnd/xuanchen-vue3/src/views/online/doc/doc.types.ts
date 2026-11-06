/** 在线协同文档列表行（与后端 OnlineDocVO 对应，FileInfo + 协作角色） */
export interface OnlineFileInfo {
  id: string;
  originalName: string;
  /** 扩展名：docx/xlsx/pptx */
  ext: string;
  username: string;
  fileSize?: number;
  createTime?: string;
  updateTime?: string;
  /** owner=属主 collaborator=协作者 */
  role: string;
  /** 协作权限（1=可编辑 2=只读；属主为空） */
  permission?: number;
}

/** 协作者记录 */
export interface CollaboratorRecord {
  id: string;
  fileId: string;
  username: string;
  permission: number;
  createTime?: string;
}
