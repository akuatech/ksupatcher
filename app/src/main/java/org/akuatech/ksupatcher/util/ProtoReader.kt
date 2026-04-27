package org.akuatech.ksupatcher.util

class ProtoReader(
    private val data: ByteArray,
    private var pos: Int = 0,
    private val end: Int = data.size,
) {
    fun hasMore(): Boolean = pos < end

    fun readTag(): Int = readVarint().toInt()

    fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (true) {
            if (pos >= end) throw IllegalStateException("Unexpected EOF in varint")
            val b = data[pos].toInt() and 0xff
            pos++
            result = result or ((b.toLong() and 0x7f) shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
            if (shift >= 64) throw IllegalStateException("Varint too long")
        }
    }

    fun readBytes(): ByteArray {
        val len = readVarint().toInt()
        if (pos + len > end) throw IllegalStateException("Length-delimited overflow")
        val out = data.copyOfRange(pos, pos + len)
        pos += len
        return out
    }

    fun readMessage(): ProtoReader {
        val len = readVarint().toInt()
        if (pos + len > end) throw IllegalStateException("Length-delimited overflow")
        val sub = ProtoReader(data, pos, pos + len)
        pos += len
        return sub
    }

    fun skipField(wireType: Int) {
        when (wireType) {
            0 -> readVarint()
            1 -> pos += 8
            2 -> {
                val len = readVarint().toInt()
                if (pos + len > end) throw IllegalStateException("Length-delimited overflow in skip")
                pos += len
            }
            5 -> pos += 4
            else -> throw IllegalStateException("Unsupported wire type $wireType")
        }
    }
}
