# 🚛 Routes Sync: Plataforma de Integração Logística

**Routes Sync** é um middleware automatizado de alta performance desenvolvido para integrar o ERP WinThor (Oracle 19c) à Plataforma Logística Concept (API SOAP). O sistema orquestra a sincronização de carregamentos, motoristas, rotas de entrega e pontos de interesse (POIs), garantindo resiliência transacional, recuperação autônoma de erros e monitoramento em tempo real.

---

## 🚀 Visão Geral

O projeto foi projetado para substituir os processos manuais e legados de roteirização por uma arquitetura moderna, resiliente e orientada a eventos. Ele gerencia todo o ciclo de vida da integração logística: consumo de filas do ERP, sanitização de dados, validação de pré-requisitos na API externa, envio de payloads XML massivos e manutenção de uma trilha de auditoria isolada para rejeições de regras de negócio — tudo isso sem bloquear a esteira operacional.

---

## 🛠 Tech Stack

* **Backend:** Java 21 (utilizando *Records*, *Text Blocks* e recursos modernos).
* **Framework:** Spring Boot 3.4.2.
* **Banco de Dados:** Oracle Database 19c (ERP WinThor) via OJDBC 11 e Spring Data JPA (Consultas Nativas).
* **Integração:** Web Services SOAP (API Concept) com clientes HTTP customizados.
* **Segurança:** Gestão nativa de variáveis de ambiente no Spring Boot (`spring.config.import`).
* **Frontend (Dashboard):** HTML5, Tailwind CSS e JavaScript Vanilla (Arquitetura assíncrona *Monitor-then-Poll*).

---

## 🏗 Arquitetura & Padrões de Projeto

O projeto segue os princípios de **Clean Architecture** e **SOLID** para garantir fácil manutenção e separação clara de responsabilidades:

* **Camada de Domínio (`Domain`):** Contém `PedidoDTO` e `PedidoMapper`. O mapper trata regras de negócio estritas para *escaping* de caracteres XML, coordenadas de fallback para POIs e formatação decimal no padrão `US Locale`.
* **Camada de Aplicação (`Application`):**
  * `RoteirizacaoUseCase`: Orquestra o fluxo de execução, a sincronização de entidades e a montagem do payload.
  * `LogRoteirizacaoService`: Gerencia os logs de auditoria de forma isolada para integrações com falhas.
* **Camada de Infraestrutura (`Infrastructure`):**
  * **Repository:** Consultas SQL nativas altamente otimizadas que ignoram mapeamentos ORM padrão, garantindo alta performance em esquemas legados.
  * **SOAP Client:** Clientes HTTP customizados para manipulação direta do envelope XML.
  * **Scheduler:** *Cron jobs* automatizados em segundo plano (`@Scheduled`) consumindo as filas de integração pendentes.

---

## 🔄 Ciclo de Vida da Roteirização (Workflow)

O fluxo principal é automatizado e guiado por eventos, seguindo os passos abaixo:

1. **Leitura da Fila (`RoteirizacaoScheduler`):** Executa em intervalos programados para buscar a lista de IDs de carregamentos pendentes no WinThor com base em *flags* de controle (ex.: `IMPORTADOAPI = 'N'`).
2. **Isolamento de Transação:** O `RoteirizacaoUseCase` percorre a fila. Cada carregamento é processado dentro de sua própria transação `@Transactional` para garantir que a falha em um lote não reverta o processamento dos demais.
3. **Extração & Mapeamento de Dados:** Executa uma consulta `SELECT` SQL nativa unindo tabelas do ERP (`PCCARREG`, `PCPEDC`, `PCCLIENT`). Os resultados são mapeados para objetos `PedidoDTO` contendo dados financeiros, dimensionais (peso/volume) e geográficos.
4. **Auto-Recuperação & Validação (*Auto-Healing*):**
   * **Sanitização de Placas:** Garante o formato estrito `XXX-XXXX` via Expressão Regular (Regex).
   * **Pré-sincronização:** Utiliza Java Streams API para filtrar Zonas e Lojas distintas do carregamento, disparando requisições SOAP preventivas para garantir que essas entidades (e o Motorista) existam na plataforma Concept antes do envio principal.
5. **Geração do Payload:** Percorre os DTOs para construir a requisição XML em lote. Mapeia estrategicamente o Nome do Cliente para a tag `<descricao>` (melhor visualização na interface da Concept) e o endereço real para a tag `<endereco>` (geocodificação precisa).
6. **Disparo do Motor Geográfico:** Envia a requisição SOAP `importarPedidos`, seguida imediatamente por `roteirizarPedidos`. O motor da Concept processa a geometria e retorna o ID da Rota/Viagem.
7. **Commit no ERP:** Executa um `UPDATE` SQL nativo no WinThor, alterando a *flag* de status do carregamento para indicar sucesso (`IMPORTADOAPI = 'S'`).

