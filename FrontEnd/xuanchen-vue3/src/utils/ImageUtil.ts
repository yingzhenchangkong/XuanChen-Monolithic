/** 文件访问基址（/api/filemanage/static/）：<img src> 是浏览器直发，需显式补 /api 前缀 */
export const FILE_VIEW_BASE_URL = import.meta.env.APP_BASE_URL + import.meta.env.APP_FILE_VIEW_PATH;

/** 已是完整 URL（http(s)/协议相对/blob:/data: 等）时不再拼接前缀 */
const isAbsoluteUrl = (path: string) => /^[a-z][a-z0-9+.-]*:/i.test(path) || path.startsWith('//');

/**
 * 相对路径规范化编码：
 * 1. 去掉前导斜杠，避免与基址尾斜杠拼成 //；
 * 2. 先 decodeURI 再 encodeURI，兼容历史数据里已编码（%20）与未编码（中文/空格）混存，避免双重编码；
 * 3. encodeURI 保留 /、?、& 等 URL 保留字符，仅对中文、空格等不安全字符转义。
 */
const encodePath = (path: string) => {
  const normalized = path.replace(/^\/+/, '');
  try {
    return encodeURI(decodeURI(normalized));
  } catch {
    // 存在无法解码的 % 序列时直接编码原文
    return encodeURI(normalized);
  }
};

/**
 * 后端存的是相对路径（如 avatar/头像 1.png），统一在此拼接为可访问 URL 并编码。
 * 传入空值返回 null（a-avatar/a-image 会走默认插槽/兜底）。
 */
export const getImageView = (imagePath?: string | null): string | null => {
  if (!imagePath) return null;
  const trimmed = imagePath.trim();
  if (!trimmed) return null;
  if (isAbsoluteUrl(trimmed)) return trimmed;
  return FILE_VIEW_BASE_URL + encodePath(trimmed);
};
