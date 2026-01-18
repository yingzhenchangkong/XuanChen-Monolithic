import { getAction } from '@/utils/httpAction';

enum DictApiUrl {
  SELECT = '/system/dict/select',
}

export const getDictSelect = async (dictCode: string) => {
  try {
    const res: any = await getAction(DictApiUrl.SELECT, { dictCode });
    if (res.code === 200) {
      return res.data;
    }
    return [];
  } catch (error) {
    console.error('获取字典数据失败:', error);
    return [];
  }
}