---

## ⚙️ Principais Recursos Técnicos

### 1. Resiliência Autônoma e Auditoria (Contornando Limitações do Oracle)
Quando o motor de roteirização da Concept rejeita um carregamento (ex.: Placa inválida ou veículo não cadastrado), o sistema ativa um fluxo de contingência:

* **Propagação `REQUIRES_NEW`:** A exceção é capturada e repassada ao `LogRoteirizacaoService`, que abre uma nova transação de banco de dados totalmente independente. Isso garante a gravação do erro mesmo se a transação principal de roteirização for revertida (*rollback*).
* **Bypass de Triggers e Sequences:** Para contornar restrições de permissão e a ausência de objetos legados (`SEQUENCES`) no Oracle do WinThor, o repositório Java gera a Chave Primária de forma dinâmica usando funções matemáticas nativas do Oracle: `TRUNC(DBMS_RANDOM.VALUE(1, 999999999))`.

### 2. Estratégia de Segurança Nativa
Credenciais sensíveis (senhas de banco, tokens de API) são desacopladas do código-fonte. O projeto utiliza o suporte nativo do Spring Boot para importar arquivos `.env` diretamente no contexto da aplicação, garantindo zero exposição no controle de versão.

### 3. Estratégia de Lote Anti-Timeout
Para garantir estabilidade contra os limites de resposta do servidor SOAP da Concept, o sistema implementa uma estratégia de fracionamento de carregamentos em lotes controlados. Isso mantém o processamento dentro de janelas de tempo HTTP seguras, prevenindo erros de *Read Timeout* em grandes volumes.

### 4. Dashboard Operacional de Alta Densidade
A interface de monitoramento adota a estratégia assíncrona *Monitor-then-Poll*. Ela carrega os metadados dos carregamentos do Oracle instantaneamente e atualiza o progresso das entregas em segundo plano, permitindo a gestão da frota sem gargalos no servidor.

---

## 💾 Estrutura da Tabela de Auditoria

Caso precise implantar ou validar manualmente a tabela de logs no Oracle, utilize o DDL abaixo:

```sql
CREATE TABLE COMPREFACIL.CF_LOG_ROTEIRIZACAO (
    ID NUMBER(19,0) NOT NULL,
    NUMCAR NUMBER(19,0),
    ERRO VARCHAR2(4000 BYTE),
    HORA_ENVIO TIMESTAMP(6),
    CONSTRAINT SYS_C00115546 PRIMARY KEY (ID)
);
```
🔧 Instalação e Configuração
1. Configuração do Ambiente
Crie um arquivo .env na raiz do projeto (no mesmo nível do pom.xml):


```sql
CREATE TABLE COMPREFACIL.CF_LOG_ROTEIRIZACAO (
    ID NUMBER(19,0) NOT NULL,
    NUMCAR NUMBER(19,0),
    ERRO VARCHAR2(4000 BYTE),
    HORA_ENVIO TIMESTAMP(6),
    CONSTRAINT SYS_C00115546 PRIMARY KEY (ID)
);
```
🔧 Instalação e Configuração
1. Configuração do Ambiente
Crie um arquivo .env na raiz do projeto (no mesmo nível do pom.xml):

```bash
DB_URL=jdbc:oracle:thin:@seu_host:1521/WINT
DB_USER=seu_usuario
DB_PASS=sua_senha

CONCEPT_CNPJ=00.000.000/0000-00
CONCEPT_SENHA_CLIENTE=seu_token_cliente
CONCEPT_SENHA_CENTRAL=seu_token_central
CONCEPT_AUTOMATION_INTERVAL=60000
```

2. Compilar e Executar
Limpe os artefatos antigos e inicie a aplicação Spring Boot:
```bash
mvn clean install
mvn spring-boot:run
```
(O servidor Tomcat embutido iniciará na porta 8081 e o agendador automático começará a buscar a fila imediatamente).
📊 Guia de Status Operacional (Dashboard)
A interface exibe as linhas coloridas de acordo com o status em tempo real das rotas e entregas:

🟢 Concluído (Verde): 100% das entregas realizadas com sucesso.

🟡 Em Andamento (Amarelo): Entregas iniciadas, mas ainda existem pontos pendentes.

🔴 Não Iniciado (Rosa): Roteirização integrada com sucesso, mas nenhuma atividade de entrega foi registrada até o momento.