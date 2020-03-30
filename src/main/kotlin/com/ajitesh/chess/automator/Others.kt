package com.ajitesh.chess.automator

import java.awt.image.BufferedImage

data class DeviceUICaptureInfo(val isSuccess: Boolean, val folderPath: String? = null)

data class Margin(val left: Int, val top: Int, val right: Int, val bottom: Int)

data class ChessPieceImage(val name : String, val image: BufferedImage)

inline fun diff(first: Int, second: Int): Int {

    val diff = first - second

    return if (diff < 0)
        -diff
    else
        diff
}

inline fun minOf(first: Int, second: Int) = if (first <= second) first else second