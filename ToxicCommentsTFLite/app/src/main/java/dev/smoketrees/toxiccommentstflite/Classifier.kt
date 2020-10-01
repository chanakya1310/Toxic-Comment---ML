package dev.smoketrees.toxiccommentstflite

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class Classifier (private val context: Context) {

    private val interpreter: Interpreter
    private val wordDict = HashMap<String, Int>()

    private fun loadJSONFromAsset(): String? {
        return try {
            val inputStream = context.assets.open(DICT_FILE)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun tokenize(text: String): IntArray {
        val parts: List<String> = text.split(" ", ".", ",")
        val tokenizedText = ArrayList<Int>()
        for (part in parts) {
            if (part.trim() != "") {
                val index = if (wordDict[part] == null) {
                    0
                } else {
                    wordDict[part] ?: 0
                }
                tokenizedText.add(index)
            }
        }
        return tokenizedText.toIntArray()
    }

    private fun padSequence(sequence: IntArray): IntArray {
        return when {
            sequence.size > MAX_LEN -> {
                sequence.sliceArray(0..MAX_LEN)
            }
            sequence.size < MAX_LEN -> {
                val array = ArrayList<Int>()
                array.addAll(sequence.asList())
                for (i in array.size until MAX_LEN) {
                    array.add(0)
                }
                array.toIntArray()
            }
            else -> {
                sequence
            }
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun init() {
        val jsonObject = JSONObject(loadJSONFromAsset() ?: "")
        val iterator: Iterator<String> = jsonObject.keys()
        var counter = 0
        while (iterator.hasNext() and (counter < MAX_FEATURE_LENGTH)) {
            val key = iterator.next()
            wordDict[key] = jsonObject.getInt(key)
            counter++
        }
        Log.d("esh", "Json Dict loaded")
    }

    fun classifyText(text: String): FloatArray {
        val tokenizedText = tokenize(text)
        val paddedMessage = padSequence(tokenizedText)
        val inputs: Array<FloatArray> = arrayOf(paddedMessage.map { it.toFloat() }.toFloatArray())
        val outputs: Array<FloatArray> = arrayOf(floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f))
        interpreter.run(inputs, outputs)
        return outputs[0]
    }

    companion object {
        private const val MODEL_FILE = "model.tflite"
        private const val DICT_FILE = "dict.json"
        private const val MAX_LEN = 200
        private const val MAX_FEATURE_LENGTH = 20000
    }

    init {
        val options = Interpreter.Options()
        options.setNumThreads(8)
        interpreter = Interpreter(loadModelFile(), options)
    }

}