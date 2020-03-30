package com.ajitesh.chess.automator

import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.awt.Color
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileFilter
import java.util.*
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.min
import kotlin.math.sqrt

object ChessMoveDetector {

    init {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    }

    val PLATFORM_TOOLS_FOLDER = "/Users/ajitesh/Library/Android/sdk/platform-tools"
    private val IMAGE_EXTENSION = "png"
    private val SCREEN_SHOT_NAME = "screen_shot"
    private val UI_DUMP_FULL_NAME = "ui_dump.xml"
    private val CHESS_BOARD_IMAGE_NAME = "chess_board"
    private val CHESS_PIECE_FOLDER = "/Users/ajitesh/Desktop/ChessPieces/"

    fun printBoard(captureFolderPath: String) {

        val chessBoardPath = "${captureFolderPath}/${CHESS_BOARD_IMAGE_NAME}.${IMAGE_EXTENSION}"
        val chessPieces = File(CHESS_PIECE_FOLDER).listFiles(FileFilter {
            it.extension == IMAGE_EXTENSION
        })
            .map { ChessPieceImage(it.nameWithoutExtension, ImageIO.read(it)) }

        val sourceImg = ImageIO.read(File(chessBoardPath))

        val sourceWidth = sourceImg.width
        val sourceHeight = sourceImg.height

        val cellWidth = sourceWidth / 8
        val cellHeight = sourceHeight / 8

        for (xIndex in 0 until 8) {

            val currX = cellWidth * xIndex

            for (yIndex in 0 until 8) {

                if(yIndex in 2..5) //TODO ajitesh delete
                    continue

                val currY = cellHeight * yIndex
                val cellImage = sourceImg.getSubimage(currX, currY, cellWidth, cellHeight)

                for (chessPiece in chessPieces) {

                    val doesContains = doesContainsTemplate(cellImage, chessPiece.image, 0.99f, 10)

                    if (doesContains) {
                        println("$xIndex, $yIndex = ${chessPiece.name}")
                        break
                    }
                }
            }
        }
    }

    fun doesContainsTemplate(
        source: BufferedImage,
        template: BufferedImage,
        acceptedPercentageMatch: Float,
        colorDiffThreshold: Int
    ): Boolean {

        require(acceptedPercentageMatch in 0.1f..1.0f) {
            "invalid match percentile"
        }

        val sourceWidth = source.width
        val sourceHeight = source.height

        val templateWidth = template.width
        val templateHeight = template.height

        val templatePixCount = (templateWidth * templateHeight).toFloat()

        for (outerX in 0 until sourceWidth) {
            for (outerY in 0 until sourceHeight) {

                val templateEndX = minOf(sourceWidth - outerX, templateWidth)
                val templateEndY = minOf(sourceHeight - outerY, templateHeight)

                val availableWindowPixels = templateEndX * templateEndY.toFloat()

                if ((availableWindowPixels / templatePixCount) < acceptedPercentageMatch) {
                    continue
                }

                var remainingPixels = availableWindowPixels
                var matchedPixels = 0.0f

                window@ for (templateX in 0 until templateEndX) {

                    val sourceX = outerX + templateX

                    for (templateY in 0 until templateEndY) {

                        val sourceColor = source.getRGB(sourceX, outerY + templateY)
                        val templateColor = template.getRGB(templateX, templateY)

                        val sourceR = sourceColor and 0x00ff0000
                        val sourceG = sourceColor and 0x0000ff00
                        val sourceB = sourceColor and 0x000000ff

                        val templateR = templateColor and 0x00ff0000
                        val templateG = templateColor and 0x0000ff00
                        val templateB = templateColor and 0x000000ff

                        val rDiff = diff(sourceR, templateR)
                        val gDiff = diff(sourceG, templateG)
                        val bDiff = diff(sourceB, templateB)

                        val colorDiff = sqrt((rDiff * rDiff) + (gDiff * gDiff) + (bDiff * bDiff).toFloat())

                        if (colorDiff <= colorDiffThreshold) {
                            matchedPixels++
                        }

                        remainingPixels--

                        val matchedPercentage = matchedPixels / templatePixCount
                        val remainingPercentage = remainingPixels / templatePixCount

                        if(matchedPercentage >= acceptedPercentageMatch){
                            return true
                        }

                        if (matchedPercentage + remainingPercentage < acceptedPercentageMatch) {
                            break@window
                        }
                    }
                }

                if (matchedPixels / templatePixCount >= acceptedPercentageMatch) {
                    return true
                }
            }
        }

        return false
    }

    private fun doesContainsTemplate(original: Mat, template: Mat): Core.MinMaxLocResult {

        val outputImage = Mat()
        val machMethod = Imgproc.TM_CCOEFF

        Imgproc.matchTemplate(original, template, outputImage, machMethod)

        val mmr = Core.minMaxLoc(outputImage)
        //val matchLoc = mmr.maxLoc

        return mmr

        /*println("${mmr.minVal}, ${mmr.maxVal}")

        return false*/
    }


