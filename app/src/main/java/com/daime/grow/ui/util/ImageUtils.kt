package com.daime.grow.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

object ImageUtils {

    /**
     * Redimensiona e comprime uma imagem para o formato WebP.
     * Ideal para upload em servidores como Supabase ou Firebase.
     */
    fun compressImageToWebP(context: Context, uri: Uri, maxWidth: Int = 1024, quality: Int = 75): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

            // 1. Calcula as novas dimensões mantendo a proporção
            val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
            val targetWidth: Int
            val targetHeight: Int

            if (originalBitmap.width > originalBitmap.height) {
                targetWidth = maxWidth
                targetHeight = (maxWidth / ratio).toInt()
            } else {
                targetHeight = maxWidth
                targetWidth = (maxWidth * ratio).toInt()
            }

            // 2. Redimensiona o Bitmap
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

            // 3. Comprime para WebP
            val outputStream = ByteArrayOutputStream()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, outputStream)
            } else {
                @Suppress("DEPRECATION")
                scaledBitmap.compress(Bitmap.CompressFormat.WEBP, quality, outputStream)
            }

            // Limpeza
            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            originalBitmap.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
