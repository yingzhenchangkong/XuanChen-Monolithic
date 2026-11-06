import { onUnmounted } from 'vue';
import Sortable from 'sortablejs';

interface DragSortOptions {
  /** 只允许从该选择器元素上按下拖动（如 '.drag-handle'），避免干扰行内输入框 */
  handle: string;
  /** 参与排序的元素选择器；antd 表格必须排除 tbody 里的 measure 行等非数据行 */
  draggable?: string;
  /** 拖拽结束回调（oldIndex → newIndex，均为数据行下标） */
  onEnd: (oldIndex: number, newIndex: number) => void;
}

/**
 * a-table 行拖拽排序（基于 sortablejs，绑定在表格 tbody 上）。
 *
 * 用法：表格渲染出数据行后调用 init(tbodyEl)；组件卸载时自动销毁实例。
 * onEnd 里通常对响应式数据源做 splice，Vue 会按键/位置补齐渲染。
 *
 * 关键点：Sortable 拖完会把被拖行物理移动到新位置，而 Vue 的虚拟 DOM 对这次移动毫不知情。
 * 若保留该 DOM 改动再改数据源，Vue 的 diff 会基于"自己上次渲染的位置"计算移动，
 * 与真实 DOM 对不上，表现为序号列（按渲染下标计算）变了、行内容却错位或回弹。
 * 因此这里先把被拖行放回原位，让数据变更触发的 Vue 渲染全权接管 DOM 顺序。
 *
 * 配套要求：a-table 必须配稳定唯一的 row-key（如 useDragSort 提供的 createRowKeyGetter），
 * 否则新增行（无 id）在 Vue 眼里是同一批"匿名"节点，diff 结果不可靠。
 */
export function useDragSort(options: DragSortOptions) {
  let instance: Sortable | null = null;

  /** 绑定（重复调用会先销毁旧实例再重建） */
  const init = (el?: HTMLElement | null) => {
    destroy();
    if (!el) {
      return;
    }
    instance = Sortable.create(el, {
      handle: options.handle,
      draggable: options.draggable ?? 'tr',
      animation: 150,
      ghostClass: 'drag-sort-ghost',
      onEnd(evt) {
        const { oldIndex, newIndex, item, from } = evt;
        if (oldIndex == null || newIndex == null || oldIndex === newIndex) {
          return;
        }
        // 把被拖行移回原位：拖动后的 tbody 里其余行仍保持原相对顺序，
        // 移除被拖行后按下标插回即可完整还原拖动前的 DOM 顺序
        if (item.parentElement === from) {
          item.remove();
          const rest = Array.from(from.querySelectorAll<HTMLElement>(options.draggable ?? 'tr'));
          if (oldIndex >= rest.length) {
            from.appendChild(item);
          } else {
            from.insertBefore(item, rest[oldIndex]);
          }
        }
        options.onEnd(oldIndex, newIndex);
      },
    });
  };

  const destroy = () => {
    instance?.destroy();
    instance = null;
  };

  onUnmounted(destroy);

  return { init, destroy };
}

/**
 * 为 a-table 的 row-key 生成稳定唯一键：按 record 对象身份分配自增序号（WeakMap 持有，
 * 不往 record 上加字段，避免把内部键提交到后端）。行的增删重排不影响已分配的键。
 */
export function createRowKeyGetter() {
  const keys = new WeakMap<object, number>();
  let seq = 0;
  return (record: object): number => {
    let key = keys.get(record);
    if (key === undefined) {
      key = ++seq;
      keys.set(record, key);
    }
    return key;
  };
}
