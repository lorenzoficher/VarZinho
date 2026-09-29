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
encapsulamento, herança, polimorfismo e tratamento de exceções. O sistema é
operado por uma interface gráfica mínima em Swing, exigida pelo enunciado, e
verificado por testes automatizados.

Não há captura real de vídeo: o `VideoClip` guarda apenas os metadados do
arquivo — caminho, duração, resolução e tamanho. Nenhum quadro é decodificado.

## Tecnologias

- Java 17 (LTS)
- Maven
- JUnit 5

## Instalando o Maven

Você precisa do JDK 17 (ou superior) e do Maven. O build para com uma mensagem
clara se o JDK for anterior ao 17.

| Sistema | Comando |
|---|---|
| Windows | `choco install maven` num terminal de administrador — depois **feche e reabra o terminal**. Sem o Chocolatey: baixe o zip em <https://maven.apache.org/download.cgi>, extraia e adicione a pasta `bin` ao `PATH` (o pacote `Apache.Maven` não existe no `winget`) |
| macOS | `brew install maven` |
| Linux (Debian/Ubuntu) | `sudo apt install maven` |

Para conferir a instalação:

```bash
java -version   # deve mostrar 17 ou superior
mvn -v          # deve mostrar a versão do Maven e o mesmo JDK
```

## Como executar

```bash
mvn compile
mvn test
mvn exec:java
```

## Documentação

| Documento | Conteúdo |
|---|---|
| [docs/ASSIGNMENT-BRIEF.md](docs/ASSIGNMENT-BRIEF.md) | O enunciado do trabalho — é ele que manda quando houver divergência |
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

Lorenzo Ficher, Lara Rios, Rafael Lopes, Artur Kraemer e Inaurrara Flores.

A divisão do trabalho é por agregado do domínio — cada pessoa é dona de um
conjunto de classes, sem dependência cruzada. Ver
[docs/WORKFLOW.md](docs/WORKFLOW.md).
