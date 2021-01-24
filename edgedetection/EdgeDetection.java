package edgedetection;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.HashMap;

/**
 * Klasa przetwarzająca obrazy za pomocą algorytmu Scharra, Sobela i Prewitta
 * @author Aneta Bień
 */

public class EdgeDetection {

    /**
     * Deklaracja zmiennych statycznych.
     */

    public static final String HORIZONTAL = "Horizontal Filter";
    public static final String VERTICAL = "Vertical Filter";
    public static final String SOBEL_VERTICAL = "Sobel Vertical Filter";
    public static final String SOBEL_HORIZONTAL = "Sobel Horizontal Filter";
    public static final String SCHARR_VERTICAL = "Scharr Vertical Filter";
    public static final String SCHARR_HORIZONTAL = "Scharr Horizontal Filter";
    public static final String CANNY_EDGE_DETECTION = "Canny Algorithm";
    public static final double HIGHER_THRESHOLD = 0.15*294;
    public static final double LOWER_THRESHOLD = 0.03*294;
    private static final double[][] VERTICAL_MASK = {{1, 0, -1}, {1, 0, -1}, {1, 0, -1}};
    private static final double[][] HORIZONTAL_MASK = {{1, 1, 1}, {0, 0, 0}, {-1, -1, -1}};
    private static final double[][] SOBEL_MASK_VERTICAL = {{1, 0, -1}, {2, 0, -2}, {1, 0, -1}};
    private static final double[][] SOBEL_MASK_HORIZONTAL = {{1, 2, 1}, {0, 0, 0}, {-1, -2, -1}};
    private static final double[][] SCHARR_MASK_VERTICAL = {{3, 0, -3}, {10, 0, -10}, {3, 0, -3}};
    private static final double[][] SCHARR_MASK_HORIZONTAL = {{3, 10, 3}, {0, 0, 0}, {-3, -10, -3}};

    /**
     * Metoda typu final deklarująca HashMapę przyjmującą zmienne typu String i tablicę dwuwymiarową typu double
     */

    private final HashMap<String, double[][]> maskMap;

    /**
     * Metoda buduje HashMapę za pomocą metody buildMaskMap()
     */

    public EdgeDetection() {
        maskMap = buildMaskMap();
    }

    /**
     * Metoda wykrywa krawędzie
     * @param bufferedImage Modyfikowany obraz
     * @param selectedFilter Wybrany filtr
     * @param lowerThresholdValue Dolny próg algorytmu Canny'ego
     * @param higherThresholdValue Górny próg algorytmu Canny'ego
     * @return output Zmodyfikowany obraz
     */

    public File detectEdges(BufferedImage bufferedImage, String selectedFilter, double lowerThresholdValue,
                            double higherThresholdValue) throws IOException {
        double[][] mixedPixels = new double[bufferedImage.getWidth()][bufferedImage.getHeight()];
        File output = null;
        if(selectedFilter.equals(CANNY_EDGE_DETECTION)) {
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

    /**
     * Metoda zmienia obraz na wektor
     * @param bufferedImage Modyfikowany obraz
     * @return image Obraz w postaci wektora z uwględnionymi składowymi RGB
     */

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

    /**
     * Metoda łączy jądra obrazu z filtrem
     * @param width Szerokość obrazu
     * @param height Wysokość obrazu
     * @param image Obraz w postaci tablicy
     * @param filter Filtr
     * @return finalMix Połączone składowe z filtrem
     */

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

    /**
     * Metoda tworzy obraz z macierzy
     * @param originalImage Obraz wejściowy
     * @param imageRGB Macierz obrazu w skali RGB
     * @return outputFile obraz wyjściowy
     * @exception IOException W przypadku błędu użytkownika
     *  @see IOException
     */

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

    /**
     * Metoda zmienia odczytane wartości na skalę RGB
     * @param value Odczytana wartość
     * @return Wartość w skali RGB
     */

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

    /**
     * Metoda implementuje HashMapę przyjmującą obiekty typu String związane z nazwą maski i tablicę dwywymiarową typu double zawierającą parametry maski
     * @return HashMap
     */

    private HashMap<String, double[][]> buildMaskMap() {
        HashMap<String, double[][]> maskMap;
        maskMap = new HashMap<>();

        maskMap.put(VERTICAL, VERTICAL_MASK);
        maskMap.put(HORIZONTAL, HORIZONTAL_MASK);
        maskMap.put(SOBEL_VERTICAL, SOBEL_MASK_VERTICAL);
        maskMap.put(SOBEL_HORIZONTAL, SOBEL_MASK_HORIZONTAL);
        maskMap.put(SCHARR_VERTICAL, SCHARR_MASK_VERTICAL);
        maskMap.put(SCHARR_HORIZONTAL, SCHARR_MASK_HORIZONTAL);
        return maskMap;
    }
}
