// Funções compartilhadas pelas abas: busca, ordenação, paginação, período e exportação.

export const PAGE_SIZE = 10

// Busca sem diferenciar maiúsculas nem acentos.
export const normalize = (text) => String(text ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
export const matches = (query, ...fields) => !query?.trim() || fields.some(f => normalize(f).includes(normalize(query.trim())))

/** Ordena uma cópia da lista pela coluna escolhida ({ key, dir }), usando getters[key]. */
export function sortRows(rows, sort, getters) {
  const get = sort && getters[sort.key]
  if (!get) return rows
  const factor = sort.dir === 'desc' ? -1 : 1
  return [...rows].sort((a, b) => {
    const x = get(a), y = get(b)
    if (typeof x === 'number' && typeof y === 'number') return (x - y) * factor
    return String(x ?? '').localeCompare(String(y ?? ''), 'pt-BR', { numeric: true, sensitivity: 'base' }) * factor
  })
}

/** Fatia a lista na página pedida, corrigindo páginas que deixaram de existir. */
export function paginate(rows, page) {
  const totalPages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE))
  const currentPage = Math.min(Math.max(page || 1, 1), totalPages)
  return { pageRows: rows.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE), currentPage, totalPages }
}

/** Gera um CSV que o Excel em português abre direto (separador ";" e BOM UTF-8). */
export function downloadCsv(name, headers, rows) {
  const filename = `${name}-${new Date().toISOString().slice(0, 10)}.csv`
  const cell = (value) => {
    const text = String(value ?? '')
    return /[";\n\r]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
  }
  const csv = [headers, ...rows].map(row => row.map(cell).join(';')).join('\r\n')
  const url = URL.createObjectURL(new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

/** Período relativo a hoje: 'hoje', '7', '30' ou '' (tudo). */
export function inPeriod(iso, period) {
  if (!period) return true
  const date = new Date(iso)
  const start = new Date()
  start.setHours(0, 0, 0, 0)
  if (period !== 'hoje') start.setDate(start.getDate() - (Number(period) - 1))
  return date >= start
}
