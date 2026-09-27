package com.fluidglass.launcherwidget

import android.app.ActivityOptions
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.layout.Alignment as GlanceAlignment
import androidx.glance.layout.Box as GlanceBox
import androidx.glance.layout.Column as GlanceColumn
import androidx.glance.layout.Row as GlanceRow
import androidx.glance.layout.Spacer as GlanceSpacer
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight as GlanceFontWeight
import androidx.glance.text.Text as GlanceText
import androidx.glance.text.TextStyle as GlanceTextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


/**
 * 10 Distinct Design Presets for the Liquid Glass Widget Engine.
 */
enum class WidgetPreset(
    val title: String,
    val description: String,
    val defaultIcon: String
) {
    MINIMAL_GLASS_PILL("Minimal Pill", "Ultra compact icon & label", "▶"),
    CYBER_NEON_GLASS("Cyber Neon", "Glowing border accent with badge", "⚡"),
    MEDIA_CARD("Media Card", "Banner layout with YouTube tag", "🎬"),
    ISOMETRIC_TILE("Isometric Tile", "Stacked floating glass aesthetic", "💎"),
    CIRCULAR_AURA_PLATE("Circular Aura", "Adaptive round dial layout", "🌌"),
    DYNAMIC_SPLIT_CARD("Split Card", "Two-tone primary & secondary", "🌓"),
    FLOATING_CAPSULE("Floating Capsule", "Minimal floating pill action", "🚀"),
    GLASS_BADGE("Glass Badge", "Micro-typography pill tag", "🏷️"),
    GLANCE_PORTAL("Glance Portal", "Concentric soft aura rings", "🌀"),
    FULL_CANVAS_CARD("Full Canvas", "Custom photo with glass overlay", "🖼️")
}

/**
 * Supported Typography Profiles.
 */
enum class WidgetFontFamily(val displayName: String, val composeFont: FontFamily) {
    SYSTEM_DEFAULT("Default", FontFamily.Default),
    SANS_SERIF_MONO("Monospace", FontFamily.Monospace),
    SERIF("Serif", FontFamily.Serif),
    ROUNDED("Cursive", FontFamily.Cursive)
}

/**
 * Configuration Entity saved to local DataStore.
 */
@Serializable
data class WidgetConfig(
    val url: String = "https://youtube.com",
    val targetPackage: String? = null,
    val targetAppLabel: String = "Default Browser",
    val preset: String = WidgetPreset.MINIMAL_GLASS_PILL.name,
    val primaryColorHex: String = "#7C3AED",     // Deep Purple
    val secondaryColorHex: String = "#38BDF8",   // Electric Blue
    val backgroundAlpha: Float = 0.22f,
    val labelText: String = "Open Stream",
    val emojiIcon: String = "▶",
    val fontSizeSp: Int = 14,
    val fontFamilyName: String = WidgetFontFamily.SYSTEM_DEFAULT.name,
    val customImageUriString: String? = null
)

// DataStore Extension
val Context.widgetDataStore by preferencesDataStore(name = "widget_glass_preferences")
val PREF_WIDGET_CONFIG_KEY = stringPreferencesKey("saved_widget_config")

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FluidGlassAppTheme {
                MainDashboardScreen(
                    onPinWidgetRequested = { config ->
                        requestPinWidget(this, config)
                    }
                )
            }
        }
    }

    private fun requestPinWidget(context: Context, config: WidgetConfig) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, FluidGlassWidgetReceiver::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val successCallbackIntent = Intent(context, WidgetPinCallbackReceiver::class.java)
                val successPendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    successCallbackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )

                appWidgetManager.requestPinAppWidget(provider, null, successPendingIntent)
                Toast.makeText(context, "Place widget on your home screen", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Launcher does not support in-app pinning", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Use home screen widget menu to add", Toast.LENGTH_LONG).show()
        }
    }
}

private val PureBlack = Color(0xFF000000)
private val SurfaceDark = Color(0xFF0D0D12)
private val GlassBorderStroke = Color(0x38FFFFFF)
private val GlassBackgroundTint = Color(0x1AFFFFFF)

