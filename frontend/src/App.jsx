import './App.css'

export default function App() {
  return (
    <main>
      <span className="label">PROJETO ACADÊMICO · FASE 1</span>
      <h1>EstoqueAPI</h1>
      <p className="intro">Consulta e transferência de produtos entre lojas.</p>
      <section aria-labelledby="status">
        <h2 id="status">Base do projeto</h2>
        <p>Esta é a interface inicial. As operações de estoque serão disponibilizadas nas próximas fases.</p>
        <ol>
          <li><strong>Base executável</strong><span>Java 21, PostgreSQL e React</span></li>
          <li><strong>Acesso e consultas</strong><span>Login, produtos e estoque por loja</span></li>
          <li><strong>Transferências</strong><span>Movimentação segura e histórico</span></li>
          <li><strong>Interface e validação</strong><span>Fluxo completo e evidências de testes</span></li>
        </ol>
      </section>
    </main>
  )
}
