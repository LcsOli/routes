package com.CF.routes.infrastructure.repository;

import java.time.LocalDateTime;

public class LogErroRoteirizacao {

    private Long id;
    private Long numcar;
    private String erro;
    private LocalDateTime horaEnvio;

    // Construtor padrão obrigatório
    public LogErroRoteirizacao() {
    }

    // Construtor utilitário
    public LogErroRoteirizacao(Long numcar, String erro, LocalDateTime horaEnvio) {
        this.numcar = numcar;
        this.erro = erro;
        this.horaEnvio = horaEnvio;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNumcar() { return numcar; }
    public void setNumcar(Long numcar) { this.numcar = numcar; }

    public String getErro() { return erro; }
    public void setErro(String erro) { this.erro = erro; }

    public LocalDateTime getHoraEnvio() { return horaEnvio; }
    public void setHoraEnvio(LocalDateTime horaEnvio) { this.horaEnvio = horaEnvio; }
}