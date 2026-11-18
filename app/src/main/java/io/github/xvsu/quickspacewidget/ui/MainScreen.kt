package io.github.xvsu.quickspacewidget.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.xvsu.quickspacewidget.R
import io.github.xvsu.quickspacewidget.engine.QuickSpaceContentEngine
import io.github.xvsu.quickspacewidget.model.AccentColorMode
import io.github.xvsu.quickspacewidget.model.AppLanguage
import io.github.xvsu.quickspacewidget.model.QuickSpaceSettings
import io.github.xvsu.quickspacewidget.model.QuoteCategory
import io.github.xvsu.quickspacewidget.model.QuoteClickAction
import io.github.xvsu.quickspacewidget.model.RenderMode
import io.github.xvsu.quickspacewidget.model.ResolvedWidgetContent
import io.github.xvsu.quickspacewidget.model.TextAlignment
import io.github.xvsu.quickspacewidget.model.TextColorMode
import io.github.xvsu.quickspacewidget.model.WidgetStyle
import io.github.xvsu.quickspacewidget.widget.ColorResolver
import io.github.xvsu.quickspacewidget.widget.DoubleShadowBitmapRenderer
import io.github.xvsu.quickspacewidget.widget.ResolvedWidgetColors
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    localizedContext: Context,
    settings: QuickSpaceSettings,
    content: ResolvedWidgetContent,
    placedWidgetCount: Int,
    hasColorOsWidgetEntry: Boolean,
    snackbarHostState: SnackbarHostState,
    onUpdatePreviewSettings: (QuickSpaceSettings) -> Unit,
    onCommitSettings: (QuickSpaceSettings) -> Unit,
    onNextQuote: () -> Unit,
    onSelectSpecificQuote: (QuoteCategory, Int) -> Unit,
    onOpenColorOsWidgetPanel: () -> Unit,
    onRequestStandardPinWidget: () -> Unit,
    onSyncWidgets: () -> Unit,
    onResetDefaults: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showQuotesSheet by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showColorOsDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = localizedContext.getString(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = localizedContext.getString(R.string.app_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSyncWidgets) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = localizedContext.getString(R.string.action_sync_widgets),
                        )
                    }
                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = localizedContext.getString(R.string.action_reset_defaults),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PreviewSectionCard(
                localizedContext = localizedContext,
                settings = settings,
                content = content,
                placedWidgetCount = placedWidgetCount,
                onNextQuote = onNextQuote,
                onAddWidget = {
                    if (hasColorOsWidgetEntry) {
                        showColorOsDialog = true
                    } else {
                        onRequestStandardPinWidget()
                    }
                },
                onBrowseQuotes = { showQuotesSheet = true },
            )

            LanguageCard(
                localizedContext = localizedContext,
                selected = settings.language,
                onSelect = { onCommitSettings(settings.copy(language = it)) },
            )

            AppearanceCard(
                localizedContext = localizedContext,
                settings = settings,
                onPreviewChange = onUpdatePreviewSettings,
                onCommitSettings = onCommitSettings,
            )

            ContentBehaviorCard(
                localizedContext = localizedContext,
                settings = settings,
                onCommitSettings = onCommitSettings,
            )

            RefreshInfoCard(
                localizedContext = localizedContext,
                onSyncWidgets = onSyncWidgets,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showColorOsDialog) {
        AlertDialog(
            onDismissRequest = { showColorOsDialog = false },
            title = { Text(localizedContext.getString(R.string.coloros_widget_dialog_title)) },
            text = {
                Text(
                    localizedContext.getString(
                        R.string.coloros_widget_dialog_body,
                        localizedContext.getString(R.string.app_name),
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showColorOsDialog = false
                        onOpenColorOsWidgetPanel()
                    }
                ) {
                    Text(localizedContext.getString(R.string.coloros_widget_open_panel))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showColorOsDialog = false
                        onRequestStandardPinWidget()
                    }
                ) {
                    Text(localizedContext.getString(R.string.coloros_widget_fallback_system))
                }
            },
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(localizedContext.getString(R.string.reset_dialog_title)) },
            text = { Text(localizedContext.getString(R.string.reset_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        onResetDefaults()
                    }
                ) {
                    Text(localizedContext.getString(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(localizedContext.getString(R.string.action_cancel))
                }
            },
        )
    }

    if (showQuotesSheet) {
        QuotesBrowserSheet(
            localizedContext = localizedContext,
            onDismiss = { showQuotesSheet = false },
            onPickQuote = { category, index ->
                onSelectSpecificQuote(category, index)
                showQuotesSheet = false
            },
        )
    }
}

