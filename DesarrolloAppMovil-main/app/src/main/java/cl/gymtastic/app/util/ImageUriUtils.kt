package cl.gymtastic.app.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

object ImageUriUtils {


    //* Crea una URI temporal en el caché para guardar una foto de la cámara.
    //* (Esta es la función que ya tenías)

    fun createTempImageUri(context: Context): Uri {
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val image = File(imagesDir, "IMG_${time}.jpg")
        return FileProvider.getUriForFile(context, "cl.gymtastic.app.fileprovider", image)
    }

    //**
    //* (NUEVA FUNCIÓN - Requerida por AdminScreen)
    //* Copia el contenido de una URI (ej. de la galería) al almacenamiento interno
    //* de la app, dándole un permiso permanente.
    //* Devuelve la URI (como String) del archivo copiado.

    fun copyUriToInternalStorage(
        context: Context,
        uri: Uri,
        fileNamePrefix: String
    ): String? {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Intenta obtener la extensión original, si no, usa "jpg"
            val extension = context.contentResolver.getType(uri)?.split("/")?.lastOrNull() ?: "jpg"
            val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.$extension"

            // Guarda en el directorio 'files' (privado de la app)
            val file = File(context.filesDir, fileName)

            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            // Devuelve la URI del *nuevo* archivo interno (file://...)
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    // (NUEVA FUNCIÓN - Requerida por AdminScreen)
    //* Elimina un archivo del almacenamiento interno usando su URI (String)
    //* Importante para limpiar imágenes cuando se borra o cambia un producto.

    fun deleteFileFromInternalStorage(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return try {
            val fileUri = Uri.parse(uriString)
            // Solo borramos si es un archivo local (file://)
            if (fileUri.scheme == "file" && fileUri.path != null) {
                File(fileUri.path!!).delete()
            } else {
                false // No borramos URLs http://
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

