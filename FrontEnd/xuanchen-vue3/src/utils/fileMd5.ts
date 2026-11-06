import SparkMD5 from 'spark-md5';

/**
 * 计算文件 MD5（分片增量读取，避免大文件全量进内存）
 * 用于文件中心秒传：上传前先问服务端是否已有同内容文件。
 */
export const computeFileMd5 = (file: File): Promise<string> =>
  new Promise((resolve, reject) => {
    const spark = new SparkMD5.ArrayBuffer();
    const chunkSize = 2 * 1024 * 1024;
    const chunks = Math.ceil(file.size / chunkSize);
    let current = 0;
    const reader = new FileReader();

    const loadNext = () => {
      const start = current * chunkSize;
      reader.readAsArrayBuffer(file.slice(start, Math.min(start + chunkSize, file.size)));
    };

    reader.onload = (e) => {
      spark.append(e.target?.result as ArrayBuffer);
      current += 1;
      if (current < chunks) {
        loadNext();
      } else {
        resolve(spark.end());
      }
    };
    reader.onerror = () => reject(new Error('读取文件失败'));
    loadNext();
  });
