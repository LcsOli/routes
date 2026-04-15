package com.CF.routes.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {
    private String numPed;
    private LocalDate data;
    private String hora;
    private Double valorTotal;
    private String nomeCliente;
    private String codCli;      
    private String endereco;    
    private String codZona;
    private String nomeZona;
    private String codVendedor;
    private String nomeVendedor;
    private String codLoja;
    private String nomeLoja;
    private Double peso;
    private Double volume;
    private Long numCar;
    private String numNota;
    private String placa;
    private String motoristaNome;
    private String motoristaCpf;
    private String motoristaMatricula;
}