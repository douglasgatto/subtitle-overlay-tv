# Guia sem computador — compilar e baixar diretamente na Philips

Este projeto é código-fonte, **não um APK pronto**. O GitHub Actions gera o APK quando o projeto for enviado a um repositório GitHub. **O código ainda não foi compilado/testado neste ambiente.**

## Preparação pelo iPhone

1. Acesse https://github.com e entre na conta. Crie um repositório **público** vazio chamado `subtitle-overlay-tv`. Público é necessário para o download direto sem login pela TV; o código ficará visível para outras pessoas. Não coloque dados pessoais ou senhas no projeto.
2. No repositório: `Code` → `Codespaces` → `Create codespace on main`. Se não aparecer, use a opção 'Solicitar site para computador' no navegador.
3. Baixe este ZIP e envie-o no Explorer do Codespaces para a pasta raiz. Abra o terminal. Execute os comandos a seguir, um de cada vez:

```sh
unzip SubtitleOverlayTV-projeto-github.zip
cp -a subtitle-overlay-tv/. .
rm -rf subtitle-overlay-tv SubtitleOverlayTV-projeto-github.zip
git add .
git commit -m "Adicionar Subtitle Overlay TV"
git push
```

4. Abra `Actions` no GitHub. A execução **Compilar e publicar APK Subtitle Overlay TV** começa no push. Se não começar, escolha `Run workflow`. Veja se apareceu marca verde. Se falhar, salve a mensagem de erro para corrigirmos.
5. Após o sucesso, acesse `Releases` no repositório; procure **Teste v0.1 - Subtitle Overlay TV**. Ali estará o APK `SubtitleOverlayTV.apk`.
6. Na Philips Google TV, abra o Downloader e digite o endereço HTTPS do arquivo APK (não o link da página Releases):

```
https://github.com/SEU_USUARIO/subtitle-overlay-tv/releases/download/v0.1-test/SubtitleOverlayTV.apk
```

Substitua `SEU_USUARIO` pelo nome de usuário do GitHub. **O endereço só funcionará após a primeira compilação e publicação bem-sucedidas.** O Downloader pode não lidar bem com redirecionamentos do GitHub; nesse caso, usaremos outra forma de transferir o APK pelo celular.

7. Instale autorizando 'Instalar apps desconhecidos' para o Downloader, se solicitado. Abra o Subtitle Overlay TV e verifique a permissão 'Exibir sobre outros apps'.

## Limitações conhecidas

- Primeiro será necessário comprovar que o APK compila e roda. Não temos teste real na sua Philips nem garantia de sobreposição sobre a UniTV.
- O aplicativo requer um arquivo `.srt` próprio, e a seleção de arquivos pode exigir um gerenciador na TV.
- O temporizador de legendas é independente do vídeo: não acompanha pausas e avanços automaticamente.
- GitHub Codespaces e Actions podem ter cotas e exigências específicas da conta.
- O APK de teste é assinado com chave **debug**, impróprio para distribuição pública definitiva.
