package com.rodrigoleao.gramado2026.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

fun openAssetFile(context: Context, assetPath: String) {
    try {
        val fileName = File(assetPath).name
        val cacheFile = File(context.cacheDir, fileName)
        context.assets.open(assetPath).use { input ->
            cacheFile.outputStream().use { output -> input.copyTo(output) }
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )
        val mime = when {
            assetPath.endsWith(".pdf",  ignoreCase = true) -> "application/pdf"
            assetPath.endsWith(".jpeg", ignoreCase = true) ||
            assetPath.endsWith(".jpg",  ignoreCase = true) -> "image/jpeg"
            assetPath.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "*/*"
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Arquivo não encontrado. Copie os vouchers para app/src/main/assets/vouchers/",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun openInternalFile(context: Context, absolutePath: String) {
    try {
        val file = File(absolutePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mime = when {
            absolutePath.endsWith(".pdf",  ignoreCase = true) -> "application/pdf"
            absolutePath.endsWith(".jpeg", ignoreCase = true) ||
            absolutePath.endsWith(".jpg",  ignoreCase = true) -> "image/jpeg"
            absolutePath.endsWith(".png",  ignoreCase = true) -> "image/png"
            absolutePath.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "*/*"
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Não foi possível abrir o arquivo.", Toast.LENGTH_SHORT).show()
    }
}

fun dialPhone(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
    context.startActivity(intent)
}

fun openWhatsApp(context: Context, phone: String) {
    val number = if (phone.startsWith("55")) phone else "55$phone"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number"))
    context.startActivity(intent)
}