@Composable
private fun PreviewSectionCard(
    localizedContext: Context,
    settings: QuickSpaceSettings,
    content: ResolvedWidgetContent,
    placedWidgetCount: Int,
    onNextQuote: () -> Unit,
    onAddWidget: () -> Unit,
    onBrowseQuotes: () -> Unit,
) {
    val colors = remember(localizedContext, settings.textColorMode, settings.accentColorMode) {
        ColorResolver.resolve(localizedContext, settings)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = localizedContext.getString(R.string.preview_title),
                style = MaterialTheme.typography.titleMedium,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onNextQuote),
            ) {
                StripedPreviewWallpaper()

                if (settings.renderMode == RenderMode.CANVAS_DOUBLE_SHADOW) {
                    CanvasWidgetPreview(
                        localizedContext = localizedContext,
                        settings = settings,
                        content = content,
                        colors = colors,
                    )
                } else {
                    NativeStylePreview(
                        settings = settings,
                        content = content,
                        colors = colors,
                    )
                }
            }

            if (content.hiddenByProbability) {
                Text(
                    text = localizedContext.getString(R.string.preview_hidden_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (placedWidgetCount > 0) {
                        localizedContext.getString(R.string.widget_count_active, placedWidgetCount)
                    } else {
                        localizedContext.getString(R.string.widget_count_none)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onAddWidget,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.AddToHomeScreen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = localizedContext.getString(R.string.action_add_widget),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FilledTonalButton(
                    onClick = onNextQuote,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Casino,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(localizedContext.getString(R.string.action_next_quote))
                }
            }

            OutlinedButton(
                onClick = onBrowseQuotes,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(localizedContext.getString(R.string.action_browse_quotes))
            }
        }
    }
}

@Composable
private fun StripedPreviewWallpaper() {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFDDD8CF), Color(0xFFCCC6BA))
                    )
                )
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFBACEDB), Color(0xFFA7BECD))
                    )
                )
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF53C1EC), Color(0xFF39B0DE))
                    )
                )
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFE7E2DB), Color(0xFFDCD6CD))
                    )
                )
        )
        Box(
            Modifier
                .weight(0.7f)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFEBA0AA), Color(0xFFDD8A95))
                    )
                )
        )
    }
}

@Composable
private fun CanvasWidgetPreview(
    localizedContext: Context,
    settings: QuickSpaceSettings,
    content: ResolvedWidgetContent,
    colors: ResolvedWidgetColors,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx().roundToInt() }.coerceAtLeast(120)
        val heightPx = with(density) { maxHeight.toPx().roundToInt() }.coerceAtLeast(60)
        val bitmap: Bitmap = remember(
            localizedContext,
            settings,
            content,
            colors,
            widthPx,
            heightPx,
        ) {
            DoubleShadowBitmapRenderer.render(
                context = localizedContext,
                settings = settings,
                content = content,
                colors = colors,
                targetWidthPx = widthPx,
                maxHeightPx = heightPx,
            )
        }
        val slackDp = ((maxHeight.value) - (bitmap.height / density.density)).coerceAtLeast(0f)
        val clampedOffsetDp = settings.verticalOffsetDp.toFloat().coerceIn(-slackDp / 2f, slackDp / 2f)
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .offset(y = clampedOffsetDp.dp),
        )
    }
}

