describe('store loadWebInfo', () => {
  let store

  beforeEach(() => {
    jest.resetModules()
    localStorage.clear()
    store = require('@/store').default
  })

  it('keeps notices as markdown text', () => {
    store.commit('loadWebInfo', {
      webName: 'Sara',
      webTitle: 'LIULIUPI',
      notices: '# 公告\n\n- 第一项',
      randomCover: '["https://example.com/cover.jpg"]'
    })

    expect(store.state.webInfo.notices).toBe('# 公告\n\n- 第一项')
    expect(JSON.parse(localStorage.getItem('webInfo')).notices).toBe('# 公告\n\n- 第一项')
  })

  it('keeps legacy notice json array as raw text', () => {
    const legacyNotices = '["旧公告一","旧公告二"]'

    store.commit('loadWebInfo', {
      webName: 'Sara',
      webTitle: 'LIULIUPI',
      notices: legacyNotices,
      randomCover: '[]'
    })

    expect(store.state.webInfo.notices).toBe(legacyNotices)
  })
})
