import { shallowMount } from '@vue/test-utils'
import AdminMain from '@/components/admin/main.vue'

describe('admin/main.vue', () => {
  it('renders overview, today and yesterday cards', () => {
    const getHistoryInfoSpy = jest.spyOn(AdminMain.methods, 'getHistoryInfo').mockImplementation(() => {})
    const wrapper = shallowMount(AdminMain, {
      data() {
        return {
          historyInfo: {
            ip_history_count: 18883,
            ip_history_province: [{ province: '山西省', num: 294 }],
            ip_history_ip: [{ ip: '116.179.37.13', num: 77 }],
            ip_count_today: 6,
            username_today: [{ username: 'alice', avatar: 'a.jpg' }],
            ip_count_yest: 4,
            username_yest: [{ username: 'bob', avatar: 'b.jpg' }]
          }
        }
      },
      stubs: [
        'el-tag',
        'el-row',
        'el-col',
        'OverviewCard',
        'TodayCard',
        'YesterdayCard'
      ]
    })
    expect(wrapper.find('overviewcard-stub').exists()).toBe(true)
    expect(wrapper.find('todaycard-stub').exists()).toBe(true)
    expect(wrapper.find('yesterdaycard-stub').exists()).toBe(true)
    getHistoryInfoSpy.mockRestore()
  })

  it('fetches history info on created', () => {
    const getHistoryInfoSpy = jest.spyOn(AdminMain.methods, 'getHistoryInfo').mockImplementation(() => {})
    shallowMount(AdminMain, {
      stubs: ['el-tag', 'el-row', 'el-col', 'OverviewCard', 'TodayCard', 'YesterdayCard']
    })
    expect(getHistoryInfoSpy).toHaveBeenCalled()
    getHistoryInfoSpy.mockRestore()
  })
})
