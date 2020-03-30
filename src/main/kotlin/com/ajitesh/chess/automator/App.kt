package com.ajitesh.chess.automator

import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc


fun main() {

   /* val uiCaptureInfo = ChessMoveDetector.dumpDeviceUI("/Users/ajitesh/Desktop/")
    val boundsRect = ChessMoveDetector.getChessBoardBoundsRect(uiCaptureInfo)
    ChessMoveDetector.generateChessBordImage(uiCaptureInfo.folderPath!!, boundsRect)*/


    /*val margin = Margin(19, 12, 19, 12)
    ChessMoveDetector.generateChessPieces("/Users/ajitesh/Desktop/Working/chess_board.png", "/Users/ajitesh/Desktop/ChessPieces/", margin, true)*/


    /*val prevTime = Date().time
    ChessMoveDetector.printBoard("/Users/ajitesh/Desktop/Working/")
    val currTime = Date().time

    val diff = (currTime - prevTime)/1000

    println("Time taken = $diff secs")*/

//    val source = "/Users/ajitesh/Desktop/ChessPieces2/rook_black_1.png"
//    val template = "/Users/ajitesh/Desktop/ChessPieces2/rook_black_2.png"
//
//    val sourceImg = ImageIO.read(File(source))
//    val templateImg = ImageIO.read(File(template))
//
//    println(ChessMoveDetector.doesContainsTemplate(sourceImg, templateImg, 0.9f, 15))

    val d = ChessMoveDetector.PLATFORM_TOOLS_FOLDER
    findContours()
}

fun findContours() {

    val chessBoardImg = Imgcodecs.imread("/Users/ajitesh/Desktop/Working/chess_board.png", Imgcodecs.IMREAD_GRAYSCALE)

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

    Imgcodecs.imwrite("/Users/ajitesh/Desktop/Working/threshold.png", threshold)
    threshold.release()
}