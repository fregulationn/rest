package com.example.face_library;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.tensorflow.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/** Sample use of the TensorFlow Java API to label images using a pre-trained model. */
public class FeatureExtractorBase {
    private int inputSize = 48;
    private int channels = 3;
    private float imageMean = (float)127.5;
    private float imageStd = (float)128.0;
    private String inputLayer = "onet/input";
    private String outputLayer = "onet/conv6-3/conv6-3";

    private Output output_normalize;
    private Session sess_normalize;

    private Session sess_inference;
    static String strClassName = FeatureExtractorBase.class.getName();
    static Logger log = Logger.getLogger(strClassName);

    public FeatureExtractorBase() {
        log.setLevel(Level.OFF);
        log.info("All log is turned off.");
    }

    public void init(int inputSize, int channels, float imageMean, float imageStd,
                                String inputLayer, String outputLayer, String modelFile) {
        byte[] graphDef = readAllBytesOrExit(Paths.get(modelFile));
        Graph g = new Graph();
        g.importGraphDef(graphDef);
        sess_inference = new Session(g);

        this.inputSize = inputSize;
        this.channels = channels;
        this.imageMean = imageMean;
        this.imageStd = imageStd;
        this.inputLayer = inputLayer;
        this.outputLayer = outputLayer;
        constructGraphToNormalizeImage();
    }

    public float[] getFeatures(Mat image) {
        MatOfByte byteMat = new MatOfByte();
        Imgcodecs.imencode(".png", image, byteMat);
        byte[] imageBytes = byteMat.toArray();

        float[] feats;

        Tensor img = executeNormalizeImage(imageBytes);

        feats = executeInceptionGraph(sess_inference, img);
        String s = "";
        for(int i=0; i<10; i++){
            s += feats[i] + ", ";
        }
        log.info(s);
        byteMat.release();
        img.close();
        return feats;
    }

    public void constructGraphToNormalizeImage() {
        Graph g = new Graph();
        GraphBuilder b = new GraphBuilder(g);
        // Some constants specific to the pre-trained model at:
        // https://storage.googleapis.com/download.tensorflow.org/models/inception5h.zip
        //
        // - The model was trained with images scaled to 224x224 pixels.
        // - The colors, represented as R, G, B in 1-byte each were converted to
        //   float using (value - Mean)/Scale.
        final int H = inputSize;
        final int W = inputSize;
        final float mean = imageMean;
        final float scale = imageStd;

        // Since the graph is being constructed once per execution here, we can use a constant for the
        // input image. If the graph were to be re-used for multiple input images, a placeholder would
        // have been more appropriate.
        final Output input = b.placeholder("input");
        final Output output =
                b.div(
                        b.sub(
                                b.resizeBilinear(
                                        b.expandDims(
                                                b.cast(b.decodeJpeg(input, channels), DataType.FLOAT),
                                                b.constant("make_batch", 0)),
                                        b.constant("size", new int[] {H, W})),
                                b.constant("mean", mean)),
                        b.constant("scale", scale));
        output_normalize = output;
        sess_normalize = new Session(g);
    }

    public Tensor executeNormalizeImage(byte[] imageBytes) {
        Tensor input = Tensor.create(imageBytes);
        Tensor output = sess_normalize.runner().feed("input", input).fetch(output_normalize.op().name()).run().get(0);
        input.close();
        return output;
    }

    public float[] executeInceptionGraph(Session sess_inference, Tensor image) {
        Tensor result = sess_inference.runner().feed(inputLayer, image).fetch(outputLayer).run().get(0);
        final long[] rshape = result.shape();
        if (result.numDimensions() != 2 || rshape[0] != 1) {
            throw new RuntimeException(
                    String.format(
                            "Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
                            Arrays.toString(rshape)));
        }
        int nlabels = (int) rshape[1];
        float[][] tmp_feats =(float[][]) result.copyTo(new float[1][nlabels]);
        float[] feats = tmp_feats[0];
        result.close();
        return feats;
    }

    public byte[] readAllBytesOrExit(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            System.err.println("Failed to read [" + path + "]: " + e.getMessage());
            System.exit(1);
        }
        return null;
    }

    public List<String> readAllLinesOrExit(Path path) {
        try {
            return Files.readAllLines(path, Charset.forName("UTF-8"));
        } catch (IOException e) {
            System.err.println("Failed to read [" + path + "]: " + e.getMessage());
            System.exit(0);
        }
        return null;
    }

    // In the fullness of time, equivalents of the methods of this class should be auto-generated from
    // the OpDefs linked into libtensorflow_jni.so. That would match what is done in other languages
    // like Python, C++ and Go.
    class GraphBuilder {
        GraphBuilder(Graph g) {
            this.g = g;
        }

        Output div(Output x, Output y) {
            return binaryOp("Div", x, y);
        }

        Output sub(Output x, Output y) {
            return binaryOp("Sub", x, y);
        }

        Output resizeBilinear(Output images, Output size) {
            return binaryOp("ResizeBilinear", images, size);
        }

        Output expandDims(Output input, Output dim) {
            return binaryOp("ExpandDims", input, dim);
        }

        Output cast(Output value, DataType dtype) {
            return g.opBuilder("Cast", "Cast").addInput(value).setAttr("DstT", dtype).build().output(0);
        }

        Output decodeJpeg(Output contents, long channels) {
            return g.opBuilder("DecodeJpeg", "DecodeJpeg")
                    .addInput(contents)
                    .setAttr("channels", channels)
                    .build()
                    .output(0);
        }

        Output constant(String name, Object value) {
            try (Tensor t = Tensor.create(value)) {
                return g.opBuilder("Const", name)
                        .setAttr("dtype", t.dataType())
                        .setAttr("value", t)
                        .build()
                        .output(0);
            }
        }

        Output placeholder(String name) {
            return g.opBuilder("Placeholder", name)
                    .setAttr("dtype", DataType.STRING)
                    .build()
                    .output(0);
        }

        private Output binaryOp(String type, Output in1, Output in2) {
            return g.opBuilder(type, type).addInput(in1).addInput(in2).build().output(0);
        }

        private Graph g;
    }
}
