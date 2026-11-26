package br.com.estoqueapi.dto;

import java.time.Instant;

/** Linha da visão do administrador: quem é o funcionário e quanto já operou. */
public record FuncionarioResumo(Long id, String nome, String email, String perfil, boolean ativo,
        long transferencias, long unidadesTransferidas, long produtosCadastrados, long entradas, Instant ultimaAtividade) {}
