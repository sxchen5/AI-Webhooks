import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

/**
 * 将 Markdown 转为 HTML（禁止内嵌 HTML，供 v-html 展示）。
 * @param {string} source
 * @returns {string}
 */
export function renderMarkdown(source) {
  if (!source) return ''
  return md.render(String(source))
}
