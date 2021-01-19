package edgedetection;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.DoubleBuffer;

import static edgedetection.EdgeDetection.*;
import static edgedetection.EdgeDetection.higherThreshold;

public class EdgeDetectionUI {

    private static final int FRAME_WIDTH = 1200;
    private static final int FRAME_HEIGHT = 600;
    private static final Font sansSerifBold = new Font("SansSerif", Font.BOLD, 22);
    private  ImagePanel sourceImage = new ImagePanel(".\\Obraz1.jpg");
    private  ImagePanel destImage = new ImagePanel(".\\Obraz1.jpg");
    private JPanel mainPanel;
    private final EdgeDetection edgeDetection;


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

    private JPanel fillNorthPanel() {
        JButton chooseButton = new JButton("Wybierz obraz");
        chooseButton.setFont(sansSerifBold);

        JPanel northPanel = new JPanel();

        JComboBox filterChoice = new JComboBox();
        filterChoice.addItem(Horizontal);
        filterChoice.addItem(Vertical);
        filterChoice.addItem(SobelVertical);
        filterChoice.addItem(SobelHorizontal);
        filterChoice.addItem(ScharrVertical);
        filterChoice.addItem(ScharrHorizontal);
        filterChoice.addItem(CannyEdgeDetection);
        filterChoice.setFont(sansSerifBold);


        JTextField lowerThreshold= new JTextField();
        lowerThreshold.setPreferredSize(new Dimension(250, 40));
        lowerThreshold.setFont(sansSerifBold);
        lowerThreshold.setText("Canny lower threshold");
        lowerThreshold.setEditable(false);

        lowerThreshold.addFocusListener(new FocusListener() {
            @Override public void focusLost(final FocusEvent pE) {}
            @Override public void focusGained(final FocusEvent pE) {
                lowerThreshold.selectAll();
            }
        });

        JTextField higherThreshold= new JTextField();
        higherThreshold.setPreferredSize(new Dimension(250, 40));
        higherThreshold.setFont(sansSerifBold);;
        higherThreshold.setText("Canny higher threshold");
        higherThreshold.setEditable(false);
        higherThreshold.addFocusListener(new FocusListener() {
            @Override public void focusLost(final FocusEvent pE) {}
            @Override public void focusGained(final FocusEvent pE) {
                higherThreshold.selectAll();
            }
        });

        filterChoice.addActionListener (new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(((String) filterChoice.getSelectedItem()).equals(CannyEdgeDetection)){
                    lowerThreshold.setEditable(true);
                    higherThreshold.setEditable(true);

                }
                else{
                    lowerThreshold.setEditable(false);
                    higherThreshold.setEditable(false);
                    lowerThreshold.setText("Canny lower threshold");
                    higherThreshold.setText("Canny higher threshold");
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
                if ((lowerThresholdValue < 0 ) && (filterChoice.getSelectedItem().equals(CannyEdgeDetection))) {
                    lowerThresholdValue = edgeDetection.lowerThreshold;
                    lowerThreshold.setText(String.valueOf(lowerThresholdValue));
                }
                if ((higherThresholdValue < 0) && (filterChoice.getSelectedItem().equals(CannyEdgeDetection))) {
                    higherThresholdValue = edgeDetection.higherThreshold;
                    higherThreshold.setText(String.valueOf(higherThresholdValue));
                }
                if (lowerThresholdValue > higherThresholdValue){
                    lowerThresholdValue = edgeDetection.lowerThreshold;
                    lowerThreshold.setText(String.valueOf(lowerThresholdValue));
                    higherThresholdValue = edgeDetection.higherThreshold;
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

    private double readThreshold(String text){
        try{
            double doubleTreshold = Double.parseDouble(text);
            return doubleTreshold;
        } catch (NumberFormatException nfe)
        {
            return -1.0;
        }
    }
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

    public class ImagePanel extends JPanel {

        private BufferedImage image;
        private String currentpath;
        public File imageFile;

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

        public String getcurrentpath(){
            return currentpath;
        }

        @Override
        public void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.drawImage(image, 0, 0, this);
        }
    }
}
