import { message } from 'ant-design-vue'
import type { Ref } from 'vue'
import type { Result } from '@/types/api'

/**
 * 列表状态列「启停切换」通用逻辑（乐观更新 + 失败回滚）。
 *
 * 抽取背景：原实现在 config / dict / post / role / user / gendatabase 六个页面里
 * 逐字重复（每份约 18 行），且各页仅「状态取值」与「接口」不同。抽到一处后，
 * 新增页面不再需要复制这段回滚逻辑。
 */
export interface UseStatusToggleOptions<T extends Record<string, any>> {
  /** 页面数据源（useList 返回的 dataSource） */
  dataSource: Ref<T[]>
  /** 状态变更接口，如 changeStatusApi */
  changeApi: (id: string, status: number) => Promise<Result>
  /** 行主键字段名，默认 'id' */
  rowKey?: string
  /** 状态字段名，默认 'status' */
  statusField?: string
  /** 状态值换算，默认 1 ↔ 0（用户管理为 1 ↔ 2） */
  compute?: (current: number) => number
  /** 切换成功后的回调 */
  onSuccess?: (payload: { record: T; index: number; status: number }) => void
}

/** 行数据以宽松形态读写（TS 7 不允许直接对泛型 T 做索引写入） */
type AnyRow = Record<string, any>

export const useStatusToggle = <T extends Record<string, any>>(opts: UseStatusToggleOptions<T>) => {
  const {
    dataSource, changeApi,
    rowKey = 'id',
    statusField = 'status',
    compute,
    onSuccess,
  } = opts

  const rows = dataSource as unknown as Ref<AnyRow[]>
  const getRow = (index: number): AnyRow => rows.value[index]

  const handleStatusChange = async (record: T, index: number) => {
    const oldStatus = getRow(index)[statusField] as number
    const newStatus = compute ? compute(oldStatus) : (oldStatus === 1 ? 0 : 1)
    // 乐观更新先翻 UI；业务失败或网络异常必须回滚，避免界面状态与数据库相反
    getRow(index)[statusField] = newStatus
    try {
      const id = String(getRow(index)[rowKey] ?? '')
      const res = await changeApi(id, newStatus)
      if (res.code === 200) {
        message.success(res.msg)
        onSuccess?.({ record, index, status: newStatus })
      } else {
        getRow(index)[statusField] = oldStatus
        message.error(res.msg)
      }
    } catch {
      getRow(index)[statusField] = oldStatus
      // HTTP / 网络错误已由响应拦截器统一提示
    }
  }

  return { handleStatusChange }
}
