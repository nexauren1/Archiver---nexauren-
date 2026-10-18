# Nexauren File

Aplicação Android de gestão de ficheiros da Nexauren, criada de raiz com identidade própria.

## Primeira versão

- Abertura direta do armazenamento partilhado do dispositivo por defeito
- Permissão especial `MANAGE_EXTERNAL_STORAGE` em Android 11+
- Seletor de pasta específica continua disponível como opção
- Seleção múltipla
- Copiar, mover e colar
- Renomear e eliminar
- Criar pastas
- Ordenar por nome, tamanho, data e tipo
- Pesquisa recursiva
- Favoritos e recentes
- Informações e partilha pelo Android
- Abrir ficheiros com a aplicação compatível instalada
- Criar e extrair ZIP
- Barra de progresso das operações
- Interface Android nativa em Kotlin + Jetpack Compose
- Tema escuro inspirado em gestores de arquivos, com acentos âmbar
- Ícone adaptativo próprio para o ecrã inicial
- Ecrã de explicação e encaminhamento para as permissões do Android
- Funcionamento local/offline das operações de ficheiros
- Verificação de atualização dentro de Definições
- Download do APK de uma GitHub Release
- Instalação através do instalador oficial do Android

## Acesso ao armazenamento

Em Android 11 e superior, o primeiro arranque pede ao utilizador que ative **Permitir gerir todos os ficheiros** nas definições do Android. Depois de concedido o acesso, o explorador abre `/storage/emulated/0` por defeito, sem exigir a seleção manual de uma única pasta. O seletor do Storage Access Framework permanece disponível para selecionar uma pasta específica quando necessário.

O acesso especial não ultrapassa as restrições do Android a diretórios privados de outras aplicações, incluindo grande parte de `Android/data`. A publicação na Google Play também está sujeita à política da loja para `MANAGE_EXTERNAL_STORAGE`.

## Sistema de atualização

A aplicação consulta a GitHub API do próprio projeto e procura a última Release que tenha um asset APK.

Para publicar uma atualização:

1. Aumente versionCode e versionName em app/build.gradle.kts.
2. Crie uma Release/tag como v1.0.1, v1.1.0, etc.
3. O workflow gera app-release.apk e coloca-o na Release.
4. Para atualizar uma versão já instalada, o APK precisa ser assinado com a mesma chave da instalação anterior.

Secrets usados para assinatura persistente no GitHub Actions:

- ANDROID_KEYSTORE_BASE64
- ANDROID_KEYSTORE_PASSWORD
- ANDROID_KEY_ALIAS
- ANDROID_KEY_PASSWORD

Sem os Secrets, o workflow ainda testa a compilação, mas o APK de teste não serve para atualizar uma instalação existente com outra assinatura.

A instalação não é silenciosa: o Android controla a instalação e pode pedir autorização para instalar aplicações desta origem.

## Arquitetura

A base separa armazenamento, operações, arquivos compactados, preferências e atualização. Está preparada para receber TAR/GZIP/7Z, palavras-passe, dividir arquivos, fila de operações, lixeira, análise de armazenamento, armazenamento externo e visualizadores avançados.


## APK Release

Mesmo sem os quatro Secrets de assinatura, o workflow consegue gerar um **APK Release instalável para testes**, usando a chave debug do Android.

Para distribuição pública e para garantir que atualizações futuras instalem por cima da versão anterior, configure uma chave de assinatura Nexauren persistente nos quatro Secrets:

ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD

Sem esses Secrets, o APK é adequado para testes. A assinatura persistente deve ser configurada antes de distribuir uma versão pública com atualização por cima.
