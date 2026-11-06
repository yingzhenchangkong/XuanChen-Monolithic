/**
 * 报表填充引擎：把数据集取数结果按绑定关系写入 Univer 工作簿快照（纯 JSON 操作）。
 * 设计器"试填充预览"与查看页渲染共用。
 */
import type { ReportBinding, ReportConfig, DatasetResult, ReportListBinding, ReportCellBinding } from '../report.types';

/** 工作簿快照（结构对齐 Univer IWorkbookData，做宽松处理避免版本耦合） */
export type Snapshot = Record<string, any>;

/** 取数结果集合：datasetCode -> 数据 */
export type DatasetDataMap = Record<string, DatasetResult>;

export interface FillOutcome {
  snapshot: Snapshot;
  warnings: string[];
}

/** 列名转下标：A->0, Z->25, AA->26 */
export function colNameToIndex(name: string): number {
  let index = 0;
  for (const ch of name.toUpperCase()) {
    index = index * 26 + (ch.charCodeAt(0) - 64);
  }
  return index - 1;
}

/** 下标转列名：0->A */
export function indexToColName(index: number): string {
  let n = index + 1;
  let name = '';
  while (n > 0) {
    const mod = (n - 1) % 26;
    name = String.fromCharCode(65 + mod) + name;
    n = Math.floor((n - 1) / 26);
  }
  return name;
}

/** 'A5' -> { row: 4, col: 0 }（0 起） */
export function parseRef(ref: string): { row: number; col: number } {
  const match = /^([A-Z]+)(\d+)$/i.exec(ref.trim());
  if (!match) return { row: 0, col: 0 };
  return { row: parseInt(match[2], 10) - 1, col: colNameToIndex(match[1]) };
}

/** 'A5:F5' -> 行列区间（0 起） */
export function parseRange(range: string): { startRow: number; startCol: number; endRow: number; endCol: number } {
  const [from, to] = range.split(':');
  const start = parseRef(from ?? 'A1');
  const end = to ? parseRef(to) : start;
  return {
    startRow: Math.min(start.row, end.row),
    startCol: Math.min(start.col, end.col),
    endRow: Math.max(start.row, end.row),
    endCol: Math.max(start.col, end.col),
  };
}

/** 深拷贝快照 */
function cloneSnapshot(snapshot: Snapshot): Snapshot {
  return JSON.parse(JSON.stringify(snapshot));
}

function getActiveSheet(snapshot: Snapshot): Record<string, any> {
  const sheets = snapshot.sheets ?? {};
  const firstKey = Object.keys(sheets)[0];
  return sheets[firstKey] ?? {};
}

/** 聚合计算 */
function aggregate(rows: Record<string, unknown>[], field: string, agg?: string): unknown {
  const values: number[] = rows.map((r) => Number(r[field]) || 0);
  const raw = rows.map((r) => r[field]);
  switch ((agg ?? '').toUpperCase()) {
    case 'SUM':
      return values.reduce((acc, v) => acc + (Number(v) || 0), 0);
    case 'AVG': {
      if (!values.length) return null;
      return values.reduce((acc, v) => acc + (Number(v) || 0), 0) / values.length;
    }
    case 'COUNT':
      return raw.filter((v) => v !== null && v !== undefined && v !== '').length;
    case 'MAX':
      return values.length ? Math.max(...values) : null;
    case 'MIN':
      return values.length ? Math.min(...values) : null;
    default:
      return raw.length ? raw[0] : null;
  }
}

/**
 * 在 cellData 中插入 delta 行：所有 >= fromRow 的行整体平移 delta（delta<0 为删除）。
 * 同时平移 mergeData 与 rowData（行高），保持模板样式与布局。
 */
function shiftRows(sheet: Record<string, any>, fromRow: number, delta: number) {
  if (delta === 0) return;
  const shiftObj = (obj: Record<string, any> | undefined) => {
    if (!obj) return;
    const keys = Object.keys(obj).map(Number).sort((a, b) => (delta > 0 ? b - a : a - b));
    for (const key of keys) {
      if (key >= fromRow) {
        if (delta < 0) {
          const target = key + delta;
          if (target >= 0) obj[target] = obj[key];
          delete obj[key];
        } else {
          obj[key + delta] = obj[key];
          delete obj[key];
        }
      }
    }
  };
  shiftObj(sheet.cellData);
  shiftObj(sheet.rowData);

  // 合并单元格平移（与区间相交的合并先移除，避免错位）
  const mergeData: any[] = sheet.mergeData ?? [];
  sheet.mergeData = mergeData
    .filter((m) => !(m.startRow <= fromRow - 1 && m.endRow >= fromRow - 1))
    .map((m) =>
      m.endRow < fromRow ? m : { ...m, startRow: m.startRow + delta, endRow: m.endRow + delta }
    );

  // 行数随扩展增加
  if (delta > 0 && typeof sheet.rowCount === 'number') {
    sheet.rowCount += delta;
  }
}

