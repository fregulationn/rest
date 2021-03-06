package com.example;

import com.example.data.device.FaceRepository;
import com.example.data.device.FaceService;
import com.example.data.domain.Face;
import org.apache.commons.io.FileUtils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tensorflow.Graph;
import org.tensorflow.Session;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.example.face_library.*;
import com.example.face_library.FaceNet;


public class MultiIdentify {
    private final static int CHANNELS = 3;
    private static int img_size = 160;
    private static int margin = 4;

//    private static Graph g;
//    private static Session s;
//    private static List<Face> all_face;
//    private static FaceRepository faceRepository;
//    MultiIdentify() {
////        pre_resample();
//        all_face = faceRepository.findAll();
//        System.out.println(all_face.size());
//    }

    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }


    /**
     * handle the result of detection ,then aligned,then embedding ,return the feature which have already been Pre-whiten
     **/
    public static float[][] handleResult(List<Object> res_all, int[] detect_top,int img_num) {
        int batch_size = 0;
        for (int i = 0; i < img_num; i++) {
            batch_size += detect_top[i];
        }
        System.out.println(batch_size);

        float[][][][] prewhite = new float[batch_size][img_size][img_size][CHANNELS];

        int index = 0;

        float[][][] box_all = (float[][][]) res_all.get(0);
        byte[][][][] image_all = (byte[][][][]) res_all.get(4);

        for (int i = 0; i < img_num; i++) {
            float[][] box = box_all[i];
            byte[][][] image = image_all[i];
            for (int j = 0; j < detect_top[i]; j++) {
                int top = (int) Math.floor((double) box[j][0]);
                int left = (int) Math.floor((double) box[j][1]);
                int height = (int) Math.ceil((double) box[j][2]);
                int width = (int) Math.ceil((double) box[j][3]);
                byte[][][] face = new byte[height + 2 * margin][width + 2 * margin][CHANNELS];

                for (int k = 0; (k < height + margin) && (top + k < image.length); k++) {
                    for (int l = 0; (l < width + margin) && (left + l < image[0].length); l++) {
                        int tmp_k = top + k - margin > 0 ? top + k - margin : 0;
                        int tmp_l = left + l - margin > 0 ? left + l - margin : 0;
                        face[k][l] = image[tmp_k][tmp_l];
                    }
                }

                Mat img_mat = new Mat(face.length, face[0].length, CvType.CV_8UC(3));
                byte[] temp = convert(face);
                img_mat.put(0, 0, temp);

//                long startTime = System.currentTimeMillis();

                FaceKeyPoint faceKeyPoint = new FaceKeyPoint();
                float[] points = faceKeyPoint.getPoints(img_mat);

                FaceAffineTransform faceAffineTransform = new FaceAffineTransform(img_size);
                Mat imageAligned = faceAffineTransform.processImage(img_mat, points);

//                String filename1 = "img/face" + Integer.toString(i * 2 + j) + ".png";
//                System.out.println(String.format("Writing %s", filename1));
//                Imgcodecs.imwrite(filename1, img_mat);
//
//                String filename = "img/faceAffineTransform" + Integer.toString(i * 2 + j) + ".png";
//                System.out.println(String.format("Writing %s", filename));
//                Imgcodecs.imwrite(filename, imageAligned);

                prewhite[index] = prewhite(imageAligned);
                ++ index;

//                long endTime = System.currentTimeMillis();
//                float seconds = (endTime - startTime) / 1000F;
//                System.out.println("align:" + Float.toString(seconds) + " seconds.");
            }
        }

        float[][] res = FaceNet.executeInceptionGraphPrewhite(prewhite);
        System.out.println("OK");
        return res;
    }

    /**
     * transform mat to float array and pre-whiten the array.
     */
    private static float[][][] prewhite(Mat imageAligned) {

        byte[] temp_res = new byte[img_size * img_size * 3];
        imageAligned.get(0, 0, temp_res);

        float mean = 0, std, std_adj, square_sum = 0f;
        int num = img_size * img_size * 3;
        float[][][] res = new float[img_size][img_size][3];
        float[][][] data = new float[img_size][img_size][3];

        for (int i = 0; i < img_size; i++) {
            for (int j = 0; j < img_size; j++) {
                for (int k = 0; k < 3; k++) {
                    int index = i * j * 3 + j * 3 + k;
                    data[i][j][k] = (float) (temp_res[index] & 0x00ff);
                }

                mean = mean + data[i][j][0] + data[i][j][1] + data[i][j][2];
                square_sum = square_sum + data[i][j][0] * data[i][j][0] +
                        data[i][j][1] * data[i][j][1] + data[i][j][2] * data[i][j][2];
            }
        }
        mean = mean / num;
        std = (square_sum - num * mean * mean) / num;

        std = (float) Math.sqrt(Math.abs(std));
        float sqrt_num = (float) Math.sqrt(num);
        std_adj = std > (1 / sqrt_num) ? std : (1 / sqrt_num);

        for (int i = 0; i < img_size; i++) {
            for (int j = 0; j < img_size; j++) {
                for (int k = 0; k < 3; k++) {
                    res[i][j][k] = (data[i][j][k] - mean) / std_adj;
                }
            }
        }
        return res;
    }


    /**
     * Convert the 3-dimensional array to one dimension
     */
    private static byte[] convert(byte[][][] img) {

        byte[] temp = new byte[img.length * img[0].length * 3];
        for (int i = 0; i < img.length; i++) {
            for (int j = 0; j < img[0].length; j++) {
                for (int k = 0; k < 3; k++) {
                    int index = i * img[0].length * 3 + j * 3 + k;
                    temp[index] = (img[i][j][k]);
                }
            }
        }
        return temp;
    }


    /**
     * Calculate the scores of feature arrays(float) and face's feature.
     */
    public static float get_score(float[] face_feature, Face f) {
        float res = 0;

        float[] feature = new float[128];
        try {
            byte[] asBytes = f.getFeature();
            ByteArrayInputStream bin = new ByteArrayInputStream(asBytes);
            DataInputStream din = new DataInputStream(bin);
            for (int i = 0; i < feature.length; i++) {
                feature[i] = din.readFloat();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

//        System.out.println(face_feature.length);

//        for (int i = 0; i < face_feature.length; i++) {
//            res += feature[i] * face_feature[i];
//        }
        for (int i = 0; i < face_feature.length; i++) {
            res += (feature[i] -face_feature[i])* (feature[i] -face_feature[i]);
        }
//        System.out.println(res);
//        System.out.println(f.getId());

//        res = (float) Math.sin((double) res);

        return res;
    }


    /**
     * Comparing the face list with the individual feature, the face is sorted according to the score(above)
     * return the number of faces limited .
     */
    public static List<Face> multi_Identify(List<Face> all_face,float[]face_feature, int user_top_num) {
        List<Face> res = new ArrayList<>();

//        List<Face> tmp = all_face.
        Comparator<Face> comparator = new Comparator<Face>() {
            public int compare(Face f1, Face f2) {
                float score1 = get_score(face_feature,f1);
                float score2 = get_score(face_feature,f2);

                if (score1 < score2) {
                    return -1;
                } else if (score1 > score2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };

        Collections.sort(all_face,comparator);

        for (int i = 0; i < user_top_num; i++) {
            res.add(all_face.get(i));
        }
        return res;
    }


    /**
     * Comparing the face list with the multiple feature, the face is sorted according to the score
     * return the number of faces limited .
     */
    public static List<Face> multi_Identify(List<Face> all_face,float[][] face_feature, int user_top_num) {
        List<Face> res = new ArrayList<>();
        for (int i = 0; i < face_feature.length; i++) {
            List<Face> tmp = multi_Identify(all_face,face_feature[i], user_top_num);
            for (Face item : tmp) res.add(new Face(item));
        }
        return res;
    }




//    private static void pre_resample() {
//        try {
//
//            byte[][][] image = new byte[img_size][img_size][3];
//
//            //通过getRGB()方式获得像素矩阵
//            //此方式为沿Height方向扫描,也就是横向所以i为width
//            for (int i = 0; i < img_size; i++) {
//                for (int j = 0; j < img_size; j++) {
//                    image[i][j][0] = (byte) (4);
//                    image[i][j][1] = (byte) (4);
//                    image[i][j][2] = (byte) (4);
//                }
//            }
//            imresample(image, img_size, img_size);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }


//    public static float[][][] imresample(byte[][][] img, int hs, int ws) {
//
//
//        float[][][] res;
//        Mat img_mat = new Mat(img.length, img[0].length, CvType.CV_64FC(3));
//
//
//        double[] temp = convert(img);
//        img_mat.put(0, 0, temp);
//
//        Mat dst = new Mat(hs, ws, CvType.CV_64FC(3));
//        long startTime = System.currentTimeMillis();
//
//        Imgproc.resize(img_mat, dst, new Size(ws, hs), 0, 0, Imgproc.INTER_AREA);
//
//        long endTime = System.currentTimeMillis();
//        float seconds = (endTime - startTime) / 1000F;
//        System.out.println("resample time:" + Float.toString(seconds) + " seconds.");
//
//        double[] temp_res = new double[hs * ws * 3];
//        dst.get(0, 0, temp_res);
//        res = convert(temp_res, hs, ws);
//
//
//        return res;
//    }
//
//    public static float[][][] convert(double[] img, int h, int w) {
//        float[][][] res = new float[h][w][3];
//        for (int i = 0; i < h; i++) {
//            for (int j = 0; j < w; j++) {
//                for (int k = 0; k < 3; k++) {
//                    int index = i * w * 3 + j * 3 + k;
//                    res[i][j][k] = (float) (img[index]);
//                }
//            }
//        }
//        return res;
//    }
//
//    public static double[] convert(byte[][][] img) {
//
//        double[] temp = new double[img.length * img[0].length * 3];
//        for (int i = 0; i < img.length; i++) {
//            for (int j = 0; j < img[0].length; j++) {
//                for (int k = 0; k < 3; k++) {
//                    int index = i * img[0].length * 3 + j * 3 + k;
//                    temp[index] = getUint8(img[i][j][k]);
//                }
//            }
//        }
//        return temp;
//    }
//
//    public static double getUint8(byte s) {
//        return (double) (s & 0x00ff);
//    }

    //    public static void main(String[] args) throws Exception {
//        long startTime = System.currentTimeMillis();
//        Rcnn Rcnn = new Rcnn();
//        String filePath = "D:\\Z\\1.jpg";
//        File file = new File(filePath);
//        byte[] imgData = FileUtils.readFileToByteArray(file);
//
//        MultiIdentify test = new MultiIdentify();
//        FaceNet facenet = new FaceNet();
//
//        List<Object> res = Rcnn.executeGraph(imgData);
//        List<List<Object>> res_all = new ArrayList<>();
//        res_all.add(res);
//        res_all.add(res);
//
//        int detect_top_num = 2;
//        int user_top_num = 2;
//        float[][] face_feature =handleResult(res_all, detect_top_num);
//
//        List<Face> top_face = multi_Identify(face_feature,user_top_num);
//        System.out.println(top_face);
//
//
//
//        long endTime = System.currentTimeMillis();
//        float seconds = (endTime - startTime) / 1000F;
//        System.out.println("load model:" + Float.toString(seconds) + " seconds.");
//    }
}



