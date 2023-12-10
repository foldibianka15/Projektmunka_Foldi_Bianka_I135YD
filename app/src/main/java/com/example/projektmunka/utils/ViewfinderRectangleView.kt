package com.example.projektmunka.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ViewfinderRectangleView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the viewfinder rectangle
        val rectangleWidth = 200
        val rectangleHeight = 100
        val centerX = width / 2
        val centerY = height / 2

        val left = centerX - rectangleWidth / 2
        val top = centerY - rectangleHeight / 2
        val right = centerX + rectangleWidth / 2
        val bottom = centerY + rectangleHeight / 2

        canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
    }
}