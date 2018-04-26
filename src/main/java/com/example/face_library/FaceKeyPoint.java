package com.example.face_library;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.Output;
import org.tensorflow.Session;

import java.util.logging.Level;
import java.util.logging.Logger;


/** Sample use of the TensorFlow Java API to label images using a pre-trained model. */
public class FaceKeyPoint extends FeatureExtractorBase {

    private final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    private final int FONT_SIZE = 5;
    private final int THICKNESS = 3;

    private Output output_normalize;
    private Session sess_normalize;

    private Session sess_inference;
    static String strClassName = FaceKeyPoint.class.getName();
    static Logger log = Logger.getLogger(strClassName);

    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public FaceKeyPoint() {
        log.setLevel(Level.OFF);
        log.info("All log is turned off.");

        int inputSize = 48;
        int channels = 3;
        float imageMean = (float)127.5;
        float imageStd = (float)128.0;
        String inputLayer = "onet/input";
        String outputLayer = "onet/conv6-3/conv6-3";

        String modelFile = "model/onet.pb";
        init(inputSize, channels, imageMean, imageStd, inputLayer, outputLayer, modelFile);
    }

//    public static void main(String[] args) {
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//
//        String imageFile = "data/im_96x96.jpg";
//        Mat image = Imgcodecs.imread(imageFile);
//
//        FaceKeyPoint faceKeyPoint = new FaceKeyPoint();
//        float[] points = faceKeyPoint.getPoints(image);
//
//        faceKeyPoint.drawPointsOnImage(image, points);
//        String filename = "data/faceKeyPoint.png";
//        System.out.println(String.format("Writing %s", filename));
//        Imgcodecs.imwrite(filename, image);
//    }

    public float[] getPoints(Mat img, Rect face){
        float[] points;
        Mat faceImg = img.submat(face);
        points = getPoints(faceImg);
        for(int i=0; i<5; i++) {
            points[i] += face.x;
            points[i+5] += face.y;
        }
        faceImg.release();
        return points;
    }

    public float[] getPoints(Mat image) {
        int w = image.cols();
        int h = image.rows();
        float[] points;
        points = getFeatures(image);
        for(int i=0; i<points.length/2; i++){
            points[i] *= w;
            points[i+5] *= h;
        }
        String s = "";
        for(int i=0; i<points.length/2; i++){
            s += " (" + points[i] + ',' + points[i+5] + ')';
        }
        log.info(s);
        return points;
    }

    public void drawRectangleAndLabelOnImage(Mat img, Rect face, String label, boolean front_camera){
        Imgproc.rectangle(img, face.tl(), face.br(), FACE_RECT_COLOR, THICKNESS);
        Point tl = face.tl();
        Imgproc.putText(img, label, tl, Core.FONT_HERSHEY_PLAIN, FONT_SIZE, FACE_RECT_COLOR, THICKNESS);
    }

    public void drawPointsOnImage(Mat img, float[] points){
        for(int i=0; i<points.length/2; i++) {
            int x = (int) points[i];
            int y = (int) points[i+5];
            Imgproc.circle(img, new Point(x, y), 2, FACE_RECT_COLOR);
        }
    }

    private int maxIndex(float[] probabilities) {
        int best = 0;
        for (int i = 1; i < probabilities.length; ++i) {
            if (probabilities[i] > probabilities[best]) {
                best = i;
            }
        }
        return best;
    }
}
