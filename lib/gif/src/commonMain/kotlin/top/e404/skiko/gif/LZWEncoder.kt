package top.e404.skiko.gif

import korlibs.datastructure.BitArray
import java.util.*

class LZWEncoder(private val colors: ColorTable, private val image: IntArray) {
    internal companion object {
        val CLEAR_CODE = listOf(-1)
        val END_OF_INFO = listOf(-2)

        const val MAX_CODE_TABLE_SIZE = 1 shl 12
    }

    private val minimumCodeSize = colors.size() + 1
    private var outputBits = BitArray(512)
    private var position = 0
    private val table: MutableMap<List<Int>, Int> = HashMap()
    private var codeSize = 0
    private var indexBuffer: List<Int> = emptyList()

    init {
        resetCodeTableAndCodeSize()
    }

    fun encode(): Pair<Int, ByteArray> {
        writeCode(table.getValue(CLEAR_CODE))
        for (rgb in image) {
            val index = colors.colors.indexOf(rgb)
            processIndex(if (index != -1) index else colors.transparency)
        }
        writeCode(table.getValue(indexBuffer))
        writeCode(table.getValue(END_OF_INFO))
        return minimumCodeSize to toBytes()
    }

    private fun processIndex(index: Int) {
        val extendedIndexBuffer = indexBuffer + index
        indexBuffer = if (extendedIndexBuffer in table) {
            extendedIndexBuffer
        } else {
            writeCode(table.getValue(indexBuffer))
            if (table.size == MAX_CODE_TABLE_SIZE) {
                writeCode(table.getValue(CLEAR_CODE))
                resetCodeTableAndCodeSize()
            } else {
                addCodeToTable(extendedIndexBuffer)
            }
            listOf(index)
        }
    }

    private fun writeCode(code: Int) {
        for (shift in 0 until codeSize) {
            val bit = code ushr shift and 1 != 0
            if (position+1 >= outputBits.size) {
                val old = outputBits
                outputBits = BitArray(old.size * 2)
                for (i in old.indices) {
                    outputBits[i] = old[i]
                }
            }
            outputBits.set(position++, bit)
        }
    }

    private fun toBytes(): ByteArray {
        val bitCount: Int = position
        val result = ByteArray((bitCount + 7) / 8)
        for (i in 0 until bitCount) {
            val byteIndex = i / 8
            val bitIndex = i % 8
            result[byteIndex] = ((if (outputBits.get(i)) 1 else 0) shl bitIndex or result[byteIndex].toInt()).toByte()
        }
        return result
    }

    private fun addCodeToTable(indices: List<Int>) {
        val newCode: Int = table.size
        table[indices] = newCode
        if (newCode == 1 shl codeSize) {
            ++codeSize
        }
    }

    private fun resetCodeTableAndCodeSize() {
        table.clear()

        val colorsInCodeTable = 1 shl minimumCodeSize
        for (i in 0 until colorsInCodeTable) {
            table[listOf(i)] = i
        }
        table[CLEAR_CODE] = table.size
        table[END_OF_INFO] = table.size

        codeSize = minimumCodeSize + 1
    }
}
