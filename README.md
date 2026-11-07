# Subtitle Overlay TV — protótipo Android TV (v0.1)

Aplicativo **experimental** para apresentar legendas SRT sobre outro app na Google TV, **sem modificar ou acessar a UniTV**. Pensado para a Philips Ambilight 55PUG7908/78.

## Estado desta entrega

- Código-fonte de protótipo Android/Kotlin; **não é um APK pronto**.
- Não foi compilado nem testado em televisão real neste ambiente (Android SDK/Gradle indisponíveis).
- NÃO gera legendas com IA; importa SRT obtidos legitimamente.
- NÃO abre nem controla o vídeo da UniTV. A sincronização é manual e **pode se perder ao pausar, avançar ou alternar aplicativos**.
- Algumas TVs ou apps podem bloquear `TYPE_APPLICATION_OVERLAY`; é necessário validar a exibição sobre a UniTV.

## Funcionalidades

- Seletor de arquivos Android para `.srt`.
- Leitura SRT com timestamps e falas em múltiplas linhas.
- Janela transparente, sem foco/toque, no rodapé (não captura o controle remoto da UniTV).
- Início após 5/10/15/20/30/45 segundos.
- Adiantamento/atraso de 1 e 5 segundos via tela principal; notificação também permite ajustes de 1 s, pausa e parada.
- Serviço em primeiro plano com notificação de reprodução; fecha a sobreposição ao parar.

## Compilar usando Android Studio

1. Instale Android Studio (e Android SDK Platform 35).
2. Abra a pasta `subtitle-overlay-tv` como projeto Gradle. Android Studio baixará os plugins Gradle especificados.
3. Execute **Build > Build APK(s)**. O APK debug costuma aparecer em `app/build/outputs/apk/debug/app-debug.apk`.
4. Transfira o APK à Google TV por meio do método de instalação de APK que você já usa, ou com `adb install -r app-debug.apk` caso ADB esteja configurado. A instalação exige habilitar a origem apropriada na TV.
5. Instale/abra um seletor de arquivos Android, se a interface de seleção da TV não mostrar os arquivos baixados.

## Usar

1. Obtenha a legenda **do episódio correto e da edição correta** (exemplo: Shameless UK S01E02 — e não a série americana). Se vier em ZIP, extraia o `.srt` antes de abrir no app. Coloque o arquivo no armazenamento acessível à TV.
2. Abra **Subtitle Overlay TV**, escolha `.srt` e permita **Exibir sobre outros apps**. No Android 11+, o link pode abrir a lista geral de apps em Configurações; selecione o app manualmente.
3. Abra a UniTV e coloque o episódio no início. Volte ao Subtitle Overlay TV, selecione atraso inicial, pressione **Iniciar legendas** e retorne à UniTV para iniciar/retomar a reprodução aproximadamente no fim da contagem.
4. Se ficar fora de sincronismo, retorne ao Subtitle Overlay TV e ajuste + ou - 1/5 s (a alternância de apps pode pausar o vídeo). Para evitar a alternância, pode tentar os comandos da notificação se forem acessíveis na TV.
5. Pressione **Parar legendas** ao finalizar. Também pode usar a ação Parar da notificação.

**Atenção sobre temporização:** a contagem é baseada no relógio do dispositivo e não recebe informações da reprodução. Pode ser difícil conseguir sincronização estável na UniTV; este é o principal ponto a validar no protótipo.

## Desenvolvimento futuro

- Calibrar deslocamento por timestamp do vídeo, sem depender de countdown.
- Modo controle remoto em segundo dispositivo (celular) para ajustes durante a exibição, com consentimento de rede local.
- Detecção assistida de pausas, quando tecnicamente permitida.
- Seleção de fonte/tamanho/posição, prévia de cue e favoritos por episódio.
- Importação de `.vtt` e configuração por episódio.

## Referências oficiais

- Android `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`: https://developer.android.com/reference/android/view/WindowManager.LayoutParams
- Android `Settings.canDrawOverlays`: https://developer.android.com/reference/android/provider/Settings#canDrawOverlays(android.content.Context)
- Restrições Android 11: https://developer.android.com/about/versions/11/privacy/permissions

## Privacidade

Sem anúncios, trackers, login ou acesso à internet. Lê somente o arquivo escolhido pelo usuário. Não captura tela, áudio nem dados de outros aplicativos.