    fun generateChessPieceContours(whiteChessBoardPath: String, destFolderPath: String){

        val chessBoardImg = Imgcodecs.imread(whiteChessBoardPath, Imgcodecs.IMREAD_GRAYSCALE)

        val threshold = Mat()
        Imgproc.threshold(chessBoardImg, threshold, 240.0, 255.0, Imgproc.THRESH_BINARY)
        chessBoardImg.release()

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(threshold, contours, Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)


        for(contour in contours){

            val contour2f = MatOfPoint2f()
            contour.convertTo(contour2f, CvType.CV_32FC2);

            val approxContour = MatOfPoint()
            val approxContour2f = MatOfPoint2f()

            Imgproc.approxPolyDP(contour2f, approxContour2f, 0.03 * Imgproc.arcLength(contour2f, true),  true)
            approxContour2f.convertTo(approxContour, CvType.CV_32S)




            //Imgproc.drawContours(threshold, mutableListOf(approxContour), 0, Scalar(150.0, 150.0, 150.0), 2)
        }

        threshold.release()
    }

    fun generateChessPieces(
        whiteChessBoardPath: String,
        destFolderPath: String,
        margin: Margin,
        isLowerPlayerWhite: Boolean
    ) {

        val sourceFile = File(whiteChessBoardPath)
        val sourceImage = ImageIO.read(sourceFile)

        val destFolder = File(destFolderPath)

        if (!destFolder.exists()) {
            destFolder.mkdir()
        }

        destFolder.listFiles().forEach {
            it.delete()
        }

        val cellWidth = sourceImage.width / 8
        val cellHeight = sourceImage.height / 8

        for (xIndex in 0 until 8) {

            for (yIndex in 0 until 8) {

                if ((yIndex in 2..5)
                    || ((yIndex == 1 || yIndex == 6) && xIndex in 2..7)
                ) {
                    continue
                }

                val xPos = (xIndex * cellWidth) + margin.left
                val yPos = (yIndex * cellHeight) + margin.top

                val width = cellWidth - margin.right - margin.left
                val height = cellHeight - margin.bottom - margin.top

                val chessPieceImage = sourceImage.getSubimage(xPos, yPos, width, height)

                var fileNameSuffix = getDefaultPosChessPieceName(xIndex, yIndex, isLowerPlayerWhite)
                var fileName = generateIndexSuffixedName(fileNameSuffix, destFolder.absolutePath)

                val outputFile = File("${destFolder.absolutePath}/${fileName}.${IMAGE_EXTENSION}")

                ImageIO.write(chessPieceImage, IMAGE_EXTENSION, outputFile)
            }
        }
    }

    fun generateChessBordImage(captureFolderPath: String, boundsRect: Rectangle) {

        val sourceFile = File("${captureFolderPath}/${SCREEN_SHOT_NAME}.${IMAGE_EXTENSION}")
        val outputFile = File("${captureFolderPath}/${CHESS_BOARD_IMAGE_NAME}.${IMAGE_EXTENSION}")

        val sourceImage = ImageIO.read(sourceFile)
        val destImage = sourceImage.getSubimage(boundsRect.x, boundsRect.y, boundsRect.width, boundsRect.height)

        if (outputFile.exists()) {
            outputFile.delete()
        }

        ImageIO.write(destImage, IMAGE_EXTENSION, outputFile)
    }


    fun getChessBoardBoundsRect(uiCaptureInfo: DeviceUICaptureInfo): Rectangle {

        require(uiCaptureInfo.isSuccess) { "Cannot read from failed result" }

        val uiHierarchyFile = File("${uiCaptureInfo.folderPath}/${UI_DUMP_FULL_NAME}")

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc: Document = dBuilder.parse(uiHierarchyFile)
        doc.documentElement.normalize()

        getChessBoardBoundsRect(doc.firstChild).let {

            if (it == null) {
                throw Exception("Cannot find matching bounds")
            } else {
                return it
            }
        }
    }

    private fun getChessBoardBoundsRect(node: Node): Rectangle? {

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

            val dataArray = boundsString.split("[", "]", ",").filter { !it.isNullOrBlank() }

            val startX = dataArray[0].toInt()
            val startY = dataArray[1].toInt()

            val width = dataArray[2].toInt() - startX
            val height = dataArray[3].toInt() - startY

            return Rectangle(startX, startY, width, height)

        } else {

            val childNodes = node.childNodes
            var childIndex = 0

            while (childIndex < childNodes.length) {

                val childResult = getChessBoardBoundsRect(childNodes.item(childIndex))

                if (childResult != null) {
                    return childResult
                }

                childIndex++
            }
        }

