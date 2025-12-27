package com.example.soundalertfinal

class FloatRingBuffer(private val size: Int) {
    private val buf = FloatArray(size)
    private var idx = 0
    private var filled = false

    fun push(x: Float) {
        buf[idx] = x
        idx++
        if (idx >= size) {
            idx = 0
            filled = true
        }
    }

    fun isReady(): Boolean = filled

    fun snapshot(): FloatArray {
        // Return a contiguous window in correct order (oldest->newest)
        val out = FloatArray(size)
        val start = idx
        var k = 0
        for (i in start until size) out[k++] = buf[i]
        for (i in 0 until start) out[k++] = buf[i]
        return out
    }
}
