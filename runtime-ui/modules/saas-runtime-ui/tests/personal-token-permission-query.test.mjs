import assert from 'node:assert/strict'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'
import { build } from 'esbuild'
import axios from 'axios'

const coreRoot = new URL('../../../jetlinks-web-core/src/', import.meta.url)
const bundle = await build({
  entryPoints: [fileURLToPath(new URL('api/system/permission.ts', coreRoot))],
  bundle: true,
  format: 'esm',
  platform: 'node',
  write: false,
  plugins: [{
    name: 'permission-query-boundary',
    setup(builder) {
      builder.onResolve({ filter: /^@jetlinks-web-core\/utils\/encodeQuery$/ }, () => ({
        path: fileURLToPath(new URL('utils/encodeQuery.ts', coreRoot)),
      }))
      builder.onResolve({ filter: /^@jetlinks-web\/core$/ }, () => ({ path: 'request', namespace: 'fixture' }))
      builder.onLoad({ filter: /.*/, namespace: 'fixture' }, () => ({
        contents: 'export const request = { get: (...args) => globalThis.permissionQueryFixture.get(...args) };',
      }))
    },
  }],
})
const { queryPermission_api } = await import(
  `data:text/javascript;base64,${Buffer.from(bundle.outputFiles[0].text).toString('base64')}`
)

async function query(params) {
  let captured
  globalThis.permissionQueryFixture = {
    get: async (url, data) => {
      captured = { url, params: data }
      return { success: true, result: [] }
    },
  }
  await queryPermission_api(params)
  return { ...captured, search: new URL(axios.getUri(captured), 'http://permission-fixture.test').searchParams }
}

test('设备关键字通过实际 Axios 编码为 Spring 可绑定的字段路径', async () => {
  const input = { paging: false, terms: [{ column: 'name$like', value: '%设备%' }] }
  const before = structuredClone(input)
  const result = await query(input)
  assert.equal(result.url, '/permission/_query/for-grant')
  assert.equal(result.search.get('paging'), 'false')
  assert.equal(result.search.get('terms[0].column'), 'name$like')
  assert.equal(result.search.get('terms[0].value'), '%设备%')
  assert.equal(result.search.has('terms[0][column]'), false)
  assert.equal(result.params.terms, undefined)
  assert.deepEqual(input, before)
})

test('清空搜索只保留非分页参数，不残留上一条查询条件', async () => {
  await query({ paging: false, terms: [{ column: 'name$like', value: '%设备%' }] })
  const result = await query({ paging: false })
  assert.deepEqual([...result.search.entries()], [['paging', 'false']])
})

test('嵌套条件和排序同样保留对象属性路径，其他根参数保持原值', async () => {
  const input = {
    paging: false,
    pageSize: 100,
    current: 2,
    terms: [{ type: 'or', terms: [{ column: 'name', termType: 'like', value: '%device%' }] }],
    sorts: [{ name: 'id', order: 'asc' }],
  }
  const before = structuredClone(input)
  const result = await query(input)
  assert.equal(result.search.get('terms[0].type'), 'or')
  assert.equal(result.search.get('terms[0].terms[0].column'), 'name')
  assert.equal(result.search.get('terms[0].terms[0].termType'), 'like')
  assert.equal(result.search.get('terms[0].terms[0].value'), '%device%')
  assert.equal(result.search.get('sorts[0].name'), 'id')
  assert.equal(result.search.get('sorts[0].order'), 'asc')
  assert.equal(result.search.get('pageSize'), '100')
  assert.equal(result.search.get('current'), '2')
  assert.equal(result.params.sorts, undefined)
  assert.deepEqual(input, before)
})
