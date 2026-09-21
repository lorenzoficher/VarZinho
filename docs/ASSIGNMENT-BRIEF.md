# Trabalho 1 - Projeto Java Orientado a Objetos com Tema Livre

## Contexto

Parabéns! Você foi contratado para desenvolver o núcleo de um sistema em Java.

O **tema do sistema é livre**: você pode escolher um domínio de sua preferência, como biblioteca, clínica, escola, loja, campeonato, hotel, transporte, streaming, eventos, jogos, finanças pessoais ou outro contexto que considere interessante.

O projeto deve possuir uma **interface gráfica mínima** e precisa demonstrar que você sabe aplicar os principais conceitos de Java e Programação Orientada a Objetos estudados até aqui. O domínio escolhido deve ser suficientemente completo para permitir o uso coerente desses conceitos. Não basta criar classes desconectadas apenas para marcar itens do checklist.

> **Exemplo de tema:** a *UrbanXP* é uma plataforma de experiências urbanas que comercializa ingressos para shows, passeios turísticos e workshops. Ao longo deste enunciado, ela será usada apenas para exemplificar como os requisitos podem ser implementados. **Você não é obrigado a desenvolver a UrbanXP.**

## 1. Escolha e descrição do tema

Antes de implementar, escreva uma descrição curta do sistema contendo:

- o problema que o sistema resolve;
- os principais usuários;
- as entidades mais importantes;
- as operações que poderão ser realizadas pela interface gráfica;
- as regras de negócio que diferenciam o seu sistema de um simples cadastro.

### Exemplo: UrbanXP

A UrbanXP gerencia experiências urbanas, como shows, passeios turísticos e workshops. Um atendente pode cadastrar experiências, selecionar o perfil de um cliente, efetuar uma reserva e emitir um ingresso. Cada experiência possui título, descrição, data e hora, capacidade máxima e preço base. O preço final depende do perfil do cliente e da política de desconto aplicável.

## 2. Requisitos gerais do projeto

Adapte os requisitos a seguir ao tema escolhido.

### 2.1 Classes, objetos, atributos e métodos

Modele as principais entidades do domínio como classes Java. Cada classe deve possuir atributos coerentes e métodos que representem comportamentos do próprio objeto. Evite criar classes que contenham apenas dados ou concentrar toda a lógica no método `main`.

**Exemplo UrbanXP:** as classes podem incluir `Experiencia`, `Show`, `PasseioTuristico`, `Workshop`, `Cliente` e `Ingresso`. Uma experiência pode oferecer métodos como `gerarResumo()` e `possuiVagas()`, enquanto um ingresso pode oferecer `calcularValorFinal()` ou `confirmarPagamento()`.

### 2.2 Encapsulamento e modificadores de acesso

Proteja os detalhes internos das classes contra acessos externos indevidos. Os atributos não devem ser manipulados diretamente por outras classes. Utilize modificadores de acesso e exponha apenas as operações realmente necessárias.

**Exemplo UrbanXP:** o atributo `capacidadeMaxima` de uma experiência pode ser privado. A quantidade de vagas deve ser alterada somente por operações controladas, como `reservarVaga()`, que impede reservas quando o evento está lotado.

### 2.3 Construtores, `this` e objetos válidos

Os dados essenciais de uma entidade devem ser informados no momento da construção do objeto, evitando objetos incompletos. Utilize `this` quando for necessário diferenciar os atributos do objeto dos parâmetros recebidos.

**Exemplo UrbanXP:** uma experiência somente pode ser criada se possuir título, data e hora, capacidade máxima e preço base. No construtor, uma atribuição como `this.titulo = titulo` diferencia o atributo do parâmetro.

### 2.4 Pacotes e organização do código

Distribua as classes em pacotes separados por responsabilidade. A organização exata pode variar conforme o tema, mas deve distinguir pelo menos:

- classes que representam o domínio ou modelo;
- classes que implementam regras de negócio ou serviços;
- classes responsáveis pela interface gráfica e pela interação com o usuário.

**Exemplo UrbanXP:** podem ser utilizados os pacotes `urbanxp.modelo`, `urbanxp.servico` e `urbanxp.interfacegrafica`.

### 2.5 Abstração, herança, `super` e polimorfismo

Identifique entidades que compartilham dados e comportamentos. Quando fizer sentido no domínio escolhido, represente o conceito mais geral por uma superclasse e os tipos específicos por subclasses. As subclasses devem reaproveitar a inicialização da classe base e fornecer comportamentos especializados. O sistema deve manipular diferentes objetos de maneira padronizada por meio do tipo mais geral.

