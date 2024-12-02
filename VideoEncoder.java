import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.*;
import java.util.List;

class Macroblock {
    public int x, y; // Position of the macroblock
    public int[] motion_vec; // Motion vector for the macroblock
    //Size of the macroblock
    public int x_size;
    public int y_size;
    public boolean isForeground; // Whether it's foreground or background
    public double[][] dctCoefficientsR;
    public double[][] dctCoefficientsG;
    public double[][] dctCoefficientsB;

    public Macroblock(int x, int y, int x_size, int y_size) {
        this.x = x;
        this.y = y;
        this.x_size = x_size;
        this.y_size = y_size;
        this.isForeground = false;
        this.dctCoefficientsR = new double[8][8];
        this.dctCoefficientsG = new double[8][8];
        this.dctCoefficientsB = new double[8][8];
    }
}

public class VideoEncoder {

    class RGBFrameData {
        public int[] R;
        public int[] G;
        public int[] B;
    
        public RGBFrameData(int width, int height) {
            R = new int[width * height];
            G = new int[width * height];
            B = new int[width * height];
        }
        
        public void addPixel(int r, int g, int b, int index) {
            //Fixing the negative values, bytes being signed is causing problems in calculatiosn later on
            if(r < 0){
                r += 256;
            }
            if(g < 0){
                g += 256;
            }
            if(b < 0){
                b += 256;
            }
    
            R[index] = r;
            G[index] = g;
            B[index] = b;
        }
    }
    
   
    
        JFrame frame;
        JLabel lbIm1;
        BufferedImage imgOne;
        int N = 8; // Block size is 8x8
    
        //REAL MAIN
        public static void main(String[] args) {
            // Check if the correct number of arguments is passed
            if (args.length != 4) {
                System.out.println("Incorrect Param Length: See README for usage instructions.");
                System.out.println(args.length);
                for (String arg : args) {
                    System.out.println(arg);
                }
                return;
            }

            long maxHeapSize = Runtime.getRuntime().maxMemory();
            System.out.println("Max Heap Size: " + maxHeapSize / (1024 * 1024) + " MB");
    
            // Parse command-line arguments
            String filePath = args[0];
            String audioPath = args[1];
            int width = 960;
            int height = 540;
            int n1 = 0;
            int n2 = 0;
    
            try {
                n1 = Integer.parseInt(args[2]);
                n2 = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                System.out.println("Error: n1, n2 should be integers.");
                return;
            }
    
            System.out.println("File Path: " + filePath);
            System.out.println("Audio Path: " + audioPath);
            System.out.println("Width: " + width);
            System.out.println("Height: " + height);
            System.out.println("Quantization inputs: " + n1 + " " + n2);
    
            VideoEncoder ren = new VideoEncoder();
    
            ren.processVideo(filePath,audioPath, width, height, n1, n2);
            System.out.println();
    
        }
    
