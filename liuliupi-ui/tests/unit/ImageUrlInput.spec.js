import { shallowMount } from '@vue/test-utils'
import ImageUrlInput from '@/components/admin/common/ImageUrlInput.vue'

describe('ImageUrlInput.vue', () => {
  it('renders input, thumb and upload button', () => {
    const wrapper = shallowMount(ImageUrlInput, {
      propsData: { value: 'https://example.com/bg.jpg' },
      stubs: ['el-input', 'el-image', 'el-button', 'el-dialog', 'upload-picture']
    })
    expect(wrapper.find('.image-url-input').exists()).toBe(true)
    expect(wrapper.find('el-input-stub').exists()).toBe(true)
    expect(wrapper.find('el-image-stub').exists()).toBe(true)
    expect(wrapper.find('el-button-stub').exists()).toBe(true)
  })

  it('emits input event when url changes', async () => {
    const wrapper = shallowMount(ImageUrlInput, {
      propsData: { value: '' },
      stubs: ['el-input', 'el-image', 'el-button', 'el-dialog', 'upload-picture']
    })
    wrapper.find('el-input-stub').vm.$emit('input', 'https://example.com/new.jpg')
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted().input).toBeTruthy()
    expect(wrapper.emitted().input[0]).toEqual(['https://example.com/new.jpg'])
  })

  it('emits input event when upload succeeds', async () => {
    const wrapper = shallowMount(ImageUrlInput, {
      propsData: { value: '' },
      stubs: ['el-input', 'el-image', 'el-button', 'el-dialog', 'upload-picture']
    })
    wrapper.find('upload-picture-stub').vm.$emit('addPicture', 'https://example.com/uploaded.jpg')
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted().input).toBeTruthy()
    expect(wrapper.emitted().input[0]).toEqual(['https://example.com/uploaded.jpg'])
  })
})
