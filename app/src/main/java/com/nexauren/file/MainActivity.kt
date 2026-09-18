package com.nexauren.file

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Rename
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.nexauren.file.core.ArchiveManager
import com.nexauren.file.core.OperationManager
import com.nexauren.file.core.OperationProgress
import com.nexauren.file.core.OperationType
import com.nexauren.file.core.StorageRepository
import com.nexauren.file.core.UpdateInfo
import com.nexauren.file.core.UpdateManager
import com.nexauren.file.data.AppPreferences
import com.nexauren.file.model.FileItem
import com.nexauren.file.ui.NexaurenFileTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexaurenFileTheme { NexaurenFileApp() }
        }
    }
}

private enum class Screen { HOME, FILES, SETTINGS }
private enum class ClipboardMode { COPY, MOVE }
private enum class SortMode { NAME, SIZE, DATE, TYPE }

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun NexaurenFileApp() {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val storage = remember { StorageRepository(context) }
    val operationManager = remember { OperationManager(storage) }
    val archiveManager = remember { ArchiveManager(storage) }
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf(Screen.HOME) }
    var root by remember { mutableStateOf<DocumentFile?>(null) }
    var current by remember { mutableStateOf<DocumentFile?>(null) }
    val stack = remember { mutableStateListOf<DocumentFile>() }
    var items by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchMode by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var clipboard by remember { mutableStateOf<List<String>>(emptyList()) }
    var clipboardMode by remember { mutableStateOf(ClipboardMode.COPY) }
    var operation by remember { mutableStateOf<OperationProgress?>(null) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<DocumentFile?>(null) }
    var infoTarget by remember { mutableStateOf<DocumentFile?>(null) }
    var folderDialog by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var compressDialog by remember { mutableStateOf(false) }
    var archiveName by remember { mutableStateOf("") }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var downloadingUpdate by remember { mutableStateOf(false) }
    var updateProgress by remember { mutableStateOf(0) }

    val chooseFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            prefs.setRootUri(uri.toString())
            val dir = storage.fromUri(uri.toString())
            root = dir
            current = dir
            stack.clear()
            if (dir != null) stack.add(dir)
            selected = emptySet()
            searchMode = false
            query = ""
            screen = Screen.FILES
            items = dir?.let(storage::list) ?: emptyList()
        }
    }

    fun refresh() {
        val dir = current ?: return
        items = storage.list(dir)
        selected = emptySet()
    }

    fun openSelectedFile(item: FileItem) {
        if (item.isDirectory) {
            stack.add(item.file)
            current = item.file
            prefs.addRecent(item.uri)
            selected = emptySet()
            searchMode = false
            query = ""
            refresh()
            return
        }

        prefs.addRecent(item.uri)
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(item.file.uri, item.file.type ?: "application/octet-stream")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            dialogMessage = "Nenhuma aplicação instalada consegue abrir este tipo de ficheiro."
        }
    }

    fun shareSelected() {
        val uri = selected.singleOrNull() ?: return
        val file = storage.fromUri(uri) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = file.type ?: "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, file.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Partilhar"))
    }

    LaunchedEffect(Unit) {
        prefs.getRootUri()?.let { saved ->
            val dir = storage.fromUri(saved)
            if (dir != null) {
                root = dir
                current = dir
                stack.clear()
                stack.add(dir)
                items = storage.list(dir)
            }
        }
    }

    LaunchedEffect(current, searchMode, query) {
        val baseDir = current ?: return@LaunchedEffect
        if (!searchMode || query.isBlank()) {
            items = storage.list(baseDir)
        } else {
            items = withContext(Dispatchers.IO) {
                searchRecursive(baseDir, query.trim()).take(500)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (screen == Screen.FILES && searchMode) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            placeholder = { Text("Pesquisar ficheiros") },
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                        )
                    } else {
                        Text("Nexauren File", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    if (screen == Screen.FILES && stack.size > 1) {
                        IconButton(onClick = {
                            stack.removeAt(stack.lastIndex)
                            current = stack.lastOrNull()
                            selected = emptySet()
                        }) { Icon(Icons.Default.ArrowBack, "Voltar") }
                    }
                },
                actions = {
                    if (screen == Screen.FILES) {
                        IconButton(onClick = {
                            if (searchMode) {
                                searchMode = false
                                query = ""
                            } else {
                                searchMode = true
                            }
                        }) {
                            Icon(if (searchMode) Icons.Default.Close else Icons.Default.Search, "Pesquisa")
                        }
                        IconButton(onClick = { refresh() }) {
                            Icon(Icons.Default.Refresh, "Atualizar")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = screen == Screen.HOME,
                    onClick = { screen = Screen.HOME },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Início") }
                )
                NavigationBarItem(
                    selected = screen == Screen.FILES,
                    onClick = {
                        if (current == null) chooseFolder.launch(null) else screen = Screen.FILES
                    },
                    icon = { Icon(Icons.Default.Folder, null) },
                    label = { Text("Ficheiros") }
                )
                NavigationBarItem(
                    selected = screen == Screen.SETTINGS,
                    onClick = { screen = Screen.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Definições") }
                )
            }
        }
    ) { padding ->
        Surface(
            Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    hasStorage = root != null,
                    favoriteCount = prefs.getFavorites().size,
                    recentCount = prefs.getRecents().size,
                    onOpen = {
                        if (current == null) chooseFolder.launch(null) else screen = Screen.FILES
                    },
                    onChoose = { chooseFolder.launch(null) },
                    onFavorites = {
                        items = prefs.getFavorites().mapNotNull { uri ->
                            storage.fromUri(uri)?.let { file ->
                                FileItem(
                                    file,
                                    file.name ?: "(sem nome)",
                                    uri,
                                    file.isDirectory,
                                    if (file.isFile) file.length() else 0L,
                                    file.lastModified()
                                )
                            }
                        }
                        screen = Screen.FILES
                    },
                    onRecents = {
                        items = prefs.getRecents().mapNotNull { uri ->
                            storage.fromUri(uri)?.let { file ->
                                FileItem(
                                    file,
                                    file.name ?: "(sem nome)",
                                    uri,
                                    file.isDirectory,
                                    if (file.isFile) file.length() else 0L,
                                    file.lastModified()
                                )
                            }
                        }
                        screen = Screen.FILES
                    }
                )

                Screen.FILES -> FilesScreen(
                    current = current,
                    items = items,
                    selected = selected,
                    clipboardCount = clipboard.size,
                    clipboardMode = clipboardMode,
                    onOpen = ::openSelectedFile,
                    onToggle = { item ->
                        selected = if (selected.contains(item.uri)) selected - item.uri
                        else selected + item.uri
                    },
                    onNewFolder = { folderDialog = true },
                    onCopy = {
                        clipboard = selected.toList()
                        clipboardMode = ClipboardMode.COPY
                        dialogMessage = "Itens preparados para copiar."
                    },
                    onMove = {
                        clipboard = selected.toList()
                        clipboardMode = ClipboardMode.MOVE
                        dialogMessage = "Itens preparados para mover."
                    },
                    onPaste = {
                        val destination = current
                        val sources = clipboard.mapNotNull(storage::fromUri)
                        if (destination != null && sources.isNotEmpty()) {
                            scope.launch {
                                operation = OperationProgress(
                                    if (clipboardMode == ClipboardMode.COPY) OperationType.COPY else OperationType.MOVE,
                                    0, sources.size, "A iniciar…"
                                )
                                val result = if (clipboardMode == ClipboardMode.COPY) {
                                    operationManager.copy(sources, destination) {
                                        operation = it
                                    }
                                } else {
                                    operationManager.move(sources, destination) {
                                        operation = it
                                    }
                                }
                                operation = null
                                clipboard = emptyList()
                                dialogMessage = result.exceptionOrNull()?.message ?: "Operação concluída."
                                refresh()
                            }
                        }
                    },
                    onDelete = {
                        val sources = selected.mapNotNull(storage::fromUri)
                        scope.launch {
                            operation = OperationProgress(OperationType.DELETE, 0, sources.size, "A eliminar…")
                            val result = operationManager.delete(sources) { operation = it }
                            operation = null
                            dialogMessage = result.exceptionOrNull()?.message ?: "Itens eliminados."
                            refresh()
                        }
                    },
                    onRename = {
                        if (selected.size == 1) renameTarget = storage.fromUri(selected.first())
                    },
                    onCompress = {
                        if (selected.isNotEmpty()) compressDialog = true
                    },
                    onExtract = {
                        val file = selected.singleOrNull()?.let(storage::fromUri)
                        val destination = current
                        if (file?.name?.endsWith(".zip", true) == true && destination != null) {
                            scope.launch {
                                operation = OperationProgress(OperationType.EXTRACT, 0, 1, "A extrair…")
                                val result = withContext(Dispatchers.IO) {
                                    archiveManager.extractZip(file, destination) { done, total, name ->
                                        scope.launch(Dispatchers.Main.immediate) {
                                            operation = OperationProgress(OperationType.EXTRACT, done, total, name)
                                        }
                                    }
                                }
                                operation = null
                                dialogMessage = result.exceptionOrNull()?.message ?: "ZIP extraído."
                                refresh()
                            }
                        } else {
                            dialogMessage = "Selecione um único arquivo ZIP."
                        }
                    },
                    onShare = ::shareSelected,
                    onInfo = {
                        if (selected.size == 1) infoTarget = storage.fromUri(selected.first())
                    },
                    onFavorite = {
                        if (selected.size == 1) {
                            val uri = selected.first()
                            val favs = prefs.getFavorites().toMutableSet()
                            if (!favs.add(uri)) favs.remove(uri)
                            prefs.setFavorites(favs)
                            dialogMessage = "Favorito atualizado."
                        }
                    }
                )

                Screen.SETTINGS -> SettingsScreen(
                    version = UpdateManager.CURRENT_VERSION,
                    onChooseFolder = { chooseFolder.launch(null) },
                    onCheckUpdate = {
                        checkingUpdate = true
                        scope.launch {
                            val result = UpdateManager.check()
                            checkingUpdate = false
                            updateInfo = result.getOrNull()
                            dialogMessage = result.exceptionOrNull()?.message
                                ?: if (updateInfo == null) "Você já está na versão mais recente."
                                else "Nova versão encontrada: " + updateInfo!!.version
                        }
                    },
                    onAbout = {
                        dialogMessage = "Nexauren File é o gestor de ficheiros da Nexauren."
                    }
                )
            }
        }
    }

    if (checkingUpdate) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Atualizações") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("A verificar a versão mais recente…")
                }
            },
            confirmButton = {}
        )
    }

    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { if (!downloadingUpdate) updateInfo = null },
            title = { Text("Atualização " + info.version) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(info.releaseName)
                    Text(if (info.notes.isBlank()) "Nova versão disponível." else info.notes.take(1400))
                    if (downloadingUpdate) {
                        LinearProgressIndicator(progress = { updateProgress / 100f }, modifier = Modifier.fillMaxWidth())
                        Text("A descarregar: " + updateProgress + "%")
                    }
                }
            },
            confirmButton = {
                Button(enabled = !downloadingUpdate, onClick = {
                    downloadingUpdate = true
                    updateProgress = 0
                    scope.launch {
                        val result = UpdateManager.downloadApk(
                            context,
                            info,
                            onProgress = { progress ->
                                scope.launch(Dispatchers.Main.immediate) {
                                    updateProgress = progress
                                }
                            }
                        )
                        downloadingUpdate = false
                        result.onSuccess { uri ->
                            updateInfo = null
                            UpdateManager.install(context, uri)
                        }.onFailure {
                            updateInfo = null
                            dialogMessage = it.message ?: "Falha ao descarregar a atualização."
                        }
                    }
                }) { Text("Instalar") }
            },
            dismissButton = {
                TextButton(enabled = !downloadingUpdate, onClick = { updateInfo = null }) {
                    Text("Agora não")
                }
            }
        )
    }

    renameTarget?.let { target ->
        var name by remember(target.uri.toString()) { mutableStateOf(target.name ?: "") }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Renomear") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Novo nome") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newName = name.trim()
                    val ok = newName.isNotBlank() && storage.rename(target, newName)
                    dialogMessage = if (ok) "Renomeado." else "Não foi possível renomear."
                    renameTarget = null
                    refresh()
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancelar") } }
        )
    }

    infoTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { infoTarget = null },
            title = { Text(target.name ?: "(sem nome)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (target.isDirectory) "Tipo: Pasta" else "Tipo: " + (target.type ?: "Ficheiro"))
                    Text("Tamanho: " + formatBytes(target.length()))
                    Text("Modificado: " + DateFormat.getDateTimeInstance().format(Date(target.lastModified())))
                    Text(target.uri.toString())
                }
            },
            confirmButton = { TextButton(onClick = { infoTarget = null }) { Text("Fechar") } }
        )
    }

    if (folderDialog) {
        AlertDialog(
            onDismissRequest = { folderDialog = false },
            title = { Text("Nova pasta") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Nome") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = folderName.trim()
                    if (name.isNotBlank()) current?.let { storage.createDirectory(it, name) }
                    folderName = ""
                    folderDialog = false
                    refresh()
                }) { Text("Criar") }
            },
            dismissButton = { TextButton(onClick = { folderDialog = false }) { Text("Cancelar") } }
        )
    }

    if (compressDialog) {
        AlertDialog(
            onDismissRequest = { compressDialog = false },
            title = { Text("Comprimir para ZIP") },
            text = {
                OutlinedTextField(
                    value = archiveName,
                    onValueChange = { archiveName = it },
                    label = { Text("Nome do arquivo") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val destination = current
                    val sources = selected.mapNotNull(storage::fromUri)
                    if (destination != null && sources.isNotEmpty()) {
                        compressDialog = false
                        scope.launch {
                            operation = OperationProgress(OperationType.COMPRESS, 0, 1, "A comprimir…")
                            val result = withContext(Dispatchers.IO) {
                                archiveManager.compressZip(
                                    sources,
                                    destination,
                                    archiveName.ifBlank { "Nexauren" }
                                ) { done, total, name ->
                                    scope.launch(Dispatchers.Main.immediate) {
                                        operation = OperationProgress(OperationType.COMPRESS, done, total, name)
                                    }
                                }
                            }
                            operation = null
                            archiveName = ""
                            dialogMessage = result.exceptionOrNull()?.message ?: "ZIP criado."
                            refresh()
                        }
                    }
                }) { Text("Criar ZIP") }
            },
            dismissButton = { TextButton(onClick = { compressDialog = false }) { Text("Cancelar") } }
        )
    }

    operation?.let { progress ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Operação em andamento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(progress.message)
                    if (progress.total > 0) {
                        LinearProgressIndicator(
                            progress = { progress.current.toFloat() / progress.total.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    dialogMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { dialogMessage = null },
            title = { Text("Nexauren File") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { dialogMessage = null }) { Text("OK") } }
        )
    }
}

