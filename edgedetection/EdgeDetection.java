package edgedetection;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.HashMap;

public class EdgeDetection {

    public static final String Horizontal = "Horizontal Filter";
    public static final String Vertical = "Vertical Filter";
    public static final String SobelVertical = "Sobel Vertical Filter";
    public static final String SobelHorizontal = "Sobel Horizontal Filter";
    public static final String ScharrVertical = "Scharr Vertical Filter";
    public static final String ScharrHorizontal = "Scharr Horizontal Filter";
    public static final String CannyEdgeDetection = "Canny Algorithm";
    public static final double higherThreshold = 0.15*294;
    public static final double lowerThreshold = 0.03*294;
    private static final double[][] VerticalMask = {{1, 0, -1}, {1, 0, -1}, {1, 0, -1}};
    private static final double[][] HorizontalMask = {{1, 1, 1}, {0, 0, 0}, {-1, -1, -1}};
    private static final double[][] SobelMaskVertical = {{1, 0, -1}, {2, 0, -2}, {1, 0, -1}};
    private static final double[][] SobelMaskHorizontal = {{1, 2, 1}, {0, 0, 0}, {-1, -2, -1}};
    private static final double[][] ScharrMaskVertical = {{3, 0, -3}, {10, 0, -10}, {3, 0, -3}};
    private static final double[][] ScharrMaskHorizontal = {{3, 10, 3}, {0, 0, 0}, {-3, -10, -3}};

    private final HashMap<String, double[][]> maskMap;

    public EdgeDetection() {
        maskMap = buildMaskMap();
    }

    public File detectEdges(BufferedImage bufferedImage, String selectedFilter, double lowerThresholdValue,
                            double higherThresholdValue) throws IOException {
        double[][] mixedPixels = new double[bufferedImage.getWidth()][bufferedImage.getHeight()];
        File output = null;
        if(selectedFilter.equals(CannyEdgeDetection)) {
            Canny cannyAlgorithm = new Canny(lowerThresholdValue, higherThresholdValue);
            output = cannyAlgorithm.detectEdges(bufferedImage);
        }
        else{
            double[][][] image = transformImageToArray(bufferedImage);
            double[][] filter = maskMap.get(selectedFilter);
            mixedPixels = applyMix(bufferedImage.getWidth(), bufferedImage.getHeight(), image, filter);
            output = createImageFromMatrix(bufferedImage, mixedPixels);
        }
        return output;
    }

    private double[][][] transformImageToArray(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        double[][][] image = new double[3][height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Color color = new Color(bufferedImage.getRGB(j, i));
                image[0][i][j] = color.getRed();
                image[1][i][j] = color.getGreen();
                image[2][i][j] = color.getBlue();
            }
        }
        return image;
    }

    private double[][] applyMix(int width, int height, double[][][] image, double[][] filter) {
        edgedetection.Mix mix = new edgedetection.Mix();
        double[][] redMix = mix.mixNext(image[0], height, width, filter, 3, 3);
        double[][] greenMix = mix.mixNext(image[1], height, width, filter, 3, 3);
        double[][] blueMix = mix.mixNext(image[2], height, width, filter, 3, 3);
        double[][] finalMix = new double[redMix.length][redMix[0].length];
        for (int i = 0; i < redMix.length; i++) {
            for (int j = 0; j < redMix[i].length; j++) {
                finalMix[i][j] = redMix[i][j] + greenMix[i][j] + blueMix[i][j];
            }
        }
        return finalMix;
    }

    private File createImageFromMatrix(BufferedImage originalImage, double[][] imageRGB) throws IOException {
        BufferedImage createNewImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < imageRGB.length; i++) {
            for (int j = 0; j < imageRGB[i].length; j++) {
                Color color = new Color(transformRGB(imageRGB[i][j]),
                        transformRGB(imageRGB[i][j]),
                        transformRGB(imageRGB[i][j]));
                createNewImage.setRGB(j, i, color.getRGB());
            }
        }
        String g = "outputimage" + LocalTime.now() ;
        g = g.replace('.','_').replace(':', '_');
        g = ".\\" + g + ".jpg";
        File outputFile = new File(g);
        ImageIO.write(createNewImage, "jpg", outputFile);
        return outputFile;
    }

    private int transformRGB(double value) {
        if (value < 0.0) {
            value = -value;
        }
        if (value > 255) {
            return 255;
        } else {
            return (int) value;
        }
    }

    private HashMap<String, double[][]> buildMaskMap() {
        HashMap<String, double[][]> maskMap;
        maskMap = new HashMap<>();

        maskMap.put(Vertical, VerticalMask);
        maskMap.put(Horizontal, HorizontalMask);
        maskMap.put(SobelVertical, SobelMaskVertical);
        maskMap.put(SobelHorizontal, SobelMaskHorizontal);
        maskMap.put(ScharrVertical, ScharrMaskVertical);
        maskMap.put(ScharrHorizontal, ScharrMaskHorizontal);
        //maskMap.put(CannyEdgeDetection, gaussianKernel);
        return maskMap;
    }
}
