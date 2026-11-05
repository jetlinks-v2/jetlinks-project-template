import type { ProjectResourceUsage, ProjectUsageMetric } from './types'

export type UsageGroupKey = 'basic' | 'ai' | 'agent' | 'other'

export interface UsageDisplayItem {
  id: string
  name: string
  titleKey?: string
  description: string
  descriptionKey?: string
  icon: string
  metric?: ProjectUsageMetric
}

export interface UsageGroup {
  key: UsageGroupKey
  items: UsageDisplayItem[]
}

const groupOrder: UsageGroupKey[] = ['basic', 'ai', 'agent', 'other']

const knownResources: Record<string, string> = {
  '业务应用': 'businessApplication', '产品': 'product', '产品分类': 'productCategory',
  '国标级联': 'gb28181', '数据源配置': 'dataSource', '用户': 'user',
  '空间': 'space', '空间类型': 'spaceType', '组织': 'organization',
  '角色': 'role', '设备': 'device', '设备消息数量': 'deviceMessage',
  '设备消息上报频率': 'deviceRate', '设备属性数量': 'deviceProperty',
  'AI任务': 'aiTask', 'AI场景': 'aiScene', 'AI工作计划': 'aiSchedule',
  'AI工具包': 'aiTool', 'AI技能包': 'aiSkill', 'AI模型': 'aiModel',
  'AI模型供应商': 'aiProvider', 'AI聚合任务': 'aiAggregate', 'AI驱动': 'aiDriver',
  'AI智能体会话': 'agentSession', 'AI智能体': 'agent', 'AI智能体分类': 'agentCategory',
  'LLM 成本总量': 'llmCost', 'LLM 成本周期预算': 'llmBudget',
  'LLM 调用并发数': 'llmConcurrency', 'AI 会话文件容量': 'sessionStorage',
  'AI 会话数量限制': 'sessionCount',
}

const metricNames: Record<string, string> = {
  costQuota: 'llmCost', costQuotaTime: 'llmBudget', callConcurrency: 'llmConcurrency',
  fileBytesQuota: 'sessionStorage', sessionQuota: 'sessionCount',
}

const icons: Record<string, string> = {
  businessApplication: 'MobileOutlined', product: 'ShoppingOutlined',
  productCategory: 'TagsOutlined', gb28181: 'ApartmentOutlined',
  dataSource: 'ApiOutlined', user: 'UserOutlined', space: 'HomeOutlined',
  spaceType: 'FolderOpenOutlined', organization: 'TeamOutlined',
  role: 'SafetyCertificateOutlined', device: 'DatabaseOutlined',
  deviceMessage: 'MailOutlined', deviceRate: 'LineChartOutlined',
  deviceProperty: 'ProfileOutlined', aiTask: 'ThunderboltOutlined',
  aiScene: 'AreaChartOutlined', aiSchedule: 'ScheduleOutlined',
  aiTool: 'ToolOutlined', aiSkill: 'GiftOutlined', aiModel: 'DatabaseOutlined',
  aiProvider: 'CloudServerOutlined', aiAggregate: 'ClusterOutlined',
  aiDriver: 'ControlOutlined', agentSession: 'MessageOutlined',
  agent: 'RobotOutlined', agentCategory: 'AppstoreOutlined',
  llmCost: 'PayCircleOutlined', llmBudget: 'WalletOutlined',
  llmConcurrency: 'BranchesOutlined', sessionStorage: 'FolderOutlined',
  sessionCount: 'CommentOutlined',
}

const classify = (resource: ProjectResourceUsage, metric?: ProjectUsageMetric): UsageGroupKey => {
  const name = `${resource.name} ${metric?.name || ''}`.replace(/\s+/g, '').toLowerCase()
  const id = resource.id.toLowerCase()
  if (/智能体|llm|会话|agent.*session/.test(name) || metricNames[metric?.code || '']) return 'agent'
  if (/(^|\s)ai|人工智能/i.test(resource.name) || /^ai[_-]/.test(id)) return 'ai'
  if (/agent|llm/.test(id)) return 'agent'
  if (knownResources[resource.name] || /^(device|product|user|role|space|organization)/.test(id)) return 'basic'
  return 'other'
}

/** 运行时资源可能同时承载实体数量和 LLM 指标，必须按指标归类而非整张资源卡归类。 */
export const groupResourceUsage = (resources: ProjectResourceUsage[]): UsageGroup[] => {
  const groups = new Map<UsageGroupKey, UsageDisplayItem[]>(groupOrder.map(key => [key, []]))
  resources.forEach(resource => {
    const metrics = resource.metrics.length ? resource.metrics : [undefined]
    metrics.forEach(metric => {
      const combinedName = `${resource.name}${metric?.name || ''}`
      const displayCode = metricNames[metric?.code || '']
        || knownResources[metric?.name || '']
        || knownResources[combinedName]
        || knownResources[resource.name]
      const genericCount = metric && /^(实体数量|数量|entityCount)$/i.test(metric.name)
      let name = resource.name
      if (metric && resource.metrics.length > 1 && !genericCount) {
        name = knownResources[combinedName] ? combinedName : metric.name
      }
      const key = classify(resource, metric)
      groups.get(key)?.push({
        id: metric?.id || resource.id,
        name,
        titleKey: metricNames[metric?.code || '']
          ? `ProjectUsage.item.${displayCode}` : undefined,
        description: metric?.description || resource.description,
        descriptionKey: displayCode ? `ProjectUsage.description.${displayCode}` : undefined,
        icon: icons[displayCode] || 'DatabaseOutlined',
        metric,
      })
    })
  })
  return groupOrder
    .filter(key => key !== 'other' || Boolean(groups.get(key)?.length))
    .map(key => ({ key, items: groups.get(key) || [] }))
}