@androidx.compose.runtime.Composable
private fun HomeScreen(
    hasStorage: Boolean,
    favoriteCount: Int,
    recentCount: Int,
    onOpen: () -> Unit,
    onChoose: () -> Unit,
    onFavorites: () -> Unit,
    onRecents: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Gestor completo para os seus ficheiros",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Navegue, pesquise, copie, mova, renomeie, comprima, extraia e partilhe."
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (hasStorage) "Armazenamento configurado" else "Comece por escolher uma pasta")
                    Button(onClick = onOpen, Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Folder, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (hasStorage) "Abrir ficheiros" else "Selecionar armazenamento")
                    }
                    OutlinedButton(onClick = onChoose, Modifier.fillMaxWidth()) {
                        Text("Alterar pasta principal")
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("Favoritos", favoriteCount, Modifier.weight(1f), onFavorites)
                MiniStat("Recentes", recentCount, Modifier.weight(1f), onRecents)
            }
        }
        item { Text("Principais funções", fontWeight = FontWeight.Bold) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("ZIP") }, leadingIcon = { Icon(Icons.Default.FolderZip, null) })
                AssistChip(onClick = {}, label = { Text("Pesquisa") }, leadingIcon = { Icon(Icons.Default.Search, null) })
                AssistChip(onClick = {}, label = { Text("Partilha") }, leadingIcon = { Icon(Icons.Default.Share, null) })
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MiniStat(title: String, value: Int, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title)
        }
    }
}

