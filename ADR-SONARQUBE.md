# ADR: Implementação de SonarQube para Análise Estática de Código - Lambda Authorizer

**Número do ADR:** LAMBDA-001  
**Título:** Implementação de SonarQube para Análise Estática de Código  
**Data:** 2026-08-26  
**Responsável:** Arquiteto do Projeto  
**Status:** Aceito

## Contexto
O projeto Lambda Authorizer é um componente crítico da arquitetura do Tech Challenge SOAT FIAP, responsável pela autenticação de usuários. Como componente de segurança, exige alta qualidade de código e manutenibilidade. Atualmente, o projeto possui logs estruturados implementados, mas falta uma ferramenta sistemática para análise estática de código que identifique:

- Code smells e dívida técnica
- Vulnerabilidades de segurança (crítico para componente de autenticação)
- Bugs potenciais
- Complexidade ciclomática excessiva
- Duplicação de código
- Conformidade com padrões de qualidade

A implementação de análise estática contínua é essencial para manter a qualidade do código ao longo do desenvolvimento e garantir que novos commits sigam os padrões estabelecidos, especialmente considerando que este é um componente de segurança.

## Decisão
Foi decidido implementar o SonarQube como ferramenta principal de análise estática de código com as seguintes configurações:

- **SonarQube Server:** Instalação local via Docker Compose (compartilhada com projeto principal)
- **SonarScanner:** Integração com Maven para análise manual sob demanda
- **Quality Gates:** Definição de portões de qualidade personalizados para o projeto
- **Métricas obrigatórias:** Cobertura de testes ≥80%, zero vulnerabilidades críticas, code smells controlados
- **Configuração otimizada para Lambda:** Exclusões apropriadas para código específico de Lambda

## Justificativa
A escolha do SonarQube baseia-se nos seguintes fatores:

- **Padrão de mercado:** SonarQube é a ferramenta mais consolidada para análise estática em projetos Java
- **Integração nativa:** Excelente integração com Maven e GitHub Actions
- **Métricas abrangentes:** Cobre segurança, confiabilidade, maintainability e cobertura
- **Quality Gates:** Permite definir regras de qualidade específicas para o projeto
- **Dashboard centralizado:** Visualização clara da evolução da qualidade do código
- **Suporte a múltiplas linguagens:** Prepara o projeto para evoluções futuras
- **Comunidade ativa:** Amplo suporte e documentação disponível
- **Alinhamento com projeto principal:** Consistência na abordagem de qualidade entre componentes
- **Performance:** Análise eficiente sem impacto significativo no tempo de build

## Alternativas Consideradas

### Checkstyle + PMD + SpotBugs
- **Vantagem:** Ferramentas específicas e leves para ecossistema Java
- **Desvantagem:** Requer configuração múltipla, dashboard fragmentado, menos integrado
- **Decisão:** Descartada pela complexidade de configuração e falta de visão unificada

### GitHub CodeQL
- **Vantagem:** Excelente para segurança, integração nativa com GitHub
- **Desvantagem:** Foco principal em segurança, menos abrangente para code smells e maintainability
- **Decisão:** Descartada por não cobrir todos os aspectos de qualidade necessários

### Veracode
- **Vantagem:** Ferramenta comercial robusta com análise profunda
- **Desvantagem:** Custo elevado, complexidade desnecessária para projeto acadêmico
- **Decisão:** Descartada por ser overkill para o contexto do projeto

## Consequências
A implementação do SonarQube trará as seguintes consequências:

### Benefícios
- **Qualidade assegurada:** Detecção precoce de problemas de código em componente crítico de segurança
- **Métricas objetivas:** Avaliação quantitativa da qualidade do código
- **Padronização:** Todos os desenvolvedores seguem os mesmos padrões
- **Documentação viva:** Dashboard mostra evolução da qualidade
- **Análise sob demanda:** Execução manual quando necessário para revisões de código
- **Aprendizado:** Equipe aprende melhores práticas de codificação
- **Segurança reforçada:** Detecção de vulnerabilidades em componente de autenticação

### Desafios
- **Curva de aprendizado:** Equipe precisa aprender a usar e interpretar o SonarQube
- **Falso positivos:** Necessidade de configurar regras para minimizar alertas irrelevantes
- **Execução manual:** Requer disciplina da equipe para executar análises regularmente
- **Manutenção:** Requer atualização periódica de regras e configurações

### Impacto no Desenvolvimento
- **Setup inicial:** Configuração do servidor e integração com Maven
- **Revisão de código:** Foco adicional em atender aos critérios do SonarQube
- **Refatoração:** Necessidade de ajustar código existente para atender aos padrões
- **Métricas de sucesso:** Novos KPIs baseados em qualidade de código
- **Análise manual:** Execução de análises sob demanda durante desenvolvimento e revisões

## Configuração Específica para Lambda

Considerando a natureza do projeto Lambda Authorizer, foram definidas configurações específicas:

- **Exclusões:** DTOs, classes de configuração e código gerado são excluídos da análise
- **Foco em segurança:** Prioridade na detecção de vulnerabilidades de segurança
- **Logs estruturados:** Integração com o sistema de logs já implementado

## Referências
- SonarQube Documentation: https://docs.sonarqube.org/
- SonarScanner for Maven: https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-maven/
- Clean Code: Robert C. Martin
- Code Quality Standards: SonarSource Best Practices
- ADR-013 (projeto principal): Implementação de SonarQube para Análise Estática de Código
- Tech Challenge SOAT FIAP Requirements Document
- AWS Lambda Best Practices: https://docs.aws.amazon.com/lambda/latest/dg/best-practices.html
