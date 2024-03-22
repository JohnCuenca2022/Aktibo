package com.example.aktibo

import android.animation.ValueAnimator
import android.widget.ProgressBar
import android.widget.TextView

object AnimationUtil {
    fun animateTextViewNumerical(textView: TextView, startValue: Int, endValue: Int, duration: Long) {
        val animator = ValueAnimator.ofInt(startValue, endValue)
        animator.duration = duration

        animator.addUpdateListener { valueAnimator ->
            val animatedValue = valueAnimator.animatedValue as Int
            textView.text = animatedValue.toString()
        }

        animator.start()
    }

    fun animateTextViewMacros(textView: TextView, startValue: Int, endValue: Int, duration: Long) {
        val animator = ValueAnimator.ofInt(startValue, endValue)
        animator.duration = duration

        animator.addUpdateListener { valueAnimator ->
            val animatedValue = valueAnimator.animatedValue as Int
            val textViewString = animatedValue.toString() + "g"
            textView.text = textViewString
        }

        animator.start()
    }

    fun animateRecipeTextViewMacros(textView: TextView, endString: String, startValue: Int, endValue: Int, duration: Long) {
        val animator = ValueAnimator.ofInt(startValue, endValue)
        animator.duration = duration

        animator.addUpdateListener { valueAnimator ->
            val animatedValue = valueAnimator.animatedValue as Int
            val textViewString = animatedValue.toString() + endString
            textView.text = textViewString
        }

        animator.start()
    }

    fun animateProgressBar(progressBar: ProgressBar, startValue: Int, endValue: Int, duration: Long) {
        val animator = ValueAnimator.ofInt(startValue, endValue)
        animator.duration = duration

        animator.addUpdateListener { valueAnimator ->
            val animatedValue = valueAnimator.animatedValue as Int
            progressBar.progress = animatedValue
        }

        animator.start()
    }

}