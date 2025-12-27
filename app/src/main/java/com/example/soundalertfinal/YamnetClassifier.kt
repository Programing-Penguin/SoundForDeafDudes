package com.example.soundalertfinal

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class YamnetScores(val labels: List<String>, val scores: FloatArray)

class YamnetClassifier(ctx: Context) {

    private val labels: List<String>
    private val interpreter: Interpreter

    init {
        labels = loadLabels(ctx, "yamnet_label_list.txt")
        val model = ctx.assets.open("yamnet.tflite").readBytes()
        val bb = ByteBuffer.allocateDirect(model.size).order(ByteOrder.nativeOrder())
        bb.put(model)
        bb.rewind()

        interpreter = Interpreter(bb)
    }

    /**
     * YAMNet input: float32 waveform [15600]
     * Output: scores [1][521] (label probabilities)
     */
    fun classify(waveform: FloatArray): YamnetScores {
        val input = arrayOf(waveform)
        val output = Array(1) { FloatArray(labels.size) }

        interpreter.run(input, output)

        return YamnetScores(labels, output[0])
    }

    fun close() {
        interpreter.close()
    }

    private fun loadLabels(ctx: Context, fileName: String): List<String> {
        val out = mutableListOf<String>()
        ctx.assets.open(fileName).use { input ->
            BufferedReader(InputStreamReader(input)).forEachLine { line ->
                val t = line.trim()
                if (t.isNotEmpty()) out.add(t)
            }
        }
        return out
    }
}
