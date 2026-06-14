import { shallowMount } from '@vue/test-utils'
import WebEdit from '@/components/admin/webEdit.vue'

describe('webEdit.vue', () => {
  let getWebInfoSpy

  beforeEach(() => {
    getWebInfoSpy = jest.spyOn(WebEdit.methods, 'getWebInfo').mockImplementation(() => {})
  })

  afterEach(() => {
    getWebInfoSpy.mockRestore()
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
        'uploadPicture',
        'mavon-editor'
      ]
    })

    const editor = wrapper.find('mavon-editor-stub')
    expect(editor.exists()).toBe(true)
    expect(editor.attributes('value')).toBe('# 公告\n\n欢迎使用')
    expect(editor.attributes('imgadd')).toBeUndefined()
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
        'uploadPicture',
        'mavon-editor'
      ]
    })

    const inputs = wrapper.findAll('el-input-stub')
    expect(inputs.filter(i => i.attributes('value') === '推送标题').length).toBeGreaterThan(0)
    expect(inputs.filter(i => i.attributes('value') === 'https://example.com/cover.jpg').length).toBeGreaterThan(0)
    expect(inputs.filter(i => i.attributes('value') === 'https://example.com/').length).toBeGreaterThan(0)
  })

  it('saveNotice calls both updateWebInfo and savePushNotification', async () => {
    const postMock = jest.fn().mockResolvedValue({})
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
        'uploadPicture',
        'mavon-editor'
      ]
    })

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
  })
})
