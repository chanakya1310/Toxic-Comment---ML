package dev.smoketrees.toxiccommentstflite

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {

    private lateinit var mJob: Job
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main

    private val labels =
        listOf("Toxic", "Severe Toxic", "Obscene", "Threat", "Insult", "Identity Hate")
    private val thresholds = listOf(0.70, 0.30, 0.30, 0.15, 0.40, 0.20)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mJob = Job()

        predictButton.isEnabled = false

        val classifier = Classifier(this)

        val meh = async {
            withContext(Dispatchers.IO) {
                classifier.init()
                withContext(Dispatchers.Main) {
                    predictButton.isEnabled = true
                }
            }
        }

        predictButton.isHapticFeedbackEnabled = true
        predictButton.setOnClickListener {
            predictButton.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )

            if (inputText.text.isEmpty()) {
                Toast.makeText(this, "Text cant be empty", Toast.LENGTH_SHORT).show()
            } else {
                predictButton.isEnabled = false
                val text = inputText.text.toString().toLowerCase(Locale.ROOT).trim()
                val resArr = classifier.classifyText(text)
                predictButton.isEnabled = true
                updateResultUI(resArr)
            }
        }
    }

    private fun updateResultUI(res: FloatArray) {
        var isAboveThreshold = false

        val sortedRes = res.sorted().reversed()
        val labelList = mutableListOf<String>()
        val thresholdList = mutableListOf<Double>()
        for (i in 0..2) {
            val index = res.indexOf(sortedRes[i])
            labelList.add(labels[index])
            thresholdList.add(thresholds[index])
        }

        for (i in 0..2) {
            if (sortedRes[i] > thresholdList[i]) {
                isAboveThreshold = true
                break
            }
        }

        val percentSortedRes = sortedRes.map { it * 100 }

        if (isAboveThreshold) {
            resultLayout.visibility = View.VISIBLE
            cleanTextLayout.visibility = View.GONE
            val labelViews = listOf(firstLabel, secondLabel, thirdLabel)
            val valueViews = listOf(firstValue, secondValue, thirdValue)
            for (i in 0..2) {
                labelViews[i].text = labelList[i]
                valueViews[i].text = percentSortedRes[i].toString() + " %"
            }
        } else {
            cleanTextLayout.visibility = View.VISIBLE
            resultLayout.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mJob.cancel()
    }
}