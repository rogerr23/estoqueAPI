// Componentes compartilhados pelas abas: busca, tabela ordenável, paginação e formulário recolhível.
import { useState } from 'react'

export function Search({ id, value, onChange, placeholder }) {
  return <div className="search"><label htmlFor={id} className="sr-only">Buscar</label>
    <input id={id} type="search" placeholder={placeholder} value={value} onChange={e => onChange(e.target.value)} /></div>
}

/**
 * Tabela com cabeçalhos clicáveis para ordenar. columns: [{ label, key? }].
 * No celular o cabeçalho some (a tabela vira lista), então um seletor "Ordenar por" assume o papel.
 */
export function SortableTable({ columns, sort, onSort, empty, children, className }) {
  const sortable = columns.filter(c => c.key)
  function toggle(key) {
    onSort(sort?.key === key ? { key, dir: sort.dir === 'asc' ? 'desc' : 'asc' } : { key, dir: 'asc' })
  }
  return <>
    {sortable.length > 0 && <div className="mobile-sort">
      <label htmlFor={`sort-${sortable[0].key}`}>Ordenar por</label>
      <select id={`sort-${sortable[0].key}`} value={sort ? `${sort.key}:${sort.dir}` : ''} onChange={e => {
        const [key, dir] = e.target.value.split(':'); onSort(key ? { key, dir } : null)
      }}>
        <option value="">Padrão</option>
        {sortable.flatMap(c => [
          <option key={`${c.key}:asc`} value={`${c.key}:asc`}>{c.label} ↑</option>,
          <option key={`${c.key}:desc`} value={`${c.key}:desc`}>{c.label} ↓</option>,
        ])}
      </select>
    </div>}
    <div className="table-scroll"><table className={className}><thead><tr>{columns.map(c => {
      if (!c.key) return <th key={c.label} scope="col">{c.label}</th>
      const active = sort?.key === c.key
      return <th key={c.label} scope="col" aria-sort={active ? (sort.dir === 'asc' ? 'ascending' : 'descending') : 'none'}>
        <button type="button" className="sort-button" onClick={() => toggle(c.key)}>{c.label}<span aria-hidden="true">{active ? (sort.dir === 'asc' ? ' ▲' : ' ▼') : ' ↕'}</span></button>
      </th>
    })}</tr></thead><tbody>{children}</tbody></table>{empty && <p className="empty">Nenhum registro encontrado.</p>}</div>
  </>
}

export function Pager({ currentPage, totalPages, count, noun, onPage: change }) {
  if (totalPages <= 1) return null
  // Ao trocar de página, volta ao topo da lista (importante no celular, onde a lista é longa).
  const onPage = (page) => { change(page); document.querySelector('.page-heading')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
  return <nav className="pagination" aria-label={`Páginas de ${noun}`}>
    <button className="row-button" onClick={() => onPage(1)} disabled={currentPage === 1} aria-label="Primeira página">«<span className="wide-only"> Primeira</span></button>
    <button className="row-button" onClick={() => onPage(currentPage - 1)} disabled={currentPage === 1} aria-label="Página anterior">‹<span className="wide-only"> Anterior</span></button>
    <span>Página {currentPage} de {totalPages}<span className="wide-only"> · {count} {noun}</span></span>
    <button className="row-button" onClick={() => onPage(currentPage + 1)} disabled={currentPage === totalPages} aria-label="Próxima página"><span className="wide-only">Próxima </span>›</button>
    <button className="row-button" onClick={() => onPage(totalPages)} disabled={currentPage === totalPages} aria-label="Última página"><span className="wide-only">Última </span>»</button>
  </nav>
}


/** No celular o formulário começa recolhido para a lista aparecer primeiro; no computador fica sempre aberto. */
export function MobileCollapse({ mobile, label, forceOpen, children }) {
  const [open, setOpen] = useState(false)
  // forceOpen: ao editar um registro, o formulário precisa aparecer mesmo no celular.
  if (!mobile || forceOpen) return children
  return <>
    <button type="button" className="collapse-toggle" aria-expanded={open} onClick={() => setOpen(v => !v)}>{open ? '− Fechar formulário' : label}</button>
    {open && children}
  </>
}
