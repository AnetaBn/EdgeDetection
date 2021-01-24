package edgedetection;

/**
 * Klasa obsługuje operację splotu obrazu z maską
 * @author Anna Plęs
 */

public class Mix {

    /**
     * Metoda pobiera obraz (w skali szarości), wyznacza jądro i pozycję oraz stosuje splot w wybranej pozycji
     * @param input Tablica reprezentująca obraz
     * @param x Współrzędna x pozycji splotu
     * @param y Współrzędna y pozycji splotu
     * @param k Tablica dwuwymiarowa reprezentująca jądro
     * @param kernelHeight Wysokość jądra
     * @param kernelWidth Szerokość jądra
     * @return output Nowa wartość piksela po splocie
     */

    public static double pixelMix(double[][] input, int x, int y, double[][] k, int kernelWidth, int kernelHeight) {
        double output = 0;
        for (int i = 0; i < kernelWidth; ++i) {
            for (int j = 0; j < kernelHeight; ++j) {
                output = output + (input[x + i][y + j] * k[i][j]);
            }
        }
        return output;
    }

    /**
     * Metoda pobiera tablicę 2D poziomów szarości oraz jądro i stosuje splot nad obszarem obrazu określonym przez szerokość i wysokość
     * @param input Podwójna tablica 2D reprezentująca obrazy
     * @param width Szerokość obrazu
     * @param height Wysokość obrazu
     * @param kernel Tablica dwuwymiarowa reprezentująca jądro
     * @param kernelHeight Wysokość jądra
     * @param kernelWidth Szerokość jądra
     * @return output Tablica 2D reprezentująca nowy obraz
     */

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

    /**
     * Metoda pobiera tablicę 2D poziomów szarości oraz jądro i stosuje splot nad obszarem obrazu określonym przez szerokość i wysokość i zwraca część obrazu wyjściowego
     * @param input Podwójna tablica 2D reprezentująca obraz
     * @param width Szerokość obrazu
     * @param height Wysokość obrazu
     * @param kernel Tablica dwuwymiarowa reprezentująca jądro
     * @param kernelHeight Wysokość jądra
     * @param kernelWidth Szerokość jądra
     * @return large Tablica 2D reprezentująca nowy obraz
     */

    public static double[][] mix2DEdge(double[][] input, int width, int height, double[][] kernel, int kernelWidth, int kernelHeight) {
        int smallWidth = width - kernelWidth + 1;
        int smallHeight = height - kernelHeight + 1;
        int top = kernelHeight / 2;
        int left = kernelWidth / 2;

        double[][] small = mix2D(input, width, height, kernel, kernelWidth, kernelHeight);
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

    /**
     * Metoda stosuje mix2DEdge dla tablicy wejściowej
     * @param input Tablica reprezentująca obraz
     * @param width Szerokość obrazu
     * @param height Wysokość obrazu
     * @param kernel Tablica 2D reprezentująca obraz
     * @param kernelHeight Wysokość jądra
     * @param kernelWidth Szerokość jądra
     * @return output Tablica 2D reprezentująca nowy obraz
     */

    public double[][] mixNext(double[][] input, int width, int height, double[][] kernel, int kernelWidth, int kernelHeight) {
        double[][] newInput = input.clone();
        double[][] output = input.clone();

        for (int i = 0; i < 1; ++i) {
            output = mix2DEdge(newInput, width, height, kernel, kernelWidth, kernelHeight);
            newInput = output.clone();
        }
        return output;
    }
}
