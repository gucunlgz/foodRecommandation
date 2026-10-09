import assert from 'node:assert/strict'
import { test } from 'node:test'
import { getItem, getItems, getRecommendations } from '../src/api.ts'

function reply(t, data) {
  t.mock.method(globalThis, 'fetch', async () => new Response(JSON.stringify({ success: true, data }), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  }))
}

test('detail normalizes missing display fields without throwing', async (t) => {
  reply(t, { id: 1, name: '缺字段测试', location: '测试地点' })
  const item = await getItem('1')
  assert.equal(item.name, '缺字段测试')
  assert.equal(item.businessHours, null)
  assert.equal(item.rating, null)
  assert.deepEqual(item.tasteTags, [])
  assert.deepEqual(item.mealPeriods, [])
  assert.deepEqual(item.dietaryTags, [])
  assert.deepEqual(item.allergens, [])
  assert.equal(item.dataUpdatedAt, '未知')
})

test('list cards normalize null and malformed tag arrays', async (t) => {
  reply(t, { content: [{ id: 2, tasteTags: null }, { id: 3, tasteTags: ['清淡', null, 7] }], total: 2, page: 0, size: 9, totalPages: 1 })
  const list = await getItems(new URLSearchParams())
  assert.equal(list.content[0].name, '未命名餐饮')
  assert.equal(list.content[0].location, '位置未提供')
  assert.deepEqual(list.content[0].tasteTags, [])
  assert.deepEqual(list.content[1].tasteTags, ['清淡'])
})

test('recommendation cards normalize missing tags and reasons', async (t) => {
  reply(t, { items: [{ item: { id: 4, name: '推荐测试' }, score: 10 }], total: 1, algorithmVersion: 'RULE_V1' })
  const result = await getRecommendations({ budgetMax: 20 })
  assert.deepEqual(result.items[0].reasons, [])
  assert.deepEqual(result.items[0].item.tasteTags, [])
  assert.deepEqual(result.suggestions, [])
})

test('missing item id gives a readable error instead of a broken link', async (t) => {
  reply(t, { name: '无编号' })
  await assert.rejects(getItem('1'), /缺少编号/)
})

test('missing list content gives a readable error', async (t) => {
  reply(t, { total: 0 })
  await assert.rejects(getItems(new URLSearchParams()), /列表数据不完整/)
})
