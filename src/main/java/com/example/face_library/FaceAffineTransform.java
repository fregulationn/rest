package com.example.face_library;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FaceAffineTransform {
    private static final String TAG = "FaceAlignment";
    private static final double[][] coord_point = {{0.31,  0.44}, {0.69,  0.44}, {0.50,  0.76}};
    private int outputImageSize;
    private MatOfPoint2f dstPoints;
    static String strClassName = FaceAffineTransform.class.getName();
    static Logger log = Logger.getLogger(strClassName);

    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public FaceAffineTransform(int outImageSize) {
        log.setLevel(Level.OFF);
        log.info("All log is turned off.");
        outputImageSize = outImageSize;
        Point[] dstTri = new Point[3];
        for (int j=0; j<3; j++){
            dstTri[j] = new Point(outputImageSize*coord_point[j][0], outputImageSize*coord_point[j][1]);
        }
        dstPoints = new MatOfPoint2f(dstTri);
    }

    public Mat processImage(Mat img, float[] points) {
        if (points == null){
            return null;
        }

        Mat warp_dst;
        Point [] srcTri = new Point[3];
        MatOfPoint2f srcPoints;
        Mat warpMat;

        // three point: two centers of eyes and the center of mouth
        srcTri[0] = new Point(points[0], points[5]);
        srcTri[1] = new Point(points[1], points[6]);
        srcTri[2] = new Point((points[3]+points[4])/2, (points[8]+points[9])/2);

        String s = "";
        for(int j=0; j<3; j++){
            s += "(" + srcTri[j].x + ',' + srcTri[j].y + ") ";
        }
        log.info(s);

        srcPoints = new MatOfPoint2f(srcTri);

        warpMat = Imgproc.getAffineTransform(srcPoints, dstPoints);

        warp_dst = Mat.zeros(outputImageSize, outputImageSize, img.type());
        Imgproc.warpAffine(img, warp_dst, warpMat, warp_dst.size());
        log.info(s);
        log.info("src " + img.cols() + 'x' + img.rows() + "->" + warp_dst.cols() + 'x' + warp_dst.rows());
        warpMat.release();

        return warp_dst;
    }

//    public static void main(String[] args) {
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//
//        String imageFile = "D:\\Z\\2.png";
//        Mat image = Imgcodecs.imread(imageFile);
//
////        FaceDetection faceDetection =new FaceDetection();
////        Rect[] rects = faceDetection.getFaces(image, 1);
////
////        if(rects.length > 0 && rects[0] != null) {
//        FaceKeyPoint faceKeyPoint = new FaceKeyPoint();
//        float[] points = faceKeyPoint.getPoints(image);
//
//        FaceAffineTransform faceAffineTransform = new FaceAffineTransform(96);
//        Mat imageAligned = faceAffineTransform.processImage(image, points);
//
//        String filename = "model/faceAffineTransform.png";
//        System.out.println(String.format("Writing %s", filename));
//        Imgcodecs.imwrite(filename, imageAligned);
//    }
}
