package com.example;


import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class HelloTF {

    private static int BATCH_SIZE = 2;
    private final static int CHANNELS = 3;
    private static int img_size = 160, emb_size = 128;
    private static Graph g;
    private static Session s;

    HelloTF() {

        long startTime = System.currentTimeMillis();

        String modelfile = "D:\\Z\\code\\video_detect\\model\\20170512-110547\\20170512-110547.pb";
        System.out.println(modelfile);
        byte[] graphDef = readAllBytesOrExit(Paths.get(modelfile));
        g = new Graph();
        g.importGraphDef(graphDef);
        s = new Session(g);

        long endTime = System.currentTimeMillis();
        float seconds = (endTime - startTime) / 1000F;
        System.out.println("load model:" + Float.toString(seconds) + " seconds.");
    }


    private static float[][][] getData(String path) {
        try {
            BufferedImage bimg = ImageIO.read(new File(path));
            int[][][] data = new int[img_size][img_size][3];


            float mean = 0, std, std_adj;
            int num = img_size * img_size * 3, square_sum = 0;
            float[][][] prewhite = new float[img_size][img_size][3];


            //通过getRGB()方式获得像素矩阵
            //此方式为沿Height方向扫描
            for (int i = 0; i < img_size; i++) {
                for (int j = 0; j < img_size; j++) {
                    int rgb = bimg.getRGB(i, j);

                    data[j][i][0] = (rgb & 0xff0000) >> 16;
                    data[j][i][1] = (rgb & 0xff00) >> 8;
                    data[j][i][2] = (rgb & 0xff);

                    mean = mean + data[j][i][0] + data[j][i][1] + data[j][i][2];
                    square_sum = square_sum + data[j][i][0] * data[j][i][0] +
                            data[j][i][1] * data[j][i][1] + data[j][i][2] * data[j][i][2];

//                    //输出一列数据比对
//                    if (i == 156 && j == 0)
//                        System.out.printf("%d\t%d\t%d\t", data[i][j][0], data[i][j][1], data[i][j][2]);
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
                        prewhite[i][j][k] = (data[i][j][k] - mean) / std_adj;
                    }
                }
            }

            return prewhite;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static double executeInceptionGraph(String path1, String path2) {
        BATCH_SIZE = 2;

        float[][][][] prewhite = new float[BATCH_SIZE][img_size][img_size][CHANNELS];

        long time = System.currentTimeMillis();
        prewhite[0] = getData(path1);
        prewhite[1] = getData(path2);


        float[] temp = new float[BATCH_SIZE * img_size * img_size * CHANNELS];
        for (int b = 0; b < BATCH_SIZE; b++) {
            for (int i = 0; i < img_size; i++) {
                for (int j = 0; j < img_size; j++) {
                    for (int k = 0; k < CHANNELS; k++) {
                        int index = b * img_size * img_size * CHANNELS + i * img_size * CHANNELS + j * CHANNELS + k;
                        temp[index] = prewhite[b][i][j][k];
                    }
                }
            }
        }


        long[] shape = new long[]{BATCH_SIZE, img_size, img_size, CHANNELS};
        Tensor<Float> imageTensor = Tensor.create(shape, FloatBuffer.wrap(temp));

        long time1 = System.currentTimeMillis();
        float seconds_pic = (time1 - time) / 1000F;
        System.out.println("creat tensor:" + Float.toString(seconds_pic) + " seconds.");


        Tensor<Boolean> phase_train = Tensor.create(false, Boolean.class);


        // Generally, there may be multiple output tensors, all of them must be closed to prevent resource leaks.
        Tensor<Float> result =
                s.runner().feed("input:0", imageTensor).feed("phase_train:0", phase_train).
                        fetch("embeddings:0").run().get(0).expect(Float.class);




        float[][] output = result.copyTo(new float[BATCH_SIZE][emb_size]);

        double distance = 0;
        for (int m = 0; m < emb_size; m++) {
            distance += (output[0][m] - output[1][m]) * (output[0][m] - output[1][m]);
        }

        distance = Math.sqrt(distance);


        long time2 = System.currentTimeMillis();
        float seconds_session = (time2 - time1) / 1000F;
        System.out.println("embedding:" + Float.toString(seconds_session) + " seconds.");

        return distance;

    }

    private static byte[] readAllBytesOrExit(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            System.err.println("Failed to read [" + path + "]: " + e.getMessage());
            System.exit(1);
        }
        return null;
    }

    public static float[] executeInceptionGraph1(String path1) {
        BATCH_SIZE = 1;
        float[][][][] prewhite = new float[BATCH_SIZE][img_size][img_size][CHANNELS];
        prewhite[0] = getData(path1);


        float[] temp = new float[BATCH_SIZE * img_size * img_size * CHANNELS];

        for (int i = 0; i < img_size; i++) {
            for (int j = 0; j < img_size; j++) {
                for (int k = 0; k < CHANNELS; k++) {
                    int index = i * img_size * CHANNELS + j * CHANNELS + k;
                    temp[index] = prewhite[0][i][j][k];
                }
            }
        }

        long[] shape = new long[]{BATCH_SIZE, img_size, img_size, CHANNELS};
        Tensor<Float> imageTensor = Tensor.create(shape, FloatBuffer.wrap(temp));

        Tensor<Boolean> phase_train = Tensor.create(false, Boolean.class);


        // Generally, there may be multiple output tensors, all of them must be closed to prevent resource leaks.
        Tensor<Float> result =
                s.runner().feed("input:0", imageTensor).feed("phase_train:0", phase_train).
                        fetch("embeddings:0").run().get(0).expect(Float.class);

        float[][] output = result.copyTo(new float[BATCH_SIZE][emb_size]);
        return output[0];
    }


//    public static void main(String[] args) throws Exception {
//
//        long startTime = System.currentTimeMillis();
//        String img1 = "D:\\Z\\2.png";
//        String img2 = "D:\\Z\\3.png";
//
//        double result = executeInceptionGraph(img1,img2);
//
//        long endTime = System.currentTimeMillis();
//        float seconds = (endTime - startTime) / 1000F;
//        System.out.println("all:"+Float.toString(seconds) + " seconds.");
//
//        System.out.println(result);
//
////        try (Graph g = new Graph()) {
////            final String value = "Hello from " + TensorFlow.version();
////            // 使用一个简单操作、一个名为 "MyConst" 的常数和一个值 "value" 来构建计算图。
////            try (Tensor t = Tensor.create(value.getBytes("UTF-8"))) {
////                // Java API 目前还不包含足够方便的函数来执行“加”操作。
////                g.opBuilder("Const", "MyConst").setAttr("dtype", t.dataType()).setAttr("value", t).build();
////            }
////
////            // 在一个 Session 中执行 "MyConst" 操作。
////            try (Session s = new Session(g);
////                 Tensor output = s.runner().fetch("MyConst").run().get(0)) {
////                System.out.println(new String(output.bytesValue(), "UTF-8"));
////            }
////        }
//    }


}