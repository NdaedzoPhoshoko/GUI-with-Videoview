import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class Main {
    static{
        //Load the OpenCv native library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    private VideoCapture camera;
    private Boolean isCameraOn = false;

    //Engine
    public static void main(String[] args) {
        Main myObject = new Main();

        //Instantiate a frame and define its properties
        JFrame myFrame = new JFrame("GUI with Live Stream");
        myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        myFrame.setSize(900, 700);
        myFrame.setLayout(new BorderLayout());

        //Including the label that will show the live stream
        JLabel myVideoLabel = new JLabel();//this label will include the live stream
        myVideoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        myVideoLabel.setPreferredSize(new Dimension(myFrame.getWidth(), myFrame.getHeight() - 200));

        /*
        * Before including the image on the label wait  until the JLabel is visible and its size has been determined.
        *Use SwingUtilities.invokeLater*/
        SwingUtilities.invokeLater(() -> {
            myObject.setLabelImage("images/prompt_press_button.png", myVideoLabel);
        });

        //Label for direction
        JLabel myDirectionLabel = new JLabel("Waiting for motion...", SwingConstants.CENTER);
        myDirectionLabel.setFont(new Font("Arial", Font.BOLD, 20));
        myDirectionLabel.setForeground(Color.RED);

        //Include a button that will start and end the camera
        JPanel buttonPanel = new JPanel();
        JButton myButton = new JButton("Start Live");
        myButton.setPreferredSize(new Dimension(150, 60));
        //Adding action listeners to the button to trigger the camera
        myButton.addActionListener(e -> myObject.controlCamera(myButton, myVideoLabel));

        buttonPanel.add(myButton);

        //Adding all our Components to the frame
        myFrame.add(myDirectionLabel, BorderLayout.NORTH);
        myFrame.add(myVideoLabel, BorderLayout.CENTER);
        myFrame.add(buttonPanel, BorderLayout.SOUTH);
        myFrame.setVisible(true);
        }
        public void controlCamera(JButton button, JLabel cameraLabel){
            if(!isCameraOn)//when the camera is off start the camera
            {
                isCameraOn = true;
                camera = new VideoCapture(0); //open the camera
                button.setText("Stop Live");
                //display the error when the camera is not opening
                if(!camera.isOpened()){
                    JOptionPane.showMessageDialog(null, "Camera is not opening!");
                    isCameraOn = false;
                    return; //stop the program
                }

                threadForCamera(cameraLabel);
            }else{ //when camera is already on then release the camera
                isCameraOn = false;
                if(camera!=null) {
                    camera.release();
                    button.setText("Start Live");
                    Main myObject2 = new Main();
                    myObject2.setLabelImage("images/live_ended.png", cameraLabel);
                }
            }
        }

        //convert a mat to buffered image
        //mat is used by openCV for images
        public BufferedImage matToBufferedImage(Mat mat){
            int type = BufferedImage.TYPE_BYTE_GRAY; //image type is grayscale if mat has one channel
            if(mat.channels() > 1){
                type = BufferedImage.TYPE_3BYTE_BGR; //image type is color if mat has more than one channel
            }
            int bufferSize = mat.channels() * mat.cols() * mat.rows();
            byte[] buffer = new byte[bufferSize]; //buffer will store the pixel data from the image
            mat.get(0,0, buffer); //extract the image pixel data from mat into byte array buffer
            BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type); //new buffered image with dimensions cols * rows
            final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);
            return image;
        }

        /*method to create a thread for camera
         *video processing requires threads for continuous processing, this helps interference with main UI thread
         *threads will also allow asynchronous processing, continuously reading frames from camera while processing them without freezing the UI
         */
        public void threadForCamera(JLabel cameraLabel){
            Thread cameraThread = new Thread(() -> {
                Mat currentMatFrame = new Mat();

                while(isCameraOn){ //when the camera is on, continuously read the frames
                    camera.read(currentMatFrame);
                    if(!currentMatFrame.empty()){
                        //if the mat frame that is captured has some data then convert this mat frame into bufferedImage
                        BufferedImage image = matToBufferedImage(currentMatFrame);
                        // Scale the image to fit the label while keeping aspect ratio
                        ImageIcon icon = new ImageIcon(image);
                        Image scaledImage = icon.getImage().getScaledInstance(
                                cameraLabel.getWidth(),
                                cameraLabel.getWidth(),
                                Image.SCALE_SMOOTH
                        );
                        //set the scaledImage to appear on the camera label
                        cameraLabel.setIcon(new ImageIcon(scaledImage));
                    }
                }
                camera.release(); //assuming the camera is triggered to be off
            });
            cameraThread.start();
        }

        //method called to store intro images on the myVideoLabel
        public void setLabelImage(String filePath, JLabel label){
            // Load and scale image
            ImageIcon imageIcon = new ImageIcon(filePath); // Load the image
            Image image = imageIcon.getImage().getScaledInstance(
                    label.getWidth(),
                    label.getHeight(),
                    Image.SCALE_SMOOTH
            );
            label.setIcon(new ImageIcon(image)); // Set scaled image to label
        }
    }