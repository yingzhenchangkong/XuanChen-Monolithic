/**
 * 报表导出与打印引擎：基于填充后的快照（纯 JSON）生成 CSV / Excel / 打印页。
 * 与渲染同源：渲染用什么快照，导出打印就是什么内容。
 * 局限：模板中未被填充覆盖的公式单元格导出为空（快照静态存储不含计算结果）。
 */
import type { Snapshot } from './fillEngine';

export interface GridMerge {
  startRow: number;
  startCol: number;
  endRow: number;
  endCol: number;
}

/** 从快照提取的二维网格（0 起行列） */
export interface Grid {
  sheetName: string;
  maxRow: number;
  maxCol: number;
  /** values[row][col]，数字列保留 number 以便 Excel 数值化 */
  values: (string | number | null)[][];
  merges: GridMerge[];
  /** 列宽（px，来自模板列配置） */
  colWidths: (number | undefined)[];
  /** 行高（px） */
  rowHeights: (number | undefined)[];
}

function getActiveSheet(snapshot: Snapshot): Record<string, any> {
  const sheets = snapshot.sheets ?? {};
  return sheets[Object.keys(sheets)[0] ?? ''] ?? {};
}

/** 单元格取值：富文本对象取拼接文本，公式无值时返回 null */
function readCellValue(cell: any): string | number | null {
  if (!cell) return null;
  const v = cell.v;
  if (v === null || v === undefined) return null;
  if (typeof v === 'number') return Number.isFinite(v) ? v : null;
  if (typeof v === 'boolean') return v ? 'TRUE' : 'FALSE';
  if (typeof v === 'object') {
    // 富文本：取 body 数据流拼接
    const stream = v?.p?.body?.dataStream;
    return typeof stream === 'string' ? stream.replace(/\r/g, '') : null;
  }
  return String(v);
}

/** 提取已使用区域的网格（忽略全空尾部行列） */
export function buildGrid(snapshot: Snapshot): Grid {
  const sheet = getActiveSheet(snapshot);
  const cellData: Record<string, Record<string, any>> = sheet.cellData ?? {};
  const merges: GridMerge[] = (sheet.mergeData ?? []).map((m: any) => ({
    startRow: m.startRow,
    startCol: m.startCol,
    endRow: m.endRow,
    endCol: m.endCol,
  }));

  // 已使用行列范围
  let maxRow = -1;
  let maxCol = -1;
  for (const rKey of Object.keys(cellData)) {
    const row = cellData[Number(rKey)];
    if (!row) continue;
    if (Object.keys(row).some((cKey) => readCellValue(row[Number(cKey)]) !== null)) {
      maxRow = Math.max(maxRow, Number(rKey));
      maxCol = Math.max(maxCol, ...Object.keys(row).map(Number));
    }
  }
  for (const m of merges) {
    maxRow = Math.max(maxRow, m.endRow);
    maxCol = Math.max(maxCol, m.endCol);
  }

  const values: (string | number | null)[][] = [];
  for (let r = 0; r <= maxRow; r++) {
    const row: (string | number | null)[] = [];
    for (let c = 0; c <= maxCol; c++) {
      row.push(readCellValue(cellData[r]?.[c]));
    }
    values.push(row);
  }

  const columnData: Record<string, any> = sheet.columnData ?? {};
  const rowData: Record<string, any> = sheet.rowData ?? {};
  const colWidths: (number | undefined)[] = [];
  for (let c = 0; c <= maxCol; c++) {
    const w = columnData[c]?.w;
    colWidths.push(typeof w === 'number' ? w : undefined);
  }
  const rowHeights: (number | undefined)[] = [];
  for (let r = 0; r <= maxRow; r++) {
    const h = rowData[r]?.h;
    rowHeights.push(typeof h === 'number' ? h : undefined);
  }

  return {
    sheetName: sheet.name ?? 'Sheet1',
    maxRow,
    maxCol,
    values,
    merges,
    colWidths,
    rowHeights,
  };
}

