# Sliced · Diário de Treino

App Android nativo para registrar treinos de musculação: carga e repetições por série, timer de descanso que avisa com a tela apagada e histórico com a evolução de cada exercício. Funciona 100% offline e já vem com a ficha de treino (divisão A–E) carregada.

<p>
  <img src="docs/screenshots/home_light.png" width="200" alt="Tela Treinos">
  <img src="docs/screenshots/workout_a_light.png" width="200" alt="Treino em andamento">
  <img src="docs/screenshots/workout_rest.png" width="200" alt="Timer de descanso">
  <img src="docs/screenshots/history_light.png" width="200" alt="Histórico">
</p>

## Funcionalidades

- **Treinos**: semana atual (Seg–Dom) com o treino feito em cada dia, a ficha A–E com o **próximo** treino já aberto.
- **Treino em andamento**: tabela de séries com o que você fez da **última vez**; ao tocar ✓, os campos vazios são preenchidos com os valores anteriores e o descanso começa. A etiqueta **Subir carga** aparece quando todas as séries da última vez bateram o topo da faixa (dupla progressão).
- **Timer de descanso**: barra fixa com −15 / +15 / Pular. Com o celular bloqueado, uma notificação mostra a contagem e toca/vibra no fim.
- **Nada se perde**: o treino em andamento é salvo a cada toque e sobrevive a fechar o app ou reiniciar o celular.
- **Histórico**: estatísticas, gráfico da maior carga por treino e detalhes de cada sessão.
- Tema claro e escuro, textos em português, alvos de toque de 48 dp.

## Instalar no celular

1. Baixe o APK no artefato `diario-de-treino-apk` do GitHub Actions ou gere com `./gradlew :app:assembleRelease` (sai em `app/build/outputs/apk/release/`).
2. No celular, abra o arquivo e permita **Instalar apps desconhecidos** para o app que abriu o APK (Arquivos, Chrome...).
3. Na primeira vez que começar um treino, aceite a permissão de **notificações**: é ela que avisa o fim do descanso com a tela apagada.

O APK é assinado com a chave `app/debug.keystore`, que está no repositório. Assim, qualquer build novo instala **por cima** do anterior sem apagar o histórico. Essa chave não é secreta; para publicar na Play Store, use outra.

## Arquitetura

```
app/src/main/java/com/pedrohmsouza/sliced/
├── model/        Ficha (Treino, PlannedExercise), sessões e rascunho. Kotlin puro.
├── domain/       Regras de negócio (WorkoutRules) e formatação pt-BR (Fmt). Funções puras.
├── data/         Repositórios (interfaces) + Room (entidades, DAOs, banco).
├── timer/        RestTimer, alarme exato, notificações, som e vibração.
├── ui/           Telas Compose + ViewModels (home, workout, history), tema e componentes.
├── AppContainer  Injeção de dependência manual.
└── SlicedApp     Application: cria o container e avisa o timer quando o app vai para o fundo.
```

**MVVM com fluxo unidirecional.** Cada tela tem um `ViewModel` que expõe um único `StateFlow<UiState>`. A tela só desenha o estado e chama funções do ViewModel; ela não sabe de onde vêm os dados. O ViewModel sobrevive à rotação da tela.

**Regras em funções puras.** "Próximo treino", "Anterior", "Subir carga" etc. ficam em `WorkoutRules`: recebem dados e devolvem dados, sem Android, sem banco e sem relógio implícito. Por isso são testadas com JUnit simples (`WorkoutRulesTest`).

**Repositórios como interfaces.** Os ViewModels conhecem `SessionRepository` e `DraftRepository`, nunca o Room direto. Os testes usam versões em memória; uma sincronização na nuvem no futuro entraria aqui sem mexer nas telas. A ficha também é uma interface (`ProgramSource`): hoje é fixa (`StaticProgram`), amanhã pode vir do banco.

**Room.** O histórico fica em três tabelas: `sessions` → `session_exercises` → `session_sets`, com `ON DELETE CASCADE`. Uma sessão é gravada numa transação: ou entra inteira, ou não entra. O treino em andamento é uma linha só com o estado em JSON. Ele muda a cada toque e só vira histórico ao finalizar, então não vale ter tabelas próprias. O esquema de cada versão fica em `app/schemas/` para escrever migrações quando o banco mudar.

**Injeção de dependência manual (sem Hilt).** O app tem um banco, dois repositórios e um timer, então `AppContainer` monta tudo à mão e deixa explícito quem cria o quê. O Hilt faz o mesmo gerando código a partir de anotações, e compensa quando o grafo cresce. Migrar é direto: cada `val` do container vira um `@Provides`.

**Timer de descanso.** O timer guarda o *horário de término*, não um contador. Um contador atrasa quando o Android suspende o app; o horário de término está sempre certo. Com a tela apagada:
- um alarme exato (`AlarmManager.setAlarmClock`) acorda o app no fim, mesmo em Doze e com a economia de bateria da Samsung. Efeito colateral: o ícone de alarme aparece na barra de status durante o descanso;
- uma notificação mostra a contagem regressiva, desenhada pelo próprio sistema (`setChronometerCountDown`), com botões +15 s e Pular. No Android 16 ela pede para virar *Live Update* (Now Bar no One UI 8);
- no fim, uma notificação de alta prioridade toca e vibra. Com o app aberto, o bipe e a vibração saem direto.

**Single module.** Para um app deste tamanho, um módulo `:app` organizado por pacotes é mais simples de entender e compila rápido. A separação por pacotes já deixa o caminho aberto para módulos (`core:data`, `feature:workout`...) se o projeto crescer.

## Testes

```bash
./gradlew :app:testDebugUnitTest
```

Todos os testes rodam na JVM, sem emulador:

- `WorkoutRulesTest`, `FmtTest`: cada regra de negócio da especificação.
- `RestTimerTest`: o timer com tempo virtual (fim, −15/+15, alarme duplicado, segundo plano).
- `WorkoutViewModelTest`: o fluxo marcar série → salvar → limpar rascunho.
- `RoomRepositoriesTest`: o Room de verdade (SQLite em memória via Robolectric).
- `*ScreenshotTest`: renderizam as telas com Roborazzi. Para gerar as imagens em `app/screenshots/`:

```bash
./gradlew :app:testDebugUnitTest -Proborazzi.test.record=true
```

## Stack

Kotlin · Jetpack Compose + Material 3 · Navigation Compose (rotas type-safe) · Room · kotlinx.serialization · Coroutines/Flow · AGP 9 · minSdk 26 · targetSdk 36 (Android 16) · JUnit + Robolectric + Roborazzi.

## Licenças

Fontes Barlow e Barlow Condensed: SIL Open Font License (`docs/barlow-OFL.txt`).