@Composable
fun FluidGlassAppTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFF7C3AED),
        secondary = Color(0xFF38BDF8),
        tertiary = Color(0xFFA855F7),
        background = PureBlack,
        surface = SurfaceDark,
        onPrimary = Color.White,
        onSecondary = PureBlack,
        onBackground = Color(0xFFF1F5F9),
        onSurface = Color(0xFFE2E8F0)
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        typography = Typography(),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    onPinWidgetRequested: (WidgetConfig) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var config by remember { mutableStateOf(WidgetConfig()) }
    var installedApps by remember { mutableStateOf<List<AppPackageItem>>(emptyList()) }
    var isPackagePickerExpanded by remember { mutableStateOf(false) }

    // Read stored configuration on launch
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val jsonStr = context.widgetDataStore.data.map { it[PREF_WIDGET_CONFIG_KEY] }.first()
            if (!jsonStr.isNullOrBlank()) {
                runCatching {
                    config = Json.decodeFromString<WidgetConfig>(jsonStr)
                }
            }
            // Fetch installed packages
            installedApps = loadInstalledLaunchers(context)
        }
    }

    // Photo picker contract for Widget Background
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val localPath = copyUriToInternalStorage(context, uri)
                config = config.copy(customImageUriString = localPath)
                persistConfiguration(context, config)
            }
        }
    }

    Scaffold(
        containerColor = PureBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF7C3AED), Color(0xFF38BDF8))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Widgets,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Liquid Glass Studio",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureBlack.copy(alpha = 0.8f)
                ),
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            persistConfiguration(context, config)
                            FluidGlassWidget().updateAll(context)
                            Toast.makeText(context, "Widgets refreshed", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Filled.Sync, contentDescription = "Sync", tint = Color(0xFF38BDF8))
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = SurfaceDark.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, GlassBorderStroke)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                persistConfiguration(context, config)
                                FluidGlassWidget().updateAll(context)
                                Toast.makeText(context, "Configuration Saved", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.6f))
                    ) {
                        Text("Save Preset", color = Color.White)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                persistConfiguration(context, config)
                                FluidGlassWidget().updateAll(context)
                                onPinWidgetRequested(config)
                            }
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF7C3AED), Color(0xFF2563EB))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AddCircle, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Pin to Home Screen",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "LIVE REAL-TIME PREVIEW",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF94A3B8)
            )

            LiveWidgetPreviewContainer(config = config)

            UrlInputCard(
                url = config.url,
                onUrlChange = { newUrl ->
                    config = config.copy(url = newUrl)
                }
            )

            TargetAppSelector(
                selectedPackage = config.targetPackage,
                selectedLabel = config.targetAppLabel,
                installedApps = installedApps,
                isExpanded = isPackagePickerExpanded,
                onToggleExpand = { isPackagePickerExpanded = !isPackagePickerExpanded },
                onSelectApp = { pkg, label ->
                    config = config.copy(targetPackage = pkg, targetAppLabel = label)
                    isPackagePickerExpanded = false
                }
            )

            PresetSelectorCarousel(
                selectedPresetName = config.preset,
                onSelectPreset = { preset ->
                    config = config.copy(
                        preset = preset.name,
                        emojiIcon = preset.defaultIcon
                    )
                }
            )

            DeepCustomizerCard(
                config = config,
                onConfigUpdate = { updated -> config = updated },
                onPickImage = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun LiveWidgetPreviewContainer(config: WidgetConfig) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E1B4B), PureBlack),
                    radius = 500f
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF7C3AED).copy(alpha = 0.5f),
                        Color(0xFF38BDF8).copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                RoundedCornerShape(26.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle grid dots in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 28.dp.toPx()
            val dotRadius = 1.2.dp.toPx()
            var x = step / 2
            while (x < size.width) {
                var y = step / 2
                while (y < size.height) {
                    drawCircle(Color.White.copy(alpha = 0.05f), radius = dotRadius, center = Offset(x, y))
                    y += step
                }
                x += step
            }
        }

        // Render chosen preset preview inside
        val currentPreset = WidgetPreset.entries.find { it.name == config.preset }
            ?: WidgetPreset.MINIMAL_GLASS_PILL

        WidgetPresetCanvas(preset = currentPreset, config = config)
    }
}

