import { shallowMount } from '@vue/test-utils'
import OverviewCard from '@/components/admin/stats/OverviewCard.vue'

describe('OverviewCard.vue', () => {
  it('renders total count, map and ip table', () => {
    const wrapper = shallowMount(OverviewCard, {
      propsData: {
        totalCount: 18883,
        provinceData: [{ province: '山西省', num: 294 }],
        ipData: [{ ip: '116.179.37.13', num: 77 }]
      },
      stubs: ['el-card', 'el-table', 'el-table-column', 'el-row', 'el-col', 'StatKpi', 'ProvinceMap']
    })
    expect(wrapper.find('statkpi-stub').exists()).toBe(true)
    expect(wrapper.find('provincemap-stub').exists()).toBe(true)
    expect(wrapper.findAll('el-table-column-stub')).toHaveLength(3)
  })
})
