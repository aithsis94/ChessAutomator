package com.ajitesh.chess.automator

import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.util.*

class StockFishEngine(private val enginePath: String) : IChessEngine {

    private lateinit var engineProcess: Process

    private lateinit var streamWriter: BufferedWriter
    private lateinit var streamReader: Scanner

    private val prevMoves = mutableListOf<String>()

    override fun getPrevMoves() = prevMoves as List<String>

    @Synchronized
    override fun startGame() {

        check(!this.isGameRunning()) { "Game is already running" }

        this.engineProcess = Runtime.getRuntime().exec(enginePath)
        this.prevMoves.clear()

        this.streamWriter = BufferedWriter(OutputStreamWriter(this.engineProcess.outputStream))
        this.streamReader = Scanner(this.engineProcess.inputStream)

        this.streamWriter.write("ucinewgame\n")
        this.streamWriter.flush()
    }

    @Synchronized
    override fun getBestCounterAgainstOpponent(depth: Int, currMove: String?): String {

        val builder = StringBuilder()
        builder.append("position startpos moves")

        if (!currMove.isNullOrBlank()) {
            this.prevMoves.add(currMove)
        }

        for (move in this.prevMoves) {
            builder.append(" ")
            builder.append(move)
        }
        builder.append("\n")

        val positionCommand = builder.toString()
        val goCommand = "go depth ${depth}\n"

        streamWriter.write(positionCommand)
        streamWriter.flush()

        streamWriter.write(goCommand)
        streamWriter.flush()

        var bestMove: String? = null

        while (true) {

            val line = streamReader.nextLine()

            if (line.startsWith("bestmove")) {
                bestMove = line.split(" ")[1]
                break
            }
        }

        prevMoves.add(bestMove!!)

        return bestMove
    }

    @Synchronized
    override fun stopGame() {

        this.streamWriter.write("quit\n")
        this.streamWriter.flush()

        this.streamWriter.close()
        this.streamReader.close()

        this.engineProcess.destroy()
    }

    @Synchronized
    override fun isGameRunning(): Boolean {

        return if (!::engineProcess.isInitialized) {
            false
        } else {
            this.engineProcess.isAlive
        }
    }
}