@Composable
fun WidgetPresetCanvas(preset: WidgetPreset, config: WidgetConfig) {
    val primaryColor = parseColorSafely(config.primaryColorHex, Color(0xFF7C3AED))
    val secondaryColor = parseColorSafely(config.secondaryColorHex, Color(0xFF38BDF8))
    val font = WidgetFontFamily.entries.find { it.name == config.fontFamilyName }?.composeFont
        ?: FontFamily.Default

    when (preset) {
        WidgetPreset.MINIMAL_GLASS_PILL -> {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.White.copy(alpha = config.backgroundAlpha))
                    .border(1.dp, GlassBorderStroke, RoundedCornerShape(50.dp))
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(config.emojiIcon, fontSize = (config.fontSizeSp + 4).sp)
                Text(
                    config.labelText,
                    color = Color.White,
                    fontFamily = font,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = config.fontSizeSp.sp
                )
            }
        }

        WidgetPreset.CYBER_NEON_GLASS -> {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PureBlack.copy(alpha = 0.8f))
                    .border(
                        2.dp,
                        Brush.horizontalGradient(listOf(primaryColor, secondaryColor)),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(config.emojiIcon, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        config.labelText,
                        color = Color.White,
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        fontSize = config.fontSizeSp.sp
                    )
                    Text(
                        config.targetAppLabel,
                        color = secondaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        WidgetPreset.MEDIA_CARD -> {
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                primaryColor.copy(alpha = config.backgroundAlpha),
                                SurfaceDark.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .border(1.dp, GlassBorderStroke, RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(config.emojiIcon, fontSize = 20.sp)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = primaryColor.copy(alpha = 0.3f),
                            border = BorderStroke(0.5.dp, primaryColor)
                        ) {
                            Text(
                                "MEDIA",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        config.labelText,
                        color = Color.White,
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        fontSize = config.fontSizeSp.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        WidgetPreset.ISOMETRIC_TILE -> {
            Box(
                modifier = Modifier
                    .offset(x = 6.dp, y = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(secondaryColor.copy(alpha = 0.2f))
                    .padding(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .offset(x = (-6).dp, y = (-6).dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceDark)
                        .border(1.5.dp, primaryColor, RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(config.emojiIcon, fontSize = 20.sp)
                    Text(
                        config.labelText,
                        color = Color.White,
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        fontSize = config.fontSizeSp.sp
                    )
                }
            }
        }

        WidgetPreset.CIRCULAR_AURA_PLATE -> {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(primaryColor.copy(alpha = 0.4f), secondaryColor.copy(alpha = 0.2f), primaryColor.copy(alpha = 0.4f))
                        )
                    )
                    .border(2.dp, primaryColor.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(config.emojiIcon, fontSize = 24.sp)
                    Text(
                        config.labelText.take(8),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        WidgetPreset.DYNAMIC_SPLIT_CARD -> {
            Row(
                modifier = Modifier
                    .width(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, GlassBorderStroke, RoundedCornerShape(20.dp))
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.35f)
                        .background(primaryColor)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(config.emojiIcon, fontSize = 22.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(0.65f)
                        .background(SurfaceDark)
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        config.labelText,
                        color = Color.White,
                        fontFamily = font,
                        fontSize = config.fontSizeSp.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        WidgetPreset.FLOATING_CAPSULE -> {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(primaryColor.copy(alpha = 0.35f), secondaryColor.copy(alpha = 0.35f))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(32.dp))
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(config.emojiIcon, fontSize = 18.sp)
                Text(
                    config.labelText,
                    color = Color.White,
                    fontFamily = font,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = config.fontSizeSp.sp
                )
            }
        }

        WidgetPreset.GLASS_BADGE -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, GlassBorderStroke)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(secondaryColor)
                    )
                    Text(
                        config.labelText.uppercase(),
                        color = Color.White,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        WidgetPreset.GLANCE_PORTAL -> {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(1.dp, primaryColor.copy(alpha = 0.25f), CircleShape)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, secondaryColor.copy(alpha = 0.6f), CircleShape)
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                Text(config.emojiIcon, fontSize = 28.sp)
            }
        }

        WidgetPreset.FULL_CANVAS_CARD -> {
            Box(
                modifier = Modifier
                    .width(230.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(primaryColor.copy(alpha = 0.5f), PureBlack)
                        )
                    )
                    .border(1.dp, GlassBorderStroke, RoundedCornerShape(22.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(config.emojiIcon, fontSize = 22.sp)
                    Column {
                        Text(
                            config.labelText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = config.fontSizeSp.sp
                        )
                        Text(
                            "Custom Background Canvas",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UrlInputCard(
    url: String,
    onUrlChange: (String) -> Unit
) {
    val context = LocalContext.current
    val isValidUrl = remember(url) {
        url.startsWith("http://") || url.startsWith("https://")
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, GlassBorderStroke)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TARGET STREAM / WEB URL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color(0xFF94A3B8)
                )

                // Quick Paste button
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val text = clip.getItemAt(0).text?.toString().orEmpty()
                            if (text.isNotBlank()) {
                                onUrlChange(text)
                            }
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Outlined.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto Paste", fontSize = 12.sp, color = Color(0xFF38BDF8))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("https://youtube.com/watch?v=...", color = Color(0xFF64748B)) },
                trailingIcon = {
                    if (url.isNotEmpty()) {
                        IconButton(onClick = { onUrlChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                        }
                    }
                },
                isError = url.isNotEmpty() && !isValidUrl,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = GlassBorderStroke,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            if (url.isNotEmpty() && !isValidUrl) {
                Text(
                    "Please enter a valid URL starting with http:// or https://",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }
        }
    }
}

data class AppPackageItem(
    val packageName: String,
    val appName: String,
    val isSuggested: Boolean = false
)

@Composable
fun TargetAppSelector(
    selectedPackage: String?,
    selectedLabel: String,
    installedApps: List<AppPackageItem>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectApp: (String?, String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, GlassBorderStroke)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "ROUTING DESTINATION APP",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Picks Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val suggestions = listOf(
                    Triple(null, "System Browser", "🌐"),
                    Triple("com.google.android.youtube", "YouTube", "▶"),
                    Triple("org.schabi.newpipe", "NewPipe", "🔴"),
                    Triple("app.revanced.android.youtube", "ReVanced", "⚡"),
                    Triple("com.brave.browser", "Brave", "🦁"),
                    Triple("com.android.chrome", "Chrome", "⭕")
                )

                suggestions.forEach { (pkg, label, emoji) ->
                    val isSelected = selectedPackage == pkg
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectApp(pkg, label) },
                        label = { Text("$emoji $label", fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF7C3AED).copy(alpha = 0.35f),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.05f),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Color(0xFF7C3AED) else GlassBorderStroke
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dropdown trigger for all installed apps
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggleExpand() },
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, GlassBorderStroke)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = selectedLabel,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = selectedPackage ?: "Implicit System Intent Action",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .padding(top = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    installedApps.forEach { appItem ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelectApp(appItem.packageName, appItem.appName)
                                },
                            color = if (selectedPackage == appItem.packageName) Color(0xFF7C3AED).copy(alpha = 0.2f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    appItem.appName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    appItem.packageName.takeLast(20),
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PresetSelectorCarousel(
    selectedPresetName: String,
    onSelectPreset: (WidgetPreset) -> Unit
) {
    Column {
        Text(
            "SELECT DESIGN PRESET (10 STYLES)",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WidgetPreset.entries.forEach { preset ->
                val isSelected = preset.name == selectedPresetName
                Surface(
                    modifier = Modifier
                        .width(130.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelectPreset(preset) },
                    color = if (isSelected) Color(0xFF7C3AED).copy(alpha = 0.25f) else SurfaceDark,
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) Color(0xFF38BDF8) else GlassBorderStroke
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(preset.defaultIcon, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            preset.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            preset.description,
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeepCustomizerCard(
    config: WidgetConfig,
    onConfigUpdate: (WidgetConfig) -> Unit,
    onPickImage: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, GlassBorderStroke)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "DEEP CUSTOMIZATION",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF94A3B8)
            )

            // Label & Emoji customization
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = config.emojiIcon,
                    onValueChange = { onConfigUpdate(config.copy(emojiIcon = it.take(2))) },
                    modifier = Modifier.width(70.dp),
                    label = { Text("Icon", fontSize = 11.sp) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = GlassBorderStroke,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = config.labelText,
                    onValueChange = { onConfigUpdate(config.copy(labelText = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Widget Title / Label", fontSize = 11.sp) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = GlassBorderStroke,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }

            // Typography Selector
            Column {
                Text("Typography Profile", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WidgetFontFamily.entries.forEach { font ->
                        val isSelected = font.name == config.fontFamilyName
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onConfigUpdate(config.copy(fontFamilyName = font.name)) },
                            color = if (isSelected) Color(0xFF7C3AED).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF7C3AED) else GlassBorderStroke
                            )
                        ) {
                            Text(
                                font.displayName,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                color = Color.White,
                                fontFamily = font.composeFont
                            )
                        }
                    }
                }
            }

            // Sliders: Font Size and Background Alpha
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Font Size: ${config.fontSizeSp}sp", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                    Text("Alpha: ${(config.backgroundAlpha * 100).toInt()}%", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                }
                Slider(
                    value = config.fontSizeSp.toFloat(),
                    onValueChange = { onConfigUpdate(config.copy(fontSizeSp = it.toInt())) },
                    valueRange = 10f..24f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF7C3AED)
                    )
                )
                Slider(
                    value = config.backgroundAlpha,
                    onValueChange = { onConfigUpdate(config.copy(backgroundAlpha = it)) },
                    valueRange = 0.05f..0.85f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF7C3AED),
                        activeTrackColor = Color(0xFF38BDF8)
                    )
                )
            }

            // Interactive Color Wheel Picker for Primary & Secondary
            InteractiveColorPaletteSelector(
                primaryColorHex = config.primaryColorHex,
                secondaryColorHex = config.secondaryColorHex,
                onPrimaryColorChange = { onConfigUpdate(config.copy(primaryColorHex = it)) },
                onSecondaryColorChange = { onConfigUpdate(config.copy(secondaryColorHex = it)) }
            )

            // Custom Background Photo Button
            OutlinedButton(
                onClick = onPickImage,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GlassBorderStroke)
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, tint = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (config.customImageUriString != null) "Change Widget Photo" else "Pick Widget Background Image",
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun InteractiveColorPaletteSelector(
    primaryColorHex: String,
    secondaryColorHex: String,
    onPrimaryColorChange: (String) -> Unit,
    onSecondaryColorChange: (String) -> Unit
) {
    var isSelectingPrimary by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Color Spectrum", fontSize = 12.sp, color = Color(0xFFCBD5E1))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Primary color tag
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isSelectingPrimary = true },
                    color = if (isSelectingPrimary) Color(0xFF7C3AED).copy(alpha = 0.4f) else Color.Transparent,
                    border = BorderStroke(1.dp, parseColorSafely(primaryColorHex, Color(0xFF7C3AED)))
                ) {
                    Text(
                        "Primary",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }

                // Secondary color tag
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isSelectingPrimary = false },
                    color = if (!isSelectingPrimary) Color(0xFF38BDF8).copy(alpha = 0.4f) else Color.Transparent,
                    border = BorderStroke(1.dp, parseColorSafely(secondaryColorHex, Color(0xFF38BDF8)))
                ) {
                    Text(
                        "Secondary",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }
        }

        // HSV Gradient Strip & Swatches
        val quickSwatches = listOf(
            "#7C3AED", "#A855F7", "#2563EB", "#38BDF8",
            "#10B981", "#F59E0B", "#EF4444", "#EC4899", "#FFFFFF"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            quickSwatches.forEach { hex ->
                val swatchColor = parseColorSafely(hex, Color.White)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable {
                            if (isSelectingPrimary) onPrimaryColorChange(hex) else onSecondaryColorChange(hex)
                        }
                )
            }
        }
    }
}

/**
 * Production Jetpack Glance Widget.
 * Highly battery optimized: updatePeriodMillis = 0 (Zero background wakeups).
 */
class FluidGlassWidget : GlanceAppWidget() {

    // Define responsive size breakpoints for 1x1, 2x1, 2x2, 4x2, etc.
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(100.dp, 48.dp),
            DpSize(180.dp, 80.dp),
            DpSize(260.dp, 120.dp)
        )
    )

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Read persisted config
        val jsonStr = context.widgetDataStore.data.map { it[PREF_WIDGET_CONFIG_KEY] }.first()
        val config = if (!jsonStr.isNullOrBlank()) {
            runCatching { Json.decodeFromString<WidgetConfig>(jsonStr) }.getOrDefault(WidgetConfig())
        } else {
            WidgetConfig()
        }

        provideContent {
            val size = LocalSize.current
            GlanceWidgetContent(context = context, config = config, widgetSize = size)
        }
    }
}