        public void processVideo(String filePath, String audioPath, int width, int height, int n1, int n2) {

            File file = new File(filePath);
            long fileSizeInBytes = file.length();

            // Calculate total frames
            int frameSize = width * height * 3;
            
            int totalFrames = (int) Math.ceil((double) fileSizeInBytes / frameSize);
            // // Read .rgb input

            RGBFrameData[] frameDatas = new RGBFrameData[(int) totalFrames];
            readFramesRGB(filePath, width, height, frameDatas, totalFrames);

            //Split the frames into macroblocks
            //Make list of lists of macroblocks
            int macroblockSize = 16;
            int macroblocksWidth = (int) Math.ceil((double) width / macroblockSize);
            int macroblocksHeight = (int) Math.ceil((double) height / macroblockSize);
            int macroblocksPerFrame = macroblocksWidth * macroblocksHeight;
            
            Macroblock[][] frameMacroblocks = new Macroblock[frameDatas.length][macroblocksPerFrame];
            for (int i = 0; i < frameDatas.length; i++) {
                System.out.println("Splitting frame " + i);
                System.out.println("Splitting frames into macroblocks " + macroblocksPerFrame);
                Macroblock[] macroblocks = segmentFrame(frameDatas[i], i > 0 ? frameDatas[i-1] : null, width, height, macroblocksPerFrame);
                frameMacroblocks[i] = macroblocks;
                

                // // WIP : DCT
                // for (Macroblock mb : macroblocks) {
                //     if (mb.isForeground) {
                //         applyDCTAndQuantize(mb, frameDatas[i], width, height, n1);
                //     }
                //     else {
                //         applyDCTAndQuantize(mb, frameDatas[i], width, height, n2);
                //     }
                // }
                
                // // WIP : IDCT
                // for (Macroblock mb : macroblocks) {
                //     if (mb.isForeground) {
                //         applyIDCTAndDequantize(mb,frameDatas[i], width, height, n1);
                //     }
                //     else {
                //         applyIDCTAndDequantize(mb,frameDatas[i], width, height, n2);
                //     }
                // }

                // final int index = i;
                // Arrays.stream(macroblocks)
                // .parallel() // Use parallel stream for concurrent processing
                // .forEach(mb -> {
                //     if (mb.isForeground) {
                //         applyDCTAndQuantize(mb, frameDatas[index], width, height, n1);
                //         applyIDCTAndDequantize(mb,frameDatas[index], width, height, n1);
                //     }
                //     else {
                //         applyDCTAndQuantize(mb, frameDatas[index], width, height, n2);
                //         applyIDCTAndDequantize(mb,frameDatas[index], width, height, n2);
                //     }
                // });
                
            }

            //Convert the RGB data to images
            ArrayList<BufferedImage> imgFrames = new ArrayList<>();
            for (RGBFrameData frame : frameDatas) {
                imgFrames.add(RGBFrameDataToImg(frame));
            }

            //Add the macroblock outlines to the images
            for (int i = 0; i < imgFrames.size(); i++) {
                drawDebugOutlines(imgFrames.get(i), frameMacroblocks[i]);
            }
    
            //Initialize the FrameAudioPlayer
            FrameAudioPlayer.playFramesWithAudio(imgFrames, 30, audioPath);
        }
    
        public void writeFrames(String outputFilePath, ArrayList<BufferedImage> frames, int width, int height) {
            try {
                File outputFile = new File(outputFilePath);
                FileOutputStream fos = new FileOutputStream(outputFile);
                int count = 0;
    
                for (BufferedImage frame : frames) {
                System.out.println("Writing frame" + count++);
                // Loop through pixels and get RGB
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int pixel = frame.getRGB(x, y);
    
                            byte r = (byte) ((pixel >> 16) & 0xFF);
                            byte g = (byte) ((pixel >> 8) & 0xFF);
                            byte b = (byte) (pixel & 0xFF);
    
                            fos.write(r);
                            fos.write(g);
                            fos.write(b);
                        }
                    }
                }
    
