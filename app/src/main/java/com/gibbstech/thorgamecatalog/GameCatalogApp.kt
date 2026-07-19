@file:OptIn(ExperimentalMaterial3Api::class)

package com.gibbstech.thorgamecatalog

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class AppScreen {
    CATALOG,
    SETTINGS,
}

@Composable
fun GameCatalogApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiPreferences = remember { ApiPreferences(context.applicationContext) }
    val catalogPreferences = remember { CatalogPreferences(context.applicationContext) }
    val websitePreferences = remember {
        PlatformWebsitePreferences(context.applicationContext)
    }
    val igdbClient = remember { IgdbApiClient() }
    val screenScraperClient = remember { ScreenScraperApiClient() }

    var apiConfig by remember { mutableStateOf(apiPreferences.load()) }
    var platformWebsites by remember { mutableStateOf(websitePreferences.load()) }
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.CATALOG) }
    var selectedGame by remember { mutableStateOf<Game?>(null) }
    var detailGame by remember { mutableStateOf<Game?>(null) }
    var isEnrichingDetail by remember { mutableStateOf(false) }

    var query by rememberSaveable { mutableStateOf("") }
    var selectedPlatformId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPlatform = selectedPlatformId?.let(Platform::fromId)
    var selectedSortId by rememberSaveable { mutableStateOf(catalogPreferences.loadSort().id) }
    val selectedSort = GameSort.fromId(selectedSortId)
    var nativeOnly by rememberSaveable { mutableStateOf(catalogPreferences.loadNativeOnly()) }
    var games by remember { mutableStateOf<List<Game>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var catalogError by remember { mutableStateOf<String?>(null) }
    var refreshRequest by remember { mutableIntStateOf(0) }
    var loadMoreRequest by remember { mutableIntStateOf(0) }
    var showSurprise by rememberSaveable { mutableStateOf(false) }
    var surpriseGames by remember { mutableStateOf<List<Game>>(emptyList()) }
    var surpriseHistory by remember { mutableStateOf<List<Int>>(emptyList()) }
    var surpriseError by remember { mutableStateOf<String?>(null) }
    var isSurpriseLoading by remember { mutableStateOf(false) }
    var surpriseRequest by remember { mutableIntStateOf(0) }

    val catalogRequestKey = remember(
        apiConfig.igdbClientId,
        apiConfig.igdbClientSecret,
        selectedPlatformId,
        query,
        selectedSortId,
        nativeOnly,
        refreshRequest,
    ) {
        listOf(
            apiConfig.igdbClientId,
            apiConfig.igdbClientSecret.hashCode().toString(),
            selectedPlatformId.orEmpty(),
            query.trim(),
            selectedSortId,
            nativeOnly.toString(),
            refreshRequest.toString(),
        ).joinToString("|")
    }

    LaunchedEffect(catalogRequestKey) {
        games = emptyList()
        catalogError = null
        hasMore = true
        if (!apiConfig.isIgdbReady) {
            isLoading = false
            return@LaunchedEffect
        }

        if (query.isNotBlank()) delay(400)
        isLoading = true
        try {
            val page = igdbClient.fetchGames(
                config = apiConfig,
                platform = selectedPlatform,
                search = query,
                offset = 0,
                sort = selectedSort,
                nativeOnly = nativeOnly,
            )
            games = page.distinctBy { it.id }
            hasMore = page.size == IgdbApiClient.PAGE_SIZE
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            catalogError = error.message ?: "Online katalóg sa nepodarilo načítať."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(loadMoreRequest, catalogRequestKey) {
        if (loadMoreRequest == 0 || isLoading || isLoadingMore || !hasMore ||
            !apiConfig.isIgdbReady
        ) {
            return@LaunchedEffect
        }

        isLoadingMore = true
        try {
            val page = igdbClient.fetchGames(
                config = apiConfig,
                platform = selectedPlatform,
                search = query,
                offset = games.size,
                sort = selectedSort,
                nativeOnly = nativeOnly,
            )
            games = (games + page).distinctBy { it.id }
            hasMore = page.size == IgdbApiClient.PAGE_SIZE
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            catalogError = error.message ?: "Ďalšie hry sa nepodarilo načítať."
        } finally {
            isLoadingMore = false
        }
    }

    LaunchedEffect(
        surpriseRequest,
        selectedPlatformId,
        nativeOnly,
        apiConfig.igdbClientId,
        apiConfig.igdbClientSecret,
    ) {
        if (surpriseRequest == 0 || !apiConfig.isIgdbReady) return@LaunchedEffect

        isSurpriseLoading = true
        surpriseError = null
        try {
            val pickedGames = igdbClient.fetchSurpriseGames(
                config = apiConfig,
                platform = selectedPlatform,
                recentGameIds = surpriseHistory.toSet(),
                nativeOnly = nativeOnly,
            )
            surpriseGames = pickedGames
            surpriseHistory = (pickedGames.map { it.igdbId } + surpriseHistory)
                .distinct()
                .take(30)
            if (pickedGames.isEmpty()) {
                surpriseError = "Pre túto platformu sa nenašli vhodné hry."
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            surpriseError = error.message ?: "Náhodný výber sa nepodarilo načítať."
        } finally {
            isSurpriseLoading = false
        }
    }

    LaunchedEffect(
        selectedGame?.id,
        apiConfig.screenScraperDeveloperId,
        apiConfig.screenScraperDeveloperPassword,
        apiConfig.screenScraperUsername,
    ) {
        val game = selectedGame ?: run {
            detailGame = null
            return@LaunchedEffect
        }
        detailGame = game
        if (!apiConfig.isScreenScraperReady) return@LaunchedEffect

        isEnrichingDetail = true
        try {
            detailGame = screenScraperClient.enrichGame(apiConfig, game)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            detailGame = game
        } finally {
            isEnrichingDetail = false
        }
    }

    BackHandler(enabled = selectedGame != null || currentScreen == AppScreen.SETTINGS) {
        if (selectedGame != null) {
            selectedGame = null
            detailGame = null
        } else {
            currentScreen = AppScreen.CATALOG
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = when {
                    selectedGame != null -> detailGame?.title ?: selectedGame?.title.orEmpty()
                    currentScreen == AppScreen.SETTINGS -> "Nastavenia"
                    else -> "Thor Game Catalog"
                },
                showBack = selectedGame != null,
                currentScreen = currentScreen,
                onBack = {
                    selectedGame = null
                    detailGame = null
                },
                onNavigate = { screen ->
                    selectedGame = null
                    detailGame = null
                    currentScreen = screen
                },
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            when {
                selectedGame != null -> GameDetailScreen(
                    game = detailGame ?: selectedGame!!,
                    isEnriching = isEnrichingDetail,
                    preferredWebsiteTemplate = platformWebsites[selectedGame!!.platform].orEmpty(),
                    onOpenPreferredWebsite = { game ->
                        buildWebsiteUrl(
                            platformWebsites[game.platform].orEmpty(),
                            game,
                        )?.let { openUrl(context, it) }
                    },
                    onConfigureWebsite = {
                        selectedGame = null
                        detailGame = null
                        currentScreen = AppScreen.SETTINGS
                    },
                )

                currentScreen == AppScreen.CATALOG -> OnlineCatalogScreen(
                    games = games,
                    query = query,
                    selectedPlatformId = selectedPlatformId,
                    selectedSort = selectedSort,
                    nativeOnly = nativeOnly,
                    apiReady = apiConfig.isIgdbReady,
                    isLoading = isLoading,
                    isLoadingMore = isLoadingMore,
                    hasMore = hasMore,
                    error = catalogError,
                    showSurprise = showSurprise,
                    surpriseGames = surpriseGames,
                    isSurpriseLoading = isSurpriseLoading,
                    surpriseError = surpriseError,
                    onQueryChange = { query = it },
                    onPlatformSelected = { selectedPlatformId = it },
                    onSortSelected = { sort ->
                        selectedSortId = sort.id
                        catalogPreferences.saveSort(sort)
                    },
                    onNativeOnlyChange = { enabled ->
                        nativeOnly = enabled
                        catalogPreferences.saveNativeOnly(enabled)
                    },
                    onSurprise = {
                        showSurprise = true
                        surpriseRequest++
                    },
                    onCloseSurprise = { showSurprise = false },
                    onGameSelected = { selectedGame = it },
                    onOpenSettings = { currentScreen = AppScreen.SETTINGS },
                    onRetry = { refreshRequest++ },
                    onLoadMore = { loadMoreRequest++ },
                )

                else -> SettingsScreen(
                    apiConfig = apiConfig,
                    platformWebsites = platformWebsites,
                    onSaveApiConfig = { config ->
                        apiPreferences.save(config)
                        apiConfig = config
                        Toast.makeText(context, "API nastavenia boli uložené.", Toast.LENGTH_SHORT).show()
                    },
                    onTestIgdb = { config ->
                        scope.launch {
                            val message = runCatching { igdbClient.testConnection(config) }
                                .fold(
                                    onSuccess = { "IGDB pripojenie funguje." },
                                    onFailure = { it.message ?: "IGDB pripojenie zlyhalo." },
                                )
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                    onTestScreenScraper = { config ->
                        scope.launch {
                            val message = runCatching {
                                screenScraperClient.testConnection(config)
                            }.fold(
                                onSuccess = { "ScreenScraper pripojenie funguje." },
                                onFailure = {
                                    it.message ?: "ScreenScraper pripojenie zlyhalo."
                                },
                            )
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                    onSaveWebsite = { platform, template ->
                        websitePreferences.save(platform, template)
                        platformWebsites = platformWebsites + (platform to template.trim())
                        Toast.makeText(
                            context,
                            "Odkaz pre ${platform.shortLabel} bol uložený.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    onResetWebsite = { platform ->
                        websitePreferences.save(platform, "")
                        platformWebsites = platformWebsites + (platform to "")
                    },
                    onTestWebsite = { platform, template ->
                        buildWebsiteUrl(template, sampleGame(platform))?.let {
                            openUrl(context, it)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AppTopBar(
    title: String,
    showBack: Boolean,
    currentScreen: AppScreen,
    onBack: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
            )
        },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Späť")
                }
            }
        },
        actions = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Katalóg") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onNavigate(AppScreen.CATALOG)
                        },
                        enabled = currentScreen != AppScreen.CATALOG || showBack,
                    )
                    DropdownMenuItem(
                        text = { Text("Nastavenia") },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onNavigate(AppScreen.SETTINGS)
                        },
                        enabled = currentScreen != AppScreen.SETTINGS || showBack,
                    )
                }
            }
        },
    )
}

@Composable
private fun OnlineCatalogScreen(
    games: List<Game>,
    query: String,
    selectedPlatformId: String?,
    selectedSort: GameSort,
    nativeOnly: Boolean,
    apiReady: Boolean,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    error: String?,
    showSurprise: Boolean,
    surpriseGames: List<Game>,
    isSurpriseLoading: Boolean,
    surpriseError: String?,
    onQueryChange: (String) -> Unit,
    onPlatformSelected: (String?) -> Unit,
    onSortSelected: (GameSort) -> Unit,
    onNativeOnlyChange: (Boolean) -> Unit,
    onSurprise: () -> Unit,
    onCloseSurprise: () -> Unit,
    onGameSelected: (Game) -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    if (!apiReady) {
        OnlineSetupRequired(onOpenSettings)
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 14.dp,
            top = 10.dp,
            end = 14.dp,
            bottom = 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            CatalogControls(
                query = query,
                selectedPlatformId = selectedPlatformId,
                selectedSort = selectedSort,
                nativeOnly = nativeOnly,
                onQueryChange = onQueryChange,
                onPlatformSelected = onPlatformSelected,
                onSortSelected = onSortSelected,
                onNativeOnlyChange = onNativeOnlyChange,
                onSurprise = onSurprise,
            )
        }

        if (showSurprise) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SurprisePanel(
                    games = surpriseGames,
                    isLoading = isSurpriseLoading,
                    error = surpriseError,
                    onGameSelected = onGameSelected,
                    onRefresh = onSurprise,
                    onClose = onCloseSurprise,
                )
            }
        }

        items(games, key = { it.id }) { game ->
            GameCard(game = game, onClick = { onGameSelected(game) })
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            when {
                isLoading || isLoadingMore -> LoadingRow()
                error != null -> ErrorBanner(error, onRetry)
                games.isEmpty() -> EmptyOnlineCatalog()
                hasMore -> {
                    LaunchedEffect(games.size, hasMore) { onLoadMore() }
                    LoadingRow()
                }
            }
        }
    }
}

