import { useState } from 'react'
import { createStore, renameStore } from './services/api'

/** Cadastro e renomeação de lojas (somente administrador). */
export default function Lojas({ stores, stocks, onDone, onError, disabled }) {
  const [name, setName] = useState('')
  const [editing, setEditing] = useState(null)
  const [editName, setEditName] = useState('')
  const [saving, setSaving] = useState(false)

  async function run(action, message) {
    if (saving) return
    setSaving(true)
    try { const loja = await action(); onDone(message(loja)) } catch (e) { onError(e) } finally { setSaving(false) }
  }

  function create(event) {
    event.preventDefault()
    run(() => createStore({ nome: name }), loja => { setName(''); return `Loja ${loja.nome} cadastrada. Ela já aparece no estoque, nas entradas e nas transferências.` })
  }

  function rename(event, loja) {
    event.preventDefault()
    run(() => renameStore(loja.id, { nome: editName }), saved => { setEditing(null); return `Loja renomeada para ${saved.nome}.` })
  }

  const off = disabled || saving
  return <>
    <form className="erp-form store-form" onSubmit={create} aria-busy={saving}>
      <div><label htmlFor="store-name">Nome da loja</label><input id="store-name" required maxLength={150} placeholder="Ex.: Loja Centro" value={name} onChange={e => setName(e.target.value)} disabled={off} /></div>
      <button className="primary-button compact" disabled={off}>{saving && !editing ? 'Cadastrando…' : 'Cadastrar loja'}</button>
    </form>
    <div className="table-scroll"><table><thead><tr><th scope="col">Loja</th><th scope="col">ID</th><th scope="col">Produtos com saldo</th><th scope="col">Unidades em estoque</th><th scope="col">Ações</th></tr></thead>
      <tbody>{stores.map(loja => {
        const saldos = stocks[loja.id] || []
        return <tr key={loja.id}>
          <td>{editing === loja.id
            ? <form className="inline-form" onSubmit={e => rename(e, loja)}><input aria-label="Novo nome" required maxLength={150} value={editName} onChange={e => setEditName(e.target.value)} disabled={off} autoFocus /><button className="row-button" disabled={off}>Salvar</button><button type="button" className="row-button" onClick={() => setEditing(null)} disabled={off}>Cancelar</button></form>
            : loja.nome}</td>
          <td>{loja.id}</td>
          <td className="quantity">{saldos.filter(p => p.quantidade > 0).length}</td>
          <td className="quantity">{saldos.reduce((s, p) => s + p.quantidade, 0)}</td>
          <td>{editing !== loja.id && <div className="row-actions"><button className="row-button" onClick={() => { setEditing(loja.id); setEditName(loja.nome) }} disabled={off}>Renomear</button></div>}</td>
        </tr>
      })}</tbody></table>{!stores.length && <p className="empty">Nenhuma loja cadastrada.</p>}</div>
  </>
}
