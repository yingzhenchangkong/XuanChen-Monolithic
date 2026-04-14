import { createPinia } from 'pinia';

const pinia = createPinia();

export default pinia;
export * from './modules/auth';
export * from './modules/menu';
export * from './modules/collapsed';
export * from './modules/tabs';