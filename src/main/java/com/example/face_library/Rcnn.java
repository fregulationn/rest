package com.example.face_library;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.types.UInt8;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Rcnn {

    private final static int CHANNELS = 3;
    private static Graph g;
    private static Session s;


    public Rcnn() {
        long startTime = System.currentTimeMillis();
        String modelfile = System.getProperty("user.dir") + "/model/" + "frozen_inference_graph_face.pb";
        System.out.println(modelfile);
        byte[] graphDef = readAllBytesOrExit(Paths.get(modelfile));
        g = new Graph();
        g.importGraphDef(graphDef);
        s = new Session(g);

        long endTime = System.currentTimeMillis();
        float seconds = (endTime - startTime) / 1000F;
        System.out.println("load model:" + Float.toString(seconds) + " seconds.");
    }


    /** Extracting picture in the form of arrays */
    private static byte[][][][] readImage(byte[][] imgs) {
        try {
            byte[][][][] images = new byte[imgs.length][][][];

//            System.out.println("batch local 2");
//            System.out.println(imgs.length);

            for (int w = 0; w < imgs.length; w++) {
                ByteArrayInputStream in = new ByteArrayInputStream(imgs[w]);    //将b作为输入流；
                BufferedImage bimg = ImageIO.read(in);

                int height = bimg.getHeight();
                int width = bimg.getWidth();
                images[w] = new byte[height][width][CHANNELS];

                //通过getRGB()方式获得像素矩阵
                //此方式为沿Height方向扫描,也就是横向所以i为width
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        int rgb = bimg.getRGB(j, i);
                        images[w][i][j][0] = (byte) ((rgb & 0xff0000) >> 16);
                        images[w][i][j][1] = (byte) ((rgb & 0xff00) >> 8);
                        images[w][i][j][2] = (byte) ((rgb & 0xff));
                    }
                }
                bimg.flush();
            }
            return images;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Convert the array to tensor */
    private static Tensor<UInt8> makeImageTensor(byte[][][][] imgs) {
        int BATCH_SIZE = imgs.length;
        int height = imgs[0].length;
        int width = imgs[0][0].length;

        byte[] temp = new byte[BATCH_SIZE * height * width * CHANNELS];

//        long time = System.currentTimeMillis();
//
//

        for (int w = 0; w < imgs.length; w++) {
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int index = w * width * height * CHANNELS + i * width * CHANNELS + j * CHANNELS;
                    temp[index] = imgs[w][i][j][0];
                    temp[index + 1] = imgs[w][i][j][1];
                    temp[index + 2] = imgs[w][i][j][2];
                }
            }

        }

//        long time11 = System.currentTimeMillis();
//        float seconds_pic1 = (time11 - time) / 1000F;
//        System.out.println("for loop:" + Float.toString(seconds_pic1) + " seconds.");

        long[] shape = new long[]{BATCH_SIZE, height, width, CHANNELS};
        return Tensor.create(UInt8.class, shape, ByteBuffer.wrap(temp));

    }

    /** detect face using a pre-trained model */
    public static List<Object> executeGraph(byte[][] imgs) {
        long time = System.currentTimeMillis();

        byte[][][][] images = readImage(imgs);

        long time11 = System.currentTimeMillis();
        float seconds_pic1 = (time11 - time) / 1000F;
        System.out.println("read images:" + Float.toString(seconds_pic1) + " seconds.");

        Tensor<UInt8> imageTensor = makeImageTensor(images);

        List<Tensor<?>> outputs = null;

        long time1 = System.currentTimeMillis();
        float seconds_pic = (time1 - time11) / 1000F;
        System.out.println("make tesor:" + Float.toString(seconds_pic) + " seconds.");

        try {
            outputs = s.runner()
                    .feed("image_tensor:0", imageTensor)
                    .fetch("detection_boxes:0")
                    .fetch("detection_scores:0")
                    .fetch("detection_classes:0")
                    .fetch("num_detections:0")
                    .run();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        long time2 = System.currentTimeMillis();
        float third = (time2 - time1) / 1000F;
        System.out.println("creat tensor:" + Float.toString(third) + " seconds.");


        List<Object> res = new ArrayList<>();

        long[] tmp_shape = outputs.get(0).shape();
        float[][][] box = outputs.get(0).copyTo(new float[(int) tmp_shape[0]][(int) tmp_shape[1]][(int) tmp_shape[2]]);
        float[][][] real_box = box_trans(box, images[0].length, images[0][0].length);
        res.add(real_box);
        tmp_shape = outputs.get(1).shape();
        float[][] scores = outputs.get(1).copyTo(new float[(int) tmp_shape[0]][(int) tmp_shape[1]]);
        res.add(scores);
        tmp_shape = outputs.get(2).shape();
        float[][] classes = outputs.get(2).copyTo(new float[(int) tmp_shape[0]][(int) tmp_shape[1]]);
        res.add(classes);
        tmp_shape = outputs.get(3).shape();
        float[] num_detections = outputs.get(3).copyTo(new float[(int) tmp_shape[0]]);
        res.add(num_detections);
        res.add(images);

        return res;
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


//    public static int byteToInt(byte b) {
//        //Java 总是把 byte 当做有符处理；我们可以通过将其和 0xFF 进行二进制与得到它的无符值
//        return b & 0xFF;
//    }


    /**
     * 0:top
     * 1:left
     * 2:height
     * 3:width
     **/
    private static float[][][] box_trans(float[][][] box, int height, int width) {

        float[][][] res = new float[box.length][box[0].length][box[0][0].length];

        for (int w = 0; w < box.length; w++) {
            for (int i = 0; i < box[0].length; i++) {
                res[w][i][0] = height * box[w][i][0];
                res[w][i][1] = width * box[w][i][1];
                res[w][i][2] = height * box[w][i][2] - height * box[w][i][0];
                res[w][i][3] = width * box[w][i][3] - width * box[w][i][1];
            }
        }
        return res;
    }
}
