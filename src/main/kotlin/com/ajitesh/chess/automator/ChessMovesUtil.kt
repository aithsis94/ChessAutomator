package com.ajitesh.chess.automator

import org.opencv.core.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ChessMovesUtil {

    companion object {

        val PROSPECTIVE_PROMO_PIECES =
            arrayOf(ChessPieceType.QUEEN, ChessPieceType.KNIGHT, ChessPieceType.ROOK, ChessPieceType.BISHOP)

        //returns next possibles moves with out current game's restrictions
        @JvmStatic
        fun getNextMovesForPiece(chessPiece: ChessPieceCoordinate, isLowerPlayerWhite: Boolean): List<Coordinate> {

            require(chessPiece.coordinate.xPos in 0..7 && chessPiece.coordinate.yPos in 0..7) { "Invalid coordinates" }

            val type = chessPiece.piece.type

            return when (type) {

                ChessPieceType.PAWN -> {
                    getNextPawnMoves(chessPiece, isLowerPlayerWhite)
                }

                ChessPieceType.BISHOP -> {
                    return getNextBishopMoves(chessPiece)
                }

                ChessPieceType.KNIGHT -> {
                    return getNextKnightMoves(chessPiece)
                }

                ChessPieceType.ROOK -> {
                    return getNextRookMoves(chessPiece)
                }

                ChessPieceType.QUEEN -> {
                    return getNextQueenMoves(chessPiece)
                }

                ChessPieceType.KING -> {
                    return getNextKingMoves(chessPiece)
                }
            }
        }

        private fun getNextKingMoves(chessPiece: ChessPieceCoordinate): List<Coordinate> {

            require(chessPiece.piece.type == ChessPieceType.KING) { "This is not a king" }
            require(chessPiece.coordinate.xPos in 0..7 && chessPiece.coordinate.yPos in 0..7) { "Invalid coordinates" }

            val coord = chessPiece.coordinate

            val nextMoves = mutableListOf<Pair<Int, Int>>()

            nextMoves.add(Pair(coord.xPos, coord.yPos + 1))
            nextMoves.add(Pair(coord.xPos, coord.yPos - 1))
            nextMoves.add(Pair(coord.xPos + 1, coord.yPos))
            nextMoves.add(Pair(coord.xPos - 1, coord.yPos))

            nextMoves.add(Pair(coord.xPos + 1, coord.yPos + 1))
            nextMoves.add(Pair(coord.xPos + 1, coord.yPos - 1))
            nextMoves.add(Pair(coord.xPos - 1, coord.yPos + 1))
            nextMoves.add(Pair(coord.xPos - 1, coord.yPos - 1))

            return nextMoves.filter { it.first in 0..7 && it.second in 0..7 }.map { Coordinate(it.first, it.second) }
        }

        private fun getNextKnightMoves(chessPiece: ChessPieceCoordinate): List<Coordinate> {

            require(chessPiece.piece.type == ChessPieceType.KNIGHT) { "This is not a knight" }
            require(chessPiece.coordinate.xPos in 0..7 && chessPiece.coordinate.yPos in 0..7) { "Invalid coordinates" }

            val coord = chessPiece.coordinate

            val nextMoves = mutableListOf<Pair<Int, Int>>()

            nextMoves.add(Pair(coord.xPos + 1, coord.yPos + 2))
            nextMoves.add(Pair(coord.xPos + 2, coord.yPos + 1))

            nextMoves.add(Pair(coord.xPos + 1, coord.yPos - 2))
            nextMoves.add(Pair(coord.xPos + 2, coord.yPos - 1))

            nextMoves.add(Pair(coord.xPos - 1, coord.yPos + 2))
            nextMoves.add(Pair(coord.xPos - 2, coord.yPos + 1))

            nextMoves.add(Pair(coord.xPos - 1, coord.yPos - 2))
            nextMoves.add(Pair(coord.xPos - 2, coord.yPos - 1))

            return nextMoves.filter {
                it.first in 0..7 && it.second in 0..7
            }.map {
                Coordinate(it.first, it.second)
            }
        }

        private fun getNextQueenMoves(chessPiece: ChessPieceCoordinate): List<Coordinate> {

            require(chessPiece.piece.type == ChessPieceType.QUEEN) { "This is not a queen" }
            require(chessPiece.coordinate.xPos in 0..7 && chessPiece.coordinate.yPos in 0..7) { "Invalid coordinates" }

            val diagonal = getNextDiagonalMoves(chessPiece.coordinate)
            val straight = getNextStraightMoves(chessPiece.coordinate)

            val consolidated = mutableListOf<Coordinate>()
            consolidated.addAll(diagonal)
            consolidated.addAll(straight)

            return consolidated
        }

        private fun getNextRookMoves(chessPiece: ChessPieceCoordinate): List<Coordinate> {

            require(chessPiece.piece.type == ChessPieceType.ROOK) { "This is not a rook" }
            require(chessPiece.coordinate.xPos in 0..7 && chessPiece.coordinate.yPos in 0..7) { "Invalid coordinates" }

            return getNextStraightMoves(chessPiece.coordinate)
        }

        private fun getNextBishopMoves(chessPiece: ChessPieceCoordinate): List<Coordinate> {

            require(chessPiece.piece.type == ChessPieceType.BISHOP) { "This is not a bishop" }
            require(chessPiece.coordinate.xPos in 0..7 && chessPiece.coordinate.yPos in 0..7) { "Invalid coordinates" }

            return getNextDiagonalMoves(chessPiece.coordinate)
        }

        private fun getNextStraightMoves(coord: Coordinate): List<Coordinate> {

            val nextMoves = mutableListOf<Coordinate>()

            //to left
            for (xPos in (coord.xPos - 1) downTo 0) {
                nextMoves.add(Coordinate(xPos, coord.yPos))
            }

            //to top
            for (yPos in (coord.yPos - 1) downTo 0) {
                nextMoves.add(Coordinate(coord.xPos, yPos))
            }

            //to right
            for (xPos in (coord.xPos + 1)..7) {
                nextMoves.add(Coordinate(xPos, coord.yPos))
            }

            //to bottom
            for (yPos in (coord.yPos + 1)..7) {
                nextMoves.add(Coordinate(coord.xPos, yPos))
            }

            return nextMoves
        }

        private fun getNextDiagonalMoves(coord: Coordinate): List<Coordinate> {

            val nextMoves = mutableListOf<Coordinate>()

            //to top left corner
            for (rem in 1..min(coord.xPos, coord.yPos)) {
                nextMoves.add(Coordinate(coord.xPos - rem, coord.yPos - rem))
            }

            //to bottom right corner
            for (rem in 1..(7 - max(coord.xPos, coord.yPos))) {
                nextMoves.add(Coordinate(coord.xPos + rem, coord.yPos + rem))
            }

            //to bottom left corner
            for (rem in 1..min(coord.xPos, 7 - coord.yPos)) {
                nextMoves.add(Coordinate(coord.xPos - rem, coord.yPos + rem))
            }

            //to top right corner
            for (rem in 1..min(7 - coord.xPos, coord.yPos)) {
                nextMoves.add(Coordinate(coord.xPos + rem, coord.yPos - rem))
            }

            return nextMoves
        }

        private fun getNextPawnMoves(chessPiece: ChessPieceCoordinate, isLowerPlayerWhite: Boolean): List<Coordinate> {

            require(chessPiece.piece.type == ChessPieceType.PAWN) { "This is not a pawn" }
            require(chessPiece.coordinate.xPos in 0..7 && chessPiece.coordinate.yPos in 0..7) { "Invalid coordinates" }
            require(chessPiece.coordinate.yPos in 1..6) { "Invalid pawn position" }

            val coord = chessPiece.coordinate
            val isWhite = chessPiece.piece.isWhite

            val nextMoves = mutableListOf<Coordinate>()

            val yAdd = if (isWhite) {
                if (isLowerPlayerWhite) -1 else 1
            } else {
                if (isLowerPlayerWhite) 1 else -1
            }

            nextMoves.add(Coordinate(coord.xPos, coord.yPos + yAdd))
            if (coord.xPos > 0) {
                nextMoves.add(Coordinate(coord.xPos - 1, coord.yPos + yAdd))
            }
            if (coord.xPos != 7) {
                nextMoves.add(Coordinate(coord.xPos + 1, coord.yPos + yAdd))
            }

            if (isLowerPlayerWhite) {

                if (isWhite && coord.yPos == 6) {
                    nextMoves.add(Coordinate(coord.xPos, 4))
                } else if (!isWhite && coord.yPos == 1) {
                    nextMoves.add(Coordinate(coord.xPos, 3))
                }

            } else {

                if (isWhite && coord.yPos == 1) {
                    nextMoves.add(Coordinate(coord.xPos, 3))
                } else if (!isWhite && coord.yPos == 6) {
                    nextMoves.add(Coordinate(coord.xPos, 4))
                }
            }

            return nextMoves
        }

        private fun getMoveActionOnDevice(move: String, defaultConfig: DefaultChessConfigInfo): () -> Unit {

            val fromNormCell = getCellPos(move.substring(0, 2), defaultConfig.isLowerPlayerWhite!!)
            val toNormCell = getCellPos(move.substring(2, 4), defaultConfig.isLowerPlayerWhite!!)

            val promotionPiece = if (move.length > 4) move[4].toLowerCase() else null

            val bounds = defaultConfig.boundRect

            val cellWidth = bounds.width / 8
            val cellHeight = bounds.height / 8

            val fromCell = Pair(
                bounds.x + (fromNormCell.xPos * cellWidth) + (cellWidth / 2),
                bounds.y + (fromNormCell.yPos * cellHeight) + (cellHeight / 2)
            )
            val toCell = Pair(
                bounds.x + (toNormCell.xPos * cellWidth) + (cellWidth / 2),
                bounds.y + (toNormCell.yPos * cellHeight) + (cellHeight / 2)
            )

            return {

                val swipeCommand =
                    "adb shell input swipe ${fromCell.first} ${fromCell.second} ${toCell.first} ${toCell.second}"

                val swipeRetVal = Runtime.getRuntime().exec(swipeCommand).waitFor()

                check(swipeRetVal == 0) { "Cannot perform move action" }

                if (promotionPiece != null) {

                    Thread.sleep(700)

                    val prospectivePromotions = PROSPECTIVE_PROMO_PIECES.map { it.notation.toLowerCase()[0] }
                    val promotionLayoutBounds =
                        Rect(465, 783, 153, 544) // Hardcoded //TODO try to generalize it if possible

                    val promotionCellHeight = promotionLayoutBounds.height / prospectivePromotions.size
                    val selectedIndex = prospectivePromotions.indexOf(promotionPiece)

                    val selectedPieceCoord = Pair<Int, Int>(
                        promotionLayoutBounds.x + (promotionLayoutBounds.width / 2),
                        (selectedIndex * promotionCellHeight) + (promotionCellHeight / 2)
                    )

                    val promotionCommand = "adb shell input tap ${selectedPieceCoord.first} ${selectedPieceCoord.second}"
                    val tapRetVal = Runtime.getRuntime().exec(promotionCommand).waitFor()

                    check(tapRetVal == 0) { "Cannot tap on the promotion piece"}
                }
            }
        }

        private fun getCellPos(cellName: String, isLowerPlayerWhite: Boolean): Coordinate {

            var xPos: Int?
            var yPos: Int?

            val xName = cellName[0]
            val yName = cellName[1]

            if (isLowerPlayerWhite) {

                xPos = xName - 'a'
                yPos = 8 - yName.toString().toInt()

            } else {

                xPos = ('h' - xName)
                yPos = yName.toString().toInt() - 1
            }

            return Coordinate(xPos!!, yPos!!)
        }

        fun performNextBestMove(
            defaultConfig: DefaultChessConfigInfo,
            currFrame: MutableList<ChessPieceCoordinate>,
            chessEngine : IChessEngine,
            searchDepth: Int,
            opponentMove : String? = null
        ) {

            val bestMove =  chessEngine.getBestCounterAgainstOpponent(searchDepth, opponentMove)
            val bestMoveAction = getMoveActionOnDevice(bestMove, defaultConfig)

            val fromNormCell = getCellPos(bestMove.substring(0, 2), defaultConfig.isLowerPlayerWhite!!)
            val toNormCell = getCellPos(bestMove.substring(2, 4), defaultConfig.isLowerPlayerWhite!!)

            val promotionPieceNotation = if (bestMove.length > 4) bestMove[4].toLowerCase() else null

            val fromCellPiece = currFrame.first { it.coordinate == fromNormCell }
            val toCellPiece = currFrame.firstOrNull { it.coordinate == toNormCell }

            fun handleCastling(): Boolean {

                val xPosDiff = fromNormCell.xPos - toNormCell.xPos

                return if (fromCellPiece.piece.type == ChessPieceType.KING && abs(xPosDiff) == 2) {

                    val castledRook = currFrame.first {
                        it.piece.isWhite == fromCellPiece.piece.isWhite
                                && it.piece.type == ChessPieceType.ROOK
                                && it.coordinate.yPos == fromCellPiece.coordinate.yPos
                                && it.coordinate.xPos == (if(xPosDiff < 0) 7 else 0)
                    }

                    currFrame.remove(fromCellPiece)
                    currFrame.remove(castledRook)

                    currFrame.add(ChessPieceCoordinate(fromCellPiece.piece, toNormCell))

                    val xAdd =

                    if (xPosDiff < 0) {
                       -1
                    } else {
                        1
                    }

                    currFrame.add(
                        ChessPieceCoordinate(
                            castledRook.piece,
                            Coordinate(toNormCell.xPos + xAdd, toNormCell.yPos)
                        )
                    )

                    true

                } else {
                    false
                }
            }

            fun handleEnPassant(): Boolean {

                if (fromCellPiece.piece.type == ChessPieceType.PAWN && fromCellPiece.coordinate.xPos != toNormCell.xPos) {

                    val capturedPawn = currFrame.firstOrNull {
                        it.piece.type == ChessPieceType.PAWN
                                && it.piece.isWhite != fromCellPiece.piece.isWhite
                                && it.coordinate.yPos == fromCellPiece.coordinate.yPos
                                && it.coordinate.xPos == toNormCell.xPos
                    }

                    return if (capturedPawn != null) {

                        currFrame.remove(fromCellPiece)
                        currFrame.remove(capturedPawn)

                        currFrame.add(ChessPieceCoordinate(fromCellPiece.piece, toNormCell))

                        true
                    } else {
                        false
                    }

                } else {
                    return false
                }
            }

            fun handleSimplePromotion(): Boolean {

                return if (fromCellPiece.piece.type == ChessPieceType.PAWN && promotionPieceNotation != null) {

                    val promotedPieceType =
                        PROSPECTIVE_PROMO_PIECES.first { it.notation.toLowerCase() == promotionPieceNotation.toString() }

                    currFrame.remove(fromCellPiece)
                    currFrame.add(
                        ChessPieceCoordinate(
                            ChessPiece(promotedPieceType, fromCellPiece.piece.isWhite),
                            toNormCell
                        )
                    )

                    true
                } else {
                    false
                }
            }

            if (toCellPiece == null) { //castling / en passant / promotion

                if (!handleCastling()) {

                    if(!handleEnPassant()){

                        if(!handleSimplePromotion()){

                            //other wise simple move
                            currFrame.remove(fromCellPiece)
                            currFrame.add(ChessPieceCoordinate(fromCellPiece.piece, toNormCell))
                        }
                    }
                }

            } else {

                currFrame.remove(fromCellPiece)
                currFrame.remove(toCellPiece)

                if (promotionPieceNotation != null) {
                    val promotedPieceType =
                        PROSPECTIVE_PROMO_PIECES.first { it.notation.toLowerCase() == promotionPieceNotation.toString() }
                    currFrame.add(
                        ChessPieceCoordinate(
                            ChessPiece(promotedPieceType, fromCellPiece.piece.isWhite),
                            toNormCell
                        )
                    )
                } else {
                    currFrame.add(ChessPieceCoordinate(fromCellPiece.piece, toNormCell))
                }
            }

            bestMoveAction()
        }
    }
}