package edgedetection;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import static edgedetection.EdgeDetection.*;

/**
 * Klasa zawierająca GUI programu, ActionListenery i wczytywanie obrazu
 * @author Aneta Bień, Anna Plęs
 */

public class EdgeDetectionUI {

    /**
     * Zestaw argumentów final i deklaracja obiektów JPanel i ImagePanel
     */

    private static final int FRAME_WIDTH = 1400;
    private static final int FRAME_HEIGHT = 600;
    private static final Font sansSerifBold = new Font("SansSerif", Font.BOLD, 22);
    private  ImagePanel sourceImage = new ImagePanel(".\\Obraz1.jpg");
    private  ImagePanel destImage = new ImagePanel(".\\Obraz1.jpg");
    private JPanel mainPanel;
    private final EdgeDetection edgeDetection;

    /**
     * Metoda implementuje GUI
     */

    public EdgeDetectionUI() throws IOException {

        edgeDetection = new EdgeDetection();
        JFrame mainFrame = createMainFrame();

        mainPanel = new JPanel(new GridLayout(1, 2));
        mainPanel.add(sourceImage);
        mainPanel.add(destImage);

        JPanel northPanel = fillNorthPanel();

        mainFrame.add(northPanel, BorderLayout.NORTH);
        mainFrame.add(mainPanel, BorderLayout.CENTER);
        mainFrame.add(new JScrollPane(mainPanel), BorderLayout.CENTER);
        mainFrame.setVisible(true);

    }

    /**
     * Metoda implementująca JPanel
     * @return NorthPanel Północny panel
     */

