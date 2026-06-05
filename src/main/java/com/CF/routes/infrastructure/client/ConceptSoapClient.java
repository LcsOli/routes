package com.CF.routes.infrastructure.client;

import com.CF.routes.domain.mapper.PedidoMapper;
import com.CF.routes.infrastructure.config.ConceptConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptSoapClient {

    private final ConceptConfig config;
    private final PedidoMapper mapper;

    private static final String NS_FACHADA = "http://fachada.concept/";

    public String listarItinerariosCarregamento(Long numCar) {
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Header/>
               <soapenv:Body>
                  <fac:listarItinerariosCarregamento>
                     <arg0>%d</arg0>
                     <arg1>%s</arg1>
                     <arg2>%s</arg2>
                     <arg3>%s</arg3>
                  </fac:listarItinerariosCarregamento>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, numCar, config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        
        String urlCorreta = "http://52.6.27.50:8181/importadorPedidos";
        
        return enviarRequestComRetorno(urlCorreta, xml, "listarItinerariosCarregamento");
    }

    public String listarViagens() {
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Header/>
               <soapenv:Body>
                  <fac:listarViagens>
                     <arg0>%s</arg0>
                     <arg1>%s</arg1>
                     <arg2>%s</arg2>
                  </fac:listarViagens>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        
        return enviarRequestComRetorno(config.getEndpoints().getAutomatizador(), xml, "listarViagens");
    }

    public void cadastrarZona(String codigo, String nome) {
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Body>
                  <fac:cadastrarZona>
                     <arg0><id>0</id><codigoZona>%s</codigoZona><nome>%s</nome></arg0>
                     <arg1>%s</arg1><arg2>%s</arg2><arg3>%s</arg3>
                  </fac:cadastrarZona>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, codigo, mapper.escapeXml(nome), config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        enviarRequest(config.getEndpoints().getAutomatizador(), xml, "cadastrarZona");
    }

    public void cadastrarLoja(String codigo, String nome) {
        String lat = "-20.797732132339135";
        String lng = "-49.32830021380005";
        
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Body>
                  <fac:cadastrarLoja>
                     <arg0>
                        <id>0</id>
                        <codigoLoja>%s</codigoLoja>
                        <nome>%s</nome>
                        <latitude>%s</latitude>
                        <longitude>%s</longitude>
                        <raio>500</raio>
                     </arg0>
                     <arg1>%s</arg1><arg2>%s</arg2><arg3>%s</arg3>
                  </fac:cadastrarLoja>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, codigo, mapper.escapeXml(nome), lat, lng, config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        enviarRequest(config.getEndpoints().getAutomatizador(), xml, "cadastrarLoja");
    }

    public void cadastrarMotorista(String matricula, String nome, String cpf) {
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Body>
                  <fac:cadastrarMotorista>
                     <arg0>
                        <id>0</id>
                        <matricula>%s</matricula>
                        <nome>%s</nome>
                        <cpf>%s</cpf>
                     </arg0>
                     <arg1>%s</arg1><arg2>%s</arg2><arg3>%s</arg3>
                  </fac:cadastrarMotorista>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, matricula, mapper.escapeXml(nome), mapper.formatarCpf(cpf), config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        enviarRequest(config.getEndpoints().getAutomatizador(), xml, "cadastrarMotorista");
    }

    public void importarPedidos(String blocosArg0Xml) {
        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Body>
                  <fac:importarPedidos>
                     %s
                     <arg1>%s</arg1>
                     <arg2>%s</arg2>
                     <arg3>%s</arg3>
                  </fac:importarPedidos>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, blocosArg0Xml, config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());
        enviarRequest(config.getEndpoints().getImportador(), xml, "importarPedidos");
    }

    public void roteirizarPedidos(Long numCar, String placa, String motoristaNome) {
        String dataHoje = java.time.LocalDate.now().toString();

        String xmlCompleto = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="http://fachada.concept/">
               <soapenv:Header/>
               <soapenv:Body>
                  <fac:roteirizarPedidos>
                     <arg0>0</arg0>
                     <arg1>%d</arg1>
                     <arg2>127695</arg2>
                     <arg3>127695</arg3>
                     <arg4>%s</arg4>
                     <arg5>%s</arg5>
                     <arg6>%s</arg6>
                     <arg7>false</arg7>
                     <arg8>%s</arg8>
                     <arg9>%s</arg9>
                     <arg10>%s</arg10>
                  </fac:roteirizarPedidos>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(
                numCar,
                dataHoje,
                placa,
                motoristaNome,
                config.getCnpj(),
                config.getSenhaCliente(),
                config.getSenhaCentral()
            );

        log.info("Enviando requisição de roteirização para o carregamento: {}", numCar);
        enviarRequestSoap(xmlCompleCompleto(xmlCompleto), config.getEndpoints().getAutomatizador());
    }

    private String xmlCompleCompleto(String xml) {
        return xml; // Auxiliar simples de legibilidade
    }

    private void enviarRequest(String url, String xmlBody, String operacao) {
        enviarRequestComRetorno(url, xmlBody, operacao);
    }

    private void enviarRequestSoap(String xmlBody, String url) {
        enviarRequestComRetorno(url, xmlBody, "roteirizarPedidos");
    }

    private String enviarRequestComRetorno(String url, String xmlBody, String operacao) {
        long start = System.currentTimeMillis();
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); 
        factory.setReadTimeout(45000); 
        
        RestTemplate rt = new RestTemplate(factory);
        rt.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        
        rt.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false; 
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                // Prevenido lançamento automático do Spring para validação manual abaixo
            }
        });
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/xml;charset=UTF-8"));
        headers.set("SOAPAction", "\"\"");
        
        try {
            HttpEntity<String> entity = new HttpEntity<>(xmlBody, headers);
            log.info("[SOAP] Enviando '{}' para {}", operacao, url);
            
            ResponseEntity<String> response = rt.postForEntity(url, entity, String.class);
            String body = response.getBody();
            long duration = System.currentTimeMillis() - start;
            
            log.info("[SOAP] Resposta de '{}' recebida em {}ms (Status: {}).", operacao, duration, response.getStatusCode().value());
            log.info("[SOAP] Payload literal da resposta de '{}': \n{}", operacao, body);
            
            if (body == null) {
                throw new RuntimeException("Resposta da Concept retornou corpo vazio (null).");
            }

            // 1. Validação de Fault estrutural do barramento SOAP (ex: erros 500 do servidor)
            if (body.contains("Fault") || body.contains("faultcode")) {
                log.error("[SOAP] A API retornou uma falha estrutural (SOAP Fault): {}", body);
                throw new RuntimeException("Falha estrutural retornada pela Concept.");
            }
            
            // 2. Validação cirúrgica de sucesso da regra de negócio interna da Concept
            validarSucessoNegocio(body, operacao);
            
            return body;
        } catch (RuntimeException e) {
            log.error("[SOAP] Falha de validação ou regra de negócio na operacao {}: {}", operacao, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[SOAP] Falha fatal de infraestrutura na operacao {}: {}", operacao, e.getMessage());
            throw new RuntimeException("Erro de comunicacao com Concept API: " + e.getMessage());
        }
    }

    private void validarSucessoNegocio(String xml, String operacao) {
        Pattern sucessoPattern = Pattern.compile("<comSucesso>(.*?)</comSucesso>");
        Matcher sucessoMatcher = sucessoPattern.matcher(xml);
        
        if (sucessoMatcher.find()) {
            String comSucessoValor = sucessoMatcher.group(1).trim();
            
if ("false".equalsIgnoreCase(comSucessoValor)) {
    Pattern mensagemPattern = Pattern.compile("<mensagem>(.*?)</mensagem>");
    Matcher mensagemMatcher = mensagemPattern.matcher(xml);
    String mensagemErro = "Motivo não explicitado no XML de retorno.";
    
    if (mensagemMatcher.find()) {
        mensagemErro = mensagemMatcher.group(1).trim();
    }
    
    // Tratamento cirúrgico: se o erro for APENAS que o pedido já existe, não quebra o fluxo
    if (mensagemErro.contains("ja cadastrado") || mensagemErro.contains("já cadastrado")) {
        log.warn("[SOAP] Aviso de duplicidade ignorado (o pedido já existe na Concept): {}", mensagemErro);
        return; // Retorna sem lançar Exception, permitindo o avanço do fluxo
    }
    
    throw new RuntimeException("A operacao '" + operacao + "' falhou na regra de negócio. Mensagem: " + mensagemErro);
}
        }
    }

    public void alterarStatusFinalizadoParaLiberadoSeparacao(List<String> numerosPedidos) {
        StringBuilder sb = new StringBuilder();
        for (String numPed : numerosPedidos) {
            sb.append("<arg0><numeroPedido>").append(numPed).append("</numeroPedido></arg0>");
        }

        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Header/>
               <soapenv:Body>
                  <fac:alterarStatusVendaFinalizadoParaLiberadoSeparacao>
                     %s
                     <arg1>%s</arg1><arg2>%s</arg2><arg3>%s</arg3>
                  </fac:alterarStatusVendaFinalizadoParaLiberadoSeparacao>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, sb.toString(), config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());

        enviarRequest(config.getEndpoints().getAutomatizador(), xml, "Status Finalizado -> LiberadoSep");
    }

    public void alterarStatusLiberadoSeparacaoParaEmSeparacao(List<String> numerosPedidos) {
        StringBuilder sb = new StringBuilder();
        for (String numPed : numerosPedidos) {
            sb.append("<arg0><numeroPedido>").append(numPed).append("</numeroPedido></arg0>");
        }

        String dataHoraConcept = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Header/>
               <soapenv:Body>
                  <fac:alterarStatusLiberadoSeparacaParaEmSeparacao>
                     %s
                     <arg1>%s</arg1>
                     <arg2>%s</arg2><arg3>%s</arg3><arg4>%s</arg4>
                  </fac:alterarStatusLiberadoSeparacaParaEmSeparacao>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, sb.toString(), dataHoraConcept, config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());

        enviarRequest(config.getEndpoints().getAutomatizador(), xml, "Status LiberadoSep -> EmSep");
    }

    /**
     * WORKFLOW STATUS 3: Altera o status de Em Separação para Separados.
     */
    public void alterarStatusEmSeparacaoParaSeparados(List<String> numerosPedidos) {
        StringBuilder sb = new StringBuilder();
        for (String numPed : numerosPedidos) {
            sb.append("<arg0><numeroPedido>").append(numPed).append("</numeroPedido></arg0>");
        }

        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Header/>
               <soapenv:Body>
                  <fac:alterarStatusEmSeparacaoParaSeparados>
                     %s
                     <arg1>%s</arg1><arg2>%s</arg2><arg3>%s</arg3>
                  </fac:alterarStatusEmSeparacaoParaSeparados>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, sb.toString(), config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());

        enviarRequest(config.getEndpoints().getAutomatizador(), xml, "Status EmSep -> Separados");
    }

    /**
     * WORKFLOW STATUS 4: Altera o status de Separados para Liberados para Roteirização.
     */
    public void alterarStatusSeparadosParaLiberadosRoteirizacao(List<String> numerosPedidos) {
        StringBuilder sb = new StringBuilder();
        for (String numPed : numerosPedidos) {
            sb.append("<arg0><numeroPedido>").append(numPed).append("</numeroPedido></arg0>");
        }

        String xml = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="%s">
               <soapenv:Header/>
               <soapenv:Body>
                  <fac:alterarStatusSeparadosParaLiberadosRoteirizacao>
                     %s
                     <arg1>%s</arg1><arg2>%s</arg2><arg3>%s</arg3>
                  </fac:alterarStatusSeparadosParaLiberadosRoteirizacao>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(NS_FACHADA, sb.toString(), config.getCnpj(), config.getSenhaCliente(), config.getSenhaCentral());

        enviarRequest(config.getEndpoints().getAutomatizador(), xml, "Status Separados -> LiberadoRot");
    }
}