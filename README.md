# VarZinho

> O sistema não filma o lance. Ele lembra dele.

Sistema de captura de lances para ginásios esportivos. As câmeras gravam
continuamente em um buffer circular de 30 segundos; quando acontece um gol ou
uma jogada bonita, o atleta aciona um botão e aquele trecho é salvo como vídeo
permanente. Depois é só pedir o vídeo ao operador, que tem o acervo inteiro.

O foco é futebol, mas o sistema não conhece esporte nem tipo de jogada: serve a
qualquer modalidade que o ginásio jogue.

## A regra central

**O acionamento não inicia a gravação. Ele preserva o que já foi gravado.**

Quando o lance acontece, ele já terminou. Se o botão apenas iniciasse a
gravação, o lance estaria perdido. Por isso a câmera grava de forma contínua
em um buffer circular que retém os últimos 30 segundos e descarta
automaticamente o conteúdo mais antigo.

Essa regra explica quase todas as decisões do projeto — inclusive por que o
lance não registra ninguém: no momento da captura não existe nenhuma camada de
identificação de usuário, apenas um botão sendo apertado. Quem quer o vídeo
procura o operador, que localiza pela quadra e pelo horário.

## Sobre o projeto

Trabalho da disciplina **AL0330 — Programação Orientada a Objetos**
(Engenharia de Software, Unipampa Alegrete, 2026/2).

O objetivo é a **modelagem do domínio** — classes, relacionamentos,
encapsulamento, herança, polimorfismo e tratamento de exceções. Não há captura
real de vídeo nem interface gráfica: o sistema é exercitado por console e por
testes automatizados.

## Tecnologias

- Java 17 (LTS)
- Maven
- JUnit 5

## Como executar

```bash
mvn compile
mvn test
mvn exec:java
```

## Documentação

| Documento | Conteúdo |
|---|---|
| [AGENTS.md](AGENTS.md) | Convenções de código, build e fluxo de trabalho |
| [CONTEXT.md](CONTEXT.md) | Glossário do domínio e decisões tomadas |
| [docs/PRD.md](docs/PRD.md) | O que o sistema faz, e o que não faz |
| [docs/DOMAIN-MODEL.md](docs/DOMAIN-MODEL.md) | Modelo de domínio e diagrama de classes |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Camadas, persistência e exceções |
| [docs/WORKFLOW.md](docs/WORKFLOW.md) | Branches, commits e pull requests |
| [docs/TESTING.md](docs/TESTING.md) | Como escrevemos testes (TDD) |
| [docs/specs/](docs/specs/) | Especificações por agregado |

## Idioma

**Todo o código e a documentação técnica são escritos em inglês.** Este README
é a única exceção, por ser a porta de entrada do repositório.

## Equipe

Projeto desenvolvido por 5 estudantes. A divisão do trabalho é por agregado do
domínio — cada pessoa é dona de um conjunto de classes, sem dependência
cruzada. Ver [docs/WORKFLOW.md](docs/WORKFLOW.md).