**Exemplo UrbanXP:** `Experiencia` pode ser uma classe abstrata, enquanto `Show`, `PasseioTuristico` e `Workshop` são subclasses. Seus construtores chamam `super(...)` para inicializar título, data, capacidade e preço. Cada subtipo sobrescreve `gerarResumo()`. Uma lista do tipo `List<Experiencia>` pode reunir todos eles e invocar o mesmo método polimorficamente.

### 2.6 Interface e comportamentos substituíveis

Quando uma regra puder variar sem alterar as entidades principais, represente-a por meio de uma interface. Isso deve permitir a inclusão futura de novas implementações.

**Exemplo UrbanXP:** a interface `PoliticaDesconto` pode declarar `calcular(double precoBase)`. As classes `SemDesconto`, `DescontoEstudante` e `DescontoPremium` implementam políticas diferentes. Assim, novas políticas podem ser adicionadas sem modificar `Experiencia` ou `Ingresso`.

### 2.7 Enumerações

Utilize `enum` quando um dado puder assumir somente um conjunto pequeno e previamente conhecido de valores. Isso deve evitar estados inválidos e substituir textos ou números soltos pelo código.

**Exemplo UrbanXP:** `StatusIngresso` pode possuir os valores `RESERVADO`, `PAGO` e `CANCELADO`. Dessa forma, não é possível atribuir ao ingresso um estado inexistente, como `"EM_ANALISE_X"`.

### 2.8 Tratamento de exceções

O sistema deve tratar situações excepcionais sem encerrar inesperadamente ou deixar os objetos em estado inconsistente. Além das exceções fornecidas pela linguagem, crie **exceções próprias do domínio**, estendendo `Exception` ou uma de suas subclasses. Lance essas exceções nos pontos adequados e trate-as na camada responsável pela interação com o usuário, apresentando mensagens claras na interface gráfica.

**Exemplo UrbanXP:** podem ser criadas as exceções `ExperienciaLotadaException`, `DataInvalidaException` e `TransicaoStatusInvalidaException`. O método `reservarVaga()` pode lançar `ExperienciaLotadaException` quando não houver vagas, e a interface deve capturar essa exceção para informar o problema ao usuário.

### 2.9 Interface gráfica mínima

O sistema deve possuir uma interface gráfica mínima desenvolvida em Java, preferencialmente com Swing. O usuário deve conseguir fornecer dados, acionar pelo menos uma operação do sistema e visualizar o resultado correspondente. A interface não precisa ser sofisticada, mas deve ser funcional, compreensível e integrada às classes e regras de negócio do projeto.

**Exemplo UrbanXP:** uma janela pode permitir o cadastro de uma experiência por meio de campos de texto, caixas de seleção e botões. Outra tela ou painel pode permitir a escolha do perfil do cliente, a reserva de uma vaga e a exibição do ingresso com o valor final. Mensagens de erro e confirmação podem ser apresentadas com `JOptionPane`.

### 2.10 Datas e horários

Caso o domínio envolva datas ou horários, utilize a API moderna `java.time`. Não utilize as classes legadas `Date` e `Calendar`.

**Exemplo UrbanXP:** `LocalDateTime` representa o momento de realização de uma experiência; `LocalDateTime.now()` ajuda a validar se ela ocorrerá no futuro; e `Duration` ou `Period` pode apoiar regras de antecedência.

### 2.11 Regra adicional do domínio

Implemente pelo menos uma regra de negócio que exija validação ou alteração controlada de estado. A regra deve ser coerente com o tema escolhido e não pode se limitar a cadastrar, listar ou excluir dados.

**Exemplo UrbanXP:** uma reserva só pode ser criada se houver vagas, a experiência estiver no futuro e o ingresso não estiver cancelado. O pagamento altera o status de `RESERVADO` para `PAGO`, mas não pode reativar um ingresso cancelado.

## 3. Checklist de avaliação

Use o checklist abaixo para revisar o projeto. Na avaliação, não será considerada apenas a presença de uma palavra-chave: será observado se o conceito foi empregado corretamente e se faz sentido no domínio escolhido.

