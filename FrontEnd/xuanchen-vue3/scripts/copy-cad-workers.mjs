/**
 * 把 CAD 查看器（@mlightcad）运行所需的 Web Worker / WASM 静态资源
 * 从 node_modules 拷贝到 public/workers，供前端按固定 URL 加载。
 *
 * 为什么不直接用包内路径：libredwg 的 worker 在运行期按「与自身同目录」
 * 加载 libredwg-web.wasm，走 Vite 打包会被改名打散，放到 public 下最稳。
 * npm postinstall 钩子自动执行，也可手动 `npm run copy:cad-workers`。
 */
import { copyFileSync, mkdirSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const destDir = join(root, 'public', 'workers');
mkdirSync(destDir, { recursive: true });

const files = [
  // DWG 解析 worker（GPL，隔离在独立 worker bundle 中）+ 其同目录加载的 wasm
  'node_modules/@mlightcad/libredwg-converter/dist/libredwg-parser-worker.js',
  'node_modules/@mlightcad/libredwg-converter/dist/libredwg-web.wasm',
  // MTEXT 排版 worker（AcApDocManager.webworkerFileUrls.mtextRender 指向它）
  'node_modules/@mlightcad/cad-simple-viewer/dist/mtext-renderer-worker.js',
];

let copied = 0;
for (const rel of files) {
  const src = join(root, rel);
  if (!existsSync(src)) {
    console.warn(`[copy-cad-workers] 跳过（不存在）：${rel}`);
    continue;
  }
  copyFileSync(src, join(destDir, rel.split('/').pop()));
  copied++;
}

// cad-viewer.js 内部自带 `import './cad-viewer.css'`，Vite 会解析包内原始 CSS。
// 两类问题都在 node_modules 里就地修补（postinstall 每次安装后都会执行，幂等）：
// 1) 产物含未编译的 v-bind() 媒体查询占位，lightningcss 压缩直接报错；
// 2) 产物含裸全局规则（body{margin:0;display:flex}、body,html{height:100%}、
//    html,body{overflow-x:hidden}、body{font-family:...}），预览打开后随 chunk
//    注入全页，body 变 flex 容器会把文件列表等页面布局压塌——改写为仅作用于
//    本组件容器 .xc-cad-viewer（display:flex 一并去掉，容器内部布局不需要它）。
//    :root 的 --ml-*/--el-* 变量定义与 html.dark 规则保留（只定义变量/不触发）。
const pkgCss = join(root, 'node_modules/@mlightcad/cad-viewer/dist/cad-viewer.css');
if (existsSync(pkgCss)) {
  const { readFileSync, writeFileSync } = await import('node:fs');
  let css = readFileSync(pkgCss, 'utf8');
  const orig = css;
  css = css.replace(/v-bind\([^)]*\)/g, '768px');
  css = css
    .split('body,html{height:100%}').join('.xc-cad-viewer{height:100%}')
    .split('html,body{overflow-x:hidden}').join('.xc-cad-viewer{overflow-x:hidden}')
    .split('body{margin:0;display:flex}').join('.xc-cad-viewer{margin:0}');
  css = css.replace(/(^|\})body\{font-family:([^}]*)\}/g, '$1.xc-cad-viewer{font-family:$2}');
  if (css !== orig) {
    writeFileSync(pkgCss, css);
    console.log('[copy-cad-workers] 已修补 cad-viewer.css（v-bind 占位 + 全局 body/html 规则作用域化）');
  }
  //兜底校验：补丁后不应再存在裸 body 全局规则（body[data-v-xxx] 等带作用域的不算）
  if (/(^|\})body\{/.test(css) || /(^|\})(html,)?body,html\{/.test(css)) {
    console.warn('[copy-cad-workers] ⚠ cad-viewer.css 仍含裸 body 全局规则，升级依赖后需核对该补丁');
  }
}

console.log(`[copy-cad-workers] 已拷贝 ${copied} 个文件`);
