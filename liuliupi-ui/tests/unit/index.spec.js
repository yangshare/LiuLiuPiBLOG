import { mount } from '@vue/test-utils'
import Index from '@/components/index.vue'

const createWrapper = (options = {}) => {
  const baseMocks = {
    $store: {
      state: {
        webInfo: {
          notices: '# 公告\n\n- 第一项\n- 第二项',
          backgroundImage: 'https://example.com/bg.jpg',
          randomCover: ['https://example.com/cover1.jpg']
        },
        sortInfo: []
      }
    },
    $common: {
      isEmpty: (v) => v === undefined || v === null || v === '' || (Array.isArray(v) && v.length === 0) || (typeof v === 'object' && Object.keys(v).length === 0),
      mobile: () => false
    },
    $http: {
      get: jest.fn().mockResolvedValue({ data: null }),
      post: jest.fn().mockResolvedValue({ data: null })
    },
    $constant: { baseURL: 'http://localhost:8080', jinrishici: '' },
    $message: jest.fn()
  }

  const mocks = options.mocks
    ? { ...baseMocks, ...options.mocks, $http: { ...baseMocks.$http, ...options.mocks.$http }, $common: { ...baseMocks.$common, ...options.mocks.$common }, $store: { ...baseMocks.$store, ...options.mocks.$store } }
    : baseMocks

  const getGuShiSpy = jest.spyOn(Index.methods, 'getGuShi').mockImplementation(() => {})
  const getSortArticlesSpy = jest.spyOn(Index.methods, 'getSortArticles').mockImplementation(() => {})

  const wrapper = mount(Index, {
    shallow: true,
    mocks,
    stubs: {
      loader: true,
      zombie: true,
      printer: true,
      'article-list': true,
      'sort-article': true,
      'el-image': true,
      'el-dialog': true,
      myAside: true,
      myFooter: true
    },
    data: options.data
  })

  getGuShiSpy.mockRestore()
  getSortArticlesSpy.mockRestore()

  return wrapper
}

describe('index.vue', () => {
  it('renders notice board with markdown content', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.announcement-board').exists()).toBe(true)
    expect(wrapper.find('.announcement-body').exists()).toBe(true)
  })

  it('renders raw text when markdown render throws', () => {
    const renderNoticeSpy = jest.spyOn(Index.methods, 'renderNotice').mockImplementation(() => {})
    const wrapper = createWrapper({
      data() {
        return {
          noticeHtml: '原始公告文本'
        }
      }
    })
    expect(wrapper.find('.announcement-body').html()).toContain('原始公告文本')
    renderNoticeSpy.mockRestore()
  })

  it('loads push notification from new endpoint', async () => {
    const getGuShiSpy = jest.spyOn(Index.methods, 'getGuShi').mockImplementation(() => {})
    const getMock = jest.fn().mockResolvedValue({
      data: { title: '推送', cover: 'https://example.com/cover.jpg', url: 'https://example.com/', enabled: true }
    })
    const wrapper = createWrapper({
      mocks: {
        $http: {
          get: getMock,
          post: jest.fn().mockResolvedValue({ data: null })
        }
      }
    })

    await wrapper.vm.$nextTick()
    await new Promise(resolve => setTimeout(resolve, 2100))

    expect(getMock).toHaveBeenCalledWith('http://localhost:8080/pushNotification/getPushNotification')
    expect(wrapper.vm.push['标题']).toBe('推送')
    getGuShiSpy.mockRestore()
  })
})
