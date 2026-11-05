import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

test('SaaS 不再隐藏统一设备列表的边缘节点接入入口', async () => {
  const source = await readFile(new URL('../index.ts', import.meta.url), 'utf8')
  const hiddenGatewayEntry = /target:\s*['"]create-gateway['"]|code:\s*['"]hide-create-gateway['"]/

  assert.match(source, /const getRegisterComponents = \(\) => \[/)
  assert.doesNotMatch(source, hiddenGatewayEntry)
})
