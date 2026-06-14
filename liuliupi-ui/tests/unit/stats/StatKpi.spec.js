import { shallowMount } from '@vue/test-utils'
import StatKpi from '@/components/admin/stats/StatKpi.vue'

describe('StatKpi.vue', () => {
  it('renders label and value', () => {
    const wrapper = shallowMount(StatKpi, {
      propsData: {
        label: '总访问量',
        value: 18883
      }
    })
    expect(wrapper.text()).toContain('总访问量')
    expect(wrapper.text()).toContain('18883')
  })

  it('renders suffix when provided', () => {
    const wrapper = shallowMount(StatKpi, {
      propsData: {
        label: '今日访问量',
        value: 6,
        suffix: '次'
      }
    })
    expect(wrapper.text()).toContain('次')
  })
})