@androidx.compose.runtime.Composable
private fun FilesScreen(
    current: DocumentFile?,
    items: List<FileItem>,
    selected: Set<String>,
    clipboardCount: Int,
    clipboardMode: ClipboardMode,
    onOpen: (FileItem) -> Unit,
    onToggle: (FileItem) -> Unit,
    onNewFolder: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onPaste: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onCompress: () -> Unit,
    onExtract: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit,
    onFavorite: () -> Unit
) {
    var sortMode by remember { mutableStateOf(SortMode.NAME) }
    var sortOpen by remember { mutableStateOf(false) }

    val sortedItems = when (sortMode) {
        SortMode.NAME -> items.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() })
        SortMode.SIZE -> items.sortedByDescending { it.size }
        SortMode.DATE -> items.sortedByDescending { it.modified }
        SortMode.TYPE -> items.sortedBy { it.name.substringAfterLast('.', "").lowercase() }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(current?.name ?: "Armazenamento", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { sortOpen = true }) { Icon(Icons.Default.Sort, "Ordenar") }
            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                DropdownMenuItem(text = { Text("Nome") }, onClick = { sortMode = SortMode.NAME; sortOpen = false })
                DropdownMenuItem(text = { Text("Tamanho") }, onClick = { sortMode = SortMode.SIZE; sortOpen = false })
                DropdownMenuItem(text = { Text("Data") }, onClick = { sortMode = SortMode.DATE; sortOpen = false })
                DropdownMenuItem(text = { Text("Tipo") }, onClick = { sortMode = SortMode.TYPE; sortOpen = false })
            }
            if (selected.isEmpty()) {
                IconButton(onClick = onNewFolder) { Icon(Icons.Default.CreateNewFolder, "Nova pasta") }
            }
        }

        if (selected.isNotEmpty()) {
            Surface(shadowElevation = 2.dp) {
                LazyColumn {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selected.size.toString() + " selecionado(s)", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            IconButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, "Copiar") }
                            IconButton(onClick = onMove) { Icon(Icons.Default.ContentCut, "Mover") }
                            IconButton(onClick = onCompress) { Icon(Icons.Default.Archive, "Comprimir") }
                            IconButton(onClick = onExtract) { Icon(Icons.Default.Upload, "Extrair") }
                            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar") }
                            MoreMenu(onRename, onShare, onInfo, onFavorite)
                        }
                    }
                }
            }
        }

        if (clipboardCount > 0) {
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        clipboardCount.toString() + " item(ns) para " +
                            if (clipboardMode == ClipboardMode.COPY) "copiar" else "mover",
                        Modifier.weight(1f)
                    )
                    Button(onClick = onPaste) { Text("Colar") }
                }
            }
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(sortedItems, key = { it.uri }) { item ->
                FileRow(item, selected.contains(item.uri), onOpen, onToggle)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MoreMenu(
    onRename: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit,
    onFavorite: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, "Mais") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Renomear") }, onClick = { open = false; onRename() },
                leadingIcon = { Icon(Icons.Default.Rename, null) })
            DropdownMenuItem(text = { Text("Partilhar") }, onClick = { open = false; onShare() },
                leadingIcon = { Icon(Icons.Default.Share, null) })
            DropdownMenuItem(text = { Text("Informações") }, onClick = { open = false; onInfo() },
                leadingIcon = { Icon(Icons.Default.Info, null) })
            DropdownMenuItem(text = { Text("Favorito") }, onClick = { open = false; onFavorite() },
                leadingIcon = { Icon(Icons.Default.Star, null) })
        }
    }
}

