package com.moneymanager.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/*
 * The thin Android half of receipt scanning.
 *
 * ML Kit's text recogniser runs entirely on the device: no key, no per-call fee, no image leaving
 * the phone. That last part is the reason it was chosen over anything hosted -- a receipt records
 * where somebody was and what they bought, and it has no business on someone else's server.
 *
 * Deliberately small. Everything that decides what a receipt *means* lives in ReceiptParser,
 * which is pure text and is covered by tests; this file only turns pixels into lines.
 */

/** Where a captured photo is kept, once the user has confirmed the transaction it belongs to. */
fun receiptFileFor(context: Context, id: String): File {
    val dir = File(context.filesDir, "receipts").apply { mkdirs() }
    return File(dir, "$id.jpg")
}

/**
 * Reads a photograph.
 *
 * Returns an empty guess rather than throwing when the image is unreadable: a blurred photo of a
 * crumpled receipt is an ordinary outcome of pointing a camera at one, not an error worth a crash.
 */
suspend fun scanReceipt(context: Context, image: Uri): ReceiptGuess =
    suspendCancellableCoroutine { continuation ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val blank = ReceiptGuess(null, null, null, emptyList())
        runCatching { InputImage.fromFilePath(context, image) }
            .onFailure { continuation.resume(blank) }
            .onSuccess { input ->
                recognizer.process(input)
                    .addOnSuccessListener { result -> continuation.resume(parseReceipt(result.text)) }
                    .addOnFailureListener { continuation.resume(blank) }
                    .addOnCompleteListener { recognizer.close() }
            }
    }
