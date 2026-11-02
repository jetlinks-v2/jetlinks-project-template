/**
 * 产品模板状态管理
 */
import { defineStore } from 'pinia'
import { queryNoPaging, getTemplateDetail } from '../api/template'
import type { TemplateItem } from '../views/device/Template/typings'

export const useTemplateStore = defineStore('template', {
  state: () => ({
    templateList: [] as TemplateItem[],
    current: {} as TemplateItem,
    tabActiveKey: 'Info'
  }),

  actions: {
    /**
     * 加载所有模板（不分页）
     */
    async loadAllTemplates() {
      try {
        const response = await queryNoPaging({
          sorts: [{ name: 'createTime', order: 'desc' }]
        })
        this.templateList = response.data || []
        return this.templateList
      } catch (error) {
        console.error('加载模板列表失败', error)
        return []
      }
    },

    /**
     * 刷新当前模板详情
     */
    async refresh(id: string) {
      try {
        const response = await getTemplateDetail(id)
        if (response.status === 200) {
          this.current = response.result
        }
      } catch (error) {
        console.error('加载模板详情失败', error)
      }
    },

    /**
     * 重置状态
     */
    reSet() {
      this.current = {} as TemplateItem
      this.tabActiveKey = 'Info'
    }
  },

  getters: {
    /**
     * 获取启用的模板列表
     */
    enabledTemplates: (state) => {
      return state.templateList.filter(t => t.state.value === 'enabled')
    }
  }
})
