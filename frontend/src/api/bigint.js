/**
 * 长整数保护（见 issue.md 问题 29）
 *
 * 背景：后端主键是 MyBatis-Plus 的雪花 ID（约 19 位，如 2099362928699224066），
 * 而 JS 的 Number 只能安全表示 16 位（Number.MAX_SAFE_INTEGER = 9007199254740991）。
 * 直接 JSON.parse 会把 id 四舍五入 —— 前端再把它发回后端就永远对不上，
 * 表现为「提供商不存在或无权操作」「模型不存在」这类莫名其妙的错误。
 *
 * 做法：在 JSON.parse 之前，把「值位置上」的 ≥16 位整数先加上引号，让它以**字符串**
 * 进来、原样发回去。后端 Jackson 会把数字字符串自动转回 Long（已实测 code=200）。
 *
 * 为什么逐字符扫描而不是正则：正则容易误伤字符串内容里恰好长得像数字的片段，
 * 例如 content 里出现 "abc:1234567890123456," 就会被错误加引号。
 */
export function quoteBigInts(text) {
  let out = ''
  let inStr = false
  for (let i = 0; i < text.length; ) {
    const ch = text[i]

    // 字符串内部：原样复制（处理 \\ 转义，避免误判字符串结束）
    if (inStr) {
      if (ch === '\\') {
        out += text.slice(i, i + 2)
        i += 2
        continue
      }
      if (ch === '"') inStr = false
      out += ch
      i++
      continue
    }

    // 进入字符串
    if (ch === '"') {
      inStr = true
      out += ch
      i++
      continue
    }

    // 值位置上的数字 token（可带负号）
    if (ch === '-' || (ch >= '0' && ch <= '9')) {
      let j = i
      if (text[j] === '-') j++
      const start = j
      while (j < text.length && text[j] >= '0' && text[j] <= '9') j++
      const digits = j - start
      const tail = text[j]
      // 后面跟 . 或 e/E 的是浮点，不处理
      const isFloat = tail === '.' || tail === 'e' || tail === 'E'
      out += digits >= 16 && !isFloat ? `"${text.slice(i, j)}"` : text.slice(i, j)
      i = j
      continue
    }

    out += ch
    i++
  }
  return out
}
