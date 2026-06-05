package com.CF.routes.application.service;

import com.CF.routes.infrastructure.repository.LogErroRoteirizacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogRoteirizacaoService {

    private final LogErroRoteirizacaoRepository logErroRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarErro(Long numcar, String mensagemErro) {
        try {
            // Instanciação correta da subclasse pública estática
            LogErroRoteirizacaoRepository.LogErroRoteirizacao logErro = 
                new LogErroRoteirizacaoRepository.LogErroRoteirizacao(
                    numcar,
                    mensagemErro,
                    LocalDateTime.now()
                );

            logErroRepository.save(logErro);
            log.info("[AUDITORIA] Falha do carregamento {} registrada com sucesso na tabela CF_LOG_ROTEIRIZACAO.", numcar);
        } catch (Exception ex) {
            log.error("Erro crítico ao tentar salvar o log de erro no banco de dados para o numcar {}: {}", numcar, ex.getMessage());
        }
    }
}