@Composable
private fun CatalogControls(
    query: String,
    selectedPlatformId: String?,
    selectedSort: GameSort,
    nativeOnly: Boolean,
    onQueryChange: (String) -> Unit,
    onPlatformSelected: (String?) -> Unit,
    onSortSelected: (GameSort) -> Unit,
    onNativeOnlyChange: (Boolean) -> Unit,
    onSurprise: () -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Hľadať online hru…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Vymazať")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.SwapVert, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = selectedSort.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                ) {
                    GameSort.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sort.label) },
                            leadingIcon = {
                                if (sort == selectedSort) {
                                    Icon(Icons.Default.Star, contentDescription = null)
                                }
                            },
                            onClick = {
                                sortMenuExpanded = false
                                onSortSelected(sort)
                            },
                        )
                    }
                }
            }
            Button(
                onClick = onSurprise,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Casino, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Surprise me", maxLines = 1)
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedPlatformId == null,
                    onClick = { onPlatformSelected(null) },
                    label = { Text("Všetky") },
                )
            }
            items(Platform.entries, key = { it.id }) { platform ->
                FilterChip(
                    selected = selectedPlatformId == platform.id,
                    onClick = { onPlatformSelected(platform.id) },
                    label = { Text(platform.shortLabel) },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onNativeOnlyChange(!nativeOnly) }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = nativeOnly,
                onCheckedChange = null,
            )
            Spacer(Modifier.width(4.dp))
            Column {
                Text("Iba natívne hry", fontWeight = FontWeight.Bold)
                Text(
                    text = "Skryje staré kompatibilné a Virtual Console tituly",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SurprisePanel(
    games: List<Game>,
    isLoading: Boolean,
    error: String?,
    onGameSelected: (Game) -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Casino, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Surprise me",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Zavrieť náhodný výber")
                }
            }

            when {
                isLoading -> LoadingRow()
                error != null -> {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Skúsiť znova")
                    }
                }
                else -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(games, key = { "surprise-${it.id}" }) { game ->
                            GameCard(
                                game = game,
                                onClick = { onGameSelected(game) },
                                modifier = Modifier.width(152.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onRefresh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Prekvap ma znova")
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCard(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    var hasFocus by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)

    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { hasFocus = it.hasFocus }
            .border(
                width = if (hasFocus) 3.dp else 0.dp,
                color = if (hasFocus) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            ),
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (hasFocus) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        GameCover(
            game = game,
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp),
        )
        Column(modifier = Modifier.padding(11.dp)) {
            Text(
                text = game.title,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${game.platform.shortLabel}  •  ${game.year ?: "—"}",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                game.rating?.let { rating ->
                    Text(
                        text = "★ ${rating.roundToInt()}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameCover(game: Game, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = game.coverUrl,
        contentDescription = "Obal hry ${game.title}",
        modifier = modifier,
        contentScale = ContentScale.Crop,
        loading = { CoverPlaceholder(game.platform) },
        error = { CoverPlaceholder(game.platform) },
    )
}

@Composable
private fun CoverPlaceholder(platform: Platform) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(Color(0xFF28203E), Color(0xFF122D2A))),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = platform.shortLabel,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun GameDetailScreen(
    game: Game,
    isEnriching: Boolean,
    preferredWebsiteTemplate: String,
    onOpenPreferredWebsite: (Game) -> Unit,
    onConfigureWebsite: () -> Unit,
) {
    var showVideo by remember(game.id) { mutableStateOf(false) }
    var selectedScreenshotIndex by remember(game.id) { mutableStateOf<Int?>(null) }
    val hasVideo = !game.previewVideoUrl.isNullOrBlank() || !game.youtubeVideoId.isNullOrBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (isEnriching) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        item {
            Row(verticalAlignment = Alignment.Top) {
                GameCover(
                    game = game,
                    modifier = Modifier
                        .width(132.dp)
                        .height(186.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${game.platform.label} • ${game.year ?: "rok neznámy"}",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                    )
                    game.rating?.let { rating ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "★ ${rating.roundToInt()}/100  •  ${game.ratingCount} hodnotení",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                    game.developer?.let {
                        Spacer(Modifier.height(10.dp))
                        Text("Vývojár: $it", fontSize = 13.sp)
                    }
                    game.publisher?.let { Text("Vydavateľ: $it", fontSize = 13.sp) }
                    if (game.genres.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = game.genres.joinToString(" • "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = game.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 21.sp,
            )
        }

        if (game.screenshotUrls.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Screenshoty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(
                            items = game.screenshotUrls.withIndex().toList(),
                            key = { it.value },
                        ) { indexedScreenshot ->
                            ScreenshotThumbnail(
                                url = indexedScreenshot.value,
                                gameTitle = game.title,
                                onClick = {
                                    selectedScreenshotIndex = indexedScreenshot.index
                                },
                            )
                        }
                    }
                }
            }
        }

        item {
            Column {
                Text(
                    text = "Gameplay video",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                when {
                    !hasVideo && isEnriching -> LoadingRow()
                    !hasVideo -> Text(
                        text = "Pre túto hru nie je dostupné video.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    showVideo -> GameVideoPlayer(
                        game = game,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                    else -> Button(onClick = { showVideo = true }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Prehrať gameplay")
                    }
                }
            }
        }

        item {
            if (preferredWebsiteTemplate.isNotBlank()) {
                Button(
                    onClick = { onOpenPreferredWebsite(game) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Otvoriť preferovanú stránku")
                }
            } else {
                OutlinedButton(
                    onClick = onConfigureWebsite,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nastaviť stránku pre ${game.platform.shortLabel}")
                }
            }
        }
    }

    selectedScreenshotIndex?.let { initialIndex ->
        ScreenshotGalleryDialog(
            urls = game.screenshotUrls,
            gameTitle = game.title,
            initialIndex = initialIndex.coerceIn(game.screenshotUrls.indices),
            onDismiss = { selectedScreenshotIndex = null },
        )
    }
}

@Composable
private fun ScreenshotThumbnail(
    url: String,
    gameTitle: String,
    onClick: () -> Unit,
) {
    var hasFocus by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .width(280.dp)
            .height(158.dp)
            .onFocusChanged { hasFocus = it.hasFocus }
            .border(
                width = if (hasFocus) 3.dp else 0.dp,
                color = if (hasFocus) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            ),
        shape = shape,
    ) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = "Otvoriť screenshot z hry $gameTitle",
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            loading = { LoadingRow() },
        )
    }
}

@Composable
private fun ScreenshotGalleryDialog(
    urls: List<String>,
    gameTitle: String,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    if (urls.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { urls.size },
    )
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val movePage: (Int) -> Unit = { direction ->
        val target = (pagerState.currentPage + direction).coerceIn(urls.indices)
        if (target != pagerState.currentPage) {
            scope.launch { pagerState.animateScrollToPage(target) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            movePage(-1)
                            true
                        }
                        Key.DirectionRight -> {
                            movePage(1)
                            true
                        }
                        else -> false
                    }
                },
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                ZoomableScreenshot(
                    url = largeScreenshotUrl(urls[page]),
                    gameTitle = gameTitle,
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.72f),
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${urls.size}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(50)),
            ) {
                Icon(Icons.Default.Close, contentDescription = "Zavrieť galériu", tint = Color.White)
            }

            if (pagerState.currentPage > 0) {
                IconButton(
                    onClick = { movePage(-1) },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(50)),
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Predchádzajúci screenshot",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }

            if (pagerState.currentPage < urls.lastIndex) {
                IconButton(
                    onClick = { movePage(1) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(50)),
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Nasledujúci screenshot",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }

            Text(
                text = "Potiahni alebo použi D-pad  •  B zatvorí galériu",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                color = Color.White,
                fontSize = 12.sp,
            )
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun ZoomableScreenshot(
    url: String,
    gameTitle: String,
) {
    var scale by remember(url) { mutableFloatStateOf(1f) }
    var offset by remember(url) { mutableStateOf(Offset.Zero) }
    var viewportSize by remember(url) { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .pointerInput(url) {
                awaitEachGesture {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val isZoomGesture = event.changes.size > 1 || scale > 1.01f
                            if (isZoomGesture) {
                                val newScale = (scale * zoomChange).coerceIn(1f, 4f)
                                val maxX = viewportSize.width * (newScale - 1f) / 2f
                                val maxY = viewportSize.height * (newScale - 1f) / 2f
                                scale = newScale
                                offset = if (newScale <= 1.01f) {
                                    Offset.Zero
                                } else {
                                    Offset(
                                        x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + panChange.y).coerceIn(-maxY, maxY),
                                    )
                                }
                                event.changes.forEach { it.consume() }
                            }
                            if (event.changes.none { it.pressed }) break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = "Zväčšený screenshot z hry $gameTitle",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
            contentScale = ContentScale.Fit,
            loading = { LoadingRow() },
            error = {
                Text(
                    text = "Screenshot sa nepodarilo načítať.",
                    color = Color.White,
                )
            },
        )
    }
}

internal fun largeScreenshotUrl(url: String): String =
    url.replace("/t_screenshot_big/", "/t_1080p/")

@Composable
private fun SettingsScreen(
    apiConfig: ApiConfig,
    platformWebsites: Map<Platform, String>,
    onSaveApiConfig: (ApiConfig) -> Unit,
    onTestIgdb: (ApiConfig) -> Unit,
    onTestScreenScraper: (ApiConfig) -> Unit,
    onSaveWebsite: (Platform, String) -> Unit,
    onResetWebsite: (Platform) -> Unit,
    onTestWebsite: (Platform, String) -> Unit,
) {
    var draft by remember(apiConfig) { mutableStateOf(apiConfig) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SettingsInfoCard()
        }

        item {
            ApiSettingsCard(
                title = "IGDB – online katalóg",
                description = "Povinné. Bez IGDB údajov sa nezobrazia žiadne hry.",
            ) {
                OutlinedTextField(
                    value = draft.igdbClientId,
                    onValueChange = { draft = draft.copy(igdbClientId = it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Client ID") },
                )
                OutlinedTextField(
                    value = draft.igdbClientSecret,
                    onValueChange = { draft = draft.copy(igdbClientSecret = it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Client Secret") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                OutlinedButton(
                    onClick = { onTestIgdb(draft) },
                    enabled = draft.isIgdbReady,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Otestovať IGDB")
                }
            }
        }

        item {
            ApiSettingsCard(
                title = "ScreenScraper – retro médiá",
                description = "Voliteľné. Doplní screenshoty a krátke MP4 gameplay video.",
            ) {
                OutlinedTextField(
                    value = draft.screenScraperDeveloperId,
                    onValueChange = { draft = draft.copy(screenScraperDeveloperId = it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Developer ID") },
                )
                OutlinedTextField(
                    value = draft.screenScraperDeveloperPassword,
                    onValueChange = {
                        draft = draft.copy(screenScraperDeveloperPassword = it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Developer Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                OutlinedTextField(
                    value = draft.screenScraperSoftName,
                    onValueChange = { draft = draft.copy(screenScraperSoftName = it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Softname") },
                )
                OutlinedTextField(
                    value = draft.screenScraperUsername,
                    onValueChange = { draft = draft.copy(screenScraperUsername = it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("ScreenScraper používateľ – voliteľné") },
                )
                OutlinedTextField(
                    value = draft.screenScraperPassword,
                    onValueChange = { draft = draft.copy(screenScraperPassword = it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("ScreenScraper heslo – voliteľné") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                OutlinedButton(
                    onClick = { onTestScreenScraper(draft) },
                    enabled = draft.isScreenScraperReady,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Otestovať ScreenScraper")
                }
            }
        }

        item {
            Button(
                onClick = { onSaveApiConfig(draft) },
                modifier = Modifier.fillMaxWidth(),
                enabled = draft != apiConfig,
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Uložiť API nastavenia")
            }
        }

        item {
            Text(
                text = "Preferované stránky platforiem",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = "Premenné: {title}, {platform}, {region}, {year}. " +
                        "Adresa bez premennej sa otvorí presne tak, ako ju zadáš.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }

        items(Platform.entries, key = { it.id }) { platform ->
            PlatformWebsiteCard(
                platform = platform,
                savedTemplate = platformWebsites[platform].orEmpty(),
                onSave = { onSaveWebsite(platform, it) },
                onReset = { onResetWebsite(platform) },
                onTest = { onTestWebsite(platform, it) },
            )
        }
    }
}

@Composable
private fun SettingsInfoCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Iba online katalóg", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Aplikácia už neobsahuje lokálny zoznam hier. API údaje sa ukladajú " +
                    "šifrovane pomocou Android Keystore a neposielajú sa nikam okrem IGDB, " +
                    "Twitch a ScreenScraper.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun ApiSettingsCard(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            content()
        }
    }
}

@Composable
private fun PlatformWebsiteCard(
    platform: Platform,
    savedTemplate: String,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
    onTest: (String) -> Unit,
) {
    var draft by remember(platform, savedTemplate) { mutableStateOf(savedTemplate) }
    val trimmedDraft = draft.trim()
    val isValid = isValidWebsiteTemplate(trimmedDraft)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Link, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(platform.label, fontWeight = FontWeight.Bold)
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Preferovaná URL") },
                placeholder = { Text("https://example.com/search?q={title}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = trimmedDraft.isNotEmpty() && !isValid,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSave(trimmedDraft) },
                    enabled = isValid && trimmedDraft != savedTemplate,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Uložiť")
                }
                OutlinedButton(
                    onClick = { onTest(trimmedDraft) },
                    enabled = isValid,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Otestovať")
                }
            }
            TextButton(
                onClick = {
                    draft = ""
                    onReset()
                },
                enabled = savedTemplate.isNotBlank(),
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Odstrániť odkaz")
            }
        }
    }
}

@Composable
private fun OnlineSetupRequired(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.SportsEsports,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text("Online katalóg nie je nastavený", fontWeight = FontWeight.Black, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Zadaj vlastné IGDB Client ID a Client Secret. Lokálne hry boli úplne odstránené.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        Button(onClick = onOpenSettings) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Otvoriť nastavenia")
        }
    }
}

@Composable
private fun LoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(34.dp))
    }
}

@Composable
private fun EmptyOnlineCatalog() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(10.dp))
        Text("Nenašli sa žiadne online hry", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Skúsiť znova")
            }
        }
    }
}

private fun sampleGame(platform: Platform): Game = Game(
    id = "sample-${platform.id}",
    igdbId = 0,
    title = "Ukážková hra",
    platform = platform,
    year = 2000,
    description = "",
    coverUrl = null,
)

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Nenašiel sa webový prehliadač.", Toast.LENGTH_SHORT).show()
    }
}
