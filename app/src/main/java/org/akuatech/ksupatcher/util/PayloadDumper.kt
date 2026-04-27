package org.akuatech.ksupatcher.util

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.tukaani.xz.XZInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel
import java.security.MessageDigest

class PayloadDumper(
    private val channel: SeekableByteChannel,
    private val baseOffset: Long,
) {
    data class Extent(val startBlock: Long, val numBlocks: Long)

    data class Operation(
        val type: Int,
        val dataOffset: Long,
        val dataLength: Long,
        val dstExtents: List<Extent>,
        val dataSha256Hash: ByteArray,
    )

    data class Partition(
        val name: String,
        val operations: List<Operation>,
        val size: Long,
        val hash: ByteArray,
    )

    data class Manifest(
        val blockSize: Long,
        val partitions: List<Partition>,
    )

    val manifest: Manifest
    private val dataOffset: Long

    init {
        val (m, off) = readHeaderAndManifest()
        manifest = m
        dataOffset = off
    }

    fun partitionNames(): List<String> = manifest.partitions.map { it.name }

    fun canExtract(partitionName: String): Boolean {
        val partition = manifest.partitions.firstOrNull { it.name == partitionName } ?: return false
        return partition.operations.all { it.type in SUPPORTED_OPS }
    }

    fun extract(
        partitionName: String,
        output: File,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    ) {
        val partition = manifest.partitions.firstOrNull { it.name == partitionName }
            ?: throw IOException("Partition '$partitionName' not found in payload")

        val blockSize = manifest.blockSize
        val total = partition.operations.size

        RandomAccessFile(output, "rw").use { out ->
            if (partition.size > 0) out.setLength(partition.size)
            partition.operations.forEachIndexed { index, op ->
                applyOperation(partition.name, op, blockSize, out)
                onProgress(index + 1, total)
            }
        }

        if (partition.hash.isNotEmpty()) {
            val actual = sha256OfFile(output)
            if (!actual.contentEquals(partition.hash)) {
                throw IOException(
                    "Partition hash mismatch for ${partition.name}: extracted file is corrupt"
                )
            }
        }
    }

    private fun applyOperation(
        partitionName: String,
        op: Operation,
        blockSize: Long,
        out: RandomAccessFile,
    ) {
        when (op.type) {
            OP_ZERO, OP_DISCARD -> {
                val zeros = ByteArray(8192)
                for (extent in op.dstExtents) {
                    out.seek(extent.startBlock * blockSize)
                    var remaining = extent.numBlocks * blockSize
                    while (remaining > 0) {
                        val w = minOf(remaining, zeros.size.toLong()).toInt()
                        out.write(zeros, 0, w)
                        remaining -= w
                    }
                }
            }
            OP_REPLACE, OP_REPLACE_BZ, OP_REPLACE_XZ -> {
                if (op.dstExtents.isEmpty()) throw IOException("Operation has no dst_extents")
                channel.position(baseOffset + dataOffset + op.dataOffset)
                val raw = readChannel(op.dataLength.toInt())

                if (op.dataSha256Hash.isNotEmpty()) {
                    val actual = MessageDigest.getInstance("SHA-256").digest(raw)
                    if (!actual.contentEquals(op.dataSha256Hash)) {
                        throw IOException(
                            "Operation data hash mismatch in $partitionName at payload offset ${op.dataOffset}"
                        )
                    }
                }

                val decompressed = when (op.type) {
                    OP_REPLACE -> raw
                    OP_REPLACE_BZ -> BZip2CompressorInputStream(ByteArrayInputStream(raw)).use { it.readBytes() }
                    OP_REPLACE_XZ -> XZInputStream(ByteArrayInputStream(raw)).use { it.readBytes() }
                    else -> throw IllegalStateException()
                }

                val expected = op.dstExtents.sumOf { it.numBlocks * blockSize }
                if (decompressed.size.toLong() != expected) {
                    throw IOException(
                        "Decompressed size mismatch in $partitionName: got ${decompressed.size}, expected $expected"
                    )
                }

                var srcPos = 0
                for (extent in op.dstExtents) {
                    val len = (extent.numBlocks * blockSize).toInt()
                    out.seek(extent.startBlock * blockSize)
                    out.write(decompressed, srcPos, len)
                    srcPos += len
                }
            }
            else -> throw IOException(
                "Unsupported install operation type ${op.type}; this looks like a delta/incremental OTA, only full OTAs are supported"
            )
        }
    }

    private fun sha256OfFile(file: File): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buf = ByteArray(1024 * 1024)
            while (true) {
                val r = fis.read(buf)
                if (r <= 0) break
                md.update(buf, 0, r)
            }
        }
        return md.digest()
    }

    private fun readChannel(len: Int): ByteArray {
        val buf = ByteBuffer.allocate(len)
        while (buf.hasRemaining()) {
            val r = channel.read(buf)
            if (r < 0) throw IOException("Unexpected EOF reading payload data")
        }
        return buf.array()
    }

    private fun readHeaderAndManifest(): Pair<Manifest, Long> {
        channel.position(baseOffset)
        val header = ByteBuffer.allocate(24).order(ByteOrder.BIG_ENDIAN)
        while (header.hasRemaining()) {
            val r = channel.read(header)
            if (r < 0) throw IOException("Unexpected EOF reading payload header")
        }
        header.flip()
        val magic = ByteArray(4).also { header.get(it) }
        if (String(magic, Charsets.US_ASCII) != MAGIC) {
            throw IOException("Not a valid payload.bin (bad magic)")
        }
        val version = header.long
        if (version != MAJOR_VERSION) {
            throw IOException("Unsupported payload version: $version")
        }
        val manifestLen = header.long
        val signatureLen = header.int.toLong()

        if (manifestLen <= 0 || manifestLen > MAX_MANIFEST_SIZE) {
            throw IOException("Implausible manifest size: $manifestLen")
        }
        val manifestBuf = ByteBuffer.allocate(manifestLen.toInt())
        while (manifestBuf.hasRemaining()) {
            val r = channel.read(manifestBuf)
            if (r < 0) throw IOException("Unexpected EOF reading manifest")
        }
        val manifest = parseManifest(manifestBuf.array())
        val blobsOffset = HEADER_SIZE + manifestLen + signatureLen
        return manifest to blobsOffset
    }

    private fun parseManifest(bytes: ByteArray): Manifest {
        val r = ProtoReader(bytes)
        var blockSize = 4096L
        val partitions = mutableListOf<Partition>()
        while (r.hasMore()) {
            val tag = r.readTag()
            when (tag ushr 3) {
                3 -> blockSize = r.readVarint()
                13 -> partitions += parsePartition(r.readMessage())
                else -> r.skipField(tag and 7)
            }
        }
        return Manifest(blockSize, partitions)
    }

    private fun parsePartition(r: ProtoReader): Partition {
        var name = ""
        val ops = mutableListOf<Operation>()
        var size = 0L
        var hash = EMPTY_BYTES
        while (r.hasMore()) {
            val tag = r.readTag()
            when (tag ushr 3) {
                1 -> name = String(r.readBytes(), Charsets.UTF_8)
                7 -> {
                    val info = parsePartitionInfo(r.readMessage())
                    size = info.first
                    hash = info.second
                }
                8 -> ops += parseOperation(r.readMessage())
                else -> r.skipField(tag and 7)
            }
        }
        return Partition(name, ops, size, hash)
    }

    private fun parsePartitionInfo(r: ProtoReader): Pair<Long, ByteArray> {
        var size = 0L
        var hash = EMPTY_BYTES
        while (r.hasMore()) {
            val tag = r.readTag()
            when (tag ushr 3) {
                1 -> size = r.readVarint()
                2 -> hash = r.readBytes()
                else -> r.skipField(tag and 7)
            }
        }
        return size to hash
    }

    private fun parseOperation(r: ProtoReader): Operation {
        var type = -1
        var dataOff = 0L
        var dataLen = 0L
        val dst = mutableListOf<Extent>()
        var dataHash = EMPTY_BYTES
        while (r.hasMore()) {
            val tag = r.readTag()
            when (tag ushr 3) {
                1 -> type = r.readVarint().toInt()
                2 -> dataOff = r.readVarint()
                3 -> dataLen = r.readVarint()
                6 -> dst += parseExtent(r.readMessage())
                8 -> dataHash = r.readBytes()
                else -> r.skipField(tag and 7)
            }
        }
        return Operation(type, dataOff, dataLen, dst, dataHash)
    }

    private fun parseExtent(r: ProtoReader): Extent {
        var start = 0L
        var num = 0L
        while (r.hasMore()) {
            val tag = r.readTag()
            when (tag ushr 3) {
                1 -> start = r.readVarint()
                2 -> num = r.readVarint()
                else -> r.skipField(tag and 7)
            }
        }
        return Extent(start, num)
    }

    companion object {
        private const val MAGIC = "CrAU"
        private const val MAJOR_VERSION = 2L
        private const val HEADER_SIZE = 24L
        private const val MAX_MANIFEST_SIZE = 256L * 1024 * 1024

        private const val OP_REPLACE = 0
        private const val OP_REPLACE_BZ = 1
        private const val OP_ZERO = 6
        private const val OP_DISCARD = 7
        private const val OP_REPLACE_XZ = 8

        private val SUPPORTED_OPS = setOf(OP_REPLACE, OP_REPLACE_BZ, OP_REPLACE_XZ, OP_ZERO, OP_DISCARD)

        private val EMPTY_BYTES = ByteArray(0)
    }
}
