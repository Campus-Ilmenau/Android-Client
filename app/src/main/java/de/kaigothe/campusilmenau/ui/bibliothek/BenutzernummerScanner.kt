package de.kaigothe.campusilmenau.ui.bibliothek

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import de.kaigothe.campusilmenau.R
import de.kaigothe.campusilmenau.domain.model.library.BenutzernummerFormat
import de.kaigothe.campusilmenau.ui.components.PrimaryButton
import de.kaigothe.campusilmenau.ui.theme.CampusTheme
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.suspendCancellableCoroutine

/** Was der Scanner gerade zeigt. */
sealed interface ScannerState {
    /** Die Berechtigung wird geprüft bzw. das System fragt gerade. */
    data object Checking : ScannerState
    data object Scanning : ScannerState

    /** Nummer erkannt; nach kurzer Pause schließt der Scanner. */
    data class Found(val number: String) : ScannerState

    /** Kein Zugriff auf die Kamera. */
    data object Denied : ScannerState

    /** Die Kamera ist belegt oder nicht vorhanden. */
    data object Unavailable : ScannerState

    val showsCamera: Boolean get() = this is Scanning || this is Found
}

private val ScanFormats = intArrayOf(
    Barcode.FORMAT_CODE_39,
    Barcode.FORMAT_CODE_93,
    Barcode.FORMAT_CODE_128,
    Barcode.FORMAT_ITF,
    Barcode.FORMAT_EAN_8,
    Barcode.FORMAT_EAN_13,
    Barcode.FORMAT_CODABAR,
)

/**
 * Vollbild-Scanner für die Benutzernummer auf der Rückseite der Thoska (Barcode, nicht Text). Die Nummer bleibt im
 * Speicher wie eine getippte; es werden keine Bilder gespeichert und nichts geloggt. [onScanned] bekommt die geprüfte
 * Nummer, [onCancel] schließt ohne Nummer, [onEnterManually] schließt zur Eingabe von Hand.
 */
@Composable
fun BenutzernummerScanner(onScanned: (String) -> Unit, onCancel: () -> Unit, onEnterManually: () -> Unit) {
    val context = LocalContext.current
    var state: ScannerState by remember {
        mutableStateOf(
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) ScannerState.Scanning
            else ScannerState.Checking,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        state = if (granted) ScannerState.Scanning else ScannerState.Denied
    }
    LaunchedEffect(Unit) {
        if (state == ScannerState.Checking) launcher.launch(Manifest.permission.CAMERA)
    }
    val found = state as? ScannerState.Found
    LaunchedEffect(found) {
        if (found != null) {
            delay(600.milliseconds)
            onScanned(found.number)
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BenutzernummerScannerContent(
            state = state,
            onCancel = onCancel,
            onEnterManually = onEnterManually,
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                runCatching { context.startActivity(intent) }
            },
            camera = { onTorchAvailable, torchOn ->
                ScannerCamera(
                    torchOn = torchOn,
                    onTorchAvailable = onTorchAvailable,
                    onNumber = { if (state == ScannerState.Scanning) state = ScannerState.Found(it) },
                    onUnavailable = { state = ScannerState.Unavailable },
                )
            },
        )
    }
}

/** Oberfläche ohne Kamera, damit sie sich in Vorschauen in jedem Zustand zeigen lässt. */
@Composable
fun BenutzernummerScannerContent(
    state: ScannerState,
    onCancel: () -> Unit,
    onEnterManually: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    camera: @Composable (onTorchAvailable: (Boolean) -> Unit, torchOn: Boolean) -> Unit = { _, _ -> },
) {
    val colors = CampusTheme.colors
    val onDark = colors.onBlue
    var hasTorch by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim)) {
        if (state.showsCamera) camera({ hasTorch = it }, torchOn)

        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.bib_scan_titel),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = onDark,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (state.showsCamera && hasTorch) {
                    IconButton(onClick = { torchOn = !torchOn }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = if (torchOn) Icons.Outlined.FlashlightOff else Icons.Outlined.FlashlightOn,
                            contentDescription = stringResource(if (torchOn) R.string.bib_scan_lampe_aus else R.string.bib_scan_lampe_an),
                            tint = onDark,
                        )
                    }
                }
                IconButton(onClick = onCancel, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.bib_scan_schliessen), tint = onDark)
                }
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                when (state) {
                    ScannerState.Checking -> Message(stringResource(R.string.bib_scan_pruefe), null, onDark)
                    ScannerState.Denied -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Message(stringResource(R.string.bib_scan_verweigert_titel), stringResource(R.string.bib_scan_verweigert_text), onDark)
                        PrimaryButton(text = stringResource(R.string.bib_scan_einstellungen), onClick = onOpenSettings)
                    }
                    ScannerState.Unavailable -> Message(stringResource(R.string.bib_scan_nicht_verfuegbar_titel), stringResource(R.string.bib_scan_nicht_verfuegbar_text), onDark)
                    is ScannerState.Scanning, is ScannerState.Found -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .border(3.dp, if (state is ScannerState.Found) colors.green else onDark, RoundedCornerShape(16.dp)),
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(if (state is ScannerState.Found) R.string.bib_scan_gefunden else R.string.bib_scan_hinweis),
                    style = MaterialTheme.typography.bodyLarge,
                    color = onDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                TextButton(onClick = onEnterManually, modifier = Modifier.padding(top = 8.dp).height(48.dp)) {
                    Text(stringResource(R.string.bib_scan_von_hand), color = onDark, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
private fun Message(title: String, text: String?, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color, textAlign = TextAlign.Center)
        if (text != null) Text(text, style = MaterialTheme.typography.bodyMedium, color = color, textAlign = TextAlign.Center)
    }
}

private suspend fun cameraProvider(context: android.content.Context): ProcessCameraProvider = suspendCancellableCoroutine { continuation ->
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener(
        {
            try {
                continuation.resume(future.get())
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        },
        ContextCompat.getMainExecutor(context),
    )
}

/** Vorschau und Barcode-Erkennung über CameraX und ML Kit. Es wird kein Bild gespeichert. */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun ScannerCamera(torchOn: Boolean, onTorchAvailable: (Boolean) -> Unit, onNumber: (String) -> Unit, onUnavailable: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val currentOnNumber by rememberUpdatedState(onNumber)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    val currentOnTorch by rememberUpdatedState(onTorchAvailable)
    var camera by remember { mutableStateOf<Camera?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(ScanFormats[0], *ScanFormats.drop(1).toIntArray()).build())
    }

    LaunchedEffect(Unit) {
        try {
            val provider = cameraProvider(context)
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(executor) { proxy -> analyze(proxy, scanner) { currentOnNumber(it) } }
            provider.unbindAll()
            val bound = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            camera = bound
            currentOnTorch(bound.cameraInfo.hasFlashUnit())
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (_: Exception) {
            currentOnUnavailable()
        }
    }
    LaunchedEffect(camera, torchOn) { camera?.cameraControl?.enableTorch(torchOn) }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            scanner.close()
            executor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun analyze(proxy: ImageProxy, scanner: com.google.mlkit.vision.barcode.BarcodeScanner, onNumber: (String) -> Unit) {
    val image = proxy.image
    if (image == null) {
        proxy.close()
        return
    }
    scanner.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
        .addOnSuccessListener { barcodes ->
            val number = barcodes.firstNotNullOfOrNull { code ->
                code.rawValue?.let { BenutzernummerFormat.normalize(it, isCodabar = code.format == Barcode.FORMAT_CODABAR) }
            }
            if (number != null) onNumber(number)
        }
        .addOnCompleteListener { proxy.close() }
}
