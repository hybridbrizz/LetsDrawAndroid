package com.ericversteeg.radiofrost.fragment

import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.ericversteeg.radiofrost.R
import com.ericversteeg.radiofrost.model.InteractiveCanvas
import com.ericversteeg.radiofrost.model.Command
import com.ericversteeg.radiofrost.model.SessionSettings
import kotlinx.android.synthetic.main.fragment_terminal.*
import java.lang.Exception

class TerminalFragment: Fragment() {

    var textSize = 50F
    private var minimumTextSize = 10F
    private var maximumTextSize = 50F

    val commands = arrayOf(
        Command("draw", "(x, y)", Regex("^\\(([0-9x]{1,4}),\\s([0-9y]{1,4})\\)$")),
        Command("lookat", "(x, y)", Regex("^\\(([0-9x]{1,4}),\\s([0-9y]{1,4})\\)$")),
        Command("erase", "(x, y)", Regex("^\\(([0-9x]{1,4}),\\s([0-9y]{1,4})\\)$")),
        Command("setcolor", "(r, g, b)", Regex("^\\(([0-9r]{1,3}),\\s([0-9g]{1,3}),\\s([0-9b]{1,3})\\)$"))
    )

    //Command("setbg", "1-32", Regex("[0-9]{1,2}")),
    //Command("gridlines", "on | off", Regex("^o[nf]f?$"))

    lateinit var interactiveCanvas: InteractiveCanvas