/** 写单元格值（保留原样式 s 与公式之外的结构） */
function setCellValue(sheet: Record<string, any>, row: number, col: number, value: unknown) {
  if (!sheet.cellData) sheet.cellData = {};
  if (!sheet.cellData[row]) sheet.cellData[row] = {};
  const cell: Record<string, any> = sheet.cellData[row][col] ?? {};
  // 明细/汇总填充覆盖原值与公式，保留样式
  delete cell.f;
  delete cell.si;
  cell.v = value === null || value === undefined ? '' : value;
  if (typeof value === 'number') {
    cell.t = 2; // CellValueType.NUMBER
  } else if (typeof value === 'boolean') {
    cell.t = 3; // CellValueType.BOOLEAN
  } else {
    cell.t = 1; // CellValueType.STRING
  }
  sheet.cellData[row][col] = cell;
}

/** 清空一行区间内容（值与公式，保留样式） */
function clearRowRange(sheet: Record<string, any>, row: number, startCol: number, endCol: number) {
  for (let col = startCol; col <= endCol; col++) {
    const cell = sheet.cellData?.[row]?.[col];
    if (cell) {
      delete cell.v;
      delete cell.f;
      delete cell.si;
    }
  }
}

/**
 * 按绑定关系把取数结果填充进快照副本。
 * list 绑定：以模板行为准按数据行数扩展/收缩行（含合并区与行高平移）；
 * cell 绑定：写单值（直接取首行字段值或按聚合函数计算）。
 */
export function fillSnapshot(
  config: ReportConfig,
  dataMap: DatasetDataMap,
  options: { maxRows?: number } = {}
): FillOutcome {
  const snapshot = cloneSnapshot((config.snapshot ?? {}) as Snapshot);
  const sheet = getActiveSheet(snapshot);
  const warnings: string[] = [];
  const maxRows = options.maxRows ?? 10000;
  const bindings = Object.values(config.bindings ?? {});

  // 先处理 list 绑定（引发行位移），cell 绑定在行位移完成后按新位置写入
  const listBindings = bindings.filter((b): b is ReportListBinding => b.type === 'list');
  const cellBindings = bindings.filter((b): b is ReportCellBinding => b.type === 'cell');

  // 记录 list 绑定造成的累计行位移，用于修正 cell 绑定引用
  let accumulatedShift = 0;

  for (const binding of listBindings) {
    const data = dataMap[binding.dataset];
    if (!data) {
      warnings.push(`数据集 ${binding.dataset} 没有取数结果，已跳过列表填充。`);
      continue;
    }
    const rows = data.rows.slice(0, Math.min(binding.limit ?? maxRows, maxRows));
    const range = parseRange(binding.range);
    const templateRows = range.endRow - range.startRow + 1;
    const neededRows = Math.max(1, rows.length);
    const delta = neededRows - templateRows;

    // 模板行区间以下的现有内容整体位移，给数据行腾位/收行
    if (delta !== 0) {
      shiftRows(sheet, range.endRow + 1, delta);
      accumulatedShift += delta;
    }

    // 模板行样式：以第一模板行各列 cell 为样式母版
    const templateCells: Record<number, any> = {};
    for (let col = range.startCol; col <= range.endCol; col++) {
      templateCells[col] = sheet.cellData?.[range.startRow]?.[col];
    }

    // 清空整个数据区（模板行 + 扩展区），再逐行写入
    for (let r = range.startRow; r < range.startRow + neededRows; r++) {
      clearRowRange(sheet, r, range.startCol, range.endCol);
      const dataRow = rows[r - range.startRow];
      for (let col = range.startCol; col <= range.endCol; col++) {
        const colName = indexToColName(col);
        const mapping = binding.columns?.[colName];
        if (!mapping) continue;
        const field = typeof mapping === 'string' ? mapping : mapping.field;
        // 无字段映射的列：保留模板行样式母版的原值（如序号列公式等仅在首行保留）
        if (!field) continue;
        if (r > range.startRow && templateCells[col]) {
          // 继承母版样式
          if (!sheet.cellData[r]) sheet.cellData[r] = {};
          if (!sheet.cellData[r][col]) sheet.cellData[r][col] = {};
          const style = templateCells[col]?.s;
          if (style !== undefined) sheet.cellData[r][col].s = style;
        }
        const value = dataRow ? dataRow[field] : null;
        setCellValue(sheet, r, col, value);
      }
    }
  }

  for (const binding of cellBindings) {
    const data = dataMap[binding.dataset];
    if (!data) {
      warnings.push(`数据集 ${binding.dataset} 没有取数结果，已跳过单元格填充。`);
      continue;
    }
    const parsed = parseRef(binding.ref);
    // cell 绑定位于 list 绑定下方时按位移修正（简化：统一累计位移）
    const value = aggregate(data.rows, binding.field, binding.agg);
    setCellValue(sheet, parsed.row + accumulatedShift, parsed.col, value);
  }

  return { snapshot, warnings };
}