/** 下载二进制/文本文件 */
function download(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/** 导出 CSV（UTF-8 BOM，Excel 直接打开不乱码） */
export function exportCsv(snapshot: Snapshot, fileName: string) {
  const grid = buildGrid(snapshot);
  const lines: string[] = [];
  for (const row of grid.values) {
    lines.push(
      row
        .map((v) => {
          if (v === null || v === undefined) return '';
          const s = String(v);
          return /[",\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
        })
        .join(',')
    );
  }
  download(new Blob(['\ufeff' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8' }), `${fileName}.csv`);
}

/** 导出 Excel（xlsx：值 + 合并区 + 列宽行高） */
export async function exportXlsx(snapshot: Snapshot, fileName: string) {
  const ExcelJS = await import('exceljs');
  const grid = buildGrid(snapshot);
  const workbook = new ExcelJS.Workbook();
  const sheet = workbook.addWorksheet(grid.sheetName.slice(0, 31) || 'Sheet1');

  for (let c = 0; c <= grid.maxCol; c++) {
    const w = grid.colWidths[c];
    // Univer 列宽是 px，ExcelJS 宽度是字符数，约 px/7
    sheet.getColumn(c + 1).width = typeof w === 'number' ? Math.max(4, Math.round(w / 7)) : 12;
  }
  for (let r = 0; r <= grid.maxRow; r++) {
    const h = grid.rowHeights[r];
    if (typeof h === 'number') sheet.getRow(r + 1).height = Math.round(h * 0.75);
    for (let c = 0; c <= grid.maxCol; c++) {
      const v = grid.values[r][c];
      if (v !== null) sheet.getCell(r + 1, c + 1).value = v;
    }
  }
  for (const m of grid.merges) {
    if (m.endRow > m.startRow || m.endCol > m.startCol) {
      sheet.mergeCells(m.startRow + 1, m.startCol + 1, m.endRow + 1, m.endCol + 1);
    }
  }

  const buffer = await workbook.xlsx.writeBuffer();
  download(
    new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
    `${fileName}.xlsx`
  );
}

/** 合并区覆盖矩阵：被合并覆盖（非左上角）的格子导出时跳过 */
function buildCoverMap(grid: Grid): Set<string> {
  const covered = new Set<string>();
  for (const m of grid.merges) {
    for (let r = m.startRow; r <= m.endRow; r++) {
      for (let c = m.startCol; c <= m.endCol; c++) {
        if (r === m.startRow && c === m.startCol) continue;
        covered.add(`${r}:${c}`);
      }
    }
  }
  return covered;
}

/** 打印/导出共用片段：标题 + 表格（含合并区、列宽行高、基础边框） */
function buildPrintFragment(snapshot: Snapshot, title: string): { styles: string; bodyHtml: string } {
  const grid = buildGrid(snapshot);
  const covered = buildCoverMap(grid);
  const esc = (s: string) =>
    s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

  const colGroup = grid.colWidths
    .map((w) => `<col style="width:${typeof w === 'number' ? w : 72}px">`)
    .join('');
  const rows = grid.values
    .map((row, r) => {
      const cells = row
        .map((v, c) => {
          if (covered.has(`${r}:${c}`)) return '';
          const m = grid.merges.find((x) => x.startRow === r && x.startCol === c);
          const rs = m ? ` rowspan="${m.endRow - m.startRow + 1}"` : '';
          const cs = m ? ` colspan="${m.endCol - m.startCol + 1}"` : '';
          return `<td${rs}${cs}>${v === null ? '' : esc(String(v))}</td>`;
        })
        .join('');
      const h = grid.rowHeights[r];
      return `<tr style="${typeof h === 'number' ? `height:${h}px;` : ''}">${cells}</tr>`;
    })
    .join('');

  return {
    styles: `
  body { font-family: "Microsoft YaHei", SimSun, sans-serif; font-size: 12px; color: #000; }
  h1 { font-size: 16px; text-align: center; margin: 0 0 8px; }
  table { border-collapse: collapse; }
  td { border: 1px solid #666; padding: 3px 6px; word-break: break-all; }`,
    bodyHtml: `<h1>${esc(title)}</h1>
<table>${colGroup}${rows}</table>`,
  };
}

/** 快照转打印用 HTML（完整文档） */
export function snapshotToPrintHtml(snapshot: Snapshot, title: string): string {
  const { styles, bodyHtml } = buildPrintFragment(snapshot, title);
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>${title}</title>
<style>
  @page { margin: 10mm; }
${styles}
</style>
</head>
<body>
${bodyHtml}
</body>
</html>`;
}

/** 打印：隐藏 iframe 加载打印页后调用浏览器打印 */
export function printSnapshot(snapshot: Snapshot, title: string) {
  const html = snapshotToPrintHtml(snapshot, title);
  const iframe = document.createElement('iframe');
  iframe.style.position = 'fixed';
  iframe.style.right = '0';
  iframe.style.bottom = '0';
  iframe.style.width = '0';
  iframe.style.height = '0';
  iframe.style.border = '0';
  document.body.appendChild(iframe);
  const doc = iframe.contentWindow?.document;
  if (!doc) return;
  doc.open();
  doc.write(html);
  doc.close();
  // 等待渲染完成后调起打印，打印完移除 iframe
  iframe.contentWindow?.focus();
  setTimeout(() => {
    iframe.contentWindow?.print();
    setTimeout(() => document.body.removeChild(iframe), 500);
  }, 300);
}

/** 导出 PDF：打印片段渲染到屏幕外容器，html2canvas 截图后 jsPDF 按 A4 分页（宽表自动横向） */
export async function exportPdf(snapshot: Snapshot, fileName: string) {
  const [{ jsPDF }, html2canvasModule] = await Promise.all([import('jspdf'), import('html2canvas')]);
  const html2canvas = html2canvasModule.default;

  // 屏幕外渲染容器（html2canvas 需要元素在文档中且可见区域外）
  const host = document.createElement('div');
  host.style.cssText = 'position:fixed;left:-10000px;top:0;background:#fff;';
  const { styles, bodyHtml } = buildPrintFragment(snapshot, fileName);
  const styleEl = document.createElement('style');
  styleEl.textContent = styles;
  host.appendChild(styleEl);
  const content = document.createElement('div');
  content.style.cssText = 'padding:10mm;background:#fff;width:max-content;';
  content.innerHTML = bodyHtml;
  host.appendChild(content);
  document.body.appendChild(host);

  try {
    const canvas = await html2canvas(content, { scale: 2, backgroundColor: '#ffffff', logging: false });
    // 表格总宽超过 A4 纵向可用宽度（~730px）时用横向
    const totalWidth = gridTotalWidth(snapshot);
    const pdf = new jsPDF({
      orientation: totalWidth > 730 ? 'landscape' : 'portrait',
      unit: 'mm',
      format: 'a4',
    });
    const pageW = pdf.internal.pageSize.getWidth();
    const pageH = pdf.internal.pageSize.getHeight();
    const imgW = pageW;
    const imgH = (canvas.height * imgW) / canvas.width;
    if (imgH <= pageH) {
      pdf.addImage(canvas.toDataURL('image/jpeg', 0.92), 'JPEG', 0, 0, imgW, imgH);
    } else {
      // 分页：整图按页高逐页下移绘制，PDF 自动裁切
      for (let offset = 0; offset < imgH; offset += pageH) {
        if (offset > 0) pdf.addPage();
        pdf.addImage(canvas.toDataURL('image/jpeg', 0.92), 'JPEG', 0, -offset, imgW, imgH);
      }
    }
    pdf.save(`${fileName}.pdf`);
  } finally {
    document.body.removeChild(host);
  }
}

/** 表格总宽（px）：列宽求和，用于 PDF 横/纵向判断 */
function gridTotalWidth(snapshot: Snapshot): number {
  const grid = buildGrid(snapshot);
  return grid.colWidths.reduce<number>((sum, w) => sum + (typeof w === 'number' ? w : 72), 0);
}

/** 导出 Word（.docx）：真实 docx 格式，含合并单元格、列宽行高、标题 */
export async function exportDocx(snapshot: Snapshot, fileName: string) {
  const docx = await import('docx');
  const {
    Document,
    Packer,
    Paragraph,
    TextRun,
    Table,
    TableRow,
    TableCell,
    WidthType,
    AlignmentType,
    HeightRule,
  } = docx;

  const grid = buildGrid(snapshot);
  const covered = buildCoverMap(grid);

  const rows = [];
  for (let r = 0; r <= grid.maxRow; r++) {
    const cells = [];
    for (let c = 0; c <= grid.maxCol; c++) {
      if (covered.has(`${r}:${c}`)) continue;
      const m = grid.merges.find((x) => x.startRow === r && x.startCol === c);
      const v = grid.values[r][c];
      const text = v === null ? '' : String(v);
      cells.push(
        new TableCell({
          rowSpan: m ? m.endRow - m.startRow + 1 : undefined,
          columnSpan: m ? m.endCol - m.startCol + 1 : undefined,
          width: { size: Math.round((grid.colWidths[c] ?? 72) * 15), type: WidthType.DXA },
          children: [new Paragraph({ children: [new TextRun(text)] })],
        })
      );
    }
    const h = grid.rowHeights[r];
    rows.push(
      new TableRow({
        children: cells,
        height: typeof h === 'number' ? { value: Math.round(h * 15), rule: HeightRule.ATLEAST } : undefined,
      })
    );
  }

  const table = new Table({ rows, width: { size: 100, type: WidthType.PERCENTAGE } });
  const doc = new Document({
    styles: {
      default: {
        document: {
          run: { font: 'Microsoft YaHei', size: 21 }, // 21 半磅 = 10.5pt
        },
      },
    },
    sections: [
      {
        children: [
          new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: fileName, bold: true, size: 32 })],
          }),
          table,
        ],
      },
    ],
  });

  const blob = await Packer.toBlob(doc);
  download(blob, `${fileName}.docx`);
}