@Composable
private fun NativeStylePreview(
    settings: QuickSpaceSettings,
    content: ResolvedWidgetContent,
    colors: ResolvedWidgetColors,
) {
    val density = LocalDensity.current
    val useShadow = colors.isLightText && settings.shadowStrengthPercent > 0
    val shadow = if (useShadow) {
        with(density) {
            Shadow(
                color = Color(0xB0000000),
                offset = Offset(0.5f, 1f),
                blurRadius = 2.dp.toPx(),
            )
        }
    } else {
        null
    }

    val horizontalAlign = when (settings.alignment) {
        TextAlignment.START -> Alignment.Start
        TextAlignment.CENTER -> Alignment.CenterHorizontally
        TextAlignment.END -> Alignment.End
    }
    val textAlign = when (settings.alignment) {
        TextAlignment.START -> TextAlign.Start
        TextAlignment.CENTER -> TextAlign.Center
        TextAlignment.END -> TextAlign.End
    }
    val textColor = Color(colors.textColor)
    val accentColor = Color(colors.accentColor)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = settings.horizontalPaddingDp.dp, vertical = 6.dp)
            .offset(y = settings.verticalOffsetDp.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = horizontalAlign,
    ) {
        if (settings.style == WidgetStyle.EXTENDED) {
            val greetSize = (settings.titleSizeSp - 2).coerceAtLeast(13).sp
            val dateLineSize = (settings.titleSizeSp - 4).coerceAtLeast(12).sp
            Text(
                text = content.greeting,
                style = TextStyle(
                    color = accentColor,
                    fontSize = greetSize,
                    shadow = shadow,
                    textAlign = textAlign,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = content.clockPrefix,
                    style = TextStyle(
                        color = accentColor,
                        fontSize = dateLineSize,
                        shadow = shadow,
                    ),
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = content.dateText,
                    style = TextStyle(
                        color = textColor,
                        fontSize = dateLineSize,
                        shadow = shadow,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Text(
                text = content.dateText,
                style = TextStyle(
                    color = textColor,
                    fontSize = settings.titleSizeSp.sp,
                    shadow = shadow,
                    textAlign = textAlign,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val quote = content.quoteText
        if (!quote.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(settings.lineSpacingDp.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = when (settings.alignment) {
                    TextAlignment.START -> Arrangement.Start
                    TextAlignment.CENTER -> Arrangement.Center
                    TextAlignment.END -> Arrangement.End
                },
            ) {
                if (content.showEasterIcon) {
                    Image(
                        painter = painterResource(R.drawable.ic_quickspace_crdroid),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(textColor),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(20.dp),
                    )
                }
                Text(
                    text = quote,
                    style = TextStyle(
                        color = textColor,
                        fontSize = settings.subtitleSizeSp.sp,
                        letterSpacing = 0.01.sp,
                        shadow = shadow,
                        textAlign = textAlign,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LanguageCard(
    localizedContext: Context,
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    SettingsSectionCard(title = localizedContext.getString(R.string.section_language)) {
        val options = AppLanguage.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, lang ->
                SegmentedButton(
                    selected = selected == lang,
                    onClick = { onSelect(lang) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = {
                        Text(
                            text = localizedContext.getString(
                                when (lang) {
                                    AppLanguage.SYSTEM -> R.string.lang_system
                                    AppLanguage.ZH_CN -> R.string.lang_zh
                                    AppLanguage.EN -> R.string.lang_en
                                }
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
        Text(
            text = localizedContext.getString(R.string.lang_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppearanceCard(
    localizedContext: Context,
    settings: QuickSpaceSettings,
    onPreviewChange: (QuickSpaceSettings) -> Unit,
    onCommitSettings: (QuickSpaceSettings) -> Unit,
) {
    SettingsSectionCard(title = localizedContext.getString(R.string.section_style_render)) {
        OptionLabel(localizedContext.getString(R.string.label_widget_style))
        val styles = WidgetStyle.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            styles.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = settings.style == item,
                    onClick = { onCommitSettings(settings.copy(style = item)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = styles.size),
                    label = {
                        Text(
                            text = localizedContext.getString(
                                when (item) {
                                    WidgetStyle.DOUBLE_LINE -> R.string.style_double_line
                                    WidgetStyle.EXTENDED -> R.string.style_extended
                                }
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }

        OptionLabel(localizedContext.getString(R.string.label_render_mode))
        val renderModes = RenderMode.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            renderModes.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = settings.renderMode == item,
                    onClick = { onCommitSettings(settings.copy(renderMode = item)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = renderModes.size),
                    label = {
                        Text(
                            text = localizedContext.getString(
                                when (item) {
                                    RenderMode.NATIVE_TEXT -> R.string.render_native
                                    RenderMode.CANVAS_DOUBLE_SHADOW -> R.string.render_canvas
                                }
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
        Text(
            text = localizedContext.getString(
                if (settings.renderMode == RenderMode.NATIVE_TEXT) {
                    R.string.render_mode_hint_native
                } else {
                    R.string.render_mode_hint_canvas
                }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OptionLabel(localizedContext.getString(R.string.label_alignment))
        val alignments = TextAlignment.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            alignments.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = settings.alignment == item,
                    onClick = { onCommitSettings(settings.copy(alignment = item)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = alignments.size),
                    label = {
                        Text(
                            text = localizedContext.getString(
                                when (item) {
                                    TextAlignment.START -> R.string.align_start
                                    TextAlignment.CENTER -> R.string.align_center
                                    TextAlignment.END -> R.string.align_end
                                }
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }

        OptionLabel(localizedContext.getString(R.string.label_text_color))
        val textColors = TextColorMode.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            textColors.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = settings.textColorMode == item,
                    onClick = { onCommitSettings(settings.copy(textColorMode = item)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = textColors.size),
                    label = {
                        Text(
                            text = localizedContext.getString(
                                when (item) {
                                    TextColorMode.WHITE -> R.string.color_white
                                    TextColorMode.DARK -> R.string.color_dark
                                    TextColorMode.AUTO -> R.string.color_auto
                                }
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }

        if (settings.style == WidgetStyle.EXTENDED) {
            OptionLabel(localizedContext.getString(R.string.label_accent_color))
            val accents = AccentColorMode.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                accents.forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = settings.accentColorMode == item,
                        onClick = { onCommitSettings(settings.copy(accentColorMode = item)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = accents.size),
                        label = {
                            Text(
                                text = localizedContext.getString(
                                    when (item) {
                                        AccentColorMode.DYNAMIC -> R.string.accent_dynamic
                                        AccentColorMode.CRDROID_SKY -> R.string.accent_sky
                                        AccentColorMode.MATCH_TEXT -> R.string.accent_match
                                    }
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        IntSliderSetting(
            label = localizedContext.getString(R.string.label_title_size, settings.titleSizeSp),
            value = settings.titleSizeSp,
            range = 16..28,
            onPreviewValue = { onPreviewChange(settings.copy(titleSizeSp = it)) },
            onCommitValue = { onCommitSettings(settings.copy(titleSizeSp = it)) },
        )
        IntSliderSetting(
            label = localizedContext.getString(R.string.label_subtitle_size, settings.subtitleSizeSp),
            value = settings.subtitleSizeSp,
            range = 12..22,
            onPreviewValue = { onPreviewChange(settings.copy(subtitleSizeSp = it)) },
            onCommitValue = { onCommitSettings(settings.copy(subtitleSizeSp = it)) },
        )
        IntSliderSetting(
            label = localizedContext.getString(R.string.label_shadow_strength, settings.shadowStrengthPercent),
            value = settings.shadowStrengthPercent,
            range = 0..200,
            step = 10,
            onPreviewValue = { onPreviewChange(settings.copy(shadowStrengthPercent = it)) },
            onCommitValue = { onCommitSettings(settings.copy(shadowStrengthPercent = it)) },
        )
        if (settings.renderMode == RenderMode.NATIVE_TEXT) {
            Text(
                text = localizedContext.getString(R.string.native_shadow_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IntSliderSetting(
            label = localizedContext.getString(R.string.label_horizontal_padding, settings.horizontalPaddingDp),
            value = settings.horizontalPaddingDp,
            range = 0..32,
            step = 2,
            onPreviewValue = { onPreviewChange(settings.copy(horizontalPaddingDp = it)) },
            onCommitValue = { onCommitSettings(settings.copy(horizontalPaddingDp = it)) },
        )
        val verticalOffsetValueText = when {
            settings.verticalOffsetDp < 0 ->
                localizedContext.getString(R.string.offset_up, abs(settings.verticalOffsetDp))
            settings.verticalOffsetDp > 0 ->
                localizedContext.getString(R.string.offset_down, settings.verticalOffsetDp)
            else -> localizedContext.getString(R.string.offset_center)
        }
        IntSliderSetting(
            label = localizedContext.getString(R.string.label_vertical_offset, verticalOffsetValueText),
            value = settings.verticalOffsetDp,
            range = -32..32,
            step = 2,
            onPreviewValue = { onPreviewChange(settings.copy(verticalOffsetDp = it)) },
            onCommitValue = { onCommitSettings(settings.copy(verticalOffsetDp = it)) },
        )
        IntSliderSetting(
            label = localizedContext.getString(R.string.label_line_spacing, settings.lineSpacingDp),
            value = settings.lineSpacingDp,
            range = 2..14,
            onPreviewValue = { onPreviewChange(settings.copy(lineSpacingDp = it)) },
            onCommitValue = { onCommitSettings(settings.copy(lineSpacingDp = it)) },
        )
    }
}

@Composable
private fun ContentBehaviorCard(
    localizedContext: Context,
    settings: QuickSpaceSettings,
    onCommitSettings: (QuickSpaceSettings) -> Unit,
) {
    SettingsSectionCard(title = localizedContext.getString(R.string.section_content)) {
        SwitchRow(
            title = localizedContext.getString(R.string.label_show_quotes),
            subtitle = localizedContext.getString(R.string.desc_show_quotes),
            checked = settings.showQuotes,
            onCheckedChange = { onCommitSettings(settings.copy(showQuotes = it)) },
        )

        SwitchRow(
            title = localizedContext.getString(R.string.label_crdroid_prob),
            subtitle = localizedContext.getString(R.string.desc_crdroid_prob),
            checked = settings.crDroidProbabilityHide,
            enabled = settings.showQuotes,
            onCheckedChange = { onCommitSettings(settings.copy(crDroidProbabilityHide = it)) },
        )

        SwitchRow(
            title = localizedContext.getString(R.string.label_easter_eggs),
            subtitle = localizedContext.getString(R.string.desc_easter_eggs),
            checked = settings.enableEasterEggs,
            enabled = settings.showQuotes,
            onCheckedChange = { onCommitSettings(settings.copy(enableEasterEggs = it)) },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        OptionLabel(localizedContext.getString(R.string.label_quote_click))
        val clickActions = QuoteClickAction.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            clickActions.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = settings.quoteClickAction == item,
                    onClick = { onCommitSettings(settings.copy(quoteClickAction = item)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = clickActions.size),
                    label = {
                        Text(
                            text = localizedContext.getString(
                                when (item) {
                                    QuoteClickAction.NEXT_QUOTE -> R.string.click_next_quote
                                    QuoteClickAction.OPEN_CALENDAR -> R.string.click_open_calendar
                                }
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
        Text(
            text = localizedContext.getString(R.string.desc_title_click),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RefreshInfoCard(
    localizedContext: Context,
    onSyncWidgets: () -> Unit,
) {
    SettingsSectionCard(title = localizedContext.getString(R.string.section_refresh)) {
        Text(
            text = localizedContext.getString(R.string.refresh_info_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(
            onClick = onSyncWidgets,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(localizedContext.getString(R.string.action_sync_widgets))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuotesBrowserSheet(
    localizedContext: Context,
    onDismiss: () -> Unit,
    onPickQuote: (QuoteCategory, Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categories = remember {
        listOf(
            QuoteCategory.MORNING to R.string.quotes_cat_morning,
            QuoteCategory.NOON to R.string.quotes_cat_noon,
            QuoteCategory.EARLY_EVENING to R.string.quotes_cat_early_evening,
            QuoteCategory.EVENING to R.string.quotes_cat_evening,
            QuoteCategory.MIDNIGHT to R.string.quotes_cat_midnight,
            QuoteCategory.RANDOM to R.string.quotes_cat_random,
        )
    }
    var selectedCategory by remember { mutableStateOf(QuoteCategory.MORNING) }
    val totalCount = remember(localizedContext) {
        categories.sumOf { (cat, _) ->
            QuickSpaceContentEngine.arrayResForCategory(cat)
                ?.let { localizedContext.resources.getStringArray(it).size } ?: 0
        }
    }
    val items = remember(localizedContext, selectedCategory) {
        QuickSpaceContentEngine.arrayResForCategory(selectedCategory)
            ?.let { localizedContext.resources.getStringArray(it).toList() }
            ?: emptyList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = localizedContext.getString(R.string.quotes_sheet_title, totalCount),
                style = MaterialTheme.typography.titleMedium,
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories.size) { i ->
                    val (cat, labelRes) = categories[i]
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(localizedContext.getString(labelRes)) },
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
            ) {
                itemsIndexed(items) { index, quote ->
                    ListItem(
                        headlineContent = { Text(quote) },
                        leadingContent = {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPickQuote(selectedCategory, index) },
                    )
                    if (index < items.lastIndex) {
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun OptionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                },
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun IntSliderSetting(
    label: String,
    value: Int,
    range: IntRange,
    step: Int = 1,
    onPreviewValue: (Int) -> Unit,
    onCommitValue: (Int) -> Unit,
) {
    var sliderPos by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val stepsCount = remember(range, step) {
        ((range.last - range.first) / step - 1).coerceAtLeast(0)
    }
    val snap: (Float) -> Int = { raw ->
        (((raw - range.first) / step).roundToInt() * step + range.first)
            .coerceIn(range.first, range.last)
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = sliderPos,
            onValueChange = { raw ->
                sliderPos = raw
                val snapped = snap(raw)
                if (snapped != value) {
                    onPreviewValue(snapped)
                }
            },
            onValueChangeFinished = {
                val snapped = snap(sliderPos)
                sliderPos = snapped.toFloat()
                onCommitValue(snapped)
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = stepsCount,
        )
    }
}
