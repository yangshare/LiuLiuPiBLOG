import { shallowMount } from '@vue/test-utils'
import ProvinceMap from '@/components/admin/stats/ProvinceMap.vue'

jest.mock('echarts', () => ({
  getMap: jest.fn(() => null),
  registerMap: jest.fn(),
  init: jest.fn(() => ({
    setOption: jest.fn(),
    dispose: jest.fn()
  }))
}))

describe('ProvinceMap.vue', () => {
  const mockGeoJson = {
    type: 'FeatureCollection',
    features: []
  }

  beforeEach(() => {
    global.fetch = jest.fn(() => Promise.resolve({
      ok: true,
      json: () => Promise.resolve(mockGeoJson)
    }))
  })

  afterEach(() => {
    global.fetch.mockRestore()
    delete global.fetch
  })

  it('renders map container', async () => {
    const wrapper = shallowMount(ProvinceMap, {
      propsData: {
        provinceData: [
          { province: '山西省', num: 294 },
          { province: '浙江省', num: 157 }
        ]
      },
      stubs: ['el-table', 'el-table-column']
    })
    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.province-map').exists()).toBe(true)
    expect(wrapper.find('.province-map__fallback').exists()).toBe(false)
  })

  it('shows fallback table when load fails', async () => {
    global.fetch = jest.fn(() => Promise.reject(new Error('network error')))
    const wrapper = shallowMount(ProvinceMap, {
      propsData: {
        provinceData: [{ province: '山西省', num: 294 }]
      },
      stubs: ['el-table', 'el-table-column']
    })
    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 0))
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.province-map__fallback').exists()).toBe(true)
    expect(wrapper.text()).toContain('地图加载失败')
  })
})