    private JPanel fillNorthPanel() {
        JButton chooseButton = new JButton("Wybierz obraz");
        chooseButton.setFont(sansSerifBold);

        JPanel northPanel = new JPanel();

        JComboBox filterChoice = new JComboBox();
        filterChoice.addItem(HORIZONTAL);
        filterChoice.addItem(VERTICAL);
        filterChoice.addItem(SOBEL_VERTICAL);
        filterChoice.addItem(SOBEL_HORIZONTAL);
        filterChoice.addItem(SCHARR_VERTICAL);
        filterChoice.addItem(SCHARR_HORIZONTAL);
        filterChoice.addItem(CANNY_EDGE_DETECTION);
        filterChoice.setFont(sansSerifBold);


        JTextField lowerThreshold= new JTextField();
        lowerThreshold.setPreferredSize(new Dimension(250, 40));
        lowerThreshold.setFont(sansSerifBold);
        lowerThreshold.setText("Dolny próg (Canny)");
        lowerThreshold.setEditable(false);

        lowerThreshold.addFocusListener(new FocusListener() {
            @Override public void focusLost(final FocusEvent pE) {}
            @Override public void focusGained(final FocusEvent pE) {
                lowerThreshold.selectAll();
            }
        });

        JTextField higherThreshold= new JTextField();
        higherThreshold.setPreferredSize(new Dimension(250, 40));
        higherThreshold.setFont(sansSerifBold);
        higherThreshold.setText("Górny próg (Canny)");
        higherThreshold.setEditable(false);


        higherThreshold.addFocusListener(new FocusListener() {
            @Override public void focusLost(final FocusEvent pE) {}
            @Override public void focusGained(final FocusEvent pE) {
                higherThreshold.selectAll();
            }
        });


        filterChoice.addActionListener (new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(((String) filterChoice.getSelectedItem()).equals(CANNY_EDGE_DETECTION)){
                    lowerThreshold.setEditable(true);
                    higherThreshold.setEditable(true);

                }
                else{
                    lowerThreshold.setEditable(false);
                    higherThreshold.setEditable(false);
                    lowerThreshold.setText("Dolny próg (Canny)");
                    higherThreshold.setText("Górny próg (Canny)");
                }
            }
        });

        JButton detect = new JButton("Wykryj krawedzie");
        detect.setFont(sansSerifBold);

        northPanel.add(filterChoice);
        northPanel.add(chooseButton);
        northPanel.add(lowerThreshold);
        northPanel.add(higherThreshold);
        northPanel.add(detect);


        chooseButton.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(".\\"));
            int action = chooser.showOpenDialog(null);
            if (action == JFileChooser.APPROVE_OPTION) {
                try {
                    sourceImage = new ImagePanel(chooser.getSelectedFile().getAbsolutePath());
                    mainPanel.removeAll();
                    mainPanel.add(sourceImage);
                    mainPanel.add(destImage);
                    mainPanel.updateUI();
                } catch (Exception e) {
                    System.err.println("Blad wczytania panelu.");
                    throw new RuntimeException(e);
                }
            }
        });

        detect.addActionListener(event -> {
            try {
                BufferedImage bufferedImage = ImageIO.read(new File(sourceImage.getcurrentpath()));
                double lowerThresholdValue = readThreshold(lowerThreshold.getText());
                double higherThresholdValue = readThreshold(higherThreshold.getText());
                if ((lowerThresholdValue < 0 ) && (filterChoice.getSelectedItem().equals(CANNY_EDGE_DETECTION))) {
                    lowerThresholdValue = LOWER_THRESHOLD;
                    lowerThreshold.setText(String.valueOf(lowerThresholdValue));
                }
                if ((higherThresholdValue < 0) && (filterChoice.getSelectedItem().equals(CANNY_EDGE_DETECTION))) {
                    higherThresholdValue = HIGHER_THRESHOLD;
                    higherThreshold.setText(String.valueOf(higherThresholdValue));
                }
                if (lowerThresholdValue > higherThresholdValue){
                    lowerThresholdValue = LOWER_THRESHOLD;
                    lowerThreshold.setText(String.valueOf(lowerThresholdValue));
                    higherThresholdValue = HIGHER_THRESHOLD;
                    higherThreshold.setText(String.valueOf(higherThresholdValue));
                }
                File mixedFile = edgeDetection.detectEdges(bufferedImage, (String) filterChoice.getSelectedItem(),
                        lowerThresholdValue, higherThresholdValue);
                destImage = new ImagePanel(mixedFile.getAbsolutePath());
                mainPanel.removeAll();
                mainPanel.add(sourceImage);
                mainPanel.add(destImage);
                mainPanel.updateUI();
            } catch (IOException e) {
                System.out.println("Bląd detekcji krawędzi.");
                throw new RuntimeException(e);
            }
        });

        return northPanel;
    }

    /**
     * Metoda pobiera wartości progów dla algorytmu Canny'ego
     * @return Zwraca wartość -1 w przypadku wystapienia wyjątku
     * @exception IOException W przypadku źle podanej wartości
     */

    private double readThreshold(String text){
        try{
            return Double.parseDouble(text);
        } catch (NumberFormatException nfe)
        {
            return -1.0;
        }
    }

    /**
     * Metoda tworzy JFrame i implementuje WindowListener
     * @return mainFrame Zwraca obiekt JFrame
     */

    private JFrame createMainFrame() {
        JFrame mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        mainFrame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        mainFrame.getContentPane().setBackground(Color.black);
        mainFrame.setTitle("Detekcja krawędzi");
        mainFrame.setLocationRelativeTo(null);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
        return mainFrame;
    }

    /**
     * Klasa implementuje panel wyswietlania obrazu i sposób wczytywania obrazu
     */

    public class ImagePanel extends JPanel {

        private BufferedImage image;
        private String currentpath;
        public File imageFile;

        /**
         * Metoda obsługuje odczyt obrazka
         * @exception IOException W przypadku błędu odczytu obrazka
         */

        public ImagePanel(String sourceImage) {
            super();
            currentpath = sourceImage;
            imageFile = new File(sourceImage);
            try {
                image = ImageIO.read(imageFile);

            } catch (IOException e) {
                System.err.println("Bląd odczytu obrazka.");
                e.printStackTrace();
            }

            Dimension dimension = new Dimension(500,510);
            setPreferredSize(dimension);

        }

        /**
         * Metoda pobiera ścieżkę do pliku
         * @return currentpath ścieżka do pliku
         */

        public String getcurrentpath(){
            return currentpath;
        }

        /**
         * Metoda rysuje obiekt Component
         */

        @Override
        public void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.drawImage(image, 0, 0, this);
        }
    }
}