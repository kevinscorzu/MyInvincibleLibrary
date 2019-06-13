package tec.ac.cr.mil.ml;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ObjectRecognizer {

    byte[] graphDef;
    String imagepath;
    List<String> labels;
    String modelpath;


    private static float[] executeInceptionGraph(byte[] graphDef, Tensor image) {
        try (Graph graph = new Graph()) {
            graph.importGraphDef(graphDef);
            try (Session s = new Session(graph);
                 Tensor result = s.runner().feed("DecodeJpeg/contents", image).fetch("softmax").run().get(0)) {
                final long[] rshape = result.shape();
                if (result.numDimensions() != 2 || rshape[0] != 1) {
                    throw new RuntimeException(
                            String.format(
                                    "Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
                                    Arrays.toString(rshape)));
                }
                int nlabels = (int) rshape[1];
                float[][] resultArray = new float[1][nlabels];
                result.copyTo(resultArray);
                return resultArray[0];
            }
        }
    }

    private static int maxIndex(float[] probabilities) {
        int best = 0;
        for (int i = 1; i < probabilities.length; ++i) {
            if (probabilities[i] > probabilities[best]) {
                best = i;
            }
        }
        return best;
    }

    private static byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            System.err.println("Failed to read [" + path + "]: " + e.getMessage());
            System.exit(1);
        }
        return null;
    }

    private static List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path, Charset.forName("UTF-8"));
        } catch (IOException e) {
            System.err.println("Failed to read [" + path + "]: " + e.getMessage());
            System.exit(0);
        }
        return null;
    }

    public void recognize(File loadedImg) {
        // Define ML model
        File file = new File("." + File.separator + "server" +
                File.separator + "src" + File.separator + "tec" +
                File.separator + "ac" + File.separator + "cr" +
                File.separator + "mil" + File.separator + "ml" +
                File.separator + "model");
        modelpath = file.getAbsolutePath();
        System.out.println("Opening: " + file.getAbsolutePath());
        graphDef = ObjectRecognizer.readBytes(Paths.get(modelpath,"tensorflow_inception_graph.pb"));
        labels = ObjectRecognizer.readLines(Paths.get(modelpath, "imagenet_comp_graph_label_strings.txt"));

        // Load image
        imagepath = loadedImg.getAbsolutePath();
        System.out.println("Image Path: " + imagepath);

        // Apply Machine Learning
        byte[] imageBytes = ObjectRecognizer.readBytes(Paths.get(imagepath));
        try (Tensor image = Tensor.create(imageBytes)) {
            float[] labelProbabilities = ObjectRecognizer.executeInceptionGraph(graphDef, image);
            int bestLabelIdx = ObjectRecognizer.maxIndex(labelProbabilities);
            System.out.println(
                    String.format(
                            "BEST MATCH: %s (%.2f%% likely)",
                            labels.get(bestLabelIdx), labelProbabilities[bestLabelIdx] * 100f));
            System.out.println(
                    String.format(
                            "Option 2: %s (%.2f%% likely)",
                            labels.get(bestLabelIdx + 1), labelProbabilities[bestLabelIdx + 1] * 100f));
            System.out.println(
                    String.format(
                            "Option 3: %s (%.2f%% likely)",
                            labels.get(bestLabelIdx + 2), labelProbabilities[bestLabelIdx + 2] * 100f));
            System.out.println(
                    String.format(
                            "Option 4: %s (%.2f%% likely)",
                            labels.get(bestLabelIdx + 3), labelProbabilities[bestLabelIdx + 3] * 100f));
            System.out.println(
                    String.format(
                            "Option 5: %s (%.2f%% likely)",
                            labels.get(bestLabelIdx + 4), labelProbabilities[bestLabelIdx + 4] * 100f));
        }
    }

}