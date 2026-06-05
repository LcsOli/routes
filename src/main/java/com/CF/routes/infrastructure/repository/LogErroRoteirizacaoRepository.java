package com.CF.routes.infrastructure.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Repository
public class LogErroRoteirizacaoRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void save(LogErroRoteirizacao logErro) {
        // Mudamos o ID para ser gerado via Math/Random nativo do Oracle 
        // Isso evita depender de SEQUENCE ou TRIGGER criadas no banco
        String sql = """
            INSERT INTO CF_LOG_ROTEIRIZACAO (ID, NUMCAR, ERRO, HORA_ENVIO) 
            VALUES (TRUNC(DBMS_RANDOM.VALUE(1, 999999999)), ?, ?, ?)
            """;
        
        entityManager.createNativeQuery(sql)
                .setParameter(1, logErro.getNumcar())
                .setParameter(2, logErro.getErro())
                .setParameter(3, java.sql.Timestamp.valueOf(logErro.getHoraEnvio()))
                .executeUpdate();
    }

    public static class LogErroRoteirizacao {
        private Long id;
        private Long numcar;
        private String erro;
        private LocalDateTime horaEnvio;

        public LogErroRoteirizacao() {}

        public LogErroRoteirizacao(Long numcar, String erro, LocalDateTime horaEnvio) {
            this.numcar = numcar;
            this.erro = erro;
            this.horaEnvio = horaEnvio;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getNumcar() { return numcar; }
        public void setNumcar(Long numcar) { this.numcar = numcar; }
        public String getErro() { return erro; }
        public void setErro(String erro) { this.erro = erro; }
        public LocalDateTime getHoraEnvio() { return horaEnvio; }
        public void setHoraEnvio(LocalDateTime horaEnvio) { this.horaEnvio = horaEnvio; }
    }
}