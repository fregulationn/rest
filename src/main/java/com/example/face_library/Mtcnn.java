package com.example.face_library;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.util.ArrayUtil;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * transform mtcnn python version to java ,but haven't completed.
 */

public class Mtcnn {

    private final static int BATCH_SIZE = 2;
    private final static int CHANNELS = 3;
    private static int img_size = 160;
    private static Graph g;
    private static Session s;

    Mtcnn() {
        String modelfile = System.getProperty("user.dir") + "/model/" + "mtcnn_graph.pb";
        System.out.println(modelfile);
        byte[] graphDef = readAllBytesOrExit(Paths.get(modelfile));
        g = new Graph();
        g.importGraphDef(graphDef);
        s = new Session(g);
    }

    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }


    //      """Detects faces in an image, and returns bounding boxes and points for them.
//    img: input image
//    minsize: minimum faces' size
//    pnet, rnet, onet: caffemodel
//    threshold: threshold=[th1, th2, th3], th1-3 are three steps's threshold
//    factor: the factor used to create a scaling pyramid of face sizes to Rcnn in the image.
//            """
    public static ArrayList<float[]> detect_face(byte[][][] img, int minsize, double[] threshold, double factor) {

        int factor_count = 0;
        ArrayList<float[]> total_boxes = new ArrayList<float[]>();
        INDArray total_boxes_nd = Nd4j.zeros(img.length, img[0].length);
//        points = np.empty(0)
        int h = img.length;
        int w = img[0].length;
        double minl = h < w ? h : w;
        double m = 12.0 / minsize;
        minl = minl * m;

        List<Double> scales = new ArrayList<Double>();
//    # create scale pyramid
        while (minl >= 12) {
            double temp = m * Math.pow(factor, factor_count);
            scales.add(temp);
            minl = minl * factor;
            factor_count += 1;
        }

        for (double scale : scales) {
            int hs = (int) Math.ceil(h * scale);
            int ws = (int) Math.ceil(w * scale);
            float[][][] im_data = imresample(img, hs, ws);

            for (int i = 0; i < hs; i++) {
                for (int j = 0; j < ws; j++) {
                    for (int k = 0; k < 3; k++) {
                        im_data[i][j][k] = (im_data[i][j][k] - 127.5f) * 0.0078152f;
                    }
                }
            }

            //python 脚本中的img_x只在这里出现，所以事实上其实没有什么用
//            double [][][][] img_x = new double[1][hs][ws][3];//我的这里的转置没有写错，因为img_y是空的
            float[][][][] img_y = new float[1][ws][hs][3];
            for (int i = 0; i < hs; i++) {
                for (int j = 0; j < ws; j++) {
                    for (int k = 0; k < 3; k++) {
                        img_y[0][j][i] = im_data[i][j];
                    }
                }
            }

            float[] temp = convert_float(img_y[0]);
            long[] shape = new long[]{1, img_y[0].length, img_y[0][0].length, CHANNELS};
            Tensor<Float> imageTensor = Tensor.create(shape, FloatBuffer.wrap(temp));
            // transpose 在pnet中实现
            float[][][][] out0 = pnet(imageTensor, 0);
            float[][][][] out1 = pnet(imageTensor, 1);
//            printArray(out1[0]);


            float[][] out1_copy = new float[out1[0].length][out1[0][0].length];
            for (int i = 0; i < out1[0].length; i++) {
                for (int j = 0; j < out1[0][0].length; j++) {
                    out1_copy[i][j] = out1[0][i][j][1];
                }
            }
            INDArray boxes = generateBoundingBox(out1_copy, out0[0].clone(), scale, threshold[0]);
//        # inter-scale nms
            if (boxes == null)
                continue;

            INDArray pick = nms(boxes.dup(), 0.5f, "Union");

            for (int i = 0; i < pick.size(0); i++) {
                float[] temp_box = new float[boxes.size(1)];
                for (int j = 0; j < boxes.size(1); j++) {
                    int pick_i = pick.getInt(i);
                    temp_box[j] = boxes.getFloat(pick_i, j);
                }
                total_boxes.add(temp_box);
            }
        }

        int numbox = total_boxes.size();

        if (numbox > 0) {
            INDArray total_boxes_copy = list_to_nd(total_boxes, numbox, total_boxes.get(0).length);
//            printArray(total_boxes_array);
            INDArray pick = nms(total_boxes_copy, 0.7f, "Union");

            INDArray total_boxes_pick = Nd4j.zeros(pick.size(0), total_boxes.get(0).length);
            for (int i = 0; i < pick.size(0); i++) {
                total_boxes_pick.putRow(i, total_boxes_copy.getRow(pick.getInt(i)));
            }

            //这里直接写个for循环也不会太麻烦
            INDArray regw = total_boxes_pick.get(NDArrayIndex.all(), NDArrayIndex.point(2)).
                    sub(total_boxes_pick.get(NDArrayIndex.all(), NDArrayIndex.point(0)));
            INDArray regh = total_boxes_pick.get(NDArrayIndex.all(), NDArrayIndex.point(3)).
                    sub(total_boxes_pick.get(NDArrayIndex.all(), NDArrayIndex.point(1)));
            INDArray qq1 = total_boxes_pick.get(NDArrayIndex.all(), NDArrayIndex.point(0)).
                    add(total_boxes_pick.get(NDArrayIndex.all(), NDArrayIndex.point(5)).mul(regw));
            INDArray qq2 = total_boxes_pick.get(NDArrayIndex.all(), NDArrayIndex.point(1)).
                    add(total_boxes_pick.get(NDArrayIndex.all(), NDArrayIndex.point(6)).mul(regh));
            INDArray qq3 = total_boxes_pick.get(NDArrayIndex.all(), NDArrayIndex.point(2)).
                    add(total_boxes_pick.get(NDArrayIndex.all(), NDArrayIndex.point(7)).mul(regw));
            INDArray qq4 = total_boxes_pick.get(NDArrayIndex.all(), NDArrayIndex.point(3)).
                    add(total_boxes_pick.get(NDArrayIndex.all(), NDArrayIndex.point(8)).mul(regh));
            INDArray total_boxes_result = Nd4j.hstack(qq1, qq2, qq3, qq4,
                    total_boxes_pick.get(NDArrayIndex.all(), NDArrayIndex.point(4)));
            total_boxes_result = rerec(total_boxes_result.dup());
            total_boxes_result = fix(total_boxes_result);
            total_boxes_nd = total_boxes_result;

            total_boxes = nd_to_list(total_boxes_result.dup());
        }

        numbox = total_boxes.size();
        if (numbox > 0) {
//             # second stage


            float[][][][] tempimg = pad_then_resmaple(total_boxes_nd, img, w, h);
            float[][][][] tempimg1 = new float[numbox][24][24][3];
            for (int i = 0; i < numbox; i++) {
                for (int j = 0; j < 24; j++) {
                    for (int k = 0; k < 24; k++) {
                        tempimg1[i][j][k] = tempimg[i][k][j];
                        for (int l = 0; l < 3; l++) {
                            tempimg1[i][j][k][l] = (tempimg1[i][j][k][l] - 127.5f) * 0.0078125f;
                        }
                    }
                }
            }

            float[] tmp = ArrayUtil.flattenFloatArray(tempimg1);
//            printArray(tmp);
            long[] shape = new long[]{numbox, 24, 24, CHANNELS};
            Tensor<Float> boxTensor = Tensor.create(shape, FloatBuffer.wrap(tmp));
            INDArray out0 = rnet(boxTensor, 0).transpose();
            INDArray out1 = rnet(boxTensor, 1).transpose();
            INDArray score = out1.get(NDArrayIndex.point(1), NDArrayIndex.all());

            List<Integer> ipass = new ArrayList<>();
            for (int i = 0; i < score.size(1); i++) {
                if (score.getFloat(i) > threshold[1])
                    ipass.add(i);
            }

//            printArray(list_to_nd(total_boxes,total_boxes.size(),total_boxes.get(0).length));

            ArrayList<float[]> tmp_total_boxes = new ArrayList<>();
            tmp_total_boxes = (ArrayList<float[]>) total_boxes.clone();

            total_boxes.clear();
            INDArray mv = Nd4j.zeros(out0.size(0), ipass.size());
            for (int i = 0; i < ipass.size(); i++) {
                float[] tmp_box = new float[5];
                int tmp_index = ipass.get(i);
                System.arraycopy(tmp_total_boxes.get(tmp_index), 0, tmp_box, 0, 4);
                tmp_box[4] = score.getFloat(tmp_index);
                total_boxes.add(tmp_box);

                mv.get(NDArrayIndex.all(), NDArrayIndex.point(i)).
                        assign(out0.get(NDArrayIndex.all(), NDArrayIndex.point(tmp_index)));
            }

            if (total_boxes.size() > 0) {
                INDArray total_boxes_tmp = list_to_nd(total_boxes, ipass.size(), total_boxes.get(0).length);
//            printArray(total_boxes_array);
                INDArray pick = nms(total_boxes_tmp, 0.7f, "Union");

                INDArray total_boxes_pick = Nd4j.zeros(pick.size(0), total_boxes.get(0).length);

                INDArray mv_pick = Nd4j.zeros(mv.size(0), pick.size(0));
                for (int i = 0; i < pick.size(0); i++) {
                    total_boxes_pick.putRow(i, total_boxes_tmp.getRow(pick.getInt(i)));
                    mv_pick.get(NDArrayIndex.all(), NDArrayIndex.point(i)).
                            assign(mv.get(NDArrayIndex.all(), NDArrayIndex.point(pick.getInt(i))));
                }

                total_boxes_pick = bbreg(total_boxes_pick.dup(), mv_pick.transpose());
                total_boxes_pick = rerec(total_boxes_pick.dup());
                total_boxes = nd_to_list(fix(total_boxes_pick.dup()));
                total_boxes_nd = total_boxes_pick;
            }
        }
        numbox = total_boxes.size();
        if (numbox > 0) {
//            # third stage
//            total_boxes = np.fix(total_boxes).astype(np.int32）这步扔到第二步的结尾做了
//            dy, edy, dx, edx, y, ey, x, ex, tmpw, tmph = pad(total_boxes.copy(), w, h)
//            tempimg = np.zeros((48, 48, 3, numbox))
//            for k in range(0, numbox):
//            tmp = np.zeros((int(tmph[k]), int(tmpw[k]), 3))
//            tmp[dy[k] - 1:edy[k], dx[k] - 1:edx[k], :] = img[y[k] - 1:ey[k], x[k] - 1:ex[k], :]
//            if tmp.shape[0] > 0 and tmp.shape[1] > 0 or tmp.shape[0] == 0 and tmp.shape[1] == 0:
//            tempimg[:, :, :, k] = imresample(tmp, (48, 48))
//            else:
//            return np.empty()
//            tempimg = (tempimg - 127.5) * 0.0078125
//            tempimg1 = np.transpose(tempimg, (3, 1, 0, 2))
        }
        System.out.println("OK");
        return total_boxes;


    }

    public static INDArray list_to_nd(List<float[]> total_boxes, int h, int w) {
        float[][] total_boxes_array = new float[h][w];
        for (int i = 0; i < h; i++) {
            total_boxes_array[i] = total_boxes.get(i);
        }
        return Nd4j.create(total_boxes_array);
    }

    public static ArrayList<float[]> nd_to_list(INDArray total_boxes_pick) {
        ArrayList<float[]> total_boxes = new ArrayList<>();
        for (int i = 0; i < total_boxes_pick.size(0); i++) {
            float[] tmp_box = new float[total_boxes_pick.size(1)];
            for (int j = 0; j < total_boxes_pick.size(1); j++) {
                tmp_box[j] = total_boxes_pick.getFloat(i, j);
            }
            total_boxes.add(tmp_box);
        }
        return total_boxes;
    }

    public static float[][][][] pad_then_resmaple(INDArray total_boxes, byte[][][] img, int w, int h) {
        List<INDArray> pad_res = new ArrayList<>();
        pad_res = pad(total_boxes.dup(), w, h);
        INDArray dy = pad_res.get(0);
        INDArray edy = pad_res.get(1);
        INDArray dx = pad_res.get(2);
        INDArray edx = pad_res.get(3);
        INDArray y = pad_res.get(4);
        INDArray ey = pad_res.get(5);
        INDArray x = pad_res.get(6);
        INDArray ex = pad_res.get(7);
        INDArray tmpw = pad_res.get(8);
        INDArray tmph = pad_res.get(9);

        int numbox = total_boxes.size(0);
        float[][][][] tempimg = new float[numbox][24][24][3];
        for (int k = 0; k < numbox; k++) {

            byte[][][] tmp = new byte[(int) (tmph.getFloat(k))][(int) (tmpw.getFloat(k))][3];

            int tmp_dy = dy.getInt(k) - 1;
            int tmp_dx = dx.getInt(k) - 1;
            int tmp_y = y.getInt(k) - 1;
            int tmp_x = x.getInt(k) - 1;
            for (int i = 0; i < (edy.getInt(k) - tmp_dy); i++) {
                for (int j = 0; j < (edx.getInt(k) - tmp_dx); j++) {
                    tmp[tmp_dy + i][tmp_dx + j] = img[tmp_y + i][tmp_x + j];
                }
            }
            if ((tmp.length > 0 && tmp[0].length > 0) || (tmp.length == 0 && tmp[0].length == 0))
                tempimg[k] = imresample(tmp, 24, 24);
            else
                return null;
//                System.out.println("OK");
        }
        return tempimg;
    }


    public static float[][][][] pnet(Tensor<Float> imageTensor, int swtich) {
        long time = System.currentTimeMillis();

        Tensor<Float> result;
        if (swtich == 0) {
            // Generally, there may be multiple output tensors, all of them must be closed to prevent resource leaks.
            result = s.runner().feed("pnet/input:0", imageTensor).fetch("pnet/conv4-2/BiasAdd:0")
                    .run().get(0).expect(Float.class);

        } else {
            result = s.runner().feed("pnet/input:0", imageTensor).
                    fetch("pnet/prob1:0").run().get(0).expect(Float.class);
        }
        long[] res_shape = result.shape();
        float[][][][] output = result.copyTo(new float[(int) res_shape[0]][(int) res_shape[1]][(int) res_shape[2]][(int) res_shape[3]]);
        float[][][][] res = new float[(int) res_shape[0]][(int) res_shape[2]][(int) res_shape[1]][(int) res_shape[3]];
        for (int i = 0; i < (int) res_shape[1]; i++) {
            for (int j = 0; j < (int) res_shape[2]; j++) {
                res[0][j][i] = output[0][i][j];
            }
        }
        long time1 = System.currentTimeMillis();
        float seconds_pic = (time1 - time) / 1000F;
        System.out.println("creat tensor:" + Float.toString(seconds_pic) + " seconds.");

        return res;
    }

    public static INDArray rnet(Tensor<Float> imageTensor, int swtich) {
        long time = System.currentTimeMillis();

        Tensor<Float> result;
        if (swtich == 0) {
            // Generally, there may be multiple output tensors, all of them must be closed to prevent resource leaks.
            result = s.runner().feed("rnet/input:0", imageTensor).fetch("rnet/conv5-2/conv5-2:0")
                    .run().get(0).expect(Float.class);

        } else {
            result = s.runner().feed("rnet/input:0", imageTensor).
                    fetch("rnet/prob1:0").run().get(0).expect(Float.class);
        }
        long[] res_shape = result.shape();
        float[][] output = result.copyTo(new float[(int) res_shape[0]][(int) res_shape[1]]);


//        float[] tmp_output = ArrayUtil.flattenFloatArray(output);
//        int[]  tmp_shape = new int[]{(int) res_shape[0],(int) res_shape[1],(int) res_shape[2],(int) res_shape[3]};
        INDArray res = Nd4j.create(output);

        long time1 = System.currentTimeMillis();
        float seconds_pic = (time1 - time) / 1000F;
        System.out.println("creat tensor:" + Float.toString(seconds_pic) + " seconds.");

        return res;
    }

    public static INDArray bbreg(INDArray boundingbox, INDArray reg) {
        //        "" "Calibrate bounding boxes" ""
        if (reg.size(1) == 1) {
            System.out.println("reshape");
//            reg = np.reshape(reg, (reg.shape[2], reg.shape[3])),二维的为什么会有shape[2]和shape[4]

        }
        INDArray w = boundingbox.getColumn(2).sub(boundingbox.getColumn(0)).add(1);
        INDArray h = boundingbox.getColumn(3).sub(boundingbox.getColumn(1)).add(1);
        INDArray b1 = boundingbox.getColumn(0).add(reg.getColumn(0).mul(w));
        INDArray b2 = boundingbox.getColumn(1).add(reg.getColumn(1).mul(h));
        INDArray b3 = boundingbox.getColumn(2).add(reg.getColumn(2).mul(w));
        INDArray b4 = boundingbox.getColumn(3).add(reg.getColumn(3).mul(h));
        boundingbox.get(NDArrayIndex.all(), NDArrayIndex.interval(0, 4)).assign(Nd4j.hstack(b1, b2, b3, b4));
        return boundingbox;

    }


    public static INDArray generateBoundingBox(float[][] imap, float[][][] reg, double scale, double t) {
        int stride = 2;
        int cellsize = 12;

        INDArray Imap = Nd4j.create(imap).transpose();

        float[] flat = ArrayUtil.flattenFloatArray(reg);
        int[] shape = new int[]{reg.length, reg[0].length, reg[0][0].length};    //Array shape here
        INDArray Reg = Nd4j.create(flat, shape, 'c');


        INDArray dx1 = Reg.get(NDArrayIndex.all(), NDArrayIndex.all(), NDArrayIndex.point(0)).transpose();
        INDArray dy1 = Reg.get(NDArrayIndex.all(), NDArrayIndex.all(), NDArrayIndex.point(1)).transpose();
        INDArray dx2 = Reg.get(NDArrayIndex.all(), NDArrayIndex.all(), NDArrayIndex.point(2)).transpose();
        INDArray dy2 = Reg.get(NDArrayIndex.all(), NDArrayIndex.all(), NDArrayIndex.point(3)).transpose();

//        double[] temmp = new double[]{5.2,4.2,4.233692,3.25};
//        INDArray rest =Nd4j.create(temmp);
//        Nd4j.getExecutioner().execAndReturn(new ScalarGreaterThanOrEqual(rest,4));
//        INDArray yx = Imap.dup();
//        Nd4j.getExecutioner().execAndReturn(new ScalarGreaterThanOrEqual(yx,t));
        List<Integer> y = new ArrayList<Integer>();
        List<Integer> x = new ArrayList<Integer>();
        for (int i = 0; i < imap[0].length; i++) {
            for (int j = 0; j < imap.length; j++) {
                if (Imap.getFloat(i, j) < t)
                    continue;
                y.add(i);
                x.add(j);
            }
        }

        if (y.size() == 0) {
            return null;
        }

        if (y.size() == 1) {
            Nd4j.rot90(dx1);
            Nd4j.rot90(dy1);
            Nd4j.rot90(dx2);
            Nd4j.rot90(dy2);
        }

//        System.out.println(y.size());
        float[][] score_temp = new float[y.size()][1];
        INDArray temp_dx1 = Nd4j.zeros(y.size());
        INDArray temp_dy1 = Nd4j.zeros(y.size());
        INDArray temp_dx2 = Nd4j.zeros(y.size());
        INDArray temp_dy2 = Nd4j.zeros(y.size());
        INDArray temp_x = Nd4j.zeros(y.size());
        INDArray temp_y = Nd4j.zeros(y.size());

        for (int i = 0; i < y.size(); i++) {
            score_temp[i][0] = Imap.getFloat(y.get(i), x.get(i));
            temp_dx1.putScalar(i, dx1.getFloat(y.get(i), x.get(i)));
            temp_dy1.putScalar(i, dy1.getFloat(y.get(i), x.get(i)));
            temp_dx2.putScalar(i, dx2.getFloat(y.get(i), x.get(i)));
            temp_dy2.putScalar(i, dy2.getFloat(y.get(i), x.get(i)));
            temp_x.putScalar(i, (float) x.get(i));
            temp_y.putScalar(i, (float) y.get(i));

        }
        INDArray score = Nd4j.create(score_temp);
        INDArray reg_new = Nd4j.vstack(temp_dx1, temp_dy1, temp_dx2, temp_dy2).transpose();

//        if (reg_new.size(0) == 0) {
//            reg_new = Nd4j.randn(0, 3);
//        }

        INDArray bb = Nd4j.vstack(temp_y, temp_x).transpose();
        INDArray q1 = bb.dup();
        q1.muli(stride);
        q1.addi(1);
        q1.divi(scale);
        INDArray q2 = bb.dup();
        q2.muli(stride);
        q2.addi(cellsize);
        q2.divi(scale);

        INDArray boundingbox = Nd4j.hstack(q1, q2, score, reg_new);

        return fix(boundingbox);
    }

    public static INDArray fix(INDArray boundingbox) {
        for (int i = 0; i < boundingbox.size(0); i++) {
            for (int j = 0; j < 4; j++) {
                double temp = boundingbox.getDouble(i, j);
                if (temp > 0)
                    temp = Math.floor(temp);
                else
                    temp = Math.ceil(temp);
                boundingbox.putScalar(i, j, temp);
            }
        }
        return boundingbox;
    }


    //     function pick = nms(boxes,threshold,type)
    public static INDArray nms(INDArray boxes, float threshold, String method) {
        if (boxes.size(0) == 0) {
            return null;
        }
//        printArray(boxes);

        INDArray x1 = boxes.get(NDArrayIndex.all(), NDArrayIndex.point(0));
        INDArray y1 = boxes.get(NDArrayIndex.all(), NDArrayIndex.point(1));
        INDArray x2 = boxes.get(NDArrayIndex.all(), NDArrayIndex.point(2));
        INDArray y2 = boxes.get(NDArrayIndex.all(), NDArrayIndex.point(3));
        INDArray ss = boxes.get(NDArrayIndex.all(), NDArrayIndex.point(4));

        INDArray area1 = x2.dup();
        area1.subi(x1);
        area1.addi(1);
        INDArray area2 = y2.dup();
        area2.subi(y1);
        area2.addi(1);
        INDArray area = area1.mul(area2);

        double[] temp_ss = new double[ss.size(0)];
        for (int i = 0; i < temp_ss.length; i++) {
            temp_ss[i] = ss.getDouble(i);
        }
        int[] I = argsort(temp_ss, true);

        INDArray pick = Nd4j.zeros(ss.shape());
        int counter = 0;
        while (I.length > 0) {
            int i = I[I.length - 1];
            pick.putScalar(counter, i);
            counter += 1;
            int[] idx = Arrays.copyOfRange(I, 0, I.length - 1);

            List<Integer> newI = new ArrayList<Integer>();
            for (int j = 0; j < I.length - 1; j++) {
                double xx1 = x1.getDouble(i) > x1.getDouble(idx[j]) ? x1.getDouble(i) : x1.getDouble(idx[j]);
                double yy1 = y1.getDouble(i) > y1.getDouble(idx[j]) ? y1.getDouble(i) : y1.getDouble(idx[j]);
                double xx2 = x2.getDouble(i) < x2.getDouble(idx[j]) ? x2.getDouble(i) : x2.getDouble(idx[j]);
                double yy2 = y2.getDouble(i) < y2.getDouble(idx[j]) ? y2.getDouble(i) : y2.getDouble(idx[j]);
                double temp1 = xx2 - xx1 + 1;
                double temp2 = yy2 - yy1 + 1;
                double w = 0.0 > temp1 ? 0.0 : temp1;
                double h = 0.0 > temp2 ? 0.0 : temp2;
                double inter = w * h;
                double o;
                if (method.equals("Min")) {
                    double temp_area = area.getDouble(i) < area.getDouble(idx[j]) ? area.getDouble(i) : area.getDouble(idx[j]);
                    o = inter / temp_area;
                } else {
                    o = inter / (area.getDouble(i) + area.getDouble(idx[j]) - inter);
                }
                if (o <= threshold) {
                    newI.add(I[j]);
                }
            }

            I = Ints.toArray(newI);
        }

        return pick.get(NDArrayIndex.interval(0, counter));
    }

    public static int[] argsort(final double[] a, final boolean ascending) {
        Integer[] indexes = new Integer[a.length];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
        }
        Arrays.sort(indexes, new Comparator<Integer>() {
            @Override
            public int compare(final Integer i1, final Integer i2) {
                return (ascending ? 1 : -1) * Doubles.compare(a[i1], a[i2]);
            }
        });

        int[] ret = new int[indexes.length];
        for (int i = 0; i < ret.length; i++)
            ret[i] = indexes[i];

        return ret;
    }

    //    """Compute the padding coordinates (pad the bounding boxes to square)"""
    public static List<INDArray> pad(INDArray total_boxes, int w, int h) {
        List<INDArray> pad_res = new ArrayList<>();

        INDArray tmpw = (total_boxes.getColumn(2).sub(total_boxes.getColumn(0)).add(1).transpose());
        INDArray tmph = (total_boxes.getColumn(3).sub(total_boxes.getColumn(1)).add(1).transpose());
        int numbox = total_boxes.size(0);

        INDArray dx = Nd4j.ones(numbox);
        INDArray dy = Nd4j.ones(numbox);
        INDArray edx = tmpw.dup();
        INDArray edy = tmph.dup();

        INDArray x = total_boxes.getColumn(0).dup().transpose();
        INDArray y = total_boxes.getColumn(1).dup().transpose();
        INDArray ex = total_boxes.getColumn(2).dup().transpose();
        INDArray ey = total_boxes.getColumn(3).dup().transpose();

        List<Integer> tmptmp = new ArrayList<>();
        for (int i = 0; i < numbox; i++) {
            if (ex.getFloat(i) > w) {
                float tmp = -ex.getFloat(i) + w + tmpw.getFloat(i);
                edx.putScalar(i, tmp);
                ex.putScalar(i, w);
            }
            if (ey.getFloat(i) > h) {
                float tmp = -ey.getFloat(i) + h + tmph.getFloat(i);
                edy.putScalar(i, tmp);
                ey.putScalar(i, h);
            }
            if (x.getFloat(i) < 1) {
                float tmp = 2 - x.getFloat(i);
                dx.putScalar(i, 2 - x.getFloat(i));
                x.putScalar(i, 1);
                tmptmp.add(dx.getInt(i));

            }
            if (y.getFloat(i) < 1) {
                dy.putScalar(i, 2 - y.getFloat(i));
                y.putScalar(i, 1);
            }
        }
        pad_res.add(dy);
        pad_res.add(edy);
        pad_res.add(dx);
        pad_res.add(edx);
        pad_res.add(y);
        pad_res.add(ey);
        pad_res.add(x);
        pad_res.add(ex);
        pad_res.add(tmpw);
        pad_res.add(tmph);
        return pad_res;
    }


    public static INDArray rerec(INDArray bboxA) {
//        """Convert bboxA to square."""
        INDArray l = Nd4j.zeros(bboxA.size(0), 2);
        for (int i = 0; i < bboxA.size(0); i++) {
            float h = bboxA.getFloat(i, 3) - bboxA.getFloat(i, 1);
            float w = bboxA.getFloat(i, 2) - bboxA.getFloat(i, 0);
            float templ = h > w ? h : w;

            bboxA.putScalar(i, 0, (bboxA.getFloat(i, 0) + w * 0.5 - templ * 0.5));
            bboxA.putScalar(i, 1, (bboxA.getFloat(i, 1) + h * 0.5 - templ * 0.5));
            l.putScalar(i, 0, templ);
            l.putScalar(i, 1, templ);
        }

        bboxA.get(NDArrayIndex.all(), NDArrayIndex.interval(2, 4)).
                assign(bboxA.get(NDArrayIndex.all(), NDArrayIndex.interval(0, 2)).add(l));
        return bboxA;

    }


    public static float[][][] imresample(byte[][][] img, int hs, int ws) {
        float[][][] res;
//        Mat img_mat = new Mat(img.length,img[0].length,CvType.CV_64FC(3));
//        double [] temp = convert(img);
//        img_mat.put(0, 0,temp);
//
//        Mat dst=new Mat(hs,ws,CvType.CV_64FC(3));
//        Imgproc.resize(img_mat, dst, new Size(ws,hs),0,0,Imgproc.INTER_AREA);
//
//        double [] temp_res = new double[hs*ws*3];
//        dst.get(0,0,temp_res);
//        res = convert(temp_res,hs,ws);

        Mat img_mat = new Mat(img.length, img[0].length, CvType.CV_8UC(3));
        byte[] temp = convert(img);
        img_mat.put(0, 0, temp);

        Mat dst = new Mat(hs, ws, CvType.CV_64FC(3));
        Imgproc.resize(img_mat, dst, new Size(ws, hs), 0, 0, Imgproc.INTER_AREA);

        byte[] temp_res = new byte[hs * ws * 3];
        dst.get(0, 0, temp_res);
        res = convert(temp_res, hs, ws);

        return res;
    }

    public static double[] convert(double[][][] img) {

        double[] temp = new double[img.length * img[0].length * 3];
        for (int i = 0; i < img.length; i++) {
            for (int j = 0; j < img[0].length; j++) {
                for (int k = 0; k < 3; k++) {
                    int index = i * img[0].length * 3 + j * 3 + k;
                    temp[index] = img[i][j][k];
                }
            }
        }
        return temp;
    }

    public static float[] convert_float(float[][][] img) {

        float[] temp = new float[img.length * img[0].length * 3];
        for (int i = 0; i < img.length; i++) {
            for (int j = 0; j < img[0].length; j++) {
                for (int k = 0; k < 3; k++) {
                    int index = i * img[0].length * 3 + j * 3 + k;
                    temp[index] = (float) img[i][j][k];
                }
            }
        }
        return temp;
    }

