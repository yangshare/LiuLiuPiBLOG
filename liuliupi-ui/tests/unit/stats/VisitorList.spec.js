import { shallowMount } from '@vue/test-utils'
import VisitorList from '@/components/admin/stats/VisitorList.vue'

describe('VisitorList.vue', () => {
  it('renders user list', () => {
    const users = [
      { username: 'alice', avatar: 'https://example.com/a.jpg' },
      { username: 'bob', avatar: 'https://example.com/b.jpg' }
    ]
    const wrapper = shallowMount(VisitorList, {
      propsData: { users },
      stubs: ['el-table', 'el-table-column', 'el-avatar', 'el-empty']
    })
    expect(wrapper.findAll('el-table-column-stub')).toHaveLength(2)
  })

  it('shows empty state when no users', () => {
    const wrapper = shallowMount(VisitorList, {
      propsData: { users: [] },
      stubs: ['el-table', 'el-table-column', 'el-avatar', 'el-empty']
    })
    expect(wrapper.find('el-empty-stub').exists()).toBe(true)
  })
})
