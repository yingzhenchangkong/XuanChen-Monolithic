import { getAction, httpAction } from '@/utils/httpAction';
import type { GenTable } from './gentable.types';

enum GenTableApiUrl {
  INDEX_LIST = '/tool/generator/table/list',
  INDEX_DELETE = '/tool/generator/table/delete',
  INDEX_DELETE_BATCH = '/tool/generator/table/deleteBatch',

  OPERATION_ADD = '/tool/generator/table/add',
  OPERATION_EDIT = '/tool/generator/table/edit',

  GENERATOR = '/codeGenerator',
}

export { GenTableApiUrl };

export const saveOrUpdate = async (data: GenTable) => {
  const httpUrl = data.id ? GenTableApiUrl.OPERATION_EDIT : GenTableApiUrl.OPERATION_ADD;
  const method = data.id ? 'put' : 'post';
  return await httpAction(httpUrl, data, method);
};

export const generator = async () => {
  return await getAction(GenTableApiUrl.GENERATOR, {});
}