//    public static float[] convert_float(float[][][][] img) {
//
//        float[] temp = new float[img.length * img[0].length *img[0][0].length* 3];
//        for (int i = 0; i < img.length; i++) {
//            for (int j = 0; j < img[0].length; j++) {
//                for (int k = 0; k <img[0][0].length; k++) {
//                    for (int l = 0; l < 3; l++) {
//                        int index = i * img[0].length *img[0][0].length * 3 + j *img[0][0].length * 3 + k*3+l;
//                        temp[index] =  img[i][j][k][l];
//                    }
//                }
//            }
//        }
//        return temp;
//    }


    public static double[][][] convert(double[] img, int h, int w) {
        double[][][] res = new double[h][w][3];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                for (int k = 0; k < 3; k++) {
                    int index = i * w * 3 + j * 3 + k;
                    res[i][j][k] = img[index];
                }
            }
        }
        return res;
    }

    public static float[][][] convert(byte[] img, int h, int w) {
        float[][][] res = new float[h][w][3];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                for (int k = 0; k < 3; k++) {
                    int index = i * w * 3 + j * 3 + k;
                    res[i][j][k] = (float) (img[index] & (0xff));
                }
            }
        }
        return res;
    }

    public static byte[] convert(byte[][][] img) {

        byte[] temp = new byte[img.length * img[0].length * 3];
        for (int i = 0; i < img.length; i++) {
            for (int j = 0; j < img[0].length; j++) {
                for (int k = 0; k < 3; k++) {
                    int index = i * img[0].length * 3 + j * 3 + k;
                    temp[index] = img[i][j][k];
                }
            }
        }
        return temp;
    }

    // 泛型方法
    public static void printArray(float[][][] inputArray) {
        try { // 防止文件建立或读取失败，用catch捕捉错误并打印，也可以throw
            /* 写入Txt文件 */
            File writename = new File("output.txt"); // 相对路径，如果没有则要建立一个新的output。txt文件
            writename.createNewFile(); // 创建新文件
            BufferedWriter out = new BufferedWriter(new FileWriter(writename));
            for (int i = 0; i < inputArray.length; i++) {
                for (int j = 0; j < inputArray[0].length; j++) {
                    for (int k = 0; k < inputArray[0][0].length; k++) {

                        float temp = inputArray[i][j][k];
                        System.out.println(temp);
                        out.write(Float.toString(inputArray[i][j][k])); // \r\n即为换行
                        out.write(System.getProperty("line.separator")); // \r\n即为换行

                    }

                }
            }
            out.flush(); // 把缓存区内容压入文件
            out.close(); // 最后记得关闭文件

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 泛型方法
    public static void printArray(INDArray inputArray) {
        try { // 防止文件建立或读取失败，用catch捕捉错误并打印，也可以throw
            /* 写入Txt文件 */
            File writename = new File("output.txt"); // 相对路径，如果没有则要建立一个新的output。txt文件
            writename.createNewFile(); // 创建新文件
            BufferedWriter out = new BufferedWriter(new FileWriter(writename));
            for (int i = 0; i < inputArray.size(0); i++) {
                for (int j = 0; j < inputArray.size(1); j++) {
                    float temp = inputArray.getFloat(i, j);
                    System.out.println(temp);
                    out.write(Float.toString(temp)); // \r\n即为换行
                    out.write(System.getProperty("line.separator")); // \r\n即为换行
                }
            }
            out.flush(); // 把缓存区内容压入文件
            out.close(); // 最后记得关闭文件

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printArray(float[] inputArray) {
        try { // 防止文件建立或读取失败，用catch捕捉错误并打印，也可以throw
            /* 写入Txt文件 */
            File writename = new File("output.txt"); // 相对路径，如果没有则要建立一个新的output。txt文件
            writename.createNewFile(); // 创建新文件
            BufferedWriter out = new BufferedWriter(new FileWriter(writename));
            for (int i = 0; i < inputArray.length; i++) {
                float temp = inputArray[i];
                System.out.println(temp);
                out.write(Double.toString(temp)); // \r\n即为换行
                out.write(System.getProperty("line.separator")); // \r\n即为换行
            }
            out.flush(); // 把缓存区内容压入文件
            out.close(); // 最后记得关闭文件

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void printArray(float[][] inputArray) {
        try { // 防止文件建立或读取失败，用catch捕捉错误并打印，也可以throw
            /* 写入Txt文件 */
            File writename = new File("output.txt"); // 相对路径，如果没有则要建立一个新的output。txt文件
            writename.createNewFile(); // 创建新文件
            BufferedWriter out = new BufferedWriter(new FileWriter(writename));
            for (int i = 0; i < inputArray.length; i++) {
                for (int j = 0; j < inputArray[0].length; j++) {
                    float temp = inputArray[i][j];
                    System.out.println(temp);
                    out.write(Double.toString(temp)); // \r\n即为换行
                    out.write(System.getProperty("line.separator")); // \r\n即为换行
                }


            }
            out.flush(); // 把缓存区内容压入文件
            out.close(); // 最后记得关闭文件

        } catch (Exception e) {
            e.printStackTrace();
        }
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


    public static void img_process(String img_path, String face_dir) {
        try {
            BufferedImage bimg = ImageIO.read(new File(img_path));
            int height = bimg.getHeight(), width = bimg.getWidth();

            byte[][][] img = new byte[height][width][3];

            //通过getRGB()方式获得像素矩阵
            //此方式为沿Height方向扫描
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int rgb = bimg.getRGB(j, i);

                    img[i][j][0] = (byte) ((rgb & 0xff0000) >> 16);
                    img[i][j][1] = (byte) ((rgb & 0xff00) >> 8);
                    img[i][j][2] = (byte) ((rgb & 0xff));

//                    //输出一列数据比对
//                    if (i == 156 && j == 0)
//                        System.out.printf("%d\t%d\t%d\t", data[i][j][0], data[i][j][1], data[i][j][2]);
                }
            }

            int minsize = 20;  // minimum size of face
            double[] threshold = new double[]{0.6, 0.7, 0.7};// three steps's threshold
            double factor = 0.709;  // scale factor
            List<float[]> bounding_boxes = new ArrayList<float[]>();

            bounding_boxes = detect_face(img, minsize, threshold, factor);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


//    public static void main(String[] args) throws Exception {
//        long startTime = System.currentTimeMillis();
//        Mtcnn mtcnn = new Mtcnn();
//
//
//        img_process("D:\\Z\\1.jpg", "D:\\Z\\code\\video_detect\\data\\api\\3");
//
//
//        long endTime = System.currentTimeMillis();
//        float seconds = (endTime - startTime) / 1000F;
//        System.out.println("load model:" + Float.toString(seconds) + " seconds.");
//    }


}