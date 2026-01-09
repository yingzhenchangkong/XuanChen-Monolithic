import { defineStore } from "pinia";
import { ref, computed } from "vue";

export const useCollapsedStore = defineStore("collapsed", () => {
  const isCollapsed = ref(false);
  const getCollapsed = computed(() => {
    return isCollapsed.value
  });
  const setCollapsed = () => {
    isCollapsed.value = !isCollapsed.value;
  };
  return { getCollapsed, setCollapsed };
})