package edgedetection;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;

public class Canny {
    private static final double[][] xGradientKernel = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
    private static final double[][] yGradientKernel = {{1, 2, 1}, {0, 0, 0}, {-1, -2, -1}};
    public static final double[][] gaussianKernel = {{2./159, 4./159, 5./159, 4./159, 2./159}, {4./159, 9./159, 12./159, 9./159, 4./159}, {5./159, 12./159, 15./159, 12./159, 5./159}, {4./159, 9./159, 12./159, 9./159, 4./159}, {2./159, 4./159, 5./159, 4./159, 2./159}};
    private double lowerThreshold;
    private double higherThreshold;

    public Canny(double lowerThresholdValue, double higherThresholdValue) {
        this.lowerThreshold = lowerThresholdValue;
        this.higherThreshold = higherThresholdValue;
    }


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
                //top left corner
                paddedArray[i][j] = smallArray[gapWidth-1-i][gapHeight-1-j];
                //top right corner
                paddedArray[width-i-1][j] = smallArray[(smallArrayWidth)-gapWidth+i][gapHeight-1-j];
                //bottom left corner
                paddedArray[i][height-j-1] = smallArray[gapWidth-i-1][(smallArrayHeight)-gapHeight+i-1];
                //bottom right corner
                paddedArray[width-i-1][height-j-1] =  smallArray[(smallArrayWidth)-gapWidth+i][(smallArrayHeight)-gapHeight+i];
            }
        }
        //top
        for(int i = gapWidth; i < smallArrayWidth + gapWidth; ++i) {
            for (int j = 0; j < gapHeight; ++j) {
                paddedArray[i][j] = smallArray[i-gapWidth][gapHeight-1-j];
            }
        }
        //left
        for(int i = 0; i < gapWidth; ++i) {
            for (int j = gapHeight; j < smallArrayHeight + gapHeight; ++j) {
                paddedArray[i][j] = smallArray[gapWidth-1-i][j-gapHeight];
            }
        }
        //right
        for(int i = 0; i < gapWidth; ++i) {
            for (int j = gapHeight; j < smallArrayHeight + gapHeight; ++j) {
                paddedArray[width-1-i][j] = smallArray[smallArrayWidth-gapWidth-1+i][j-gapHeight];
            }
        }
        //bottom
        for(int i = gapWidth; i < smallArrayWidth + gapWidth; ++i) {
            for (int j = 0; j < gapHeight; ++j) {
                paddedArray[i][height-1-j] = smallArray[i-gapWidth][smallArrayHeight-gapHeight-1+j];
            }
        }
        //inside
        for(int i = gapWidth; i < smallArrayWidth + gapWidth; ++i) {
            for (int j = gapHeight; j < smallArrayHeight + gapHeight; ++j) {
                paddedArray[i][j] = smallArray[i-gapWidth][j-gapHeight];
            }
        }
        return paddedArray;
    }


    private double[][] applyKernel(double[][] pixelArray, double[][] kernel){
        double[][] biggerPixelArray = createPaddedArray(pixelArray, kernel);
        double[][] afterConv = applyConvolution(biggerPixelArray, kernel);

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
                double pixelDirection = Math.atan2(yGradient[i][j], xGradient[i][j]);
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
