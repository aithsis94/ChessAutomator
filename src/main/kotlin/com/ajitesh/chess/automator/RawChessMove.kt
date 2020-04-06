package com.ajitesh.chess.automator

import kotlin.math.abs

abstract sealed class RawChessMove(val from: Coordinate, val to: Coordinate, val isLowerPlayerWhite: Boolean) {

    override fun toString(): String {

        val fromCellName = ChessBoardUtil.getCellName(from, isLowerPlayerWhite!!)
        val toCellName = ChessBoardUtil.getCellName(to, isLowerPlayerWhite!!)

        return "${fromCellName}${toCellName}"
    }

    open class SimpleMove(val poiPiece: ChessPiece, from: Coordinate, to: Coordinate, isLowerPlayerWhite: Boolean) :
        RawChessMove(from, to, isLowerPlayerWhite) {

        init {
            val possibleMoves =
                ChessMovesUtil.getNextMovesForPiece(ChessPieceCoordinate(poiPiece, from), isLowerPlayerWhite)
            require(possibleMoves.contains(to)) {
                "Invalid move piece = ${poiPiece.type}(${poiPiece.isWhite}) - (${from.xPos}, ${from.yPos}) -> (${to.xPos}, ${to.yPos})"
            }
        }
    }

    class KillMove(
        poiPiece: ChessPiece,
        val victimPiece: ChessPiece,
        from: Coordinate,
        to: Coordinate,
        isLowerPlayerWhite: Boolean
    ) : SimpleMove(poiPiece, from, to, isLowerPlayerWhite) {

        init {
            val possibleMoves =
                ChessMovesUtil.getNextMovesForPiece(ChessPieceCoordinate(poiPiece, from), isLowerPlayerWhite)
            require(possibleMoves.contains(to)) { "Invalid move" }
        }
    }

    class CastlingMove(val king: ChessPieceCoordinate, val rook: ChessPieceCoordinate, isLowerPlayerWhite: Boolean) :
        RawChessMove(king.coordinate, rook.coordinate, isLowerPlayerWhite) {

        init {

            require(
                king.piece.isWhite == king.piece.isWhite
                        && king.piece.type == ChessPieceType.KING
                        && rook.piece.type == ChessPieceType.ROOK
            ) {
                "Invalid castling peices"
            }

            val expectedYPos = if (isLowerPlayerWhite) {
                if (king.piece.isWhite) 7 else 0
            } else {
                if (king.piece.isWhite) 0 else 7
            }

            require(king.coordinate.yPos == rook.coordinate.yPos && (rook.coordinate.yPos == expectedYPos)) {
                "Invalid castling position"
            }

            require(abs(king.coordinate.xPos - rook.coordinate.xPos) in 3..4) { "Invalid castling window" }
        }

        override fun toString(): String {

            val xPosAdd = if (king.coordinate.xPos > rook.coordinate.xPos) -2 else 2

            val fromCellName = ChessBoardUtil.getCellName(king.coordinate, isLowerPlayerWhite)
            val toCellName = ChessBoardUtil.getCellName(
                Coordinate(king.coordinate.xPos + xPosAdd, king.coordinate.yPos),
                isLowerPlayerWhite
            )

            return "${fromCellName}${toCellName}"
        }
    }

    class PromotionMove(
        val upgradedRank: ChessPiece,
        from: Coordinate,
        to: Coordinate,
        isLowerPlayerWhite: Boolean
    ) : RawChessMove(from, to, isLowerPlayerWhite) {

        init {

            val expectedYPos = if (isLowerPlayerWhite) {
                if (upgradedRank.isWhite) 0 else 7
            } else {
                if (upgradedRank.isWhite) 7 else 0
            }

            require(to.yPos == expectedYPos) { "Invalid position" }
            require(ChessMovesUtil.PROSPECTIVE_PROMO_PIECES.contains(upgradedRank.type)) { "Invalid promotion" }

            require(abs(from.yPos - to.yPos) != 1 || abs(from.xPos - to.xPos) <= 1) { "Invalid promotion" }
        }

        override fun toString(): String {

            val baseString = super.toString()

            var notation = upgradedRank.type.notation

            if (!upgradedRank.isWhite) {
                notation = notation.toLowerCase()
            }

            return "${baseString}${if(upgradedRank.isWhite) notation.toUpperCase() else notation.toLowerCase()}"
        }
    }
}