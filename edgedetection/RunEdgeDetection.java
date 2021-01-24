package edgedetection;

import java.io.IOException;

/**
 * Klasa wywołująca program za pomocą klasy EdgeDetectionUI
 * @author Aneta Bień, Anna Plęs
 */

public class RunEdgeDetection {

    /**
     * Przykład użycia klasy EdgeDetectionUI
     * @param args Nieużywany
     * @exception IOException W przypadku błędu użytkownika wywołuje wyjątek
     */

    public static void main(String[] args) throws IOException {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        new EdgeDetectionUI();
    }
}
