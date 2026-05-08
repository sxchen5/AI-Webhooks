import MarkdownIt from 'markdown-it'

function slugify(text) {
  const s = String(text || '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^\w\u4e00-\u9fff-]+/g, '')
    .replace(/^-+|-+$/g, '')
  return (s || 'section').slice(0, 56)
}

function uniqueSlug(baseText, used) {
  let base = slugify(baseText)
  if (!used.has(base)) {
    used.add(base)
    return base
  }
  let n = 2
  let s = `${base}-${n}`
  while (used.has(s)) {
    n += 1
    s = `${base}-${n}`
  }
  used.add(s)
  return s
}

/**
 * 与 markdown-it 解析顺序一致的标题列表（含 setext 等）。
 * @returns {{ level: number, text: string, slug: string }[]}
 */
export function extractMarkdownHeadings(source) {
  const src = source || ''
  const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
  const tokens = md.parse(src, {})
  const used = new Set()
  const headings = []
  for (let i = 0; i < tokens.length; i++) {
    const t = tokens[i]
    if (t.type !== 'heading_open') continue
    const level = Number(String(t.tag).replace(/^h/i, '')) || 1
    let text = ''
    let j = i + 1
    while (j < tokens.length && tokens[j].type !== 'heading_close') {
      const inline = tokens[j]
      if (inline.type === 'inline' && inline.children) {
        for (const c of inline.children) {
          if (c.type === 'text') text += c.content
          else if (c.type === 'code_inline') text += c.content
        }
      }
      j++
    }
    const slug = uniqueSlug(text, used)
    headings.push({ level, text, slug })
  }
  return headings
}

const mdAnchors = new MarkdownIt({ html: false, linkify: true, breaks: true })

let anchorRenderState = null

const origHeadingOpen = mdAnchors.renderer.rules.heading_open
mdAnchors.renderer.rules.heading_open = (tokens, idx, options, env, self) => {
  const st = anchorRenderState
  if (st?.headings && st.i < st.headings.length) {
    const h = st.headings[st.i]
    tokens[idx].attrSet('id', `${st.prefix}-${h.slug}`)
    tokens[idx].attrJoin('class', 'md-heading')
    st.i += 1
  }
  if (origHeadingOpen) {
    return origHeadingOpen.call(self, tokens, idx, options, env, self)
  }
  return self.renderToken(tokens, idx, options)
}

/**
 * 渲染 Markdown 并为标题注入 id，供目录锚点跳转。
 * @returns {{ html: string, headings: { level: number, text: string, slug: string, id: string }[] }}
 */
export function renderMarkdownWithAnchors(source, idPrefix) {
  const prefix = idPrefix || 'md'
  const headings = extractMarkdownHeadings(source)
  anchorRenderState = { headings, i: 0, prefix }
  const tokens = mdAnchors.parse(source || '', {})
  const html = mdAnchors.renderer.render(tokens, mdAnchors.options, {})
  anchorRenderState = null
  const withIds = headings.map((h) => ({ ...h, id: `${prefix}-${h.slug}` }))
  return { html, headings: withIds }
}
