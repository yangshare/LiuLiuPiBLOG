import common from '@/utils/common'
import store from '@/store'
import MarkdownIt from 'markdown-it'

describe('$common.imageSrc', () => {
  beforeEach(() => {
    localStorage.clear()
    store.state.sysConfig = {
      'qiniu.downloadUrl': 'https://cdn.example.com/',
      'local.downloadUrl': 'https://local.example.com/files/'
    }
    store.state.webInfo = { defaultStoreType: 'qiniu' }
  })

  it('keeps absolute and non-http resource URLs unchanged', () => {
    expect(common.imageSrc('https://external.example/a.png')).toBe('https://external.example/a.png')
    expect(common.imageSrc('//external.example/a.png')).toBe('//external.example/a.png')
    expect(common.imageSrc('data:image/png;base64,abc')).toBe('data:image/png;base64,abc')
    expect(common.imageSrc('blob:https://example.com/id')).toBe('blob:https://example.com/id')
  })

  // 协议前缀大小写不敏感（实现正则带 /i）
  it('treats uppercase HTTPS: as absolute URL', () => {
    expect(common.imageSrc('HTTPS://external.example/a.png')).toBe('HTTPS://external.example/a.png')
  })

  it('joins qiniu and local keys without duplicate slashes', () => {
    localStorage.setItem('defaultStoreType', 'qiniu')
    expect(common.imageSrc('/article/a.png')).toBe('https://cdn.example.com/article/a.png')
    localStorage.setItem('defaultStoreType', 'local')
    expect(common.imageSrc('comment/a.png')).toBe('https://local.example.com/files/comment/a.png')
  })

  // fallback 优先级：localStorage 为空时回退到 store.state.webInfo.defaultStoreType
  it('falls back to store webInfo.defaultStoreType when localStorage is empty', () => {
    store.state.webInfo = { defaultStoreType: 'local' }
    expect(common.imageSrc('article/a.png')).toBe('https://local.example.com/files/article/a.png')
  })

  // 未知存储类型（非 local）一律走 qiniu 前缀
  it('uses qiniu prefix for unknown store type', () => {
    localStorage.setItem('defaultStoreType', 'oss')
    expect(common.imageSrc('article/a.png')).toBe('https://cdn.example.com/article/a.png')
  })

  it('returns an empty source while configuration is unavailable', () => {
    store.state.sysConfig = {}
    expect(common.imageSrc('article/a.png')).toBe('')
    expect(common.imageSrc('')).toBe('')
    expect(common.imageSrc(null)).toBe('')
    expect(common.imageSrc(undefined)).toBe('')
    expect(common.imageSrc('   ')).toBe('')
  })
})

describe('$common.applyImagePrefix', () => {
  beforeEach(() => {
    localStorage.clear()
    localStorage.setItem('defaultStoreType', 'qiniu')
    store.state.sysConfig = { 'qiniu.downloadUrl': 'https://cdn.example.com/' }
  })

  it('prepends download url to markdown image key', () => {
    const md = common.applyImagePrefix(new MarkdownIt())
    const html = md.render('![封面](article/a.png)')
    expect(html).toContain('src="https://cdn.example.com/article/a.png"')
  })

  it('leaves absolute URLs untouched', () => {
    const md = common.applyImagePrefix(new MarkdownIt())
    const html = md.render('![外链](https://external.example/a.png)')
    expect(html).toContain('src="https://external.example/a.png"')
  })

  // 配置未加载（前缀为空）时 src 被替换为空字符串，不抛异常
  it('does not throw and empties src when prefix missing', () => {
    store.state.sysConfig = {}
    const md = common.applyImagePrefix(new MarkdownIt())
    const html = md.render('![封面](article/a.png)')
    expect(html).not.toContain('undefined')
    expect(html).not.toContain('cdn.example.com')
  })
})

describe('$common.pictureReg', () => {
  beforeEach(() => {
    localStorage.clear()
    localStorage.setItem('defaultStoreType', 'qiniu')
    store.state.sysConfig = { 'qiniu.downloadUrl': 'https://cdn.example.com/' }
  })

  it('converts [name,key] into an img whose src is prefixed', () => {
    const html = common.pictureReg('看图[风景,article/a.png]结尾')
    expect(html).toContain('src="https://cdn.example.com/article/a.png"')
    expect(html).toContain('title="风景"')
  })

  it('leaves absolute URL in [name,url] untouched', () => {
    const html = common.pictureReg('[外链,https://external.example/a.png]')
    expect(html).toContain('src="https://external.example/a.png"')
  })

  it('returns content unchanged when bracket has no comma', () => {
    expect(common.pictureReg('[无逗号文本]')).toBe('[无逗号文本]')
  })
})
