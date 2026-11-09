package com.pedrohmsouza.sliced.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Cópias locais das fotos/GIFs dos exercícios (interface para os testes usarem uma versão falsa). */
interface MediaFiles {
    /** Copia a imagem e devolve o caminho da cópia (ou null se não deu para ler). */
    suspend fun import(uri: Uri): String?

    /** Apaga uma cópia que não é mais usada. */
    suspend fun delete(path: String?)
}

/**
 * Fotos e GIFs escolhidos no celular para os exercícios.
 *
 * O seletor de fotos do Android devolve um endereço temporário (content://), que deixa de
 * funcionar depois. Por isso copiamos o arquivo para a pasta do app e guardamos o caminho
 * da cópia na ficha: a foto continua lá mesmo se for apagada da galeria.
 */
class ExerciseMediaStore(private val context: Context) : MediaFiles {
    private val dir: File get() = File(context.filesDir, "exercise_media").apply { mkdirs() }

    override suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val ext = when (context.contentResolver.getType(uri)) {
                "image/gif" -> "gif"
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val file = File(dir, "${UUID.randomUUID()}.$ext")
            context.contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
            file.absolutePath
        }.getOrNull()
    }

    /** Só mexe em arquivos da pasta do app (nunca numa foto da galeria). */
    override suspend fun delete(path: String?) = withContext(Dispatchers.IO) {
        val f = path?.let(::File) ?: return@withContext
        if (f.parentFile?.canonicalPath == dir.canonicalPath) f.delete()
    }
}