    var lastText = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_terminal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        terminal_input.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)

        terminal_input.addTextChangedListener {
            it?.apply {
                var text = toString()

                // send command
                if (text.isNotEmpty()) {
                    if (text.toCharArray()[text.length - 1] == '\n') {
                        autoCompleteView.visibility = View.INVISIBLE
                        sendCommand(text.substring(0, text.length - 1))
                        text = ""
                        terminal_input.setText(text)
                    }
                }

                // only 1 white space at the end
                if (text.endsWith("  ")) {
                    text = text.substring(0, text.length - 1)
                    terminal_input.setText(text)
                    terminal_input.setSelection(terminal_input.length())
                }

                // auto complete
                val tokens = text.split("\\s".toRegex())
                if (tokens.isNotEmpty()) {
                    val command = findCommand(tokens[0])

                    if (command != null) {
                        val inputArgsPrefix = buildInputArgsPrefix(text)

                        if (matchArgSyntax(command, inputArgsPrefix, true)) {
                            val replacedSyntax = command.replacedArgSyntax

                            autoCompleteView.autoCompletedString = "${command.name} $replacedSyntax"
                            autoCompleteView.prefixString = text
                            autoCompleteView.textPaint = terminal_input.paint
                            autoCompleteView.invalidate()

                            autoCompleteView.visibility = View.VISIBLE
                        }
                        else {
                            autoCompleteView.visibility = View.INVISIBLE
                        }
                    }
                    else {
                        autoCompleteView.visibility = View.INVISIBLE
                    }
                }

                lastText = text

                /*val bounds = Rect()
                terminal_input.paint.getTextBounds(text, 0, text.length, bounds)

                val shrinkBoundary = terminal_input.width - Utils.dpToPx(context, 20)
                val growBoundary = terminal_input.width - Utils.dpToPx(context, 50)

                var numResizes = 0
                if (bounds.width() > shrinkBoundary && textSize >= minimumTextSize) {
                    do {
                        textSize -= 1
                        terminal_input.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)

                        terminal_input.paint.getTextBounds(text, 0, text.length, bounds)

                        numResizes++
                    }
                    while (bounds.width() > shrinkBoundary && numResizes < 10)
                }
                else if (bounds.width() < growBoundary && textSize <= maximumTextSize) {
                    do {
                        textSize += 1
                        terminal_input.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)

                        terminal_input.paint.getTextBounds(text, 0, text.length, bounds)

                        numResizes++
                    }
                    while (bounds.width() < growBoundary && numResizes < 10)
                }*/
            }
        }
    }

    private fun buildInputArgsPrefix(input: String): String {
        val tokens = input.split("\\s".toRegex())

        var argsStr = ""
        if (tokens.size > 1) {
            for (i in 1 until tokens.size) {
                argsStr += tokens[i] + " "
            }
            argsStr = argsStr.substring(0, argsStr.length - 1)
        }

        return argsStr
    }

    private fun matchArgSyntax(command: Command, inputArgsPrefix: String, forward: Boolean): Boolean {
        val regex = command.argRegex
        val syntax = command.argSyntax

        var matches = false

        // input + part of syntax
        for (i in syntax.indices) {
            val syntaxSuffix = syntax.substring(i, syntax.length)
            if (regex.matches("$inputArgsPrefix$syntaxSuffix")) {
                command.replacedArgSyntax = "$inputArgsPrefix$syntaxSuffix"

                // if backward, use the first regex match
                if (!forward) {
                    return true
                }

                // if forward, use the last regex match
                matches = true
            }
        }

        return matches
    }

    private fun findCommand(str: String): Command? {
        if (str.isEmpty()) {
            return null
        }

        for (command in commands) {
            val commandName = command.name
            if (str.length <= commandName.length) {
                if (commandName.substring(0, str.length) == str) {
                    return command
                }
            }
        }

        return null
    }

    private fun sendCommand(str: String) {
        val tokens = str.split("\\s".toRegex())
        if (tokens.size > 1) {
            val commandName = tokens[0]
            val command = findCommand(commandName)

            if (commands.contains(command)) {
                command?.apply {
                    val regex = argRegex

                    if (regex.matches(replacedArgSyntax)) {
                        val match = regex.find(replacedArgSyntax)
                        val groups = match!!.destructured.toList()

                        when (commandName) {
                            "draw" -> {
                                if (groups.size > 1) {
                                    val xStr = groups[0]
                                    val yStr = groups[1]

                                    try {
                                        val x = xStr.toInt()
                                        val y = yStr.toInt()

                                        if (interactiveCanvas.unitInBounds(Point(x, y))) {
                                            interactiveCanvas.paintUnitOrUndo(Point(x, y))
                                            interactiveCanvas.commitPixels()
                                        }
                                    }
                                    catch (ex: Exception) {

                                    }
                                }
                            }
                            "erase" -> {
                                if (groups.size > 1) {
                                    val xStr = groups[0]
                                    val yStr = groups[1]

                                    try {
                                        val x = xStr.toInt()
                                        val y = yStr.toInt()

                                        if (interactiveCanvas.unitInBounds(Point(x, y))) {
                                            val lastColor = SessionSettings.instance.paintColor
                                            SessionSettings.instance.paintColor = 0

                                            interactiveCanvas.paintUnitOrUndo(Point(x, y))
                                            interactiveCanvas.commitPixels()

                                            SessionSettings.instance.paintColor = lastColor
                                        }
                                    }
                                    catch (ex: Exception) {

                                    }
                                }
                            }
                            "lookat" -> {
                                if (groups.size > 1) {
                                    val xStr = groups[0]
                                    val yStr = groups[1]

                                    try {
                                        val x = xStr.toInt()
                                        val y = yStr.toInt()

                                        if (interactiveCanvas.unitInBounds(Point(x, y))) {
                                            context?.apply {
                                                interactiveCanvas.updateDeviceViewport(this, x.toFloat(), y.toFloat())
                                                interactiveCanvas.interactiveCanvasDrawer?.notifyRedraw()
                                            }
                                        }
                                    }
                                    catch (ex: Exception) {

                                    }
                                }
                            }
                            "setcolor" -> {
                                if (groups.size > 2) {
                                    val rStr = groups[0]
                                    val gStr = groups[1]
                                    val bStr = groups[2]

                                    try {
                                        val r = rStr.toInt()
                                        val g = gStr.toInt()
                                        val b = bStr.toInt()

                                        if (r in 0..255 && g in 0..255 && b in 0..255) {
                                            SessionSettings.instance.paintColor = Color.argb(255, r, g, b)
                                            interactiveCanvas.interactiveCanvasListener?.notifyPaintColorUpdate(SessionSettings.instance.paintColor)
                                        }
                                    }
                                    catch (ex: Exception) {

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}