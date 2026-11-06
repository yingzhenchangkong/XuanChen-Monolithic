/**
 * bpmn-js 类型声明（bpmn-js 未内置 TS 类型，按需宽松声明）
 */
declare module 'bpmn-js/lib/Modeler' {
  const BpmnModeler: any;
  export default BpmnModeler;
  export type BpmnModdleDescriptor = Record<string, unknown>;
}

declare module 'bpmn-js/lib/Viewer' {
  const BpmnViewer: any;
  export default BpmnViewer;
}

declare module 'bpmn-js/lib/features/palette' {
  const paletteModule: any;
  export default paletteModule;
}