| Conceito | O que deve ser demonstrado | Como a UrbanXP poderia cumprir o item |
|---|---|---|
| [Classes e objetos Java](https://www.w3schools.com/java/java_classes.asp) | Classes coerentes e instanciação de objetos do domínio | Instanciar `Show`, `Cliente` e `Ingresso` |
| [Atributos de classe Java](https://www.w3schools.com/java/java_class_attributes.asp) | Estado relevante armazenado nos objetos | `titulo`, `dataHora`, `capacidadeMaxima` e `precoBase` em `Experiencia` |
| [Métodos de classe Java](https://www.w3schools.com/java/java_class_methods.asp) | Comportamentos associados às classes corretas | `possuiVagas()`, `reservarVaga()` e `gerarResumo()` |
| [Desafio da aula de Java](https://www.w3schools.com/java/java_challenges_class.asp) | Aplicação prática dos fundamentos vistos em aula | Resolver o desafio indicado e reaproveitar o raciocínio na modelagem do projeto |
| [Construtores em Java](https://www.w3schools.com/java/java_constructors.asp) | Inicialização obrigatória e criação de objetos válidos | Exigir título, data, capacidade e preço no construtor de `Experiencia` |
| [Palavra-chave `this`](https://www.w3schools.com/java/java_this.asp) | Referência inequívoca ao objeto atual | Usar `this.titulo = titulo` no construtor |
| [Modificadores Java](https://www.w3schools.com/java/java_modifiers.asp) | Visibilidade adequada de classes, atributos e métodos | Manter atributos `private` e expor somente operações públicas necessárias |
| [Encapsulamento em Java](https://www.w3schools.com/java/java_encapsulation.asp) | Proteção do estado e alterações por métodos controlados | Impedir alteração direta das vagas e usar `reservarVaga()` |
| [Pacotes Java / API](https://www.w3schools.com/java/java_packages.asp) | Organização do código por responsabilidade | Separar `modelo`, `servico` e `interfacegrafica` |
| [Herança em Java](https://www.w3schools.com/java/java_inheritance.asp) | Especialização coerente de um conceito geral | `Show`, `PasseioTuristico` e `Workshop` estendem `Experiencia` |
| [Polimorfismo em Java](https://www.w3schools.com/java/java_polymorphism.asp) | Uso do tipo geral para manipular objetos diferentes | Percorrer `List<Experiencia>` e chamar `gerarResumo()` |
| [Palavra-chave `super`](https://www.w3schools.com/java/java_super.asp) | Reaproveitamento da inicialização ou comportamento da superclasse | Construtores das subclasses chamam `super(...)` |
| [Abstração em Java](https://www.w3schools.com/java/java_abstract.asp) | Representação de um conceito geral que não deve ser instanciado diretamente | Declarar `Experiencia` como abstrata |
| [Interfaces em Java](https://www.w3schools.com/java/java_interface.asp) | Contrato com implementações substituíveis | `PoliticaDesconto` implementada por diferentes políticas |
| [Enum em Java](https://www.w3schools.com/java/java_enums.asp) | Conjunto fechado de valores válidos | `StatusIngresso { RESERVADO, PAGO, CANCELADO }` |
| [Tratamento de exceções em Java](https://www.w3schools.com/java/java_try_catch.asp) | Lançamento e tratamento adequado de erros, incluindo exceções próprias do domínio | Criar e lançar `ExperienciaLotadaException` e tratá-la na interface gráfica |
| [Interface gráfica com Swing](https://docs.oracle.com/javase/tutorial/uiswing/) | Interface mínima funcional para entrada, execução de operações e apresentação de resultados | Usar `JFrame`, componentes de formulário, botões e `JOptionPane` para cadastro e reserva |
| [Data e hora em Java](https://www.w3schools.com/java/java_date.asp) | Uso da API `java.time` | Usar `LocalDateTime` na agenda das experiências |

## 4. O que deve ser entregue

Entregue um repositório contendo:

1. o código-fonte Java organizado em pacotes;
2. um arquivo `README.md` com:
   - nome e descrição do sistema;
   - instruções para executar o projeto;
   - identificação das classes principais;
   - explicação breve das regras de negócio;
   - checklist preenchido, indicando onde cada conceito foi utilizado ou justificando por que não se aplica;
3. capturas de tela ou uma breve demonstração do funcionamento da interface gráfica;
4. histórico de commits que permita acompanhar a evolução do trabalho.

## 5. Orientação final

O checklist **não significa que todos os conceitos precisam ser utilizados obrigatoriamente**. Ele significa que você deve saber identificar quando cada conceito é adequado e, quando houver aplicação coerente, implementá-lo corretamente.

Não force herança, classes internas ou qualquer outro recurso apenas para aumentar a quantidade de itens marcados. Uma solução menor e bem justificada é melhor do que uma solução complexa sem necessidade.

Esse é o checklist que será utilizado pelo professor na avaliação do Trabalho 1. Esses também são os conceitos que você precisa compreender para a Prova 1.

> **Dica:** os nomes dos conceitos no checklist são links para páginas da W3Schools com explicações e exemplos.
