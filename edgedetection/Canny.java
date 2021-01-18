package edgedetection;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;

public class Canny {

    private static final double higherThreshold = 0.15*294;
    private static final double lowerThreshold = 0.05*294;

    private static final double[][] xGradientKernel = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
    private static final double[][] yGradientKernel = {{1, 2, 1}, {0, 0, 0}, {-1, -2, -1}};
    public static final double[][] gaussianKernel = {{2./159, 4./159, 5./159, 4./159, 2./159}, {4./159, 9./159, 12./159, 9./159, 4./159}, {5./159, 12./159, 15./159, 12./159, 5./159}, {4./159, 9./159, 12./159, 9./159, 4./159}, {2./159, 4./159, 5./159, 4./159, 2./159}};


    public File detectEdges(BufferedImage sourceImage) throws IOException {
        //BufferedImage sourceImage = readImage(pathname);
        double[][][] pixelArray = convertToArray(sourceImage);
        System.out.println("pixelArray " + String.valueOf(pixelArray.length) + ' ' + String.valueOf(pixelArray[0].length) + ' ' +String.valueOf(pixelArray[0][0].length));
        print_stats(pixelArray);

        double[][] grayscaleArray = convertToGrayscale(pixelArray);
        System.out.println("grayscaleArray " + String.valueOf(grayscaleArray.length) + ' ' + String.valueOf(grayscaleArray[0].length));
        print_stats(grayscaleArray);

        double[][] denoisedArray = applyKernel(grayscaleArray, gaussianKernel);
        System.out.println("denoisedArray " + String.valueOf(denoisedArray.length) + ' ' + String.valueOf(denoisedArray[0].length));
        print_stats(denoisedArray);
        double[][] xGradient = applyKernel(denoisedArray, xGradientKernel);
        System.out.println("xGradient " + String.valueOf(xGradient.length) + ' ' + String.valueOf(xGradient[0].length));
        print_stats(xGradient);

        double[][] yGradient = applyKernel(denoisedArray, yGradientKernel);
        System.out.println("yGradient " + String.valueOf(yGradient.length) + ' ' + String.valueOf(yGradient[0].length));
        print_stats(yGradient);

        double[][] magnitude = computeMagnitude(xGradient, yGradient);
        System.out.println("magnitude " + String.valueOf(magnitude.length) + ' ' + String.valueOf(magnitude[0].length));
        print_stats(magnitude);

        int[][] direction = computeDirection(xGradient, yGradient);
        System.out.println("direction " + String.valueOf(direction.length) + ' ' + String.valueOf(direction[0].length));
        print_stats(direction);

        double[][] suppressedMagnitude = nonMaximumSuppression(direction, magnitude);
        System.out.println("suppressedMagnitude " + String.valueOf(suppressedMagnitude.length) + ' ' + String.valueOf(suppressedMagnitude[0].length));
        print_stats(suppressedMagnitude);


        double[][] thresholdFlags = setStrengthFlag(suppressedMagnitude);
        System.out.println("thresholdFlags " + String.valueOf(thresholdFlags.length) + ' ' + String.valueOf(thresholdFlags[0].length));

        double[][] connected = checkWeakPixelConnection(thresholdFlags, suppressedMagnitude);
        System.out.println("connected " + String.valueOf(connected.length) + ' ' + String.valueOf(connected[0].length));

        return createImageFromMatrix(connected);

    }
    private void print_stats(double[][] array){
        double min = 1000000000000.0, max=0.0;
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                double value = array[i][j];
                if (value < min){min = value;}
                if (value > max){max = value;}
            }
        }
        System.out.println("min " + String.valueOf(min) + " max " + String.valueOf(max));

    }
    private void print_stats(double[][][] array){
        double min = 1000000000000.0, max=0.0;
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                for (int k = 0; k < array[0][0].length; k++) {
                    double value = array[i][j][k];
                    if (value < min) {
                        min = value;
                    }
                    if (value > max) {
                        max = value;
                    }

                }
            }
        }
        System.out.println("min " + String.valueOf(min) + " max " + String.valueOf(max));

    }

    private void print_stats(int[][] array){
        int min = 1000000000, max=0;
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                int value = array[i][j];
                if (value < min){min = value;}
                if (value > max){max = value;}
            }
        }
        System.out.println("min " + String.valueOf(min) + " max " + String.valueOf(max));

    }

    private BufferedImage readImage(String pathname) {
        File imageFile = new File(pathname);
        BufferedImage sourceImage = null;
        try {
            sourceImage = ImageIO.read(imageFile);
        } catch (IOException e) {
            System.err.println("Blad odczytu obrazka");
            e.printStackTrace();
        }
        return sourceImage;
    }

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

    private double[][] applyKernel(double[][] pixelArray, double[][] kernel){
        int inputWidth = pixelArray.length;
        int inputHeight = pixelArray[0].length;
        int kernelWidth = kernel.length;
        int kernelHeight = kernel[0].length;
        int gapWidth = (int) ((kernelWidth) / 2.0);
        int gapHeight = (int) ((kernelHeight) / 2.0);
        double[][] biggerPixelArray = createZerosArray(inputWidth + gapWidth*2, inputHeight + gapHeight*2);


        for (int i = gapWidth; i < inputWidth + gapWidth; ++i) {
            for (int j = gapHeight; j < inputHeight + gapHeight; ++j) {
                biggerPixelArray[i][j] = pixelArray[i-gapWidth][j - gapHeight];
            }
        }
        double[][] afterConv = applyConvolution(biggerPixelArray, kernel);
        if ((afterConv.length != pixelArray.length) || (afterConv[0].length != pixelArray[0].length)){
            System.out.println(
                    String.valueOf(afterConv.length) + " " + String.valueOf(pixelArray.length) + " " + String.valueOf(afterConv[0].length) + " " +String.valueOf(pixelArray.length));
        }
        return afterConv;

    }
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
                output[i-gapWidth][j-gapHeight] = returnConvValue(input, i,j, kernel);
            }
        }

        return output;
    }
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

    //atan2 returns value from -pi, pi
    private int[][] computeDirection(double[][] xGradient, double[][] yGradient){
        int width = xGradient.length;
        int height = xGradient[0].length;
        int roundedDirection = 0;
        int[][] direction = new int[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                double pixelDirection = Math.atan2(xGradient[i][j], yGradient[i][j]);
                roundedDirection = roundDirection(pixelDirection);
                direction[i][j] = roundedDirection;
            }
        }
        return direction;
    }

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
    private double[][] createZerosArray(int width, int height){
        double[][] array = new double[width][height];
        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                array[i][j] = 0;
            }
        }
        return array;
    }

    private double[][] nonMaximumSuppression(int[][] direction, double[][] magnitude) {
        int width = magnitude.length;
        int height = magnitude[0].length;
        double[][] suppressedMagnitude = createZerosArray(width, height);

        for (int i = 1; i < width-1; ++i) {
            for (int j = 1; j < height-1; ++j) {
                suppressedMagnitude[i][j] = magnitude[i][j];
            }
        }

        for (int i = 1; i < width-1; ++i) {
            for (int j = 1; j < height-1; ++j) {
                if (direction[i][j] == 0) {
                    if ((magnitude[i][j] > magnitude[i-1][j]) && (magnitude[i][j] > magnitude[i+1][j])) {
                        suppressedMagnitude[i][j] = magnitude[i][j];
                    }
                }
                else if (direction[i][j] == 45) {
                    if ((magnitude[i][j] > magnitude[i-1][j+1]) && (magnitude[i][j] > magnitude[i+1][j-1])) {
                        suppressedMagnitude[i][j] = magnitude[i][j];
                    }
                }
                else if (direction[i][j] == 90) {
                    if ((magnitude[i][j] > magnitude[i][j+1]) && (magnitude[i][j] > magnitude[i][j-1])) {
                        suppressedMagnitude[i][j] = magnitude[i][j];
                    }
                }
                else if (direction[i][j] == 135) {
                    if ((magnitude[i][j] > magnitude[i+1][j+1]) && (magnitude[i][j] > magnitude[i-1][j-1])) {
                        suppressedMagnitude[i][j] = magnitude[i][j];
                    }
                }
            }
        }
        return suppressedMagnitude;
    }

    //flag values: strong pixel magnitude (above higher threshold) = 1,
    //weak pixel (above lower threshold) = 0,5
    //suppressed pixel (under lower threshold) = 0
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
