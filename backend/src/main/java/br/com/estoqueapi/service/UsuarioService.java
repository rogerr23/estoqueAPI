package br.com.estoqueapi.service;

import br.com.estoqueapi.dto.*;
import br.com.estoqueapi.entity.Perfil;
import br.com.estoqueapi.entity.Usuario;
import br.com.estoqueapi.exception.NegocioException;
import br.com.estoqueapi.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

/** CRUD de funcionários e visão das atividades de cada um (somente administradores). */
@Service
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final TransferenciaRepository transferencias;
    private final ProdutoRepository produtos;
    private final ProdutoEdicaoRepository edicoes;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, TransferenciaRepository transferencias,
            ProdutoRepository produtos, ProdutoEdicaoRepository edicoes, PasswordEncoder encoder) {
        this.usuarios = usuarios; this.transferencias = transferencias;
        this.produtos = produtos; this.edicoes = edicoes; this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResumo> listar() {
        Map<Long, Object[]> movimentos = new HashMap<>();
        transferencias.resumoPorUsuario().forEach(l -> movimentos.put((Long) l[0], l));
        Map<Long, Object[]> cadastros = new HashMap<>();
        produtos.resumoPorUsuario().forEach(l -> cadastros.put((Long) l[0], l));
        Map<Long, Instant> ultimasEdicoes = new HashMap<>();
        edicoes.ultimaPorUsuario().forEach(l -> ultimasEdicoes.put((Long) l[0], (Instant) l[1]));
        return usuarios.findAllByOrderByNomeAscIdAsc().stream().map(u -> {
            var m = movimentos.get(u.getId());
            var c = cadastros.get(u.getId());
            Instant ultima = maisRecente(maisRecente(m == null ? null : (Instant) m[3], c == null ? null : (Instant) c[2]),
                ultimasEdicoes.get(u.getId()));
            return new FuncionarioResumo(u.getId(), u.getNome(), u.getEmail(), u.getPerfil().name(), u.isAtivo(),
                m == null ? 0 : (Long) m[1], m == null ? 0 : ((Number) m[2]).longValue(),
                c == null ? 0 : (Long) c[1], ultima);
        }).toList();
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest entrada) {
        if (entrada.senha() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", "Senha é obrigatória no cadastro");
        }
        if (usuarios.existsByEmailIgnoreCase(entrada.email())) throw emailDuplicado();
        var usuario = new Usuario(entrada.nome(), entrada.email(), encoder.encode(entrada.senha()), entrada.perfil());
        if (Boolean.FALSE.equals(entrada.ativo())) usuario.desativar();
        return UsuarioResponse.from(usuarios.saveAndFlush(usuario));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest entrada, Long adminId) {
        var usuario = buscar(id);
        if (usuarios.existsByEmailIgnoreCaseAndIdNot(entrada.email(), id)) throw emailDuplicado();
        boolean ativo = entrada.ativo() == null ? usuario.isAtivo() : entrada.ativo();
        if (id.equals(adminId) && (!ativo || entrada.perfil() != Perfil.ADMIN)) {
            throw new NegocioException(HttpStatus.CONFLICT, "OPERACAO_NAO_PERMITIDA",
                "Você não pode remover seu próprio acesso de administrador");
        }
        usuario.atualizar(entrada.nome(), entrada.email(), entrada.perfil(), ativo);
        if (entrada.senha() != null) usuario.alterarSenha(encoder.encode(entrada.senha()));
        return UsuarioResponse.from(usuarios.saveAndFlush(usuario));
    }

    /** Desativa em vez de apagar: o histórico continua mostrando quem fez cada operação. */
    @Transactional
    public void desativar(Long id, Long adminId) {
        if (id.equals(adminId)) {
            throw new NegocioException(HttpStatus.CONFLICT, "OPERACAO_NAO_PERMITIDA", "Você não pode desativar a si mesmo");
        }
        buscar(id).desativar();
    }

    @Transactional(readOnly = true)
    public List<AtividadeResponse> atividades(Long id) {
        buscar(id);
        var lista = new ArrayList<AtividadeResponse>();
        transferencias.findAllByUsuarioIdOrderByDataHoraDescIdDesc(id).forEach(t -> lista.add(new AtividadeResponse(
            "TRANSFERENCIA", t.getDataHora(), "Transferiu %d un. de %s (%s) de %s para %s".formatted(
                t.getQuantidade(), t.getProduto().getCodigo(), t.getProduto().getNome(),
                t.getLojaOrigem().getNome(), t.getLojaDestino().getNome()))));
        produtos.findAllByCadastradoPorIdOrderByCadastradoEmDescIdDesc(id).forEach(p -> lista.add(new AtividadeResponse(
            "CADASTRO_PRODUTO", p.getCadastradoEm(), "Cadastrou o produto %s (%s)".formatted(p.getCodigo(), p.getNome()))));
        edicoes.findAllByUsuarioIdOrderByDataHoraDescIdDesc(id).forEach(e -> lista.add(new AtividadeResponse(
            "EDICAO_PRODUTO", e.getDataHora(), "Editou o produto %s: %s".formatted(e.getProduto().getCodigo(), e.getAlteracoes()))));
        lista.sort(Comparator.comparing(AtividadeResponse::dataHora, Comparator.nullsLast(Comparator.reverseOrder())));
        return lista;
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() ->
            new NegocioException(HttpStatus.NOT_FOUND, "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado"));
    }

    private static Instant maisRecente(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    private NegocioException emailDuplicado() {
        return new NegocioException(HttpStatus.CONFLICT, "EMAIL_DUPLICADO", "Já existe um usuário com esse e-mail");
    }
}
