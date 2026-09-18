# Nexauren File

Aplicação Android de gestão de ficheiros da Nexauren, criada de raiz com identidade própria.

## Primeira versão

- Navegação por pastas com Storage Access Framework
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
- Interface Android moderna e clara
- Funcionamento local/offline das operações de ficheiros
- Verificação de atualização dentro de Definições
- Download do APK de uma GitHub Release
- Instalação através do instalador oficial do Android

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
