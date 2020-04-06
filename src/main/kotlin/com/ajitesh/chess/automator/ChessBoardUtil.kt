package com.ajitesh.chess.automator

import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.awt.Rectangle
import java.io.File
import java.lang.StringBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs
import kotlin.math.sqrt

object ChessBoardUtil {

    private val whiteContourParams: ContourGenerationParams
    private val blackContourParams: ContourGenerationParams

    init {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)

        this.whiteContourParams = ContourGenerationParams(true, 244.0, 255.0, Imgproc.THRESH_BINARY, 0.07)
        this.blackContourParams = ContourGenerationParams(false, 60.0, 255.0, Imgproc.THRESH_BINARY_INV, 0.07)
    }

    private const val IMAGE_EXTENSION = "png"
    private const val SCREEN_SHOT_NAME = "screen_shot"
    private const val UI_DUMP_FULL_NAME = "ui_dump.xml"
    private const val CHESS_BOARD_IMAGE_NAME = "chess_board"

    fun readDefaultPosConfigFromDevice(
        parentFolderPath: String,
        workingFolderSuffix: String
    ): DefaultChessConfigInfo {

        var defaultCaptureInfo = dumpDeviceUI(parentFolderPath, workingFolderSuffix)
        defaultCaptureInfo = generateDefaultChessPieceContours(defaultCaptureInfo)

        return defaultCaptureInfo
    }

    fun readPiecePositionsFromDevice(
        inputParams: DefaultChessConfigInfo,
        diffThreshold: Float
    ): List<ChessPieceCoordinate> {
        generateChessBoardImage(inputParams)
        return readBoardFromImage(inputParams, diffThreshold)
    }

    fun detectChessMoveFromDevice(
        defaultConfig: DefaultChessConfigInfo,
        prevFrame: List<ChessPieceCoordinate>,
        diffThreshold: Float
    ): MoveInfo? {

        val currFrame = readPiecePositionsFromDevice(defaultConfig, diffThreshold).toMutableList()
        val detectedMove = detectRawChessMove(defaultConfig, prevFrame, currFrame) ?: return null
        return MoveInfo(detectedMove.toString(), currFrame)
    }

    private fun readBoardFromImage(
        inputParams: DefaultChessConfigInfo,
        diffThreshold: Float
    ): List<ChessPieceCoordinate> {

        val chessBoardPath = "${inputParams.workingFolderPath}/${CHESS_BOARD_IMAGE_NAME}.${IMAGE_EXTENSION}"

        val pieceCoordinates = mutableListOf<ChessPieceCoordinate>()

        val boardImg = Imgcodecs.imread(chessBoardPath, Imgcodecs.IMREAD_GRAYSCALE)

        for (xPos in 0 until 8) {

            for (yPos in 0 until 8) {

                val currCellContours = getContourForCell(boardImg, xPos, yPos).getContourPoints()

                for (pieceContour in inputParams.defaultContours!!) {

                    if (matchContourPoints(pieceContour.points, currCellContours, diffThreshold)) {
                        pieceCoordinates.add(ChessPieceCoordinate(pieceContour.piece, Coordinate(xPos, yPos)))
                    }
                }
            }
        }

        boardImg.release()

        return pieceCoordinates
    }

    private fun generateDefaultChessPieceContours(inputParams: DefaultChessConfigInfo): DefaultChessConfigInfo {

        val chessBoardPath = "${inputParams.workingFolderPath}/${CHESS_BOARD_IMAGE_NAME}.${IMAGE_EXTENSION}"

        val boardImg = Imgcodecs.imread(chessBoardPath, Imgcodecs.IMREAD_GRAYSCALE)

        val pieceContourList = mutableListOf<ChessPieceContour>()

        for (xIndex in 0 until 5) {

            for (yIndex in 0 until 8) {

                if ((yIndex in 2..5)
                    || ((yIndex == 1 || yIndex == 6) && xIndex in 1..7)
                ) {
                    continue
                }

                val rawContours = getContourForCell(boardImg, xIndex, yIndex)

                if (rawContours.isEmpty())
                    continue

                val chessPiece = getDefaultPosChessPiece(xIndex, yIndex, inputParams.isLowerPlayerWhite!!)
                val pieceContour = ChessPieceContour(chessPiece, rawContours.getContourPoints())

                pieceContourList.add(pieceContour)
            }
        }

        boardImg.release()

        if (pieceContourList.size != 12) {
            throw Exception("Cannot recognise all the pieces")
        }

        return DefaultChessConfigInfo(
            inputParams.workingFolderPath,
            inputParams.boundRect,
            inputParams.isLowerPlayerWhite,
            pieceContourList
        )
    }

    private fun getContourForCell(boardImg: Mat, xPos: Int, yPos: Int): List<MatOfPoint> {

        val whiteContours = getContourForCell(boardImg, xPos, yPos, this.whiteContourParams)

        return if (whiteContours.isNotEmpty()) {
            whiteContours
        } else {
            getContourForCell(boardImg, xPos, yPos, this.blackContourParams)
        }
    }

    private fun getContourForCell(
        boardImg: Mat,
        xPos: Int,
        yPos: Int,
        params: ContourGenerationParams
    ): List<MatOfPoint> {

        require(xPos in 0..7 && yPos in 0..7) { "Invalid position passed" }

        val cellWidth = boardImg.width() / 8
        val cellHeight = boardImg.height() / 8

        val cellImg = boardImg.submat(Rect(xPos * cellWidth, yPos * cellHeight, cellWidth, cellHeight))

        val greyScaleImage = Mat()

        Imgproc.threshold(cellImg, greyScaleImage, params.threshold, params.maxValue, params.thresholdType)
        cellImg.release()

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(greyScaleImage, contours, Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)

        greyScaleImage.release()

        return contours.map { contour ->

            val contour2f = MatOfPoint2f()
            contour.convertTo(contour2f, CvType.CV_32FC2)

            val approxContour = MatOfPoint()
            val approxContour2f = MatOfPoint2f()

            Imgproc.approxPolyDP(
                contour2f,
                approxContour2f,
                params.epsilonFactor * Imgproc.arcLength(contour2f, true),
                true
            )

            approxContour2f.convertTo(approxContour, CvType.CV_32S)

            approxContour
        }
    }

    private fun generateChessBoardImage(input: DefaultChessConfigInfo): DefaultChessConfigInfo {

        val screenShotCommand =
            "adb shell screencap -p /sdcard/${SCREEN_SHOT_NAME}.${IMAGE_EXTENSION}"
        val screenShotPullCommand =
            "adb pull /sdcard/${SCREEN_SHOT_NAME}.${IMAGE_EXTENSION} ${input.workingFolderPath}/${SCREEN_SHOT_NAME}.${IMAGE_EXTENSION}"

        val screenShotRetVal = Runtime.getRuntime().exec(screenShotCommand).waitFor()
        check(screenShotRetVal == 0) { "Cannot take screen shot" }

        val screenShotPullRetVal = Runtime.getRuntime().exec(screenShotPullCommand).waitFor()
        check(screenShotPullRetVal == 0) { "Cannot pull screen shot" }

        val boundsRect = input.boundRect

        val screenShotFile = File("${input.workingFolderPath}/${SCREEN_SHOT_NAME}.${IMAGE_EXTENSION}")
        val chessBoardFile = File("${input.workingFolderPath}/${CHESS_BOARD_IMAGE_NAME}.${IMAGE_EXTENSION}")

        if (chessBoardFile.exists()) {
            chessBoardFile.delete()
        }

        val screenShotMat = Imgcodecs.imread(screenShotFile.absolutePath, Imgcodecs.IMREAD_GRAYSCALE)
        val chessBoardMat = Mat(screenShotMat, Rect(boundsRect.x, boundsRect.y, boundsRect.width, boundsRect.height))
        Imgcodecs.imwrite(chessBoardFile.absolutePath, chessBoardMat)

        val detectX = (chessBoardMat.width() * (1f / 16)).toInt()
        val detectY = (chessBoardMat.height() * (1f / 16)).toInt()
        val isLowerPlayerWhite = chessBoardMat.get(detectX, detectY)[0] < 90

        chessBoardMat.release()
        screenShotMat.release()
        screenShotFile.delete()

        return DefaultChessConfigInfo(input.workingFolderPath, input.boundRect, isLowerPlayerWhite)
    }

    private fun getChessBoardBoundsRectRec(node: Node): Rectangle? {

        val attrs = node.attributes ?: return null

        var isChessBoard = false
        var currIndex = 0
        var boundsString: String? = null

        while (currIndex < attrs.length) {

            val currItem = attrs.item(currIndex)

            if ("bounds" == currItem.nodeName) {
                boundsString = currItem.nodeValue
            }

            if ("resource-id" == currItem.nodeName && "com.chess:id/chessBoardView" == currItem.nodeValue) {
                isChessBoard = true
            }

            currIndex++
        }

        if (isChessBoard && !boundsString.isNullOrBlank()) {

            val dataArray = boundsString.split("[", "]", ",").filter { !it.isBlank() }

            val startX = dataArray[0].toInt()
            val startY = dataArray[1].toInt()

            val width = dataArray[2].toInt() - startX
            val height = dataArray[3].toInt() - startY

            return Rectangle(startX, startY, width, height)

        } else {

            val childNodes = node.childNodes
            var childIndex = 0

            while (childIndex < childNodes.length) {

                val childResult = getChessBoardBoundsRectRec(childNodes.item(childIndex))

                if (childResult != null) {
                    return childResult
                }

                childIndex++
            }
        }

        return null
    }

    private fun dumpDeviceUI(parentFolderPath: String, workingFolderSuffix: String): DefaultChessConfigInfo {

        val parentFolder = File(parentFolderPath)
        if (!parentFolder.exists() || !parentFolder.isDirectory) {
            throw IllegalArgumentException("$parentFolderPath folder doesn't exists")
        }

        val workingFolder = File("${parentFolder.absolutePath}/UI_DUMP_${workingFolderSuffix}")

        if (workingFolder.exists()) {
            workingFolder.delete()
        }
        workingFolder.mkdir()

        val workingFolderPath = workingFolder.absolutePath

        var uiCaptureInfo = this.dumpUIHierarchy(workingFolderPath)
        uiCaptureInfo = this.generateChessBoardImage(uiCaptureInfo)

        return uiCaptureInfo
    }

    private fun dumpUIHierarchy(destFolderPath: String): DefaultChessConfigInfo {

        val uiDumpCommand = "adb shell uiautomator dump"
        val uiDumpPullCommand =
            "adb pull /sdcard/window_dump.xml ${destFolderPath}/${UI_DUMP_FULL_NAME}"


        val uiDumpRetVal = Runtime.getRuntime().exec(uiDumpCommand).waitFor()
        check(uiDumpRetVal == 0) { "Cannot take UI dump" }

        val dumpPullRetVal = Runtime.getRuntime().exec(uiDumpPullCommand).waitFor()
        check(dumpPullRetVal == 0) { "Cannot pull UI dump" }

        val uiHierarchyFile = File("${destFolderPath}/${UI_DUMP_FULL_NAME}")
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc: Document = dBuilder.parse(uiHierarchyFile)
        doc.documentElement.normalize()

        val boundsRect = getChessBoardBoundsRectRec(doc.firstChild) ?: throw Exception("Cannot find matching bounds")

        return DefaultChessConfigInfo(destFolderPath, boundsRect)
    }

    private fun getDefaultPosChessPiece(xPos: Int, yPos: Int, isLowerPlayerWhite: Boolean): ChessPiece {

        fun getDefaultChessPieceTypeWithXPos(x: Int): ChessPieceType {
            return when (x) {

                0, 7 -> ChessPieceType.ROOK
                1, 6 -> ChessPieceType.KNIGHT
                2, 5 -> ChessPieceType.BISHOP
                3 -> {
                    if (isLowerPlayerWhite) ChessPieceType.QUEEN else ChessPieceType.KING
                }
                else -> {
                    if (isLowerPlayerWhite) ChessPieceType.KING else ChessPieceType.QUEEN
                }
            }
        }

        if (xPos < 0 || xPos > 7 || yPos in 2..5 || yPos < 0 || yPos > 7) {
            throw java.lang.IllegalArgumentException("Invalid position in unmodified chessboard")
        }

        val isWhite = if (isLowerPlayerWhite) {
            yPos > 1
        } else {
            yPos <= 1
        }

        val type = if (yPos <= 1) {

            when (yPos) {

                1 -> ChessPieceType.PAWN

                else -> {
                    getDefaultChessPieceTypeWithXPos(xPos)
                }
            }

        } else {

            when (yPos) {

                6 -> ChessPieceType.PAWN

                else -> {
                    getDefaultChessPieceTypeWithXPos(xPos)
                }
            }
        }

        return ChessPiece(type, isWhite)
    }

    private fun matchContourPoints(lhs: List<Point>, rhs: List<Point>, diffThreshold: Float): Boolean {

        if (lhs.size != rhs.size)
            return false

        for (index in lhs.indices) {

            val lhsPoint = lhs[index]
            val rhsPoint = rhs[index]

            val xDiff = abs(lhsPoint.x - rhsPoint.x)
            val yDiff = abs(lhsPoint.y - rhsPoint.y)

            val diff = sqrt((xDiff * xDiff) + (yDiff * yDiff))

            if (diff > diffThreshold)
                return false
        }

        return true
    }

    private fun detectRawChessMove(
        defaultConfig: DefaultChessConfigInfo,
        previousFrame: List<ChessPieceCoordinate>,
        currFrame: List<ChessPieceCoordinate>
    ): RawChessMove? {

        val alteredLhsPieceSet = previousFrame.subtract(currFrame)

        if (alteredLhsPieceSet.isEmpty()) {
            return null
        }

        if (alteredLhsPieceSet.size > 2) {

            val errorBuilder = alteredLhsPieceSet.toList().fold(StringBuilder()){ builder, it ->
                builder.append("-- ${it.piece.type} (${it.piece.isWhite}), [${it.coordinate.xPos}, ${it.coordinate.yPos}] --")
                builder
            }.insert(0,  "Detected more than two alterations")

            throw Exception(errorBuilder.toString())
        }

        getCastlingIfPresent(alteredLhsPieceSet, defaultConfig.isLowerPlayerWhite!!)?.let {
            return it
        }

        getAdditionIfPresent(previousFrame, currFrame, defaultConfig.isLowerPlayerWhite)?.let {
            return it
        }

        return if (alteredLhsPieceSet.size == 1) {

            val toCoords = currFrame.subtract(previousFrame)

            if (toCoords.size != 1) {
                throw Exception("Invalid move") //TODO Check
            }

            val fromCoord = alteredLhsPieceSet.first()
            val toCoord = toCoords.first()

            RawChessMove.SimpleMove(
                fromCoord.piece,
                fromCoord.coordinate,
                toCoord.coordinate,
                defaultConfig.isLowerPlayerWhite
            )

        } else {

            val alteredList = alteredLhsPieceSet.toList()
            val first = alteredList[0]
            val second = alteredList[1]

            val isFirstKiller = currFrame.any { it.piece == first.piece && it.coordinate == second.coordinate }

            if (isFirstKiller) {
                RawChessMove.KillMove(
                    first.piece,
                    second.piece,
                    first.coordinate,
                    second.coordinate,
                    defaultConfig.isLowerPlayerWhite
                )
            } else {
                RawChessMove.KillMove(
                    second.piece,
                    first.piece,
                    second.coordinate,
                    first.coordinate,
                    defaultConfig.isLowerPlayerWhite
                )
            }
        }
    }

    private fun getCastlingIfPresent(
        alteredLhsPieces: Set<ChessPieceCoordinate>,
        isLowerPlayerWhite: Boolean
    ): RawChessMove.CastlingMove? {

        alteredLhsPieces.let {

            val whiteKing = alteredLhsPieces.firstOrNull { it.piece.type == ChessPieceType.KING && it.piece.isWhite }
            val whiteRook = alteredLhsPieces.firstOrNull { it.piece.type == ChessPieceType.ROOK && it.piece.isWhite }

            if (whiteKing != null && whiteRook != null) {
                return RawChessMove.CastlingMove(whiteKing, whiteRook, isLowerPlayerWhite)
            }
        }

        alteredLhsPieces.let {

            val blackKing = alteredLhsPieces.firstOrNull { it.piece.type == ChessPieceType.KING && !it.piece.isWhite }
            val blackRook = alteredLhsPieces.firstOrNull { it.piece.type == ChessPieceType.ROOK && !it.piece.isWhite }

            if (blackKing != null && blackRook != null) {
                return RawChessMove.CastlingMove(blackKing, blackRook, isLowerPlayerWhite)
            }
        }

        return null
    }

    private fun getAdditionIfPresent(
        previousFrame: List<ChessPieceCoordinate>,
        currFrame: List<ChessPieceCoordinate>,
        isLowerPlayerWhite: Boolean
    ): RawChessMove.PromotionMove? {

        val lhsPieces = previousFrame
            .filter { it.piece.type != ChessPieceType.PAWN && it.piece.type != ChessPieceType.KING }
            .groupBy { it.piece }

        val rhsPieces = currFrame
            .filter { it.piece.type != ChessPieceType.PAWN && it.piece.type != ChessPieceType.KING }
            .groupBy { it.piece }

        val additions = mutableListOf<ChessPieceCoordinate>()

        for (rhsPiece in rhsPieces) {

            if (lhsPieces.containsKey(rhsPiece.key)) {

                val lhsCoords = lhsPieces[rhsPiece.key] ?: error("Cannot Check for addition")
                val rhsCoords = rhsPiece.value

                if (lhsCoords.size < rhsCoords.size) {
                    additions.addAll(rhsCoords.subtract(lhsCoords))
                }

            } else {
                additions.addAll(rhsPiece.value)
            }
        }

        return when {

            additions.size > 1 -> {
                throw Exception("More than one addition detected")
            }
            additions.isEmpty() -> {
                null
            }

            else -> {

                val addition = additions.first()

                if ((isLowerPlayerWhite && addition.coordinate.yPos != 7)
                    || (!isLowerPlayerWhite && addition.coordinate.yPos != 0)
                ) {
                    throw Exception("Invalid promotion position")
                }


                val alteredLhsPieces = previousFrame.subtract(currFrame)

                val prevYPos = if (addition.piece.isWhite) {
                    if (isLowerPlayerWhite) 1 else 6
                } else {
                    if (isLowerPlayerWhite) 6 else 1
                }

                val prosPromCandidates =
                    alteredLhsPieces.filter { pc ->
                        pc.coordinate.yPos == prevYPos
                                && pc.piece.isWhite == addition.piece.isWhite
                                && pc.piece.type == ChessPieceType.PAWN
                                && abs(pc.coordinate.xPos - addition.coordinate.xPos) <= 1
                    }

                if (prosPromCandidates.size != 1) {
                    throw Exception("Invalid promotion")
                }

                RawChessMove.PromotionMove(
                    addition.piece,
                    prosPromCandidates.first().coordinate,
                    addition.coordinate,
                    isLowerPlayerWhite
                )
            }
        }
    }

    fun getCellName(coord: Coordinate, isLowerPlayerWhite: Boolean): String {

        require(coord.xPos in 0..7 && coord.yPos in 0..7) { "Invalid coordinates" }

        val xName = if (isLowerPlayerWhite) {
            'a' + coord.xPos
        } else {
            'h' - coord.xPos
        }

        val yName = if (isLowerPlayerWhite) {
            (8 - coord.yPos)
        } else {
            coord.yPos + 1
        }

        return "${xName}${yName}"
    }
}