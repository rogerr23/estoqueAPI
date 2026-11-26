import { useEffect, useState } from 'react'
import { employees, employeeActivity, createEmployee, updateEmployee, deactivateEmployee } from './services/api'

const dateFormat = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
const empty = { nome: '', email: '', senha: '', perfil: 'FUNCIONARIO', ativo: true }
const activityLabel = { TRANSFERENCIA: 'Transferência', CADASTRO_PRODUTO: 'Cadastro de produto', EDICAO_PRODUTO: 'Edição de produto' }

export default function Funcionarios({ user, revision, onExpired }) {
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState(empty)
  const [editing, setEditing] = useState(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [selected, setSelected] = useState(null)
  const [activity, setActivity] = useState(null)
  const [reload, setReload] = useState(0)

  function fail(e) { if (e.name === 'AbortError') return; if (e.status === 401) onExpired(); else setError(e.campos ? `${e.message} ${Object.values(e.campos).join(' ')}` : e.message) }

  useEffect(() => {
    const controller = new AbortController()
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true)
    employees(controller.signal).then(setList).catch(fail).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [revision, reload])

  useEffect(() => {
    const controller = new AbortController()
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setActivity(null)
    if (selected) employeeActivity(selected.id, controller.signal).then(setActivity).catch(fail)
    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected, revision, reload])

  function edit(u) { setEditing(u.id); setForm({ nome: u.nome, email: u.email, senha: '', perfil: u.perfil, ativo: u.ativo }); setError(''); setNotice('') }
  function cancel() { setEditing(null); setForm(empty); setError('') }

  async function save(event) {
    event.preventDefault()
    if (saving) return
    setSaving(true); setError(''); setNotice('')
    try {
      if (editing) { await updateEmployee(editing, form); setNotice(`Funcionário ${form.nome} atualizado.`) }
      else { await createEmployee(form); setNotice(`Funcionário ${form.nome} cadastrado.`) }
      cancel(); setReload(v => v + 1)
    } catch (e) { fail(e) } finally { setSaving(false) }
  }

  async function deactivate(u) {
    if (!window.confirm(`Desativar ${u.nome}? O histórico dele continua disponível.`)) return
    setError(''); setNotice('')
    try { await deactivateEmployee(u.id); setNotice(`${u.nome} foi desativado.`); setReload(v => v + 1) } catch (e) { fail(e) }
  }

  const set = (field) => (e) => setForm(v => ({ ...v, [field]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }))

  return <>
    {error && <p className="message error" role="alert">{error}</p>}
    {notice && <p className="message success" role="status">{notice}</p>}
    <form className="erp-form employee-form" onSubmit={save} aria-busy={saving}>
      <div><label htmlFor="emp-name">Nome</label><input id="emp-name" required maxLength={150} value={form.nome} onChange={set('nome')} disabled={saving} /></div>
      <div><label htmlFor="emp-email">E-mail</label><input id="emp-email" type="email" required maxLength={254} value={form.email} onChange={set('email')} disabled={saving} /></div>
      <div><label htmlFor="emp-pass">{editing ? 'Nova senha (opcional)' : 'Senha'}</label><input id="emp-pass" type="password" autoComplete="new-password" minLength={6} maxLength={72} required={!editing} value={form.senha} onChange={set('senha')} disabled={saving} /></div>
      <div><label htmlFor="emp-role">Perfil</label><select id="emp-role" value={form.perfil} onChange={set('perfil')} disabled={saving}><option value="FUNCIONARIO">Funcionário</option><option value="ADMIN">Administrador</option></select></div>
      {editing && <label className="checkbox"><input type="checkbox" checked={form.ativo} onChange={set('ativo')} disabled={saving} /> Ativo</label>}
      <div className="form-actions">
        <button className="primary-button compact" disabled={saving}>{saving ? 'Salvando…' : editing ? 'Salvar alterações' : 'Cadastrar funcionário'}</button>
        {editing && <button type="button" className="secondary-button compact" onClick={cancel} disabled={saving}>Cancelar</button>}
      </div>
    </form>

    {loading ? <p role="status" className="muted">Carregando funcionários…</p> :
      <div className="table-scroll"><table><thead><tr>{['Nome', 'E-mail', 'Perfil', 'Situação', 'Transferências', 'Unidades', 'Produtos cadastrados', 'Última atividade', 'Ações'].map(h => <th key={h} scope="col">{h}</th>)}</tr></thead>
        <tbody>{list.map(u => <tr key={u.id} className={selected?.id === u.id ? 'selected' : undefined}>
          <td>{u.nome}{u.id === user.id && <span className="muted"> (você)</span>}</td>
          <td>{u.email}</td>
          <td>{u.perfil === 'ADMIN' ? 'Administrador' : 'Funcionário'}</td>
          <td><span className={u.ativo ? 'badge' : 'badge off'}>{u.ativo ? 'Ativo' : 'Inativo'}</span></td>
          <td className="quantity">{u.transferencias}</td>
          <td className="quantity">{u.unidadesTransferidas}</td>
          <td className="quantity">{u.produtosCadastrados}</td>
          <td className="nowrap">{u.ultimaAtividade ? dateFormat.format(new Date(u.ultimaAtividade)) : '—'}</td>
          <td className="row-actions">
            <button className="text-button" onClick={() => setSelected(u)}>Ver atividades</button>
            <button className="text-button" onClick={() => edit(u)}>Editar</button>
            {u.ativo && u.id !== user.id && <button className="text-button danger" onClick={() => deactivate(u)}>Desativar</button>}
          </td>
        </tr>)}</tbody></table></div>}

    {selected && <section className="activity" aria-label={`Atividades de ${selected.nome}`}>
      <div className="page-heading"><h3>Atividades de {selected.nome}</h3><button className="text-button" onClick={() => setSelected(null)}>Fechar</button></div>
      {!activity ? <p role="status" className="muted">Carregando atividades…</p> : !activity.length ? <p className="empty">Nenhuma atividade registrada.</p> :
        <div className="table-scroll"><table><thead><tr><th scope="col">Data e hora</th><th scope="col">Tipo</th><th scope="col">Descrição</th></tr></thead>
          <tbody>{activity.map((a, i) => <tr key={i}><td className="nowrap">{a.dataHora ? dateFormat.format(new Date(a.dataHora)) : '—'}</td><td>{activityLabel[a.tipo] || a.tipo}</td><td>{a.descricao}</td></tr>)}</tbody></table></div>}
    </section>}
  </>
}
