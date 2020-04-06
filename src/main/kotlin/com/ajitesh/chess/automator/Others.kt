package com.ajitesh.chess.automator

import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import java.awt.Rectangle
import java.awt.image.BufferedImage

data class DefaultChessConfigInfo(val workingFolderPath: String, val boundRect: Rectangle, val isLowerPlayerWhite : Boolean? = null, val defaultContours : List<ChessPieceContour>? = null)

enum class ChessPieceType(val notation : String) {
    PAWN(""),
    KNIGHT("N"),
    BISHOP("B"),
    ROOK("R"),
    QUEEN("Q"),
    KING("K")
}

data class ChessPiece(val type: ChessPieceType, val isWhite: Boolean)

data class ChessPieceContour(val piece: ChessPiece, val points: List<Point>)

data class ChessPieceCoordinate(val piece: ChessPiece, val coordinate: Coordinate)

data class Coordinate(val xPos: Int, val yPos: Int) {

    init {
        require(xPos in 0..7 && yPos in 0..7) { "Invalid coordinate($xPos, $yPos)" }
    }
}

data class ContourGenerationParams(
    val isWhite: Boolean,
    val threshold: Double,
    val maxValue: Double,
    val thresholdType: Int,
    val epsilonFactor: Double
)

data class MoveInfo(val latestMove : String?, val currFrame : List<ChessPieceCoordinate>)

fun List<MatOfPoint>.getContourPoints(): List<Point> {

    return this.fold(mutableListOf(), { acc, matOfPoint ->
        acc.addAll(matOfPoint.toList())
        acc
    })
}
