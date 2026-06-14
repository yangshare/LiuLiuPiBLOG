import { shallowMount } from '@vue/test-utils'
import TodayCard from '@/components/admin/stats/TodayCard.vue'
import YesterdayCard from '@/components/admin/stats/YesterdayCard.vue'

describe('DailyCard.vue', () => {
  it('TodayCard renders count and visitor list', () => {
    const wrapper = shallowMount(TodayCard, {
      propsData: {
        count: 6,
        users: [{ username: 'alice', avatar: 'a.jpg' }]
      },
      stubs: ['el-card', 'StatKpi', 'VisitorList']
    })
    expect(wrapper.find('statkpi-stub').props('value')).toBe(6)
    expect(wrapper.find('visitorlist-stub').exists()).toBe(true)
  })

  it('YesterdayCard renders count and visitor list', () => {
    const wrapper = shallowMount(YesterdayCard, {
      propsData: {
        count: 4,
        users: [{ username: 'bob', avatar: 'b.jpg' }]
      },
      stubs: ['el-card', 'StatKpi', 'VisitorList']
    })
    expect(wrapper.find('statkpi-stub').props('value')).toBe(4)
    expect(wrapper.text()).toContain('昨日访问')
  })
})