        return null
    }

    fun dumpDeviceUI(destFolderPath: String): DeviceUICaptureInfo {

        val time = Date().time
        val grandParentFolder = File(destFolderPath)
        if (!grandParentFolder.exists() || !grandParentFolder.isDirectory) {
            throw IllegalArgumentException("$destFolderPath folder doesn't exists")
        }

        val targetFolder = "${grandParentFolder.absolutePath}/UI_DUMP_${time}"

        val parentFolder = File(targetFolder)

        if (parentFolder.exists()) {
            parentFolder.delete()
        }

        parentFolder.mkdir()

        val isSuccess =
            dumpUIHierarchy(parentFolder.absolutePath) and takeDeviceScreenShot(parentFolder.absolutePath)

        return if (isSuccess) {
            DeviceUICaptureInfo(true, parentFolder.absolutePath)
        } else {
            parentFolder.delete()
            DeviceUICaptureInfo(false)
        }
    }

    fun dumpUIHierarchy(destFolderPath: String): Boolean {

        val uiDumpCommand = "${PLATFORM_TOOLS_FOLDER}/adb shell uiautomator dump"
        val uiDumpPullCommand =
            "${PLATFORM_TOOLS_FOLDER}/adb pull /sdcard/window_dump.xml ${destFolderPath}/${UI_DUMP_FULL_NAME}"


        val uiDumpRetVal =
            Runtime.getRuntime().exec(uiDumpCommand).waitFor()

        if (uiDumpRetVal != 0)
            return false

        val dumpPullRetVal = Runtime.getRuntime().exec(uiDumpPullCommand).waitFor()

        return dumpPullRetVal == 0
    }


    fun takeDeviceScreenShot(destFolderPath: String): Boolean {

        val screenShotCommand =
            "${PLATFORM_TOOLS_FOLDER}/adb shell screencap -p /sdcard/${SCREEN_SHOT_NAME}.${IMAGE_EXTENSION}"
        val screenShotPullCommand =
            "${PLATFORM_TOOLS_FOLDER}/adb pull /sdcard/${SCREEN_SHOT_NAME}.${IMAGE_EXTENSION} ${destFolderPath}/${SCREEN_SHOT_NAME}.${IMAGE_EXTENSION}"


        val screenShotRetVal =
            Runtime.getRuntime().exec(screenShotCommand).waitFor()

        if (screenShotRetVal != 0)
            return false

        val screenShotPullRetVal = Runtime.getRuntime().exec(screenShotPullCommand).waitFor()

        return screenShotPullRetVal == 0
    }

    fun isCurrentPlayWhite(
        captureFolderPath: String,
        boundsRect: Rectangle
    ): Boolean { //TODO ajitesh contains error

        val height = boundsRect.height

        val cellHeight = height / 8

        val inspectY = height - (cellHeight / 2)
        val inspectX = 10

        val chessBoardImageFile = File("${captureFolderPath}/${CHESS_BOARD_IMAGE_NAME}.${IMAGE_EXTENSION}")
        val image = ImageIO.read(chessBoardImageFile)

        val color = Color(image.getRGB(inspectX, inspectY))

        return color.red < 127 || color.green < 127 || color.blue < 127
    }


    private fun generateIndexSuffixedName(prefix: String, folderPath: String): String {

        val folder = File(folderPath)

        val maxSuffix = folder.listFiles { it -> it.nameWithoutExtension.startsWith(prefix) }
            .map { it.nameWithoutExtension.substring(prefix.length + 1) }
            .map { it.toInt() }
            .max() ?: 0

        return "${prefix}_${maxSuffix + 1}"
    }

    fun getDefaultPosChessPieceName(x: Int, y: Int, isLowerPlayerWhite: Boolean): String {

        if (x < 0 || x > 7 || y in 2..5 || y < 0 || y > 7) {
            throw java.lang.IllegalArgumentException("Invalid position in unmodified chessboard")
        }

        var suffix = if (isLowerPlayerWhite) {

            if (y <= 1) {
                "black"
            } else {
                "white"
            }
        } else {

            if (y <= 1) {
                "white"
            } else {
                "black"
            }
        }

        var piece = if (y <= 1) {

            when (y) {

                1 -> "pawn"

                else -> {

                    when (x) {

                        0, 7 -> "rook"
                        1, 6 -> "knight"
                        2, 5 -> "bishop"
                        3 -> "queen"
                        else -> "king"
                    }
                }
            }

        } else {

            when (y) {

                6 -> "pawn"

                else -> {

                    when (x) {

                        0, 7 -> "rook"
                        1, 6 -> "knight"
                        2, 5 -> "bishop"
                        3 -> "queen"
                        else -> "king"
                    }
                }
            }
        }

        return "${piece}_${suffix}"
    }
}
