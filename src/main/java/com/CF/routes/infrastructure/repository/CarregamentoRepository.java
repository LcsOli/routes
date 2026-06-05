package com.CF.routes.infrastructure.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CarregamentoRepository {

    // Cenário de Sucesso total
    @Modifying
    @Query("UPDATE Carregamento c SET c.importadoApi = 'S', c.enviaapi = 'N' WHERE c.numcar = :numcar")
    void marcarSucessoIntegracao(@Param("numcar") Long numcar);

    // Cenário de Falha (Não marca como importado 'S', mantém 'N' ou o estado anterior de erro e bloqueia reenvio temporário se necessário)
    @Modifying
    @Query("UPDATE Carregamento c SET c.importadoApi = 'N', c.enviaapi = 'N' WHERE c.numcar = :numcar")
    void marcarFalhaIntegracao(@Param("numcar") Long numcar);
}