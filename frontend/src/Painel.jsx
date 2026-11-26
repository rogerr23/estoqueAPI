const timeFormat = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })

function isToday(iso) {
  const d = new Date(iso), now = new Date()
  return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate()
}

/** Visão geral: números principais e o que aconteceu por último no sistema. */
export default function Painel({ data, lowProducts, totals, onGo }) {
  const stockByStore = data.stores.map(s => ({
    ...s, units: (data.stocks[s.id] || []).reduce((sum, p) => sum + p.quantidade, 0),
  }))
  const totalUnits = stockByStore.reduce((sum, s) => sum + s.units, 0)
  const transfersToday = data.transfers.filter(t => isToday(t.dataHora))
  const entriesToday = data.entries.filter(e => isToday(e.dataHora))
  const unitsInToday = entriesToday.reduce((sum, e) => sum + e.quantidade, 0)

  const recent = [
    ...data.transfers.map(t => ({ key: `t${t.id}`, when: t.dataHora, who: t.usuario.nome, kind: 'Transferência', tone: 'move',
      code: t.produto.codigo, text: `${t.quantidade} un. · ${t.lojaOrigem.nome} → ${t.lojaDestino.nome}` })),
    ...data.entries.map(e => ({ key: `e${e.id}`, when: e.dataHora, who: e.usuario.nome, kind: 'Entrada', tone: 'in',
      code: e.produto.codigo, text: `${e.quantidade} un. · ${e.loja.nome}` })),
    ...data.edits.map(e => ({ key: `d${e.id}`, when: e.dataHora, who: e.usuario.nome,
      kind: e.acao === 'EDICAO' ? 'Edição' : 'Exclusão', tone: e.acao === 'EDICAO' ? 'edit' : 'out', code: e.produto.codigo, text: e.alteracoes })),
  ].sort((a, b) => new Date(b.when) - new Date(a.when)).slice(0, 8)

  return <>
    <div className="stat-grid">
      <button className="stat-card" onClick={() => onGo('Produtos')}><span className="stat-label">Produtos cadastrados</span><span className="stat-value">{data.products.length}</span><span className="stat-detail">no catálogo</span></button>
      <button className="stat-card" onClick={() => onGo('Estoque')}><span className="stat-label">Unidades em estoque</span><span className="stat-value">{totalUnits}</span><span className="stat-detail">{stockByStore.map(s => `${s.nome}: ${s.units}`).join(' · ')}</span></button>
      <button className="stat-card" onClick={() => onGo('Entradas', { page: 1 })}><span className="stat-label">Entradas hoje</span><span className="stat-value">{entriesToday.length}</span><span className="stat-detail">{unitsInToday} unidade(s) recebida(s)</span></button>
      <button className={`stat-card${lowProducts.length ? ' stat-alert' : ''}`} onClick={() => onGo('Produtos', { lowOnly: true, query: '' })}><span className="stat-label">Estoque baixo</span><span className="stat-value">{lowProducts.length}</span><span className="stat-detail">{lowProducts.length ? 'produto(s) abaixo do mínimo' : 'tudo acima do mínimo'}</span></button>
      <button className="stat-card" onClick={() => onGo('Histórico', { period: 'hoje', page: 1 })}><span className="stat-label">Transferências hoje</span><span className="stat-value">{transfersToday.length}</span><span className="stat-detail">{transfersToday.reduce((s, t) => s + t.quantidade, 0)} unidade(s) movimentada(s)</span></button>
    </div>
    {lowProducts.length > 0 && <div className="panel low-panel">
      <h3 className="panel-head">Produtos com estoque baixo</h3>
      <ul className="low-list">{lowProducts.map(p => <li key={p.id}><span className="code">{p.codigo}</span> {p.nome}<span className="low-numbers">{totals[p.id] || 0} de mín. {p.estoqueMinimo}</span></li>)}</ul>
    </div>}
    <div className="panel">
      <h3 className="panel-head">Últimas atividades</h3>
      {!recent.length ? <p className="empty">Nenhuma atividade registrada ainda.</p> :
        <div className="table-scroll"><table><thead><tr><th scope="col">Quando</th><th scope="col">Tipo</th><th scope="col">O que aconteceu</th><th scope="col">Quem</th></tr></thead>
          <tbody>{recent.map(r => <tr key={r.key}><td className="nowrap">{timeFormat.format(new Date(r.when))}</td><td><span className={`kind kind-${r.tone}`}>{r.kind}</span></td><td><span className="code">{r.code}</span> · {r.text}</td><td>{r.who}</td></tr>)}</tbody></table></div>}
    </div>
  </>
}
