package edgedetection;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static edgedetection.EdgeDetection.*;

public class EdgeDetectionUI {

    private static final int FRAME_WIDTH = 1000;
    private static final int FRAME_HEIGHT = 600;
    private static final Font sansSerifBold = new Font("SansSerif", Font.BOLD, 22);
    private  ImagePanel sourceImage = new ImagePanel(".\\Obrazek1.jpg");
    private  ImagePanel destImage = new ImagePanel(".\\Obrazek1.jpg");
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
        filterChoice.setFont(sansSerifBold);
        filterChoice.addItem(CannyEdgeDetection);
        filterChoice.setFont(sansSerifBold);

        JButton detect = new JButton("Wykryj krawedzie");
        detect.setFont(sansSerifBold);

        northPanel.add(filterChoice);
        northPanel.add(chooseButton);
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
                File mixedFile = edgeDetection.detectEdges(bufferedImage, (String) filterChoice.getSelectedItem());
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
