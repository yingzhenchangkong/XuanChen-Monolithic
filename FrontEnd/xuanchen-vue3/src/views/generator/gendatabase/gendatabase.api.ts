import { getAction } from '@/utils/httpAction';
import type { Result, SelectOption } from '@/types/api';

/** 统一数据源下拉选项（sys_datasource 中方言支持且启用的数据源） */
export const getGenDatabaseSelect = (): Promise<Result<SelectOption[]>> =>
  getAction<SelectOption[]>('/tool/generator/table/selectDatasource', {});
