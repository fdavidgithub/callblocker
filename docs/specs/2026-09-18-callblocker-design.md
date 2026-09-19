# CallBlocker — Design

Bloqueador de chamadas para Android 11 (API 30), dispositivo Samsung (One UI 3.x).

## Objetivo

Rejeitar silenciosamente ligações de números que NÃO estão na agenda de contatos.
Requisitos do usuário: APK mínimo (~40-50 KB), execução rápida, instalação manual,
nada além do APK instalado no celular, build feito em Arch Linux.

## Abordagem

- Linguagem: Java puro (sem Kotlin/AndroidX).
- Build: manual com aapt2 + javac + d8 + zipalign + apksigner (sem Gradle),
  SDK mínimo baixado localmente por script para `sdk/`.
- Técnica de bloqueio: API oficial `android.telecom.CallScreeningService`
  (única forma confiável e sem root no Android 11).
- `minSdk = 30`, `targetSdk = 30`.

## Componentes

1. `MainActivity` — tela única programática (sem XML de layout):
   - Switch liga/desliga bloqueio (SharedPreferences `prefs.enabled`).
   - Botão "Ativar triagem de chamadas" → `RoleManager.createRequestRoleIntent(ROLE_CALL_SCREENING)`.
   - Botão "Permissão de contatos" → `READ_CONTACTS` runtime.
   - `TextView` de status: permissão, papel, estado; aviso Samsung sobre
     "Proteção de identificador de chamadas".
2. `BlockingService extends CallScreeningService`:
   - `onScreenCall` → extrai número do handle (`tel:...`), normaliza dígitos,
     consulta `ContactsContract.CommonDataKinds.Phone` em thread de fundo.
   - Não está na agenda → `respondToCall(reject=true, disallow=true)` (encerra sem tocar).
   - Está na agenda → permite (resposta padrão).
   - Respeita o switch: se desligado, sempre permite.
3. `build.sh` — baixa `build-tools 30.0.3` e `platform-30`, compila, dex, alinha e assina.
   Gera keystore local (senha `callblocker`).

## Manifest

- Permissões: `READ_CONTACTS` (runtime), `CALL_PHONE` (para rejectCall).
- Service: `android:permission="android.permission.BIND_SCREENING_SERVICE"`,
  intent-filter `android.telecom.CallScreeningService`, exported.
- Ícone/tema do framework (`@android:drawable/ic_menu_call`,
  `@android:style/Theme.DeviceDefault.Light`) — sem recursos próprios.

## Correlação de números

- Extrai apenas dígitos de origem e de cada contato.
- Igual se idênticos, ou se um é sufixo do outro com ≥ 8 dígitos
  (cobre +55/DDD/9º dígito). Números com < 4 dígitos (emergência) sempre permitidos.

## Riscos conhecidos

- Samsung One UI 3.x pode não repassar chamadas ao serviço de triagem mesmo com o
  papel ativado; mitigação: aviso na tela + sugerir desativar "Proteção de
  identificador de chamadas" do discador Samsung.
- Números privados/ocultos (sem número) não são bloqueados (permitidos por padrão).

## Teste

- Build local verificado (estrutura APK, assinatura).
- Manual no aparelho: ligar de número fora da agenda → cai sem tocar;
  ligar de número na agenda → toca.