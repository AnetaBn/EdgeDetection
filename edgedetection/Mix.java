package edgedetection;

public class Mix {

    public static double pixelMix(double[][] input, int x, int y, double[][] k, int kernelWidth, int kernelHeight) {
        double output = 0;
        for (int i = 0; i < kernelWidth; ++i) {
            for (int j = 0; j < kernelHeight; ++j) {
                output = output + (input[x + i][y + j] * k[i][j]);
            }
        }
        return output;
    }

    public static double[][] mix2D(double[][] input, int width, int height, double[][] kernel, int kernelWidth, int kernelHeight) {
        int smallWidth = width - kernelWidth + 1;
        int smallHeight = height - kernelHeight + 1;
        double[][] output = new double[smallWidth][smallHeight];
        for (int i = 0; i < smallWidth; ++i) {
            for (int j = 0; j < smallHeight; ++j) {
                output[i][j] = 0;
            }
        }
        for (int i = 0; i < smallWidth; ++i) {
            for (int j = 0; j < smallHeight; ++j) {
                output[i][j] = pixelMix(input, i, j, kernel, kernelWidth, kernelHeight);
            }
        }
        return output;
    }

    public static double[][] mix2DEdge(double[][] input, int width, int height, double[][] kernel, int kernelWidth, int kernelHeight) {
        int smallWidth = width - kernelWidth + 1;
        int smallHeight = height - kernelHeight + 1;
        int top = kernelHeight / 2;
        int left = kernelWidth / 2;

        double[][] small = mix2D(input, width, height,
                kernel, kernelWidth, kernelHeight);
        double large[][] = new double[width][height];
        for (int j = 0; j < height; ++j) {
            for (int i = 0; i < width; ++i) {
                large[i][j] = 0;
            }
        }
        for (int j = 0; j < smallHeight; ++j) {
            for (int i = 0; i < smallWidth; ++i) {
                large[i + left][j + top] = small[i][j];
            }
        }
        return large;
    }


    public double[][] mixNext(double[][] input, int width, int height, double[][] kernel, int kernelWidth, int kernelHeight) {
        double[][] newInput = input.clone();
        double[][] output = input.clone();

        for (int i = 0; i < 1; ++i) {
            output = mix2DEdge(newInput, width, height,
                    kernel, kernelWidth, kernelHeight);
            newInput = output.clone();
        }
        return output;
    }
}
