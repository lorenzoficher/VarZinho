# Trabalho 2 — Aplicação Java com interface profissional e banco de dados

Desenvolvam uma aplicação Java que aplique **SOLID, Clean Architecture e Clean Code**, com **interface gráfica bonita, funcional e consistente**, **banco de dados** e documentação **JavaDoc**. A interface gráfica é o principal foco de esforço desta etapa.

## Tema, grupos e prazos

* **Tema:** vocês podem continuar no mesmo tema ou escolher outro.
* **Mesmo tema:** quanto aos requisitos do Trabalho 1, preservem o que já foi atendido e implementem ou corrijam as pendências, além de cumprir as novas exigências deste trabalho.
* **Novo tema:** o novo projeto deverá contemplar todos os aprendizados e requisitos exigidos no Trabalho 1, além das novas exigências.
* **Grupos:** podem permanecer os mesmos ou ser reorganizados.
* **Prazos:** todas as mudanças devem respeitar os prazos divulgados na disciplina. Trocar de tema ou de grupo não prorroga os prazos.

**O checklist do Trabalho 1 será verificado novamente no Trabalho 2 e corresponderá a apenas um dos dez critérios de avaliação.**

## Requisitos desta etapa

1. **SOLID:** aplicar responsabilidade única (SRP), aberto/fechado (OCP), substituição de Liskov (LSP), segregação de interfaces (ISP) e inversão de dependência (DIP), com exemplos concretos no projeto.
2. **Clean Architecture:** separar entidades, casos de uso, interface gráfica e detalhes externos, mantendo as dependências direcionadas ao núcleo da aplicação.
3. **Clean Code:** utilizar nomes expressivos, métodos focados, organização consistente e tratamento adequado de erros; eliminar duplicações relevantes.
4. **Interface gráfica:** implementar telas com cuidado visual, boa organização e usabilidade. Podem utilizar JavaFX ou Swing e bibliotecas de apoio.
5. **Banco de dados:** utilizar um SGBD para persistir os dados entre execuções. Implementar operações de cadastro, consulta e alteração/exclusão pertinentes ao tema, integradas à interface; isolar o acesso em repositórios e utilizar consultas parametrizadas.
6. **JavaDoc:** documentar classes, interfaces, construtores e métodos públicos, explicando responsabilidades e contratos; utilizar `@param`, `@return` e `@throws` quando aplicáveis.

## Entrega

Entreguem o código-fonte completo e executável, capturas das telas, documentação JavaDoc gerada em HTML e um README com integrantes, instruções de execução, configuração do banco e exemplos de onde os requisitos foram atendidos. Incluam scripts ou um mecanismo automático de criação do banco. Observem os prazos e o canal de entrega divulgados na disciplina.

## Rubrica de avaliação — 10,0 pontos

|Nº|Critério|O que será observado|Valor|
|-|-|-|-:|
|1|Checklist do Trabalho 1|Atendimento dos requisitos do checklist original no projeto entregue, com correção das pendências, independentemente do tema ou da composição do grupo.|1,0|
|2|SOLID: SRP e OCP|Classes com responsabilidades coesas; demonstração de uma extensão pertinente de comportamento por abstração ou polimorfismo, preservando o código existente.|1,0|
|3|SOLID: LSP, ISP e DIP|Implementações respeitam os contratos dos tipos; interfaces atendem necessidades específicas dos clientes; regras dependem de abstrações e recebem suas implementações externamente.|1,0|
|4|Clean Architecture|Separação efetiva entre entidades, casos de uso, apresentação e detalhes externos; dependências voltadas ao núcleo; regras de negócio utilizáveis sem abrir a interface gráfica.|1,0|
|5|Clean Code|Nomes claros, métodos focados, formatação consistente, ausência de duplicações relevantes e tratamento de erros que preserve informações úteis.|1,0|
|6|Interface: identidade visual|Paleta coerente, tipografia legível, hierarquia visual clara e padronização de botões, campos e demais componentes entre as telas.|1,0|
|7|Interface: layout|Alinhamentos, margens, espaçamentos e agrupamentos consistentes; aproveitamento do espaço; ausência de cortes e sobreposições nos tamanhos de janela suportados.|1,0|
|8|Interface: usabilidade e funcionamento|Fluxos completos pela interface, navegação clara, validação das entradas e feedback de sucesso/erro; uso por teclado com foco visível e operações demoradas sem bloquear a janela.|1,0|
|9|Banco de dados e persistência|Dados preservados após reiniciar a aplicação; operações pertinentes ao tema integradas à interface; acesso isolado em repositórios, consultas parametrizadas e banco reproduzível a partir da entrega.|1,0|
|10|JavaDoc|Documentação útil e coerente com o código, cobrindo responsabilidades, parâmetros, retornos e exceções aplicáveis; HTML gerado e navegável.|1,0|
||**Total**||**10,0**|

**Escala por critério:** 0 = ausente ou incorreto; 0,25 = atendimento inicial; 0,50 = parcial, com falhas relevantes; 0,75 = quase completo, com pequenas falhas; 1,0 = completo e consistente. A avaliação considera o código e o funcionamento demonstrado. Os critérios da interface somam **3,0 pontos**.

## Referências de apoio

* **Trabalho 1:** consultem o enunciado e o checklist originais disponibilizados na disciplina.
* **SOLID:** Robert C. Martin — [SOLID Relevance](https://blog.cleancoder.com/uncle-bob/2020/10/18/Solid-Relevance.html), explicação dos cinco princípios pelo autor.
* **Clean Architecture:** Robert C. Martin — [The Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html), com foco na regra de dependência.
* **Clean Code:** Robert C. Martin — [Clean Code: A Handbook of Agile Software Craftsmanship](https://www.pearson.com/en-us/subject-catalog/p/clean-code-a-handbook-of-agile-software-craftsmanship/P200000009044/9780132350884), especialmente os capítulos sobre nomes, funções, formatação e tratamento de erros.
* **Interface gráfica:** [OpenJFX — documentação oficial](https://openjfx.io/openjfx-docs/); [FlatLaf — temas e personalização para Swing](https://www.formdev.com/flatlaf/); [Material Design — tipografia e hierarquia visual](https://m3.material.io/styles/typography/applying-type).
* **Banco de dados:** Oracle — [JDBC Basics](https://docs.oracle.com/javase/tutorial/jdbc/basics/index.html) e [Using Prepared Statements](https://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html), para conexão, persistência e consultas parametrizadas em Java.
* **JavaDoc:** Oracle — [especificação dos comentários de documentação](https://docs.oracle.com/en/java/javase/25/docs/specs/javadoc/doc-comment-spec.html), incluindo exemplos e tags.

