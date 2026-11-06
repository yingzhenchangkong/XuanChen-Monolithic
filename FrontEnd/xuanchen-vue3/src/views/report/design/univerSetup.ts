/**
 * Univer 表格引擎接入层（@univerjs/presets 1.x）。
 * 设计器与查看页共用：设计器开全部编辑能力，查看页只读渲染。
 */
import { createUniver, defaultTheme, LocaleType, mergeLocales } from '@univerjs/presets';
import { UniverSheetsCorePreset } from '@univerjs/preset-sheets-core';
import zhCN from '@univerjs/preset-sheets-core/locales/zh-CN';
import '@univerjs/preset-sheets-core/lib/index.css';

import type { FUniver } from '@univerjs/core/facade';
import type { Univer } from '@univerjs/core';

export interface UniverCreateOptions {
  /** 挂载容器（DOM id 或元素） */
  container: string | HTMLElement;
  /** 是否可编辑（默认 true；查看页传 false） */
  editable?: boolean;
  header?: boolean;
  toolbar?: boolean;
  formulaBar?: boolean;
  footer?: boolean;
  contextMenu?: boolean;
}

export interface UniverInstance {
  univerAPI: FUniver;
  dispose: () => void;
}

export function createSheetsInstance(options: UniverCreateOptions): UniverInstance {
  const {
    container,
    editable = true,
    header = true,
    toolbar = true,
    formulaBar = true,
    footer = true,
    contextMenu = true,
  } = options;

  const { univer, univerAPI } = createUniver({
    locale: LocaleType.ZH_CN,
    locales: { [LocaleType.ZH_CN]: mergeLocales(zhCN) },
    theme: defaultTheme,
    presets: [
      UniverSheetsCorePreset({
        container,
        header,
        toolbar,
        formulaBar,
        // footer 只接受 false 或配置对象；需要展示时传 undefined 走默认
        footer: footer ? undefined : false,
        contextMenu,
      }),
    ],
  });

  return {
    univerAPI,
    dispose: () => {
      try {
        univer.dispose();
      } catch {
        // 实例可能已随容器卸载
      }
    },
  };
}

/**
 * 创建工作簿单元（snapshot 为空时给一张空白表）。
 * editable=false 时整表只读，用于查看页渲染。
 */
export function createWorkbookUnit(
  univerAPI: FUniver,
  snapshot: Record<string, unknown> | undefined,
  editable: boolean
) {
  const data: Record<string, unknown> = { ...(snapshot ?? {}) };
  if (!data.id) data.id = 'report-sheet';
  if (!data.name) data.name = '报表';
  if (!data.sheets || Object.keys(data.sheets as object).length === 0) {
    data.sheets = {
      sheet1: {
        id: 'sheet1',
        name: 'Sheet1',
        rowCount: 40,
        columnCount: 16,
        cellData: {},
      },
    };
  }
  data.editable = editable;
  return univerAPI.createWorkbook(data as never);
}

/** 空白模板快照（新建报表设计器初始内容） */
export function blankSnapshot(): Record<string, unknown> {
  return {
    id: 'report-template',
    name: '报表模板',
    sheets: {
      sheet1: {
        id: 'sheet1',
        name: 'Sheet1',
        rowCount: 60,
        columnCount: 20,
        cellData: {},
      },
    },
  };
}
