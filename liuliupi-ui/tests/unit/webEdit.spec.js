import { shallowMount, config } from '@vue/test-utils'
import WebEdit from '@/components/admin/webEdit.vue'

// mavon-editor 为全局注册的第三方组件，统一 stub 避免每个用例重复声明
config.stubs['mavon-editor'] = true
config.mocks.$common = {
  isEmpty: (value) => value === undefined || value === null || value === '',
  imageSrc: (value) => value || ''
}

describe('webEdit.vue', () => {
  let getWebInfoSpy

  beforeEach(() => {
    getWebInfoSpy = jest.spyOn(WebEdit.methods, 'getWebInfo').mockImplementation(() => {})
  })

  afterEach(() => {
    getWebInfoSpy.mockRestore()
  })

  it('defaults push notification to disabled before admin config is loaded', () => {
    const wrapper = shallowMount(WebEdit, {
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })

    expect(wrapper.vm.pushNotification.enabled).toBe(false)
  })

  it('renders random image resources as grids', () => {
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          randomAvatar: ['https://example.com/avatar.jpg'],
          randomCover: ['https://example.com/cover.jpg']
        }
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })

    const grids = wrapper.findAll('.random-image-grid')
    expect(grids).toHaveLength(2)
    expect(wrapper.findAll('.random-image-item')).toHaveLength(2)
  })

  it('opens add url dialog with correct type', () => {
    const wrapper = shallowMount(WebEdit, {
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    wrapper.vm.showAddUrlDialog('avatar')
    expect(wrapper.vm.addUrlType).toBe('avatar')
    expect(wrapper.vm.addUrlDialogVisible).toBe(true)

    wrapper.vm.showAddUrlDialog('cover')
    expect(wrapper.vm.addUrlType).toBe('cover')
  })

  it('renders add image link buttons in random resource cards', () => {
    const wrapper = shallowMount(WebEdit, {
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    const buttons = wrapper.findAll('el-button-stub')
    const addLinkButtons = buttons.filter(b => b.text() === '添加图片链接')
    expect(addLinkButtons).toHaveLength(2)
  })

  it('opens add url dialog when add image link button clicked', async () => {
    const wrapper = shallowMount(WebEdit, {
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    const buttons = wrapper.findAll('el-button-stub')
    const addLinkButton = buttons.filter(b => b.text() === '添加图片链接').at(0)
    addLinkButton.vm.$emit('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.addUrlDialogVisible).toBe(true)
    expect(wrapper.vm.addUrlType).toBe('avatar')
  })

  it('adds valid avatar url to randomAvatar on confirm', async () => {
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          randomAvatar: [],
          randomCover: [],
          addUrlType: 'avatar',
          addUrlValue: 'https://example.com/avatar.jpg',
          addUrlDialogVisible: true
        }
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    wrapper.vm.confirmAddUrl()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.randomAvatar).toEqual(['https://example.com/avatar.jpg'])
    expect(wrapper.vm.addUrlDialogVisible).toBe(false)
    expect(wrapper.vm.addUrlValue).toBe('')
    expect(wrapper.vm.addUrlError).toBe('')
  })

  it('adds valid cover url to randomCover on confirm', async () => {
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          randomAvatar: [],
          randomCover: [],
          addUrlType: 'cover',
          addUrlValue: 'https://example.com/cover.png',
          addUrlDialogVisible: true
        }
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    wrapper.vm.confirmAddUrl()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.randomCover).toEqual(['https://example.com/cover.png'])
  })

  it('adds valid image url with query string or hash on confirm', async () => {
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          randomAvatar: [],
          randomCover: [],
          addUrlType: 'avatar',
          addUrlValue: 'https://cdn.example.com/avatar.jpg?token=abc#preview',
          addUrlDialogVisible: true
        }
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    wrapper.vm.confirmAddUrl()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.randomAvatar).toEqual(['https://cdn.example.com/avatar.jpg?token=abc#preview'])
    expect(wrapper.vm.addUrlDialogVisible).toBe(false)
    expect(wrapper.vm.addUrlError).toBe('')
  })

  it.each([
    [''],
    ['not-a-url'],
    ['ftp://example.com/avatar.jpg'],
    ['https://example.com/avatar.txt']
  ])('does not add invalid url "%s" and keeps dialog open', async (url) => {
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          randomAvatar: [],
          randomCover: [],
          addUrlType: 'avatar',
          addUrlValue: url,
          addUrlDialogVisible: true
        }
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    wrapper.vm.confirmAddUrl()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.randomAvatar).toEqual([])
    expect(wrapper.vm.addUrlDialogVisible).toBe(true)
    expect(wrapper.vm.addUrlError).toBe('请输入有效的图片链接')
  })

  it('keeps input value when dialog is cancelled', async () => {
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          addUrlValue: 'https://example.com/keep.jpg',
          addUrlDialogVisible: true
        }
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    wrapper.vm.addUrlDialogVisible = false
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.addUrlValue).toBe('https://example.com/keep.jpg')
  })

  it('renders upload section divider with title', () => {
    const wrapper = shallowMount(WebEdit, {
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    const dividers = wrapper.findAll('.upload-divider')
    expect(dividers).toHaveLength(2)
    expect(dividers.at(0).text()).toContain('或上传本地图片')
  })

  it('shows empty tip when random image list is empty', () => {
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          randomAvatar: [],
          randomCover: []
        }
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    const tips = wrapper.findAll('.empty-tip')
    expect(tips).toHaveLength(2)
    expect(tips.at(0).text()).toBe('暂无图片，点击下方按钮添加。')
  })

  it('renders mavon-editor in notice tab without imgAdd binding', () => {
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          webInfo: { notices: '# 公告\n\n欢迎使用' }
        }
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })

    const editor = wrapper.find('mavon-editor-stub')
    expect(editor.exists()).toBe(true)
    expect(editor.attributes('value')).toBe('# 公告\n\n欢迎使用')
  })

  it('renders push notification form in notice tab', () => {
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          pushNotification: {
            title: '推送标题',
            cover: 'https://example.com/cover.jpg',
            url: 'https://example.com/',
            enabled: true
          }
        }
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })

    const inputs = wrapper.findAll('el-input-stub')
    expect(inputs.filter(i => i.attributes('value') === '推送标题').length).toBeGreaterThan(0)
    expect(inputs.filter(i => i.attributes('value') === 'https://example.com/cover.jpg').length).toBeGreaterThan(0)
    expect(inputs.filter(i => i.attributes('value') === 'https://example.com/').length).toBeGreaterThan(0)
  })

  it('saveNotice calls both updateWebInfo and savePushNotification', async () => {
    const postMock = jest.fn().mockResolvedValue({})
    const messageMock = jest.fn()
    const getWebInfoMock = jest.fn()
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          webInfo: { id: 1, notices: '# 公告' },
          pushNotification: { title: '推送', cover: '', url: '', enabled: true }
        }
      },
      mocks: {
        $http: { post: postMock, get: jest.fn() },
        $constant: { baseURL: 'http://localhost:8080' },
        $message: messageMock
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    wrapper.vm.getWebInfo = getWebInfoMock

    await wrapper.vm.saveNotice()

    expect(postMock).toHaveBeenCalledWith(
      'http://localhost:8080/webInfo/updateWebInfo',
      { id: 1, notices: '# 公告' },
      true
    )
    expect(postMock).toHaveBeenCalledWith(
      'http://localhost:8080/pushNotification/admin/savePushNotification',
      { title: '推送', cover: '', url: '', enabled: true },
      true
    )
    expect(messageMock).toHaveBeenCalledWith({
      message: '保存成功！',
      type: 'success'
    })
    expect(getWebInfoMock).toHaveBeenCalled()
  })

  it('saveNotice stops and shows error when updateWebInfo fails', async () => {
    const postMock = jest.fn()
      .mockRejectedValueOnce(new Error('网络错误'))
    const messageMock = jest.fn()
    const getWebInfoMock = jest.fn()
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          webInfo: { id: 1, notices: '# 公告' },
          pushNotification: { title: '推送', cover: '', url: '', enabled: true }
        }
      },
      mocks: {
        $http: { post: postMock, get: jest.fn() },
        $constant: { baseURL: 'http://localhost:8080' },
        $message: messageMock
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    wrapper.vm.getWebInfo = getWebInfoMock

    await wrapper.vm.saveNotice()

    expect(postMock).toHaveBeenCalledTimes(1)
    expect(postMock).toHaveBeenCalledWith(
      'http://localhost:8080/webInfo/updateWebInfo',
      { id: 1, notices: '# 公告' },
      true
    )
    expect(messageMock).toHaveBeenCalledWith({
      message: '公告保存失败：网络错误',
      type: 'error'
    })
    expect(getWebInfoMock).not.toHaveBeenCalled()
  })

  it('saveNotice shows error when savePushNotification fails', async () => {
    const postMock = jest.fn()
      .mockResolvedValueOnce({})
      .mockRejectedValueOnce(new Error('保存失败'))
    const messageMock = jest.fn()
    const getWebInfoMock = jest.fn()
    const wrapper = shallowMount(WebEdit, {
      data() {
        return {
          webInfo: { id: 1, notices: '# 公告' },
          pushNotification: { title: '推送', cover: '', url: '', enabled: true }
        }
      },
      mocks: {
        $http: { post: postMock, get: jest.fn() },
        $constant: { baseURL: 'http://localhost:8080' },
        $message: messageMock
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })
    wrapper.vm.getWebInfo = getWebInfoMock

    await wrapper.vm.saveNotice()

    expect(postMock).toHaveBeenCalledTimes(2)
    expect(postMock).toHaveBeenCalledWith(
      'http://localhost:8080/webInfo/updateWebInfo',
      { id: 1, notices: '# 公告' },
      true
    )
    expect(postMock).toHaveBeenCalledWith(
      'http://localhost:8080/pushNotification/admin/savePushNotification',
      { title: '推送', cover: '', url: '', enabled: true },
      true
    )
    expect(messageMock).toHaveBeenCalledWith({
      message: '推送设置保存失败：保存失败',
      type: 'error'
    })
    expect(getWebInfoMock).not.toHaveBeenCalled()
  })

  it('getPushNotification loads admin push config from admin route', async () => {
    const getMock = jest.fn().mockResolvedValue({
      data: {
        title: '推送',
        cover: 'https://example.com/cover.jpg',
        url: 'https://example.com/',
        enabled: false
      }
    })
    const wrapper = shallowMount(WebEdit, {
      mocks: {
        $http: { get: getMock },
        $constant: { baseURL: 'http://localhost:8080' },
        $common: {
          isEmpty: (value) => value === undefined || value === null || value === '',
          imageSrc: (value) => value || ''
        },
        $message: jest.fn()
      },
      stubs: [
        'el-tabs',
        'el-tab-pane',
        'el-form',
        'el-form-item',
        'el-input',
        'el-switch',
        'el-button',
        'el-card',
        'el-tag',
        'el-image',
        'el-dialog',
        'ImageUrlInput',
        'uploadPicture'
      ]
    })

    await wrapper.vm.getPushNotification()
    await wrapper.vm.$nextTick()

    expect(getMock).toHaveBeenCalledWith(
      'http://localhost:8080/pushNotification/admin/getPushNotification',
      {},
      true
    )
    expect(wrapper.vm.pushNotification.enabled).toBe(false)
  })
})
