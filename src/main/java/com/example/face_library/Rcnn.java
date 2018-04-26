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
    private final static int BATCH_SIZE = 1;
    private final static int CHANNELS = 3;
    private static Graph g;
    private static Session s;
    private static byte[][][] image ;

    public Rcnn() {
        long startTime = System.currentTimeMillis();
        String modelfile = System.getProperty("user.dir") + "/model/"+"frozen_inference_graph_mobilenet_300.pb";
        System.out.println(modelfile);
        byte[] graphDef = readAllBytesOrExit(Paths.get(modelfile));
        g = new Graph();
        g.importGraphDef(graphDef);
        s = new Session(g);

        long endTime = System.currentTimeMillis();
        float seconds = (endTime - startTime) / 1000F;
        System.out.println("load model:" + Float.toString(seconds) + " seconds.");
    }


    private static Tensor<UInt8> makeImageTensor(byte[] img) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(img);    //将b作为输入流；

            BufferedImage bimg = ImageIO.read(in);

            int height = bimg.getHeight();
            int width = bimg.getWidth();

            image = new byte[height][width][3];

            //通过getRGB()方式获得像素矩阵
            //此方式为沿Height方向扫描,也就是横向所以i为width
            byte[] temp = new byte[BATCH_SIZE * height * width * CHANNELS];

            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int rgb = bimg.getRGB(j, i);
                    temp[i * width * CHANNELS + j * CHANNELS] = (byte) ((rgb & 0xff0000) >> 16);
                    temp[i * width * CHANNELS + j * CHANNELS + 1] = (byte) ((rgb & 0xff00) >> 8);
                    temp[i * width * CHANNELS + j * CHANNELS + 2] = (byte) ((rgb & 0xff));

                    image[i][j][0] = (byte) ((rgb & 0xff0000) >> 16);
                    image[i][j][1] = (byte) ((rgb & 0xff00) >> 8);
                    image[i][j][2] = (byte) ((rgb & 0xff));
                }
            }

            long[] shape = new long[]{BATCH_SIZE, height, width, CHANNELS};
            Tensor<UInt8> imageTensor = Tensor.create(UInt8.class, shape, ByteBuffer.wrap(temp));
            bimg.flush();
            return imageTensor;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List executeGraph(byte[] img) {
        long time = System.currentTimeMillis();
        Tensor<UInt8> imageTensor = makeImageTensor(img);

        List<Tensor<?>> outputs = null;

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

        long time1 = System.currentTimeMillis();
        float seconds_pic = (time1 - time) / 1000F;
        System.out.println("creat tensor:" + Float.toString(seconds_pic) + " seconds.");

        List<Object> res = new ArrayList<>();

        long[] tmp_shape = outputs.get(0).shape();
        float[][][] box = outputs.get(0).copyTo(new float[(int) tmp_shape[0]][(int) tmp_shape[1]][(int) tmp_shape[2]]);
        float[][] real_box = box_trans(box);
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
        res.add(image);
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
    private static float[][] box_trans(float[][][] box) {
        float[][] res = new float[box[0].length][box[0][0].length];
        for (int i = 0; i < box[0].length; i++) {
            res[i][0] = image.length*box[0][i][0];
            res[i][1] =image[0].length*box[0][i][1] ;
            res[i][2] =image.length*box[0][i][2]-image.length*box[0][i][0] ;
            res[i][3] = image[0].length*box[0][i][3]-image[0].length*box[0][i][1];
        }
        return res;
    }
}
