package com.ajitesh.chess.automator

interface IChessEngine {

    fun getPrevMoves(): List<String>

    fun startGame()

    fun getBestCounterAgainstOpponent(depth: Int, currMove: String? = null): String

    fun stopGame()

    fun isGameRunning(): Boolean
}