@androidx.compose.runtime.Composable
private fun FileRow(
    item: FileItem,
    selected: Boolean,
    onOpen: (FileItem) -> Unit,
    onToggle: (FileItem) -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (item.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                null,
                Modifier.size(38.dp).clickable { onOpen(item) }
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).clickable { onOpen(item) }) {
                Text(item.name, fontWeight = FontWeight.Medium)
                Text(
                    if (item.isDirectory) "Pasta" else formatBytes(item.size),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = { onToggle(item) }) {
                Icon(if (selected) Icons.Default.Check else Icons.Default.MoreVert, "Selecionar")
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SettingsScreen(
    version: String,
    onChooseFolder: () -> Unit,
    onCheckUpdate: () -> Unit,
    onAbout: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Definições", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Armazenamento", fontWeight = FontWeight.Bold)
                    Text("Escolha a pasta principal que o Nexauren File pode gerir.")
                    OutlinedButton(onClick = onChooseFolder) { Text("Alterar pasta") }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Atualizações", fontWeight = FontWeight.Bold)
                    Text("Versão instalada: " + version)
                    Button(onClick = onCheckUpdate) {
                        Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Verificar atualização")
                    }
                    Text(
                        "As versões são procuradas no GitHub Releases e instaladas através do instalador oficial do Android.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Privacidade", fontWeight = FontWeight.Bold)
                    Text("As operações de ficheiros são locais. A Internet só é usada para atualizações.")
                }
            }
        }
        item {
            Card {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Sobre", fontWeight = FontWeight.Bold)
                        Text("Nexauren File")
                    }
                    TextButton(onClick = onAbout) { Text("Ver") }
                }
            }
        }
    }
}

private fun searchRecursive(root: DocumentFile, query: String): List<FileItem> {
    val result = mutableListOf<FileItem>()
    fun visit(dir: DocumentFile) {
        for (child in dir.listFiles()) {
            val item = FileItem(
                child,
                child.name ?: "(sem nome)",
                child.uri.toString(),
                child.isDirectory,
                if (child.isFile) child.length() else 0L,
                child.lastModified()
            )
            if (item.name.contains(query, ignoreCase = true)) result.add(item)
            if (child.isDirectory && result.size < 500) visit(child)
        }
    }
    visit(root)
    return result
}

private fun formatBytes(value: Long): String {
    if (value < 1024) return value.toString() + " B"
    val kb = value / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.1f GB".format(mb / 1024.0 / 1024.0)
}
