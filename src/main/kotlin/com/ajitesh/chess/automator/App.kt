package com.ajitesh.chess.automator


fun main() {

    val parentFolder = "/Users/ajitesh/Desktop/"
    val suffix = "Working"

    println("reading default configuration & position ...")
    val defaultConfig = ChessBoardUtil.readDefaultPosConfigFromDevice(parentFolder, suffix)
    println("Success, Monitoring...")

    val chessEngine: IChessEngine = StockFishEngine("/Users/ajitesh/Desktop/stockfish")
    chessEngine.startGame()

    var prevFrame = ChessBoardUtil.readPiecePositionsFromDevice(defaultConfig, 8.0f).toMutableList()

    if (defaultConfig.isLowerPlayerWhite!!) {
        println("You are white.")
        ChessMovesUtil.performNextBestMove(defaultConfig, prevFrame, chessEngine, 15)
        val myMove = chessEngine.getPrevMoves().last()
        println("Bot - $myMove")
    } else {
        println("You are black.")
        println("Press enter after opponent's move")
        readLine()
    }

    while (true) {

        Thread.sleep(300)

        println("Detecting opponent's move...")

        var opponentMoveInfo = try {
            ChessBoardUtil.detectChessMoveFromDevice(defaultConfig, prevFrame, 8.0f)
        } catch (e: Exception) {
            null
        }

        if (opponentMoveInfo == null) {
            println("No opponent move detected")
            continue
        }

        println("Opponent - ${opponentMoveInfo.latestMove}")

        val currFrame = opponentMoveInfo.currFrame.toMutableList()
        val opponentsMove = opponentMoveInfo.latestMove

        ChessMovesUtil.performNextBestMove(defaultConfig, currFrame, chessEngine, 20, opponentsMove)

        val myMove = chessEngine.getPrevMoves().last()
        println("Bot - $myMove")

        prevFrame = currFrame
    }
}

/*
fun drawContours() {

    ChessBoardUtil.getCellName(Coordinate(1, 1), true)

    val path = "/Users/ajitesh/Desktop/Working/chess_bug.png"

    val screenShotImg = Imgcodecs.imread(path, Imgcodecs.IMREAD_GRAYSCALE)

    val blackWhite = Mat()
    Imgproc.threshold(screenShotImg, blackWhite, 244.0, 255.0, Imgproc.THRESH_BINARY)
    Imgcodecs.imwrite("/Users/ajitesh/Desktop/Working/black_highlighted.png", blackWhite)


    val contours = mutableListOf<MatOfPoint>()
    Imgproc.findContours(blackWhite, contours, Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)

    val toDrawContours = contours.map { contour ->

        val contour2f = MatOfPoint2f()
        contour.convertTo(contour2f, CvType.CV_32FC2)

        val approxContour = MatOfPoint()
        val approxContour2f = MatOfPoint2f()

        Imgproc.approxPolyDP(
            contour2f,
            approxContour2f,
            0.07 * Imgproc.arcLength(contour2f, true),
            true
        )

        approxContour2f.convertTo(approxContour, CvType.CV_32S)
        approxContour
    }

    val contourImg = Mat(blackWhite.rows(), blackWhite.cols(), CvType.CV_8U, Scalar.all(0.0))

    toDrawContours.forEachIndexed {index, it ->
        Imgproc.drawContours(contourImg, contours, index, Scalar.all(255.0), 1)
    }

    Imgcodecs.imwrite("/Users/ajitesh/Desktop/Working/black_contours.png", contourImg)
    contourImg.release()
    screenShotImg.release()
    blackWhite.release()
}
*/
