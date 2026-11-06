/**
 * simple-mind-map 类型声明（包声称带 types 但发布产物缺失，按需宽松声明）
 * 参考版本：0.14.x；仅声明本项目用到的 API 面
 */
declare module 'simple-mind-map' {
  export interface MindMapNodeData {
    data: {
      text: string;
      uid?: string;
      [key: string]: unknown;
    };
    children: MindMapNodeData[];
  }

  export interface MindMapFullSnapshot {
    layout?: string;
    root: MindMapNodeData;
    theme?: {
      template?: string;
      config?: Record<string, unknown>;
    };
    view?: unknown;
  }

  export interface MindMapNode {
    getData<T = unknown>(key: string): T;
    /** 是否为根节点 */
    isRoot?: boolean;
    [key: string]: unknown;
  }

  export interface MindMapOptions {
    el: HTMLElement;
    data?: MindMapNodeData;
    layout?: string;
    theme?: string;
    readonly?: boolean;
    /** 鼠标滚轮行为：zoom=缩放（默认 move=平移画布） */
    mousewheelAction?: 'zoom' | 'move';
    [key: string]: unknown;
  }

  export default class MindMap {
    constructor(options: MindMapOptions);

    /** 注册插件：精简入口 index.js 默认不注册任何插件，导出前必须注册 Export */
    static usePlugin(plugin: unknown): typeof MindMap;

    /** 获取数据；withConfig=true 时返回 {layout, root, theme:{template,config}, view} 全量快照 */
    getData(withConfig?: boolean): MindMapNodeData | MindMapFullSnapshot;
    /** 执行命令：INSERT_CHILD_NODE / INSERT_NODE / REMOVE_NODE / SET_NODE_FONT_SIZE 等 */
    execCommand(...args: unknown[]): void;
    on(event: string, handler: (...args: any[]) => void): void;
    off(event: string, handler: (...args: any[]) => void): void;
    destroy(): void;

    view: {
      reset(): void;
      [key: string]: unknown;
    };
    render: {
      render(): void;
      [key: string]: unknown;
    };
    renderer: {
      /** 当前选中节点列表 */
      activeNodeList: MindMapNode[];
      /** 根节点移到画布中心（保持当前缩放） */
      setRootNodeCenter(): void;
      [key: string]: unknown;
    };
    /**
     * 导出：第二参是 isDownload（true 时库内部自行触发下载），不是 includeConfig。
     * png/svg 返回 data:URL 字符串；内部异常被库吞掉时返回 undefined。
     */
    export(
      type: 'png' | 'svg' | 'json' | 'pdf' | 'smm',
      isDownload?: boolean,
      name?: string,
      ...rest: unknown[]
    ): Promise<string | null | undefined>;
  }
}

/** 按需引入的导出插件（精简入口未注册，需 MindMap.usePlugin 注册） */
declare module 'simple-mind-map/src/plugins/Export.js' {
  const plugin: unknown;
  export default plugin;
}
