package com.CF.routes.domain.mapper;

import org.springframework.stereotype.Component;
import java.util.Locale;

@Component
public class PedidoMapper {

    public String formatarDecimal(Double valor) {
        if (valor == null) return "0.00";
        return String.format(Locale.US, "%.2f", valor);
    }

    public String escapeXml(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }

    public String formatarCpf(String cpf) {
        if (cpf == null) return "";
        String limpo = cpf.replaceAll("\\D", "");
        if (limpo.length() != 11) return cpf;
        
        return String.format("%s.%s.%s-%s",
                limpo.substring(0, 3),
                limpo.substring(3, 6),
                limpo.substring(6, 9),
                limpo.substring(9, 11));
    }
}