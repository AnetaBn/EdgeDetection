package edgedetection;

import java.io.IOException;

public class RunEdgeDetection {

    public static void main(String[] args) throws IOException {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        new EdgeDetectionUI();
    }
}
