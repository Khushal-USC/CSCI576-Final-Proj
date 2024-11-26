import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class Macroblock {
    public int x, y; // Position of the macroblock
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
        public List<Integer> R;
        public List<Integer> G;
        public List<Integer> B;
    
        public RGBFrameData() {
            R = new ArrayList<>();
            G = new ArrayList<>();
            B = new ArrayList<>();
        }
        
        public void addPixel(int r, int g, int b) {
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
    
            R.add(r);
            G.add(g);
            B.add(b);
        }
    }
    
   
    
        JFrame frame;
        JLabel lbIm1;
        BufferedImage imgOne;
        int N = 8; // Block size is 8x8
    
        //REAL MAIN
        public static void main(String[] args) {
            // Check if the correct number of arguments is passed
            if (args.length != 3) {
                System.out.println("Incorrect Param Length: See README for usage instructions.");
                System.out.println(args.length);
                for (String arg : args) {
                    System.out.println(arg);
                }
                return;
            }
    
            // Parse command-line arguments
            String filePath = args[0];
            int width = 960;
            int height = 540;
            int n1 = 0;
            int n2 = 0;
    
            try {
                n1 = Integer.parseInt(args[1]);
                n2 = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.out.println("Error: n1, n2 should be integers.");
                return;
            }
    
            System.out.println("File Path: " + filePath);
            System.out.println("Width: " + width);
            System.out.println("Height: " + height);
            System.out.println("Quantization inputs: " + n1 + " " + n2);
    
            VideoEncoder ren = new VideoEncoder();
    
            ren.processVideo(filePath, width, height, n1, n2);
            System.out.println();
    
        }
    
        public void processVideo(String filePath, int width, int height, int n1, int n2) {
            // // Read .rgb input
            // ArrayList<BufferedImage> frames = readFrames(filePath, width, height);
    
            // TODO: video segmentation 
    
            // // Write .cmp output
            // String outputFilePath = "temp.cmp";
            // writeFrames(outputFilePath, frames, width, height);
    
            // Display frames
            // DisplayFrames(frames);
    
            //Get the frames RGB data
            ArrayList<RGBFrameData> frameDatas = readFramesRGB(filePath, width, height);
    
            //Convert the RGB data to images
            ArrayList<BufferedImage> imgFrames = new ArrayList<>();
            for (RGBFrameData frame : frameDatas) {
                imgFrames.add(RGBFrameDataToImg(frame));
            }
    
            //Display the frames
            DisplayFrames(imgFrames);
        }
    
        public ArrayList<BufferedImage> readFrames(String inputFilePath, int width, int height) {
            ArrayList<BufferedImage> frames = new ArrayList<>();
    
            try
            {
                File file = new File(inputFilePath);
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                raf.seek(0);
    
                long totalBytes = raf.length();
                int frameLength = width*height*3;
                int numFrames = (int) (totalBytes / frameLength);
                System.out.println(totalBytes + " " + frameLength + " " + "Num Frames" + numFrames);
                // System.out.println(numFrames);
    
                for (int i = 0; i < 10; i++) {
                    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    System.out.println("Reading frame " + i);
    
                    long len = frameLength;
                    byte[] bytes = new byte[(int) len];
    
                    raf.read(bytes);
    
                    int ind = 0;
                    for(int y = 0; y < height; y++)
                    {
                        for(int x = 0; x < width; x++)
                        {
                            byte a = 0;
                            
                            byte r = bytes[ind];
                            byte g = bytes[ind + 1];
                            byte b = bytes[ind + 2];
                            ind += 3;
    
                            // R.add((int)r);
                            // G.add((int)g);
                            // B.add((int)b);
    
                            int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                            //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                            //int pix = 0xff32A852;
                            img.setRGB(x,y,pix);
                            //ind++;
                        }
                    }
    
                    frames.add(img);
                }
            }
            catch (FileNotFoundException e) 
            {
                e.printStackTrace();
            } 
            catch (IOException e) 
            {
                e.printStackTrace();
            }
    
            return frames;
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
    
        public ArrayList<RGBFrameData> readFramesRGB(String inputFilePath, int width, int height) {
            ArrayList<RGBFrameData> frames = new ArrayList<>();
    
            try
            {
                File file = new File(inputFilePath);
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                raf.seek(0);
    
                long totalBytes = raf.length();
                int frameLength = width*height*3;
                int numFrames = (int) (totalBytes / frameLength);
                System.out.println(totalBytes + " " + frameLength + " " + "Num Frames" + numFrames);
                // System.out.println(numFrames);
    
                for (int i = 0; i < numFrames; i++) {
                    RGBFrameData frameData = new RGBFrameData();
                    System.out.println("Reading frame " + i);
    
                    long len = frameLength;
                    byte[] bytes = new byte[(int) len];
    
                    raf.read(bytes);
    
                    int ind = 0;
                    for(int y = 0; y < height; y++)
                    {
                        for(int x = 0; x < width; x++)
                        {
                            byte a = 0;
                            
                            byte r = bytes[ind];
                            byte g = bytes[ind + 1];
                            byte b = bytes[ind + 2];
                            ind += 3;
    
                            // R.add((int)r);
                            // G.add((int)g);
                            // B.add((int)b);
    
                            int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                            //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                            //int pix = 0xff32A852;
                            frameData.addPixel((int)r, (int)g, (int)b);
                            //ind++;
                        }
                    }
    
                    frames.add(frameData);
                }
            }
            catch (FileNotFoundException e) 
            {
                e.printStackTrace();
            } 
            catch (IOException e) 
            {
                e.printStackTrace();
            }
    
            return frames;
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
                    
                    byte r = (byte) in.R.get(ind).intValue();
                    byte g = (byte) in.G.get(ind).intValue();
                    byte b = (byte) in.B.get(ind).intValue();
    
                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                    //int pix = 0xff32A852;
                    img.setRGB(x,y,pix);
                    ind++;
                }
            }
    
            return img;
        }
    
    
    
        public void displayLabeledFrame(BufferedImage img, BufferedImage img_prev, JLabel label, String text) {
            int outWidth = img.getWidth();
            int outHeight = img.getHeight();
        
            // Create a new BufferedImage with space for the label text
            BufferedImage newImage = new BufferedImage(outWidth, outHeight + 50, BufferedImage.TYPE_INT_RGB);

            // Get macroblocks
            System.out.println("Splitting frame into macroblocks");
            List<Macroblock> macroblocks = segmentFrame(img, img_prev);
            System.out.println("Rendering outlines");
            
        
            // Draw the original image and the label text onto the new image
            Graphics g = newImage.getGraphics();
            g.drawImage(img, 0, 0, null); 
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString(text, 20, outHeight + 30); // Adjust text position as needed

            // Draw macroblock outlines
            for (Macroblock mb : macroblocks) {
                g.setColor(mb.isForeground ? Color.RED : Color.BLUE);
                g.drawRect(mb.x, mb.y, mb.x_size, mb.y_size);
            }
            g.dispose();
        
            // Update the JLabel with the new image
            label.setIcon(new ImageIcon(newImage));
            label.revalidate();
            label.repaint();
            frame.pack();
        }
    
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
                    Thread.sleep(100);
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

    public static void renderMacroblockOutlines(BufferedImage frame, List<Macroblock> macroblocks, int blockSize) {
        Graphics2D g2d = frame.createGraphics();

        // Set stroke and color for the outlines
        g2d.setColor(Color.PINK);
        g2d.setStroke(new BasicStroke(10)); // 2-pixel wide outlines

        // Draw rectangles for each macroblock
        for (Macroblock mb : macroblocks) {
            int x = mb.x;
            int y = mb.y;

            // Draw the outline of the macroblock
            g2d.drawRect(mb.x, mb.y, mb.x_size, mb.y_size);

            // Draw a label for the macroblock
            g2d.drawString(mb.isForeground ? "F" : "B", x + 2, y + 12);
        }

        // Release the Graphics2D context
        g2d.dispose();
    }
    
    
    // WIP

    public List<Macroblock> segmentFrame(BufferedImage currentFrame, BufferedImage previousFrame) {
        int width = currentFrame.getWidth();
        int height = currentFrame.getHeight();
        int macroblockSize = 16;

        List<Macroblock> macroblocks = new ArrayList<>();

        for (int y = 0; y < height; y += macroblockSize) {
            for (int x = 0; x < width; x += macroblockSize) {
                int blockWidth = macroblockSize;
                if(x + macroblockSize > width){
                    blockWidth = macroblockSize - (x + macroblockSize - width);
                }
                int blockHeight = macroblockSize;
                if(y + macroblockSize > height){
                    blockHeight = macroblockSize - (y + macroblockSize - height);
                }
                Macroblock mb = new Macroblock(x, y, blockWidth, blockHeight);

                //System.out.printf("Macroblock at (%d, %d): %s\n", mb.x, mb.y, mb.isForeground ? "Foreground" : "Background");
                // Calculate motion vector for this macroblock
                if(previousFrame != null){
                    int motionVector = calculateMotionVector(currentFrame, previousFrame, x, y, macroblockSize);

                    // Determine if it's foreground or background
                    mb.isForeground = motionVector > 10; // Threshold for foreground motion
                }
                macroblocks.add(mb);
            }
        }
        return macroblocks;
    }

    private int calculateMotionVector(BufferedImage currentFrame, BufferedImage previousFrame, int x, int y, int size) {
        int width = currentFrame.getWidth();
        int height = currentFrame.getHeight();

        int bestMAD = Integer.MAX_VALUE;

        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                int mad = 0;

                for (int yy = 0; yy < size; yy++) {
                    for (int xx = 0; xx < size; xx++) {
                        int cx = x + xx;
                        int cy = y + yy;
                        int px = cx + dx;
                        int py = cy + dy;

                        if (cx < 0 || cx >= width || cy < 0 || cy >= height ||
                                px < 0 || px >= width || py < 0 || py >= height)
                            continue;

                        Color currentPixel = new Color(currentFrame.getRGB(cx, cy));
                        Color previousPixel = new Color(previousFrame.getRGB(px, py));

                        int diffR = Math.abs(currentPixel.getRed() - previousPixel.getRed());
                        int diffG = Math.abs(currentPixel.getGreen() - previousPixel.getGreen());
                        int diffB = Math.abs(currentPixel.getBlue() - previousPixel.getBlue());

                        mad += (diffR + diffG + diffB) / 3;
                    }
                }

                if (mad < bestMAD) {
                    bestMAD = mad;
                }
            }
        }

        return bestMAD;
    }

    public void applyDCTAndQuantize(Macroblock mb, BufferedImage frame, int n) {
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
                        if (px >= frame.getWidth() || py >= frame.getHeight())
                            continue;

                        Color pixel = new Color(frame.getRGB(px, py));
                        blockR[y][x] = pixel.getRed();
                        blockG[y][x] = pixel.getGreen();
                        blockB[y][x] = pixel.getBlue();
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
                block[y][x] = Math.round(block[y][x] / Math.pow(2, n)) * Math.pow(2, n);
            }
        }
    }
}
