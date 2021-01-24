package edgedetection;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;

/**
 * Klasa przetwarzająca obrazy za pomocą algorytmu Canny'ego
 * @author Anna Plęs
 */
public class Canny {
    private static final double[][] xGradientKernel = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
    private static final double[][] yGradientKernel = {{1, 2, 1}, {0, 0, 0}, {-1, -2, -1}};
    public static final double[][] gaussianKernel = {{2./159, 4./159, 5./159, 4./159, 2./159}, {4./159, 9./159, 12./159, 9./159, 4./159}, {5./159, 12./159, 15./159, 12./159, 5./159}, {4./159, 9./159, 12./159, 9./159, 4./159}, {2./159, 4./159, 5./159, 4./159, 2./159}};
    private double lowerThreshold;
    private double higherThreshold;

    /**
     * Metoda pozwalająca na ustawienie wartosci wyzszego i nizszego progu klasy Canny
     * @param lowerThresholdValue wartość niższego progu
     * @param higherThresholdValue wartość wyższego progu
     */
    public Canny(double lowerThresholdValue, double higherThresholdValue) {
        this.lowerThreshold = lowerThresholdValue;
        this.higherThreshold = higherThresholdValue;
    }

    /**
     * Główna metoda będąca ciągiem kolejnych kroków algorytmu
     * @param sourceImage wczytany obraz wejsciowy
     * @return obraz przetworzony przez algorytm
     * @throws IOException błąd wejścia/wyjścia na jednym z etapów algorytmu
     */
    public File detectEdges(BufferedImage sourceImage) throws IOException {
        double[][][] pixelArray = convertToArray(sourceImage);
        double[][] grayscaleArray = convertToGrayscale(pixelArray);
        double[][] denoisedArray = applyKernel(grayscaleArray, gaussianKernel);
        double[][] xGradient = applyKernel(denoisedArray, xGradientKernel);
        double[][] yGradient = applyKernel(denoisedArray, yGradientKernel);
        double[][] magnitude = computeMagnitude(xGradient, yGradient);
        int[][] direction = computeDirection(xGradient, yGradient);
        double[][] suppressedMagnitude = nonMaximumSuppression(direction, magnitude);
        double[][] thresholdFlags = setStrengthFlag(suppressedMagnitude);
        double[][] connected = checkWeakPixelConnection(thresholdFlags, suppressedMagnitude);
        return createImageFromMatrix(connected);
    }

