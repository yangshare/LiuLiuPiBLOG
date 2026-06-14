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
})
