package org.akuatech.ksupatcher.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Collections
import java.util.zip.ZipEntry

object RomZipExtractor {

    data class Result(val file: File, val partitionName: String, val sourceZipName: String?)

    fun isLikelyZip(context: Context, uri: Uri): Boolean {
        val name = DocumentFile.fromSingleUri(context, uri)?.name?.lowercase().orEmpty()
        if (name.endsWith(".zip")) return true
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val sig = ByteArray(4)
                val read = stream.read(sig)
                read >= 4 && sig[0] == 0x50.toByte() && sig[1] == 0x4B.toByte()
            } ?: false
        }.getOrDefault(false)
    }

    /**
     * Extract a boot-ish image from a ROM zip URI. If the zip contains a direct
     * `boot.img` / `init_boot.img` entry, that is preferred; otherwise we look
     * for `payload.bin` and run [PayloadDumper] against it.
     *
     * [preferInitBoot] biases the search toward init_boot when both are
     * present (matching the device's partition layout).
     */
    fun extractBootImage(
        context: Context,
        uri: Uri,
        workDir: File,
        preferInitBoot: Boolean,
        onStatus: (String) -> Unit = {},
    ): Result {
        val sourceName = DocumentFile.fromSingleUri(context, uri)?.name
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Unable to open selected zip")

        pfd.use { parcel ->
            FileInputStream(parcel.fileDescriptor).use { fis ->
                val channel = fis.channel
                val zip = ZipFile.builder().setSeekableByteChannel(channel).get()
                zip.use { zf ->
                    val candidateOrder = if (preferInitBoot) listOf("init_boot", "boot") else listOf("boot", "init_boot")

                    findDirectImage(zf, candidateOrder)?.let { (entry, partition) ->
                        onStatus("Copying ${entry.name} from zip...")
                        val output = File(workDir, "$partition.img")
                        zf.getInputStream(entry).use { input ->
                            output.outputStream().use { out -> input.copyTo(out) }
                        }
                        return Result(output, partition, sourceName)
                    }

                    val payload = zf.getEntry("payload.bin")
                        ?: throw IOException(
                            "ROM zip has no boot.img / init_boot.img and no payload.bin"
                        )
                    if (payload.method != ZipEntry.STORED) {
                        throw IOException("payload.bin is compressed inside the zip; can't read")
                    }

                    onStatus("Reading payload.bin manifest...")
                    val dumper = PayloadDumper(channel, payload.dataOffset)
                    val partitions = dumper.partitionNames().toSet()
                    val target = candidateOrder.firstOrNull { it in partitions }
                        ?: throw IOException(
                            "payload.bin has neither boot nor init_boot (found: ${partitions.joinToString()})"
                        )

                    if (!dumper.canExtract(target)) {
                        throw IOException(
                            "$target uses delta/incremental operations; only full OTAs are supported"
                        )
                    }

                    onStatus("Extracting $target.img...")
                    val output = File(workDir, "$target.img")
                    dumper.extract(target, output) { _, _ -> }
                    return Result(output, target, sourceName)
                }
            }
        }
    }

    private fun findDirectImage(
        zip: ZipFile,
        candidates: List<String>,
    ): Pair<ZipArchiveEntry, String>? {
        val entries: List<ZipArchiveEntry> = Collections.list(zip.entries)
        for (name in candidates) {
            val match = entries.firstOrNull { entry ->
                val n = entry.name
                n.equals("$name.img", ignoreCase = true) ||
                    n.endsWith("/$name.img", ignoreCase = true)
            }
            if (match != null) return match to name
        }
        return null
    }
}
