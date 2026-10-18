# Versão do Projeto

Este arquivo é a fonte de verdade da versão do projeto e deve ser mantido
atualizado pelo agente conforme a seção "Controle de Versão" do `AGENTS.md`.

## Versão Atual

```text
0.0.1
```

A versão começa em `0.0.1` quando este arquivo é criado. O histórico anterior
do repositório não é reprocessado: a contagem vale a partir da criação deste
arquivo.

Este arquivo apenas registra a versão: ele não gera tag nem release
automaticamente. Criar a tag ou a release correspondente no repositório é uma
operação manual, feita pelo usuário.

---

## Padrão

O projeto segue o [Semantic Versioning](https://semver.org/lang/pt-BR/)
(`MAJOR.MINOR.PATCH`):

| Componente | Quando incrementar                                                                 | Efeito nos demais        |
| ---------- | ---------------------------------------------------------------------------------- | ------------------------ |
| `MAJOR`    | Mudança incompatível com versões anteriores (API, contratos, esquema, comportamento) | Zera `MINOR` e `PATCH`   |
| `MINOR`    | Nova funcionalidade compatível com versões anteriores                              | Zera `PATCH`             |
| `PATCH`    | Todo merge de Pull Request ou commit direto na branch principal                     | —                        |

Regras:

- Cada merge de PR ou commit direto incrementa **exatamente um** componente.
- Se a alteração justificar `MINOR` ou `MAJOR`, incrementa-se esse componente
  em vez do `PATCH` (não os dois).
- Todo incremento de `MINOR` ou `MAJOR` deve ser registrado no histórico abaixo,
  com a justificativa.
- Incrementos de `PATCH` não precisam de registro no histórico.

---

## Histórico de MINOR e MAJOR

Registre do mais recente para o mais antigo.

| Versão  | Data       | Tipo  | Justificativa                |
| ------- | ---------- | ----- | ---------------------------- |
