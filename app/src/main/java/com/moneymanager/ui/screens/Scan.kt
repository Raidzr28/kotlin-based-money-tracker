package com.moneymanager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.moneymanager.data.ReceiptGuess
import com.moneymanager.data.money
import com.moneymanager.data.receiptFileFor
import com.moneymanager.data.scanReceipt
import com.moneymanager.ui.Flag
import com.moneymanager.ui.MoneyText
import com.moneymanager.ui.MoneyTheme
import com.moneymanager.ui.MoneyType
import com.moneymanager.ui.Pill
import com.moneymanager.ui.Plate
import com.moneymanager.ui.ScaffoldNote
import com.moneymanager.ui.SectionHeading
import com.moneymanager.ui.WaterPulse
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Photographing a receipt.
 *
 * The reading is always shown before anything is written. A scanner that posts straight to the
 * ledger is a scanner that quietly corrupts it the first time a smudged 8 reads as a 3, and the
 * user has no way of knowing which row is wrong.
 */
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onUse: (ReceiptGuess, Uri) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var photo by remember { mutableStateOf<Uri?>(null) }
    var guess by remember { mutableStateOf<ReceiptGuess?>(null) }
    var reading by remember { mutableStateOf(false) }

    // A capture lands straight in the app's own private storage, so the photo never appears in
    // shared media and nothing else on the device can read it.
    val captureId = rememberSaveable { UUID.randomUUID().toString() }
    val pending = remember(captureId) { receiptFileFor(context, captureId) }
    val pendingUri = remember(pending) {
        FileProvider.getUriForFile(context, "${context.packageName}.files", pending)
    }

    fun read(uri: Uri) {
        photo = uri
        reading = true
        scope.launch {
            guess = scanReceipt(context, uri)
            reading = false
        }
    }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) read(pendingUri)
    }
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(::read) }

    // Tapping "scan" already means "photograph a receipt", so the camera opens on arrival
    // instead of asking a second time. The flag is saved rather than remembered: returning from
    // the camera can rebuild this composable, and a plain `remember` would reopen the camera
    // every time and trap the user in a loop they cannot back out of. Cancelling leaves the
    // buttons below as the way back in.
    var opened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!opened) {
            opened = true
            takePhoto.launch(pendingUri)
        }
    }

    DetailScaffold(title = "Scan a receipt", onBack = onBack) {
        item {
            Plate(Modifier.padding(top = 4.dp), depth = 1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Photograph a receipt and the merchant, date and total are read off it on " +
                            "this device. Nothing is uploaded, and nothing is saved until you " +
                            "confirm it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Pill(
                            "Take a photo", Icons.Rounded.PhotoCamera,
                            { takePhoto.launch(pendingUri) },
                            emphasis = true,
                        )
                        Pill(
                            "Choose one", Icons.Rounded.Description,
                            {
                                pickImage.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        )
                    }
                }
            }
        }

        photo?.let { uri ->
            item {
                Plate(Modifier.padding(top = 14.dp), padding = 0.dp) {
                    ReceiptPreview(uri)
                }
            }
        }

        if (reading) {
            item {
                Plate(Modifier.padding(top = 14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Reading it",
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant,
                        )
                        WaterPulse(Modifier.fillMaxWidth())
                    }
                }
            }
        }

        guess?.let { g ->
            item { SectionHeading("What it read", caption = "Check it before saving") }
            item {
                Plate {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (g.totalMinor == null) {
                            Flag(
                                Icons.Rounded.Description,
                                "No total found. Try a straighter, brighter photo.",
                                water.alert,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Total",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = scheme.onSurfaceVariant,
                                )
                                MoneyText(g.totalMinor, style = MoneyType.large, color = scheme.onSurface)
                            }
                        }
                        Fact("Merchant", g.merchant ?: "not found")
                        Fact("Date", g.date?.toString() ?: "not found, will use today")
                    }
                }
            }

            if (g.lines.isNotEmpty()) {
                item { SectionHeading("Everything it saw", caption = "So you can tell what it misread") }
                item {
                    Plate(depth = 2) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            g.lines.take(40).forEach { line ->
                                Text(
                                    line,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    Modifier.padding(top = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Pill(
                        if (g.totalMinor == null) "Fill it in myself" else "Use ${money(g.totalMinor)}",
                        Icons.Rounded.Check,
                        { photo?.let { onUse(g, it) } },
                        emphasis = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ScaffoldNote(
                        "This opens the normal logging screen with what it read already filled in. " +
                            "Nothing is saved until you tap Log there."
                    )
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/**
 * A look at the photograph that was read.
 *
 * Decoded at a quarter size and off the main thread. A camera on a modern phone hands back a
 * twelve-megapixel image, and decoding one of those at full resolution on the UI thread is both a
 * stutter and, on a smaller device, an out-of-memory crash.
 */
@Composable
private fun ReceiptPreview(uri: Uri) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(
                        stream,
                        null,
                        BitmapFactory.Options().apply { inSampleSize = 4 },
                    )
                }?.asImageBitmap()
            }.getOrNull()
        }
    }

    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = "The receipt you photographed",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f),
        )
    }
}
