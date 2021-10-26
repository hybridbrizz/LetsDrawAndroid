package com.ericversteeg.liquidocean.model

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.RectF
import android.util.Log
import io.mockk.*
import org.junit.Before
import org.junit.Test

class InteractiveCanvasSinglePlayTests {

    private val mockContext = mockk<Context>()

    private val mockSessionSettings = mockk<SessionSettings>()

    private lateinit var interactiveCanvas: InteractiveCanvas

    @Test
    fun testRowsAndColsSize() {
        assert(interactiveCanvas.arr.size == interactiveCanvas.rows)
        assert(interactiveCanvas.arr[0].size == interactiveCanvas.cols)
    }

    @Test
    fun testDrawAndUndo() {
        val color = Color.YELLOW
        val x = 50
        val y = 51

        assert(interactiveCanvas.rows > y)
        assert(interactiveCanvas.cols > x)

        every { mockSessionSettings.dropsAmt } returns 1
        every { mockSessionSettings.paintColor } returns Color.YELLOW

        val point = Point()

        point.x = x
        point.y = y

        interactiveCanvas.paintUnitOrUndo(point, redraw = false)

        // draw
        assert(interactiveCanvas.arr[y][x] == color)

        interactiveCanvas.paintUnitOrUndo(point, mode = 1, redraw = false)

        // undo
        assert(interactiveCanvas.arr[y][x] == 0)
    }

    @Test
    fun testGetGridLineColors() {
        every { mockSessionSettings.canvasGridLineColor } returns -1

        every { mockSessionSettings.backgroundColorsIndex } returns InteractiveCanvas.BACKGROUND_BLACK
        assert(interactiveCanvas.getGridLineColor() == Color.WHITE)

        every { mockSessionSettings.backgroundColorsIndex } returns InteractiveCanvas.BACKGROUND_WHITE
        assert(interactiveCanvas.getGridLineColor() == Color.BLACK)

        every { mockSessionSettings.backgroundColorsIndex } returns InteractiveCanvas.BACKGROUND_GRAY_THIRDS
        assert(interactiveCanvas.getGridLineColor() == Color.WHITE)

        every { mockSessionSettings.backgroundColorsIndex } returns InteractiveCanvas.BACKGROUND_PHOTOSHOP
        assert(interactiveCanvas.getGridLineColor() == Color.BLACK)

        every { mockSessionSettings.backgroundColorsIndex } returns InteractiveCanvas.BACKGROUND_CLASSIC
        assert(interactiveCanvas.getGridLineColor() == Color.WHITE)

        every { mockSessionSettings.backgroundColorsIndex } returns InteractiveCanvas.BACKGROUND_CHESS
        assert(interactiveCanvas.getGridLineColor() == Color.WHITE)
    }

    @Test
    fun testScreenPointToUnit() {
        val left = 0F
        val top = 0F
        val right = 16F
        val bottom = 9F

        val screenX = 200F
        val screenY = 200F

        interactiveCanvas.deviceViewport = RectF(left, top, right, bottom)
        var point = interactiveCanvas.screenPointToUnit(screenX, screenY)

        assert(point != null)

        point?.apply {
            assert(x > left)
            assert(x < right)
            assert(y > top)
            assert(y < bottom)
        }

        interactiveCanvas.deviceViewport = null
        point = interactiveCanvas.screenPointToUnit(screenX, screenY)

        assert(point == null)
    }

    @Test
    fun testIsBackground() {
        val x = 10
        val y = 11

        interactiveCanvas.arr[y][x] = Color.YELLOW

        val point = Point()
        point.x = x
        point.y = y

        assert(!interactiveCanvas.isBackground(point))

        point.x = x - 1

        assert(interactiveCanvas.isBackground(point))

        interactiveCanvas.arr[y][x] = 0

        point.x = x

        assert(interactiveCanvas.isBackground(point))
    }

    @Test
    fun testCommitPixels() {
        val mockInteractiveCanvas = spyk(InteractiveCanvas(mockContext, mockSessionSettings))

        mockInteractiveCanvas.commitPixels()

        verify { mockInteractiveCanvas.updateRecentColors() }
    }

    @Test
    fun testUndoPendingPaint() {
        val x1 = 1
        val y1 = 1

        val x2 = 10
        val y2 = 20

        val x3 = 10
        val y3 = 1

        val point1 = Point()
        point1.x = x1
        point1.y = y1

        val point2 = Point()
        point2.x = x2
        point2.y = y2

        val point3 = Point()
        point3.x = x3
        point3.y = y3

        every { mockSessionSettings.dropsAmt } returns 1
        every { mockSessionSettings.paintColor } returns Color.YELLOW

        interactiveCanvas.paintUnitOrUndo(point1)
        interactiveCanvas.paintUnitOrUndo(point2)
        interactiveCanvas.paintUnitOrUndo(point3)

        interactiveCanvas.undoPendingPaint()

        assert(interactiveCanvas.arr[y1][x1] == 0)
        assert(interactiveCanvas.arr[y2][x2] == 0)
        assert(interactiveCanvas.arr[y3][x3] == 0)
    }

    @Before
    fun setUp() {
        every { mockSessionSettings.getSharedPrefs(any()).getString(any(), any()) } returns null
        every { mockSessionSettings.canvasGridLineColor } returns 0
        every { mockSessionSettings.numRecentColors } returns 0

        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0

        interactiveCanvas = InteractiveCanvas(mockContext, mockSessionSettings)
        interactiveCanvas.world = false
    }
}