@Composable
fun GlanceWidgetContent(
    context: Context,
    config: WidgetConfig,
    widgetSize: DpSize
) {
    val primaryColorProvider = ColorProvider(parseColorSafely(config.primaryColorHex, Color(0xFF7C3AED)))
    val secondaryColorProvider = ColorProvider(parseColorSafely(config.secondaryColorHex, Color(0xFF38BDF8)))
    val preset = WidgetPreset.entries.find { it.name == config.preset } ?: WidgetPreset.MINIMAL_GLASS_PILL

    // Construct the optimized launch action
    val launchAction = actionRunCallback<LaunchUrlActionCallback>(
        actionParametersOf(
            ActionParameters.Key<String>("TARGET_URL") to config.url,
            ActionParameters.Key<String?>("TARGET_PKG") to config.targetPackage
        )
    )

    GlanceBox(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(launchAction),
        contentAlignment = GlanceAlignment.Center
    ) {
        when (preset) {
            WidgetPreset.MINIMAL_GLASS_PILL -> {
                GlanceRow(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(32.dp)
                        .background(ColorProvider(Color.Black.copy(alpha = 0.8f)))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = GlanceAlignment.CenterVertically,
                    horizontalAlignment = GlanceAlignment.CenterHorizontally
                ) {
                    GlanceText(
                        text = config.emojiIcon,
                        style = GlanceTextStyle(
                            fontSize = 18.sp,
                            color = ColorProvider(Color.White)
                        )
                    )
                    GlanceSpacer(modifier = GlanceModifier.width(8.dp))
                    GlanceText(
                        text = config.labelText,
                        style = GlanceTextStyle(
                            color = ColorProvider(Color.White),
                            fontWeight = GlanceFontWeight.Bold,
                            fontSize = config.fontSizeSp.sp
                        )
                    )
                }
            }

            WidgetPreset.CYBER_NEON_GLASS -> {
                GlanceRow(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(18.dp)
                        .background(ColorProvider(Color(0xFF0F172A)))
                        .padding(14.dp),
                    verticalAlignment = GlanceAlignment.CenterVertically
                ) {
                    GlanceText(
                        text = config.emojiIcon,
                        style = GlanceTextStyle(fontSize = 20.sp, color = primaryColorProvider)
                    )
                    GlanceSpacer(modifier = GlanceModifier.width(10.dp))
                    GlanceColumn {
                        GlanceText(
                            text = config.labelText,
                            style = GlanceTextStyle(
                                color = ColorProvider(Color.White),
                                fontWeight = GlanceFontWeight.Bold,
                                fontSize = config.fontSizeSp.sp
                            )
                        )
                        GlanceText(
                            text = config.targetAppLabel,
                            style = GlanceTextStyle(
                                color = secondaryColorProvider,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            WidgetPreset.MEDIA_CARD -> {
                GlanceColumn(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(20.dp)
                        .background(ColorProvider(Color(0xFF18181B)))
                        .padding(14.dp),
                    verticalAlignment = GlanceAlignment.CenterVertically,
                    horizontalAlignment = GlanceAlignment.Start
                ) {
                    GlanceRow(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = GlanceAlignment.CenterVertically
                    ) {
                        GlanceText(
                            text = config.emojiIcon,
                            style = GlanceTextStyle(fontSize = 18.sp, color = primaryColorProvider)
                        )
                        GlanceSpacer(modifier = GlanceModifier.defaultWeight())
                        GlanceText(
                            text = "STREAM",
                            style = GlanceTextStyle(
                                color = secondaryColorProvider,
                                fontWeight = GlanceFontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                    GlanceSpacer(modifier = GlanceModifier.height(6.dp))
                    GlanceText(
                        text = config.labelText,
                        style = GlanceTextStyle(
                            color = ColorProvider(Color.White),
                            fontWeight = GlanceFontWeight.Bold,
                            fontSize = config.fontSizeSp.sp
                        )
                    )
                }
            }

            WidgetPreset.DYNAMIC_SPLIT_CARD -> {
                GlanceRow(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(18.dp)
                        .background(ColorProvider(Color(0xFF1E293B)))
                ) {
                    GlanceBox(
                        modifier = GlanceModifier
                            .width(50.dp)
                            .fillMaxHeight()
                            .background(primaryColorProvider),
                        contentAlignment = GlanceAlignment.Center
                    ) {
                        GlanceText(
                            text = config.emojiIcon,
                            style = GlanceTextStyle(fontSize = 20.sp, color = ColorProvider(Color.White))
                        )
                    }
                    GlanceBox(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = GlanceAlignment.CenterStart
                    ) {
                        GlanceText(
                            text = config.labelText,
                            style = GlanceTextStyle(
                                color = ColorProvider(Color.White),
                                fontWeight = GlanceFontWeight.Medium,
                                fontSize = config.fontSizeSp.sp
                            )
                        )
                    }
                }
            }

            WidgetPreset.CIRCULAR_AURA_PLATE, WidgetPreset.GLANCE_PORTAL -> {
                GlanceBox(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(40.dp)
                        .background(ColorProvider(Color(0xFF111827))),
                    contentAlignment = GlanceAlignment.Center
                ) {
                    GlanceColumn(
                        horizontalAlignment = GlanceAlignment.CenterHorizontally
                    ) {
                        GlanceText(
                            text = config.emojiIcon,
                            style = GlanceTextStyle(fontSize = 22.sp, color = primaryColorProvider)
                        )
                        GlanceText(
                            text = config.labelText.take(7),
                            style = GlanceTextStyle(
                                color = ColorProvider(Color.White),
                                fontWeight = GlanceFontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            else -> {
                // Generic Fallback & Capsule
                GlanceRow(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(24.dp)
                        .background(ColorProvider(Color(0xFF09090B)))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = GlanceAlignment.CenterVertically,
                    horizontalAlignment = GlanceAlignment.CenterHorizontally
                ) {
                    GlanceText(
                        text = config.emojiIcon,
                        style = GlanceTextStyle(fontSize = 18.sp, color = secondaryColorProvider)
                    )
                    GlanceSpacer(modifier = GlanceModifier.width(8.dp))
                    GlanceText(
                        text = config.labelText,
                        style = GlanceTextStyle(
                            color = ColorProvider(Color.White),
                            fontWeight = GlanceFontWeight.Bold,
                            fontSize = config.fontSizeSp.sp
                        )
                    )
                }
            }
        }
    }
}

class LaunchUrlActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val targetUrl = parameters[ActionParameters.Key<String>("TARGET_URL")] ?: "https://youtube.com"
        val targetPackage = parameters[ActionParameters.Key<String?>("TARGET_PKG")]

        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!targetPackage.isNullOrBlank()) {
                setPackage(targetPackage)
            }
        }

        val launchIntent = if (targetPackage != null && isIntentAvailable(context, viewIntent)) {
            viewIntent
        } else {
            // Fallback to implicit browser VIEW
            Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        // Apply smooth transition options
        val options = ActivityOptions.makeCustomAnimation(
            context,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )

        ContextCompat.startActivity(context, launchIntent, options.toBundle())
    }

    private fun isIntentAvailable(context: Context, intent: Intent): Boolean {
        return context.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        ).isNotEmpty()
    }
}

class FluidGlassWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FluidGlassWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Re-render glance state reliably across reboots
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                FluidGlassWidget().updateAll(context)
            }
        }
    }
}

class WidgetPinCallbackReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "Widget successfully pinned to Home Screen!", Toast.LENGTH_SHORT).show()
    }
}

suspend fun persistConfiguration(context: Context, config: WidgetConfig) {
    withContext(Dispatchers.IO) {
        val json = Json.encodeToString(config)
        context.widgetDataStore.edit { preferences ->
            preferences[PREF_WIDGET_CONFIG_KEY] = json
        }
    }
}

suspend fun copyUriToInternalStorage(context: Context, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "widget_custom_bg.png")
        val outputStream = FileOutputStream(file)
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    }
}

fun loadInstalledLaunchers(context: Context): List<AppPackageItem> {
    val pm = context.packageManager
    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
    return resolveInfos.mapNotNull { info ->
        val pkg = info.activityInfo.packageName
        val label = info.loadLabel(pm).toString()
        if (pkg != context.packageName) {
            AppPackageItem(packageName = pkg, appName = label)
        } else null
    }.sortedBy { it.appName }
}

fun parseColorSafely(hex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}