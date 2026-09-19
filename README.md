# CallBlocker

Bloqueador de chamadas para Android 11 (API 30). Rejeita silenciosamente ligações
de números que não estão na agenda de contatos, usando a API oficial
`android.telecom.CallScreeningService` (sem root, sem Gradle).

## Estrutura

```
AndroidManifest.xml          Manifesto do app (package com.nospam.blocker)
src/                         Código-fonte Java (MainActivity, BlockingService)
build.sh                     Script de build manual (aapt2 + javac + d8 + apksigner)
build/                       Artefatos gerados (APK assinado, keystore, classes)
sdk/                         Android SDK mínimo baixado localmente pelo script
docs/                        Especificação de design
```

## Requisitos

Build feito em **Arch Linux**. Dependências:

- `bash`
- `curl`
- `unzip`, `zip`
- JDK 17 (ex.: `jdk17-openjdk`) — fornece `javac`, `keytool`

```bash
sudo pacman -S jdk17-openjdk unzip zip
```

O script baixa automaticamente, na primeira execução, o `build-tools 30.0.3` e a
`platform-30` para a pasta local `sdk/` (Android SDK mínimo, sem Android Studio).

## Compilar

```bash
./build.sh
```

O script faz: `aapt2 link` → `javac` → `d8` → empacota `classes.dex` → `zipalign`
→ assina com `apksigner`.

APK final: **`build/CallBlocker.apk`**.

## Recompilar

Basta rodar o mesmo comando novamente. O script é idempotente:

- O SDK (`sdk/`) e o keystore (`build/keystore.jks`) só são baixados/gerados na
  primeira execução.
- Recompila do zero o código-fonte em `src/` e sobrescreve
  `build/CallBlocker.apk`.

Se quiser um build totalmente limpo:

```bash
rm -rf build && ./build.sh
```

(opcional, para também rebaixar o SDK: `rm -rf sdk build`)

## Instalar no aparelho

Com depuração USB ativada e `adb` instalado:

```bash
adb install -r build/CallBlocker.apk
```

Ou copie o APK para o celular e instale manualmente (é preciso permitir
"instalar apps de fontes desconhecidas").

## Assinatura

A assinatura é feita com um keystore local gerado na primeira execução:
`build/keystore.jks` (alias `callblocker`, senha `callblocker`). Para publicar
versões futuras, mantenha esse keystore — sem ele, o app não é atualizável sobre
a mesma instalação.

## Uso

1. Instale e abra o app.
2. Conceda "Permissão de contatos" (`READ_CONTACTS`).
3. Toque em "Ativar triagem de chamadas" e confirme no diálogo do sistema.
4. Ligue o bloqueio no switch.

Observações:

- Samsung (One UI 3.x): se chamadas fora da agenda ainda tocarem, desative a
  "Proteção de identificador de chamadas" no app Telefone.
- Números privados/ocultos (sem número) não são bloqueados.