    /** Metoda pozwalająca na konwersję obrazka na macierz zawierającą wartości pikseli
     * @param image obraz wejściowy
     * @return macierz wartosci pikseli
     */
    private double[][][] convertToArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        double[][][] pixelArray = new double[width][height][3];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Color color = new Color(image.getRGB(i, j));
                pixelArray[i][j][0] = color.getRed();
                pixelArray[i][j][1] = color.getGreen();
                pixelArray[i][j][2] = color.getBlue();
            }
        }
        return pixelArray;
    }

    /**
     * Metoda pozwalająca na konwersję obrazu do skali szarości
     * @param pixelArray trójkanałowa macierz wartości pikseli
     * @return macierz wartości pikseli w skali szarości
     */
    private double[][] convertToGrayscale(double[][][] pixelArray){
        int width = pixelArray.length;
        int height = pixelArray[0].length;
        double[][] grayscaleArray = new double[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                double gray = (pixelArray[i][j][0] + pixelArray[i][j][1] + pixelArray[i][j][2])/3;
                grayscaleArray[i][j] = gray;
            }
        }
        return grayscaleArray;
    }

    /**
     * Metoda pozwalająca na tworzenie macierzy wypełnionej zerami
     * @param width pożądana szerokość macierzy
     * @param height pożądana wysokość macierzy
     * @return stworzona macierz wypełniona zerami
     */
    private double[][] createZerosArray(int width, int height){
        double[][] array = new double[width][height];
        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                array[i][j] = 0;
            }
        }
        return array;
    }

    //creates padding using a mirror reflection of the border pixels

    /**
     * Metoda pozwalająca na stworzenie obramowania przy pomocy odbicia lustrzanego pikseli granicznych obrazu
     * @param smallArray macierz obrazka mającego zostać poddanego działaniu metody
     * @param kernel jądro maski
     * @return macierz wejściowa o wymiarach macierzy wejściowej + wymiar maski - 1
     */
    private double[][] createPaddedArray(double[][] smallArray, double[][] kernel){
        int smallArrayWidth = smallArray.length;
        int smallArrayHeight = smallArray[0].length;
        int kernelWidth = kernel.length;
        int kernelHeight = kernel[0].length;
        int gapWidth = (int) ((kernelWidth) / 2.0);
        int gapHeight = (int) ((kernelHeight) / 2.0);
        int width = smallArrayWidth + 2*gapWidth;
        int height = smallArrayHeight + 2*gapHeight;

        double[][] paddedArray = new double[width][height];
        for(int i = 0; i < gapWidth; ++i){
            for(int j = 0; j < gapHeight; ++j){
                //górny lewy róg
                paddedArray[i][j] = smallArray[gapWidth-1-i][gapHeight-1-j];
                //górny prawy róg
                paddedArray[width-i-1][j] = smallArray[(smallArrayWidth)-gapWidth+i][gapHeight-1-j];
                //dolny lewy róg
                paddedArray[i][height-j-1] = smallArray[gapWidth-i-1][(smallArrayHeight)-gapHeight+i-1];
                //dolny prawy róg
                paddedArray[width-i-1][height-j-1] =  smallArray[(smallArrayWidth)-gapWidth+i][(smallArrayHeight)-gapHeight+i];
            }
        }
        //górny pas
        for(int i = gapWidth; i < smallArrayWidth + gapWidth; ++i) {
            for (int j = 0; j < gapHeight; ++j) {
                paddedArray[i][j] = smallArray[i-gapWidth][gapHeight-1-j];
            }
        }
        //lewy pas
        for(int i = 0; i < gapWidth; ++i) {
            for (int j = gapHeight; j < smallArrayHeight + gapHeight; ++j) {
                paddedArray[i][j] = smallArray[gapWidth-1-i][j-gapHeight];
            }
        }
        //prawy pas
        for(int i = 0; i < gapWidth; ++i) {
            for (int j = gapHeight; j < smallArrayHeight + gapHeight; ++j) {
                paddedArray[width-1-i][j] = smallArray[smallArrayWidth-gapWidth-1+i][j-gapHeight];
            }
        }
        //dolny pas
        for(int i = gapWidth; i < smallArrayWidth + gapWidth; ++i) {
            for (int j = 0; j < gapHeight; ++j) {
                paddedArray[i][height-1-j] = smallArray[i-gapWidth][smallArrayHeight-gapHeight-1+j];
            }
        }
        //wnętrze obrazka
        for(int i = gapWidth; i < smallArrayWidth + gapWidth; ++i) {
            for (int j = gapHeight; j < smallArrayHeight + gapHeight; ++j) {
                paddedArray[i][j] = smallArray[i-gapWidth][j-gapHeight];
            }
        }
        return paddedArray;
    }

    /**
     * Metoda pozwalająca na stworzenie paddingu i zaaplikowanie konwolucji
     * poprzez wywołanie metody applyConvolution()
     * @param pixelArray macierz obrazu wejściowego
     * @param kernel maska, która ma zostać zaaplikowana
     * @return macierz obrazu po konwolucji
     */
    private double[][] applyKernel(double[][] pixelArray, double[][] kernel){
        double[][] biggerPixelArray = createPaddedArray(pixelArray, kernel);
        return applyConvolution(biggerPixelArray, kernel);
    }

    /**
     * Metoda pozwalająca zaaplikowanie konwolucji dzięki użyciu funkcji returnConvValue()
     * @param input macierz obrazu wejściowego z uwzględnionym paddingiem
     * @param kernel maska, która ma zostać zaaplikowana
     * @return macierz będąca wynikiem konwolucji
     */
    private double[][] applyConvolution(double[][] input, double[][] kernel){
        int width = input.length;
        int height = input[0].length;
        int kernelWidth = kernel.length;
        int kernelHeight = kernel[0].length;
        int gapWidth = (int) ((kernelWidth) / 2.0);
        int gapHeight = (int) ((kernelHeight) / 2.0);
        double[][] output = new double[width - 2*gapWidth][height - 2*gapHeight];
        for (int i = gapWidth; i < width - gapWidth; ++i) {
            for (int j = gapHeight; j < height - gapHeight; ++j) {
                output[i-gapWidth][j-gapHeight] = returnConvValue(input, i, j, kernel);
            }
        }
        return output;
    }

    /**
     * Metoda pozwalająca na obliczenie wartości wartości konkretnego piksela,
     * będącego wynikiem splotu obrazu z maską
     * @param input macierz obrazu wejściowego z uwzględnionym paddingiem
     * @param x wartość współrzędnej x dla obliczanego piksela
     * @param y wartość współrzędnej y dla obliczanego piksela
     * @param kernel aplikowana maska
     * @return wartość piksela po splocie obrazu z maską
     */
    private double returnConvValue(double[][] input, int x, int y, double[][] kernel){
        double output = 0;
        int kernelWidth = kernel.length;
        int kernelHeight = kernel[0].length;
        int gapWidth = (int) ((kernelWidth) / 2.0);
        int gapHeight = (int) ((kernelHeight) / 2.0);
        for (int i = 0; i < kernel.length; ++i) {
            for (int j = 0; j < kernel[0].length; ++j) {
                output = output + (input[x + i - gapWidth][y + j - gapHeight] * kernel[i][j]);
            }
        }
        return output;
    }

    /**
     * Metoda pozwalająca na obliczenie natężenia gradientu obrazu
     * @param xGradient macierz będąca wynikiem splotu macierzy z maską filtru horyzontalnego
     * @param yGradient macierz będąca wynikiem splotu macierzy z maską filtru wertykalnego
     * @return macierz natężeń gradietu obrazu
     */
    private double[][] computeMagnitude(double[][] xGradient, double[][] yGradient){
        int width = xGradient.length;
        int height = xGradient[0].length;
        double[][] magnitude = new double[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                double pixelMagnitude = Math.sqrt(Math.pow(xGradient[i][j], 2) + Math.pow(yGradient[i][j], 2));
                magnitude[i][j] = pixelMagnitude;
            }
        }
        return magnitude;
    }

    /**
     * Metoda pozwalająca na zwrócenie kierunku krawędzi
     * zaokrąglona do jednej z czterech możliwych wartości wynikowych metody roundDirection()
     * @param xGradient macierz będąca wynikiem splotu macierzy z maską filtru horyzontalnego
     * @param yGradient macierz będąca wynikiem splotu macierzy z maską filtru wertykalnego
     * @return macierz kierunków krawędzi
     */
    private int[][] computeDirection(double[][] xGradient, double[][] yGradient){
        int width = xGradient.length;
        int height = xGradient[0].length;
        int roundedDirection = 0;
        int[][] direction = new int[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                double pixelDirection = Math.atan2(yGradient[i][j], xGradient[i][j]);
                roundedDirection = roundDirection(pixelDirection);
                direction[i][j] = roundedDirection;
            }
        }
        return direction;
    }

    /**
     * Metoda pozwalająca na zaokrąglanie kierunków krawędzi
     * @param pixelDirection wartość kierunku dla danego poksela z zakresu od -pi do pi
     * @return jedna z czterech możliwych wartości kierunku
     */
    private int roundDirection(double pixelDirection){
        double absDirection = Math.abs(pixelDirection);
        int roundedDirection = 0;
        absDirection = Math.toDegrees(absDirection);
        if((absDirection >= 0 && absDirection < 22.5) || (absDirection >= 157.5 && absDirection <= 180)){
            roundedDirection = 0;
        }
        else if(absDirection >= 22.5 && absDirection < 67.5){
            roundedDirection = 45;
        }
        else if(absDirection >= 67.5 && absDirection < 112.5){
            roundedDirection = 90;
        }
        else if(absDirection >= 112.5 && absDirection < 157.5){
            roundedDirection = 135;
        }
        else{
            String value = String.valueOf(pixelDirection);
            throw new RuntimeException("Zły argument pixelDirection: " + value);
        }
        return roundedDirection;
    }

    /**
     * Metoda pozwalająca na pocienianie wykrytych krawędzi, poprzez zerowanie pikseli niemaksymalnych
     * @param direction macierz wartości kierunków krawędzi
     * @param magnitude macierz natężeń gradietu obrazu
     * @return macierz pocienionych krawędzi
     */
    private double[][] nonMaximumSuppression(int[][] direction, double[][] magnitude) {
        int width = magnitude.length;
        int height = magnitude[0].length;
        double[][] suppressedMagnitude = createZerosArray(width, height);

        for (int i = 1; i < width-1; ++i) {
            for (int j = 1; j < height-1; ++j) {
                if (direction[i][j] == 0) {
                    if ((magnitude[i][j] > magnitude[i-1][j]) && (magnitude[i][j] >   magnitude[i+1][j])) {
                        suppressedMagnitude[i][j] = magnitude[i][j];
                    }
                }
                else if (direction[i][j] == 45) {
                    if ((magnitude[i][j] > magnitude[i-1][j-1]) && (magnitude[i][j] > magnitude[i+1][j+1])) {
                        suppressedMagnitude[i][j] = magnitude[i][j];
                    }
                }
                else if (direction[i][j] == 90) {
                    if ((magnitude[i][j] > magnitude[i][j+1]) && (magnitude[i][j] > magnitude[i][j-1])) {
                        suppressedMagnitude[i][j] = magnitude[i][j];
                    }
                }
                else if (direction[i][j] == 135) {
                    if ((magnitude[i][j] > magnitude[i-1][j+1]) && (magnitude[i][j] > magnitude[i+1][j-1])) {
                        suppressedMagnitude[i][j] = magnitude[i][j];
                    }
                }
            }
        }
        return suppressedMagnitude;
    }

    /**
     * Metoda pozwalająca na ustawienie jednej z trzech wartości "siły" piksela
     * @param suppressedMagnitude
     * @return wartość siły piksela:
     * 1 dla piksela powyżej wyższego progu
     * 0.5 dla piksela powyżej niższego progu, a poniżej wyższego
     * 0 dla piksela poniżej niższego progu
     */
    private double[][] setStrengthFlag(double[][] suppressedMagnitude){
        int width = suppressedMagnitude.length;
        int height = suppressedMagnitude[0].length;
        double[][] thresholdFlags = new double[width][height];
        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                if (suppressedMagnitude[i][j] >= higherThreshold){
                    thresholdFlags[i][j] = 1;
                }
                else if (suppressedMagnitude[i][j] >= lowerThreshold){
                    thresholdFlags[i][j] = 0.5;
                }
                else if (suppressedMagnitude[i][j] < lowerThreshold){
                    thresholdFlags[i][j] = 0;
                }
            }
        }
        return thresholdFlags;
    }

    /**
     * Metoda pozwalająca na zachowanie większej ciągłości krawędzi
     * poprzez sprawdzenie czy wokół piksela oznaczonego jako 0.5 znajduje się piksel z siłą równą 1
     * @param thresholdFlags macierz flag dla pikseli obrazów
     * @param suppressedMagnitude macierz wartości pikseli po pocienieniu krawędzi
     * @return wartości jasności pikseli po sprawdzeniu ich połączeń
     */
    private double[][] checkWeakPixelConnection(double[][] thresholdFlags, double[][] suppressedMagnitude) {
        int width = suppressedMagnitude.length;
        int height = suppressedMagnitude[0].length;
        double[][] connected = createZerosArray(width, height);
        for (int i = 1; i < width - 1; ++i) {
            for (int j = 1; j < height - 1; ++j) {
                if (thresholdFlags[i][j] == 0) {
                    connected[i][j] = 0;
                } else if (thresholdFlags[i][j] == 1) {
                    connected[i][j] = 255;
                } else if (thresholdFlags[i][j] == 0.5) {
                    if ((thresholdFlags[i - 1][j] == 1) || (thresholdFlags[i + 1][j] == 1)) {
                        connected[i][j] = 255;
                    } else if ((thresholdFlags[i][j - 1] == 1) || (thresholdFlags[i][j + 1] == 1)) {
                        connected[i][j] = 255;
                    } else if ((thresholdFlags[i - 1][j - 1] == 1) || (thresholdFlags[i + 1][j + 1] == 1)) {
                        connected[i][j] = 255;
                    } else if ((thresholdFlags[i - 1][j + 1] == 1) || (thresholdFlags[i + 1][j - 1] == 1)) {
                        connected[i][j] = 255;
                    }
                }
            }
        }

        for (int i = 1; i < width - 1; ++i) {
            for (int j = 1; j < height - 1; ++j) {
                if (connected[i][j] == 0.5) {
                    connected[i][j] = 0;
                }
            }
        }
        double[][] connectedPixels = new double[width][height];
        for (int i = 1; i < width - 1; ++i) {
            for (int j = 1; j < height - 1; ++j) {
                connectedPixels[i - 1][j - 1] = connected[i][j];
            }
        }
        return connectedPixels;
    }

    /**
     * Metoda pozwalająca na stworzenie pliku obrazu z macierzy wartości pikseli
     * @param array macierz wartości pikseli
     * @return plik stworzonego obrazu
     * @throws IOException błąd na etapie zapisu pliku
     */
    private File createImageFromMatrix(double[][] array) throws IOException {
        int width = array.length;
        int height = array[0].length;
        BufferedImage edgeImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int pixel = (int) array[i][j];
                Color color = new Color(pixel, pixel, pixel);
                edgeImage.setRGB(i, j, color.getRGB());
            }
        }
        String g = "outputimage" + LocalTime.now();
        g = g.replace('.', '_').replace(':', '_');
        g = ".\\" + g + ".jpg";
        File outputFile = new File(g);
        ImageIO.write(edgeImage, "jpg", outputFile);
        return outputFile;
    }
}
