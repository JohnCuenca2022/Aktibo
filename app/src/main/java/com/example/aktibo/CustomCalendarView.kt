package com.example.aktibo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.icu.util.Calendar
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.CalendarView
import android.widget.GridLayout
import androidx.constraintlayout.widget.ConstraintLayout

class CustomCalendarView : CalendarView {

    private val circlePaint = Paint()

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        circlePaint.color = Color.RED
        circlePaint.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        // Customize the drawing of the days here
        // You can override how the days are rendered in the onDraw method

        // Call the superclass method to ensure the default behavior is preserved
        super.onDraw(canvas)

    }
}