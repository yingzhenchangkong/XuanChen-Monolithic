import { getAction } from '@/utils/httpAction';

enum Url {
  selectStatus = '/system/dict/select',
}

export const getStatus = async () => {
  try {
    const res: any = await getAction(Url.selectStatus, { dictCode: 'yes_no' });
    if (res.code === 200) {
      return res.data;
    }
    return [];
  } catch (error) {
    console.error('获取字典数据失败:', error);
    return [];
  }
}