                fos.close();
            } 
            catch (IOException e) {
                e.printStackTrace();
            }
        }

    public void readFramesRGB(String inputFilePath, int width, int height, RGBFrameData[] frameDatas, int totalFrames) {
        int frameLength = width * height * 3; // Size of one frame in bytes
        ByteBuffer buf = ByteBuffer.allocate(frameLength); // Buffer for one frame
        int frameCount = 0;

        try (FileInputStream fileInputStream = new FileInputStream(inputFilePath);
             FileChannel fileChannel = fileInputStream.getChannel()) {

            while (fileChannel.read(buf) > 0) {
                buf.flip(); // Prepare the buffer for reading
                RGBFrameData frameData = new RGBFrameData(width, height);

                int index = 0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        if (buf.remaining() >= 3) { // Ensure enough data remains
                            int r = buf.get() & 0xFF; // Read Red byte as unsigned
                            int g = buf.get() & 0xFF; // Read Green byte as unsigned
                            int b = buf.get() & 0xFF; // Read Blue byte as unsigned
                            frameData.addPixel(r, g, b, index++); // Add pixel to frame data
                        }
                    }
                }
                frameDatas[frameCount] = frameData;
                System.out.println("Read frame " + frameCount++);
                buf.clear(); // Clear the buffer for the next frame
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
        public BufferedImage RGBFrameDataToImg(RGBFrameData in){
            int width = 960;
            int height = 540;
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    
            int ind = 0;
            for(int y = 0; y < height; y++)
            {
                for(int x = 0; x < width; x++)
                {
                    byte a = 0;
                    
                    byte r = (byte) in.R[ind];
                    byte g = (byte) in.G[ind];
                    byte b = (byte) in.B[ind];
    
                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                    //int pix = 0xff32A852;
                    img.setRGB(x,y,pix);
                    ind++;
                }
            }
    
            return img;
        }

    public void drawDebugOutlines(BufferedImage img, Macroblock[] macroblocks) {
        Graphics g = img.getGraphics();
        g.setColor(Color.WHITE);

        // Draw macroblock outlines
        for (Macroblock mb : macroblocks) {
            g.setColor(mb.isForeground ? Color.RED : Color.BLUE);
            g.drawRect(mb.x, mb.y, mb.x_size, mb.y_size);

            //Draw the motion vector
            g.setColor(Color.GREEN);
            int blockSize = 16;

            // Draw a line indicating the motion vector
            if (mb.motion_vec != null) {
                int dx = mb.motion_vec[0];
                int dy = mb.motion_vec[1];
                g.drawLine(mb.x + blockSize / 2, mb.y + blockSize / 2, mb.x + blockSize / 2 + dx, mb.y + blockSize / 2 + dy);

                //Draw dot at the end of the motion vector
                g.setColor(Color.RED);
                g.fillOval(mb.x + blockSize / 2 + dx - 2, mb.y + blockSize / 2 + dy - 2, 2, 2);
            }
                //g.drawLine(mb.x + mb.x_size / 2, mb.y + mb.y_size / 2, mb.x + mb.x_size / 2 + mb.motion_vec[0], mb.y + mb.y_size / 2 + mb.motion_vec[1]);
        }
        g.dispose();
    }
    
    
    
    public void displayLabeledFrame(BufferedImage img, BufferedImage img_prev, JLabel label, String text) {
            int outWidth = img.getWidth();
            int outHeight = img.getHeight();
        
            // Create a new BufferedImage with space for the label text
            BufferedImage newImage = new BufferedImage(outWidth, outHeight + 50, BufferedImage.TYPE_INT_RGB);

            // // Get macroblocks
            
            // System.out.println("Rendering outlines");
            
        
            // Draw the original image and the label text onto the new image
            Graphics g = newImage.getGraphics();
            g.drawImage(img, 0, 0, null); 
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString(text, 20, outHeight + 30); // Adjust text position as needed
            g.dispose();
        
            // Update the JLabel with the new image
            label.setIcon(new ImageIcon(newImage));
            label.revalidate();
            label.repaint();
            frame.pack();
        }

        //public void displayImage
    
        public void DisplayFrames(ArrayList<BufferedImage> frames) {
            // Frame setup
            frame = new JFrame();
            GridBagLayout gLayout = new GridBagLayout();
            frame.getContentPane().setLayout(gLayout);
            lbIm1 = new JLabel();
    
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.CENTER;
            c.weightx = 0.5;
            c.gridx = 0;
            c.gridy = 0;
    
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 1;
            frame.getContentPane().add(lbIm1, c);
    
            frame.pack();
            frame.setVisible(true);
    
            
    
            // Display each frame
            for (int i = 0; i < frames.size(); i++) {
                System.out.println("Displaying frame " + i);
                displayLabeledFrame(frames.get(i), i > 0 ? frames.get(i-1) : null, lbIm1, "Frame " + i);
                
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


    public List<Macroblock> splitFrameToBlocks(BufferedImage currentFrame) {
        int width = currentFrame.getWidth();
        int height = currentFrame.getHeight();
        int macroblockSize = 16;

        List<Macroblock> macroblocks = new ArrayList<>();

        for (int y = 0; y < height; y += macroblockSize) {
            for (int x = 0; x < width; x += macroblockSize) {
                // int blockWidth = Math.min(macroblockSize, width - x); // Adjust width at edges
                // int blockHeight = Math.min(macroblockSize, height - y); // Adjust height at edges
                int blockWidth = macroblockSize;
                if(x + macroblockSize > width){
                    blockWidth = macroblockSize - (x + macroblockSize - width);
                }
                int blockHeight = macroblockSize;
                if(y + macroblockSize > height){
                    blockHeight = macroblockSize - (y + macroblockSize - height);
                }
                Macroblock mb = new Macroblock(x, y, blockWidth, blockHeight);

                macroblocks.add(mb);
            }
        }
        return macroblocks;
    }
    
    
    // WIP

    public Macroblock[] segmentFrame(RGBFrameData currentFrame, RGBFrameData previousFrame, int frame_width, int frame_height, int macroBlocksPerFrame) {
        int width = frame_width;
        int height = frame_height;
        int macroblockSize = 16;

        Macroblock[] macroblocks = new Macroblock[macroBlocksPerFrame];
        int macroblockIndex = 0;

        // Store the motion vectors for global motion estimation
        double[] globalMotion = new double[2];

        // Calculate motion vectors for all macroblocks
        for (int y = 0; y < height; y += macroblockSize) {
            for (int x = 0; x < width; x += macroblockSize) {
                int blockWidth = macroblockSize;
                if (x + macroblockSize > width) {
                    blockWidth = macroblockSize - (x + macroblockSize - width);
                }
                int blockHeight = macroblockSize;
                if (y + macroblockSize > height) {
                    blockHeight = macroblockSize - (y + macroblockSize - height);
                }

                Macroblock mb = new Macroblock(x, y, blockWidth, blockHeight);

                if (previousFrame != null) {
                    int[] motionVector = calculateMotionVector(currentFrame, previousFrame, frame_width, frame_height, x, y, macroblockSize);
                    mb.motion_vec = motionVector;

                    // Store valid motion vectors for global motion estimation
                    if (motionVector != null) {
                        globalMotion[0] += motionVector[0];
                        globalMotion[1] += motionVector[1];
                    }
                }

                macroblocks[macroblockIndex++] = mb;
            }
        }

        // Estimate global motion (average of motion vectors) since so that each video uses a threshhold relative to each frame
        globalMotion[0] /= macroblocks.length;
        globalMotion[1] /= macroblocks.length;

        System.out.printf("Global Motion Vector: (%f, %f)\n", globalMotion[0], globalMotion[1]);

        // Classify macroblocks as foreground or background
        for (Macroblock mb : macroblocks) {
            if (mb.motion_vec != null) {
                double relativeMotionX = mb.motion_vec[0] - globalMotion[0];
                double relativeMotionY = mb.motion_vec[1] - globalMotion[1];
                double relativeMagnitude = Math.sqrt(relativeMotionX * relativeMotionX + relativeMotionY * relativeMotionY);

                // System.out.println("Relative magnitude: " + relativeMagnitude);

                // Threshold based on relative magnitude
                if (relativeMagnitude > 4) {
                    mb.isForeground = true;
                } 
                else {
                    mb.isForeground = false;
                }
            }
        }

        return macroblocks;
    }

    private int[] calculateMotionVector(RGBFrameData currentFrame, RGBFrameData previousFrame, int frame_width, int frame_height, int x, int y, int size) {
        int width = frame_width;
        int height = frame_height;

        int[] bestVector = new int[2];
        double bestMAD = Integer.MAX_VALUE;

        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                double mad = 0;

                for (int yy = 0; yy < size; yy++) {
                    for (int xx = 0; xx < size; xx++) {
                        int cx = x + xx;
                        int cy = y + yy;
                        int px = cx + dx;
                        int py = cy + dy;

                        if (cx < 0 || cx >= width || cy < 0 || cy >= height ||
                                px < 0 || px >= width || py < 0 || py >= height)
                            continue;

                        int rCurrent = currentFrame.R[cy * width + cx];
                        int gCurrent = currentFrame.G[cy * width + cx];
                        int bCurrent = currentFrame.B[cy * width + cx];

                        int rPrevious = previousFrame.R[py * width + px];
                        int gPrevious = previousFrame.G[py * width + px];
                        int bPrevious = previousFrame.B[py * width + px];

                        int diffR = Math.abs(rCurrent - rPrevious);
                        int diffG = Math.abs(gCurrent - gPrevious);
                        int diffB = Math.abs(bCurrent - bPrevious);

                        mad += ((diffR + diffG + diffB) / 3.0);
                    }
                }

                // Add a penalty term to favor smaller motion vectors
                double penalty = (Math.abs(dx) + Math.abs(dy)) * 0.5;
                mad += penalty;

                if (mad < bestMAD) {
                    bestMAD = mad;
                    bestVector[0] = dx;
                    bestVector[1] = dy;
                }

                // System.out.println(bestMAD + " " + dx + " " + dy);
            }
        }

        return bestVector;
    }

    public void applyDCTAndQuantize(Macroblock mb, RGBFrameData frame, int width, int height, int n) {
        int macroblockSize = 16;
        double[][] blockR = new double[8][8];
        double[][] blockG = new double[8][8];
        double[][] blockB = new double[8][8];
        for (int i = 0; i < macroblockSize; i += 8) {
            for (int j = 0; j < macroblockSize; j += 8) {
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        int px = mb.x + i + x;
                        int py = mb.y + j + y;
                        if (px >= width || py >= height)
                            continue;
                        int idx = py * width + px;
                        blockR[y][x] = frame.R[idx];
                        blockG[y][x] = frame.G[idx];
                        blockB[y][x] = frame.B[idx];
                    }
                }
                mb.dctCoefficientsR = performDCT(blockR);
                mb.dctCoefficientsG = performDCT(blockG);
                mb.dctCoefficientsB = performDCT(blockB);
                quantize(mb.dctCoefficientsR, n);
                quantize(mb.dctCoefficientsG, n);
                quantize(mb.dctCoefficientsB, n);
            }
        }
    }

    private double[][] performDCT(double[][] block) {
        int size = 8;
        double[][] transformed = new double[size][size];

        for (int u = 0; u < size; u++) {
            for (int v = 0; v < size; v++) {
                double sum = 0.0;
                for (int x = 0; x < size; x++) {
                    for (int y = 0; y < size; y++) {
                        sum += block[y][x] *
                                Math.cos((2 * x + 1) * u * Math.PI / 16) *
                                Math.cos((2 * y + 1) * v * Math.PI / 16);
                    }
                }
                double cu = (u == 0) ? 1 / Math.sqrt(2) : 1.0;
                double cv = (v == 0) ? 1 / Math.sqrt(2) : 1.0;
                transformed[u][v] = 0.25 * cu * cv * sum;
            }
        }

        return transformed;
    }

    private void quantize(double[][] block, int n) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                block[y][x] = Math.round(block[y][x] / Math.pow(2, n));
            }
        }
    }

    private void applyIDCTAndDequantize(Macroblock mb, RGBFrameData frame, int width, int height, int n) {
        int macroblockSize = 16;
        double[][] blockR = new double[8][8];
        double[][] blockG = new double[8][8];
        double[][] blockB = new double[8][8];
        for (int i = 0; i < macroblockSize; i += 8) {
            for (int j = 0; j < macroblockSize; j += 8) {
                // Dequantize coefficients
                dequantize(mb.dctCoefficientsR, n);
                dequantize(mb.dctCoefficientsG, n);
                dequantize(mb.dctCoefficientsB, n);
    
                // Perform IDCT
                blockR = performIDCT(mb.dctCoefficientsR);
                blockG = performIDCT(mb.dctCoefficientsG);
                blockB = performIDCT(mb.dctCoefficientsB);
    
                // Store reconstructed pixel values back into the frame
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        int px = mb.x + i + x;
                        int py = mb.y + j + y;
                        if (px >= width || py >= height)
                            continue;
                        int idx = py * width + px;
                        frame.R[idx] = clipToByte(blockR[y][x]);
                        frame.G[idx] = clipToByte(blockG[y][x]);
                        frame.B[idx] = clipToByte(blockB[y][x]);
                    }
                }
            }
        }
    }
    
    private double[][] performIDCT(double[][] block) {
        int size = 8;
        double[][] reconstructed = new double[size][size];
    
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                double sum = 0.0;
                for (int u = 0; u < size; u++) {
                    for (int v = 0; v < size; v++) {
                        double cu = (u == 0) ? 1 / Math.sqrt(2) : 1.0;
                        double cv = (v == 0) ? 1 / Math.sqrt(2) : 1.0;
                        sum += cu * cv * block[u][v] *
                               Math.cos((2 * x + 1) * u * Math.PI / 16) *
                               Math.cos((2 * y + 1) * v * Math.PI / 16);
                    }
                }
                reconstructed[y][x] = 0.25 * sum;
            }
        }
    
        return reconstructed;
    }
    
    private void dequantize(double[][] block, int n) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                block[y][x] *= Math.pow(2, n);
            }
        }
    }
    
    private int clipToByte(double value) {
        return (int) Math.max(0, Math.min(255, Math.round(value)));
    }
}
