import { getAction } from '@/utils/httpAction';

enum UserApiUrl {
  SELECT = '/system/user/select',
}

export const getUserSelect = async () => {
  try {
    const res: any = await getAction(UserApiUrl.SELECT, {});
    if (res.code === 200) {
      return res.data;
    }
    return [];
  } catch (error) {
    console.error('获取用户数据失败:', error);
    return [];
  }
}