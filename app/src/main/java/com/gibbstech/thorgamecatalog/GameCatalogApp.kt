@file:OptIn(ExperimentalMaterial3Api::class)

package com.gibbstech.thorgamecatalog

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

private const val ALL_PLATFORMS = "all"

@Composable
fun GameCatalogApp() {
    val context = LocalContext.current
    val games = remember { GameRepository.load(context) }
    var query by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf(ALL_PLATFORMS) }
    var selectedGame by remember { mutableStateOf<Game?>(null) }

    val filteredGames = remember(games, query, selectedPlatform) {
        games.filter { game ->
            val platformMatches = selectedPlatform == ALL_PLATFORMS || game.platform.id == selectedPlatform
            val queryMatches = query.isBlank() ||
                game.title.contains(query.trim(), ignoreCase = true) ||
                game.platform.label.contains(query.trim(), ignoreCase = true)
            platformMatches && queryMatches
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        ) {
            CatalogHeader(
                gameCount = filteredGames.size,
                query = query,
                onQueryChange = { query = it },
            )

            PlatformFilters(
                selectedPlatform = selectedPlatform,
                onPlatformSelected = { selectedPlatform = it },
            )

            if (filteredGames.isEmpty()) {
                EmptyCatalog(modifier = Modifier.weight(1f))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 148.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 14.dp,
                        top = 10.dp,
                        end = 14.dp,
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(filteredGames, key = { it.id }) { game ->
                        GameCard(game = game, onClick = { selectedGame = game })
                    }
                }
            }
        }
    }

    selectedGame?.let { game ->
        GameDetailSheet(
            game = game,
            onDismiss = { selectedGame = null },
            onOpenOfficialSearch = { openUrl(context, officialSearchUrl(game)) },
            onOpenInformation = { openUrl(context, informationUrl(game)) },
        )
    }
}

@Composable
private fun CatalogHeader(
    gameCount: Int,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF7C5CFC), Color(0xFF2EE6A6)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Thor Game Catalog",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "$gameCount hier • oficiálne a informačné odkazy",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Hľadať hru alebo konzolu…") },
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
    }
}

@Composable
private fun PlatformFilters(
    selectedPlatform: String,
    onPlatformSelected: (String) -> Unit,
) {
    val filters = remember {
        listOf(ALL_PLATFORMS to "Všetky") + Platform.entries.map { it.id to it.shortLabel }
    }

    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filters, key = { it.first }) { (id, label) ->
            FilterChip(
                selected = selectedPlatform == id,
                onClick = { onPlatformSelected(id) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun GameCard(game: Game, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = game.platform.shortLabel,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                Text(
                    text = "  •  ${game.year}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
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
        loading = { CoverPlaceholder(game) },
        error = { CoverPlaceholder(game) },
    )
}

@Composable
private fun CoverPlaceholder(game: Game) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF28203E), Color(0xFF122D2A)),
                ),
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
                text = game.platform.shortLabel,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun EmptyCatalog(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text("Nenašla sa žiadna hra", fontWeight = FontWeight.Bold)
        Text(
            "Skús zmeniť názov alebo kategóriu.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GameDetailSheet(
    game: Game,
    onDismiss: () -> Unit,
    onOpenOfficialSearch: () -> Unit,
    onOpenInformation: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 22.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                GameCover(
                    game = game,
                    modifier = Modifier
                        .width(120.dp)
                        .height(168.dp)
                        .clip(RoundedCornerShape(14.dp)),
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
                        text = "${game.platform.label} • ${game.year}",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = game.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onOpenOfficialSearch,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Vyhľadať na oficiálnom webe")
            }

            OutlinedButton(
                onClick = onOpenInformation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Informácie o hre")
            }

            Text(
                text = "Aplikácia neobsahuje ani nesťahuje herné súbory.",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

private fun officialSearchUrl(game: Game): String {
    val query = "site:${game.platform.officialDomain} \"${game.title}\" ${game.platform.label}"
    return "https://www.google.com/search?q=${Uri.encode(query)}"
}

private fun informationUrl(game: Game): String {
    val query = "${game.title} ${game.platform.label}"
    return "https://en.wikipedia.org/wiki/Special:Search?search=${Uri.encode(query)}"
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Nenašiel sa webový prehliadač.", Toast.LENGTH_SHORT).show()
    }
}
