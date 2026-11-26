import { useEffect, useState } from 'react'
import { stock, updateProduct } from './services/api'

/** Edição completa do produto (somente administrador): código, nome e saldo em cada loja. */
export default function ProdutoEditor({ product, stores, onSaved, onCancel, onExpired }) {
  const [form, setForm] = useState({ codigo: product.codigo, nome: product.nome, estoqueMinimo: String(product.estoqueMinimo ?? 0) })
  const [quantities, setQuantities] = useState(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    Promise.all(stores.map(s => stock(s.id, controller.signal)))
      .then(all => setQuantities(Object.fromEntries(stores.map((s, i) =>
        [s.id, String(all[i].find(p => p.produtoId === product.id)?.quantidade ?? 0)]))))
      .catch(e => { if (e.name !== 'AbortError') { if (e.status === 401) onExpired(); else setError(e.message) } })
    return () => controller.abort()
  }, [product.id, stores, onExpired])

  async function save(event) {
    event.preventDefault()
    if (saving || !quantities) return
    const estoques = stores.map(s => ({ lojaId: s.id, quantidade: Number(quantities[s.id]) }))
    const estoqueMinimo = form.estoqueMinimo === '' ? 0 : Number(form.estoqueMinimo)
    if (!Number.isSafeInteger(estoqueMinimo) || estoqueMinimo < 0 || estoques.some(e => !Number.isSafeInteger(e.quantidade) || e.quantidade < 0)) { setError('Informe quantidades inteiras, zero ou maiores.'); return }
    setSaving(true); setError('')
    try {
      const saved = await updateProduct(product.id, { codigo: form.codigo, nome: form.nome, estoqueMinimo, estoques })
      onSaved(saved)
    } catch (e) {
      if (e.status === 401) onExpired()
      else setError(e.campos ? `${e.message} ${Object.values(e.campos).join(' ')}` : e.message)
    } finally { setSaving(false) }
  }

  return <form className="erp-form edit-product" onSubmit={save} aria-busy={saving} aria-label={`Editar ${product.codigo}`}>
    <h3 className="wide">Editar produto {product.codigo}</h3>
    {error && <p className="message error wide" role="alert">{error}</p>}
    <div><label htmlFor="edit-code">Código</label><input id="edit-code" required maxLength={60} value={form.codigo} onChange={e => setForm(v => ({ ...v, codigo: e.target.value }))} disabled={saving} /></div>
    <div><label htmlFor="edit-name">Nome do produto</label><input id="edit-name" required maxLength={150} value={form.nome} onChange={e => setForm(v => ({ ...v, nome: e.target.value }))} disabled={saving} /></div>
    <div><label htmlFor="edit-min">Estoque mínimo</label><input id="edit-min" type="number" inputMode="numeric" min="0" max="2147483647" step="1" value={form.estoqueMinimo} onChange={e => setForm(v => ({ ...v, estoqueMinimo: e.target.value }))} disabled={saving} /></div>
    {!quantities ? <p className="muted wide" role="status">Carregando quantidades…</p> : stores.map(s =>
      <div key={s.id}><label htmlFor={`edit-qty-${s.id}`}>Quantidade · {s.nome}</label><input id={`edit-qty-${s.id}`} type="number" min="0" max="2147483647" step="1" required value={quantities[s.id]} onChange={e => setQuantities(v => ({ ...v, [s.id]: e.target.value }))} disabled={saving} /></div>)}
    <div className="form-actions wide">
      <button className="primary-button compact" disabled={saving || !quantities}>{saving ? 'Salvando…' : 'Salvar alterações'}</button>
      <button type="button" className="secondary-button compact" onClick={onCancel} disabled={saving}>Cancelar</button>
    </div>
  </form>
}
