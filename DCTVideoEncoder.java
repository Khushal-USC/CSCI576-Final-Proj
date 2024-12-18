import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.*;
import java.util.List;
import java.util.Scanner;

class Macroblock {
    //x, y positions for macroblock class
    public int x, y;
    //macroblock motion vector variable 
    public int[] motion_vec; 
    // size variables for macroblock class
    public int x_size;
    public int y_size;
    //bool check to determine background / foreground
    public boolean isForeground; 
    public double[][][] dctCoefficientsR;
    public double[][][] dctCoefficientsG;
    public double[][][] dctCoefficientsB;

    public Macroblock(int x, int y, int x_size, int y_size) {
        this.x = x;
        this.y = y;
        this.x_size = x_size;
        this.y_size = y_size;
        this.isForeground = false;
        this.dctCoefficientsR = new double[4][8][8];
        this.dctCoefficientsG = new double[4][8][8];
        this.dctCoefficientsB = new double[4][8][8];
    }
}

public class DCTVideoEncoder {

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
            //fix for negative values since signed bytes are causing issues in calculations
            if (r < 0) {
                r += 256;
            }
            if (g < 0) {
                g += 256;
            }
            if (b < 0) {
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
    //our block size is 8x8
    int DCTBlockN = 8; 
    double piOver2N = Math.PI / (2 * DCTBlockN);
    private static final double[][] cosTableX = new double[8][8];
    private static final double[][] cosTableY = new double[8][8];

    private static final double oneOverRoot2 = 1.0 / Math.sqrt(2);

    private static String cmpFilePath = "";

    public static void createCmpFile(String filename, int n1, int n2) throws IOException {
        // creating the .cmp file
        try (FileWriter writer = new FileWriter(filename)) {
            //writing n1 and n2 to the file
            writer.write(n1 + " " + n2 + "\n");
        }
    }

    private static void validateDimensions(double[][] coeffs) {
        if (coeffs.length != 8 || coeffs[0].length != 8) {
            throw new IllegalArgumentException("Each coefficient array must be an 8x8 matrix.");
        }
    }

    //main function
    public static void main(String[] args) {
        //check for correct number of arguments by user input
        if (args.length != 5) {
            System.out.println("Incorrect Param Length: See README for usage instructions.");
            System.out.println(args.length);
            for (String arg : args) {
                System.out.println(arg);
            }
            return;
        }

        long maxHeapSize = Runtime.getRuntime().maxMemory();
        System.out.println("Max Heap Size: " + maxHeapSize / (1024 * 1024) + " MB");

        //parsing command-line arguments that were inputted
        String filePath = args[0];
        String audioPath = args[1];
        int width = 960;
        int height = 540;
        int n1 = 0;
        int n2 = 0;
        String option = args[4];

        //start the cmp file
        System.out.println(filePath);
        String[] splitPath = filePath.split("\\\\");
        cmpFilePath = splitPath[splitPath.length - 1].split("\\.rgb")[0] + ".cmp";

        if(option.equals("c")){
            try {
                createCmpFile(cmpFilePath, n1, n2);
            } catch (IOException e) {
                System.out.println("Error creating .cmp file.");
                return;
            }
        }

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

        DCTVideoEncoder ren = new DCTVideoEncoder();

        ren.processVideo(filePath, audioPath, width, height, n1, n2, option);
        System.out.println();

    }

    public void precomputeCosTables() {
        for (int u = 0; u < 8; u++) {
            for (int x = 0; x < 8; x++) {
                cosTableX[u][x] = Math.cos((2 * x + 1) * u * piOver2N);
            }
        }
        for (int v = 0; v < 8; v++) {
            for (int y = 0; y < 8; y++) {
                cosTableY[v][y] = Math.cos((2 * y + 1) * v * piOver2N);
            }
        }
    }

    public void processVideo(String filePath, String audioPath, int width, int height, int n1, int n2, String option) {
        //precomputing cosine tables
        precomputeCosTables();

        File file = new File(filePath);
        long fileSizeInBytes = file.length();

        //getting total frame size
        int frameSize = width * height * 3;

        int totalFrames = (int) Math.ceil((double) fileSizeInBytes / frameSize);
        //read rgb file
        RGBFrameData prevFrame = null;

        if(option.equals("c")){
            //read frames, compress and write to .cmp file
            readFrameCompressAndWrite(filePath, width, height, totalFrames, prevFrame,
            n1, n2);
        } else {
            //initiallize frames array 
            ArrayList<BufferedImage> frames = new ArrayList<BufferedImage>();

            //call function to read the .cmp file and write the frames to the frames array
            int macroblockSize = 16;
            int macroblocksWidth = (int) Math.ceil((double) width / macroblockSize);
            int macroblocksHeight = (int) Math.ceil((double) height / macroblockSize);
            int macroblocksPerFrame = macroblocksWidth * macroblocksHeight;
            readCmpDecompressAndGenerateFrames(frames, width, height, macroblocksPerFrame, n1, n2);

            //initialize the FrameAudioPlayer
            FrameAudioPlayer.playFramesWithAudio(frames, 30, audioPath);
        }
    }

    public void writeFrames(String outputFilePath, ArrayList<BufferedImage> frames, int width, int height) {
        try {
            File outputFile = new File(outputFilePath);
            FileOutputStream fos = new FileOutputStream(outputFile);
            int count = 0;

            for (BufferedImage frame : frames) {
                System.out.println("Writing frame" + count++);
                //get rgb values after looping through pixels
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Macroblock parseLineToMacroblock(String line, int x, int y, int x_size, int y_size) {
        //create Macroblock instance with position and size given
        Macroblock macroblock = new Macroblock(x, y, x_size, y_size);

        try (Scanner scanner = new Scanner(line)) {
            // Parse the foreground/background flag
            //use flag to parse background / foreground
            if (scanner.hasNextInt()) {
                macroblock.isForeground = scanner.nextInt() == 1;
            } else {
                throw new IllegalArgumentException("Invalid line format: missing foreground/background flag.");
            }

            // Parse the remaining 3 DCT coefficient sets
            //parse remaining 3 DCT Coefficient sets
            for (int i = 0; i < 4; i++) {
                //parse DCT coefficients for R, G, B components
                parseCoefficients(scanner, macroblock.dctCoefficientsR[i]);
                parseCoefficients(scanner, macroblock.dctCoefficientsG[i]);
                parseCoefficients(scanner, macroblock.dctCoefficientsB[i]);
            }
        }

        return macroblock;
    }

    private static void parseCoefficients(Scanner scanner, double[][] coefficients) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (scanner.hasNextInt()) {
                    coefficients[i][j] = (double) scanner.nextInt();
                } else {
                    throw new IllegalArgumentException("Invalid line format: missing coefficient values.");
                }
            }
        }
    }

    public void readFrameCompressAndWrite(String inputFilePath, int width, int height, int totalFrames,
            RGBFrameData prevFrame, int n1, int n2) {
        //size of one frame in bytes
        int frameLength = width * height * 3;
        //buffer for one frame
        ByteBuffer buf = ByteBuffer.allocate(frameLength); 
        int frameCount = 0;

        try (FileInputStream fileInputStream = new FileInputStream(inputFilePath);
                FileChannel fileChannel = fileInputStream.getChannel()) {

            //string builder for this frame
            StringBuilder sb = new StringBuilder();
            while (fileChannel.read(buf) > 0) {
                buf.flip(); //prepare the buffer for reading
                RGBFrameData frameData = new RGBFrameData(width, height);

                int index = 0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        //ensure enough data remains and read R, G, B bytes as unsigned
                        if (buf.remaining() >= 3) { 
                            int r = buf.get() & 0xFF; 
                            int g = buf.get() & 0xFF; 
                            int b = buf.get() & 0xFF; 
                            //add pixel to frame data
                            frameData.addPixel(r, g, b, index++); 
                        }
                    }
                }
                System.out.println("Read frame " + frameCount++);

                //process the frame data here

                //if this is the first frame, skip the DCT and quantization and segmentation

                //get macroblocks
                int macroblockSize = 16;
                int macroblocksWidth = (int) Math.ceil((double) width / macroblockSize);
                int macroblocksHeight = (int) Math.ceil((double) height / macroblockSize);
                int macroblocksPerFrame = macroblocksWidth * macroblocksHeight;

                Macroblock[] macroblocks = segmentFrame(frameData, prevFrame, width, height, macroblocksPerFrame);

                //apply DCT and quantization
                for (Macroblock mb : macroblocks) {
                    if (mb.isForeground) {
                        applyDCTAndQuantize(mb, frameData, width, height, n1);
                    } else {
                        applyDCTAndQuantize(mb, frameData, width, height, n2);
                    }
                    //append the macroblock data to the string builder
                    sb.append(mb.isForeground ? "1 " : "0 ");
                    appendCoefficientsToStringBuilder(sb, mb.dctCoefficientsR, mb.dctCoefficientsG,
                            mb.dctCoefficientsB);
                    //add a newline at the end
                    sb.append("\n");
                }

                int batchSize = 5;
                if (frameCount % batchSize == 0) {
                    System.out.println(
                            "Processed " + frameCount + " frames." + " Writing " + batchSize + " frames to file.");
                    //write to file with lock and retry mechanism
                    //we will only allow 10 maximum number of retries
                    int maxRetries = 10;
                    int attempt = 0;
                    boolean success = false;

                    while (attempt < maxRetries && !success) {
                        //open in append mode
                        try (FileOutputStream fos = new FileOutputStream(cmpFilePath, true); 
                                FileChannel channel = fos.getChannel();
                                //acquire an exclusive lock
                                FileLock lock = channel.lock()) { 

                            //write the frame data to the file while the lock is held
                            fos.write(sb.toString().getBytes());
                            System.out.println("Wrote macroblocks for frame " + frameCount + " to file with lock after "
                                    + attempt + " attempts.");
                            //mark success and exit loop
                            success = true; 

                        } catch (IOException e) {
                            attempt++;
                            System.err.println(
                                    "Attempt " + attempt + " failed to write to file: " + cmpFilePath + " for frame "
                                            + frameCount + "\n" + e.getMessage() + "\nRetrying...");

                            if (attempt < maxRetries) {
                                try {
                                    //wait for 100ms before retrying
                                    Thread.sleep(100); 
                                } catch (InterruptedException ie) {
                                    //restore interrupted status
                                    Thread.currentThread().interrupt(); 
                                    System.err.println("Retry interrupted.");
                                    break;
                                }
                            } else {
                                System.err.println("Max retries reached. Failed to write to file: " + cmpFilePath);
                            }
                        }
                    }

                    if (!success) {
                        System.err.println("Unable to write to file after " + maxRetries + " attempts.");
                    }

                    //clear the string builder
                    sb.setLength(0);
                }

                // if (prevFrame == null) {
                    prevFrame = frameData;
                //     System.out.println("First frame processed");
                // }
                //clear the buffer for the next frame
                buf.clear(); 

            
            }

            //write the remaining frames
            //write to file with lock and retry mechanism
            int maxRetries = 10; // Maximum number of retries
            int attempt = 0;
            boolean success = false;

            while (attempt < maxRetries && !success) {
                //open in append mode
                try (FileOutputStream fos = new FileOutputStream(cmpFilePath, true); 
                        FileChannel channel = fos.getChannel();
                        //acquire an exclusive lock 
                        FileLock lock = channel.lock()) { 

                    //write the frame data to the file while the lock is held
                    fos.write(sb.toString().getBytes());
                    System.out.println("Wrote macroblocks for frame " + frameCount + " to file with lock after "
                            + attempt + " attempts.");
                    //mark success to exit the loop
                    success = true; 

                } catch (IOException e) {
                    attempt++;
                    System.err.println("Attempt " + attempt + " failed to write to file: " + cmpFilePath + " for frame "
                            + frameCount + "\n" + e.getMessage() + "\nRetrying...");

                    if (attempt < maxRetries) {
                        try {
                            //wait for 100ms before retrying
                            Thread.sleep(100);
                        } catch (InterruptedException ie) {
                            //restore interrupted status
                            Thread.currentThread().interrupt(); 
                            System.err.println("Retry interrupted.");
                            break;
                        }
                    } else {
                        System.err.println("Max retries reached. Failed to write to file: " + cmpFilePath);
                    }
                }
            }

            if (!success) {
                System.err.println("Unable to write to file after " + maxRetries + " attempts.");
            }

            //clear the string builder
            sb.setLength(0);

            

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //method to read the .cmp file, decompress the frames and generate the frames
    public void readCmpDecompressAndGenerateFrames(ArrayList<BufferedImage> frames, int width, int height,
                                               int macroblocksPerFrame, int n1, int n2) {
        System.out.println("Reading .cmp file and generating frames: " + cmpFilePath);
        //dynamic frame count var
        int frameCount = 0; 
        boolean isFirstFrame = true;

        try (FileInputStream fis = new FileInputStream(cmpFilePath);
            Scanner scanner = new Scanner(fis)) {

            while (scanner.hasNextLine()) {
                //for the first frame, read n1 and n2 values
                if (isFirstFrame) {
                    if (scanner.hasNextLine()) {
                        String nValuesLine = scanner.nextLine().trim();
                        if (!nValuesLine.isEmpty()) {
                            System.out.println("Reading n1 and n2 values");
                            System.out.println("n1: " + n1 + ", n2: " + n2);
                            //only read n1 and n2 once
                            isFirstFrame = false; 
                            continue;
                        }
                    }
                }

                //start processing macroblocks for the current frame
                System.out.println("Reading macroblocks for frame " + frameCount);
                RGBFrameData frameData = new RGBFrameData(width, height);
                Macroblock[] macroblocks = new Macroblock[macroblocksPerFrame];
                int x = 0, y = 0;

                for (int i = 0; i < macroblocksPerFrame; i++) {
                    if (!scanner.hasNextLine()) {
                        System.err.println("Warning: Unexpected end of file while processing frame " + frameCount);
                        //break if no more lines are left
                        break;
                    }

                    //read and parse the current line
                    String line = scanner.nextLine().trim();
                    if (line.isEmpty()) {
                        //retry current macroblock is the line is empty
                        i--; 
                        continue;
                    }

                    int blockWidth = Math.min(16, width - x);
                    int blockHeight = Math.min(16, height - y);

                    //parse macroblock from line
                    Macroblock mb = parseLineToMacroblock(line, x, y, blockWidth, blockHeight);
                    macroblocks[i] = mb;

                    //apply IDCT and dequantization
                    applyIDCTAndDequantize(mb, frameData, width, height, mb.isForeground ? n1 : n2);

                    //update coordinates
                    x += 16;
                    if (x >= width) {
                        x = 0;
                        y += 16;
                    }
                }

                //create a new frame image
                BufferedImage img = RGBFrameDataToImg(frameData);

                //draw debug outlines
                drawDebugOutlines(img, macroblocks);

                //add the frame to the list
                frames.add(img);
                frameCount++;
            }

            System.out.println("Total frames read: " + frameCount);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //method appendCoefficientsToStringBuilder(sb, mb.dctCoefficientsR,
    //mb.dctCoefficientsG, mb.dctCoefficientsB)
    public void appendCoefficientsToStringBuilder(StringBuilder sb, double[][][] rCoeffs, double[][][] gCoeffs,
            double[][][] bCoeffs) {

        for (int i = 0; i < 4; i++) {
            //append R coefficients
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    sb.append((int) rCoeffs[i][y][x]).append(" ");
                }
            }
            //append G coefficients
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    sb.append((int) gCoeffs[i][y][x]).append(" ");
                }
            }
            //append B coefficients
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    sb.append((int) bCoeffs[i][y][x]).append(" ");
                }
            }
        }

    }

    //method to append stringbuilder to file
    public void appendToFile(String filename, StringBuilder sb) {
        try (FileWriter writer = new FileWriter(filename, true)) {
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BufferedImage RGBFrameDataToImg(RGBFrameData in) {
        int width = 960;
        int height = 540;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int ind = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                byte a = 0;

                byte r = (byte) in.R[ind];
                byte g = (byte) in.G[ind];
                byte b = (byte) in.B[ind];

                int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                img.setRGB(x, y, pix);
                ind++;
            }
        }

        return img;
    }

    public void drawDebugOutlines(BufferedImage img, Macroblock[] macroblocks) {
        Graphics g = img.getGraphics();
        g.setColor(Color.WHITE);

        //draw macroblock outlines
        for (Macroblock mb : macroblocks) {
            g.setColor(mb.isForeground ? Color.RED : Color.BLUE);
            g.drawRect(mb.x, mb.y, mb.x_size, mb.y_size);

            //draw the motion vector
            g.setColor(Color.GREEN);
            int blockSize = 16;

            //draw a line indicating the motion vector
            if (mb.motion_vec != null) {
                int dx = mb.motion_vec[0];
                int dy = mb.motion_vec[1];
                g.drawLine(mb.x + blockSize / 2, mb.y + blockSize / 2, mb.x + blockSize / 2 + dx,
                        mb.y + blockSize / 2 + dy);

                //draw dot at the end of the motion vector
                g.setColor(Color.RED);
                g.fillOval(mb.x + blockSize / 2 + dx - 2, mb.y + blockSize / 2 + dy - 2, 2, 2);
            }
        }
        g.dispose();
    }

    public void displayLabeledFrame(BufferedImage img, BufferedImage img_prev, JLabel label, String text) {
        int outWidth = img.getWidth();
        int outHeight = img.getHeight();

        //create a new BufferedImage with space for the label text
        BufferedImage newImage = new BufferedImage(outWidth, outHeight + 50, BufferedImage.TYPE_INT_RGB);

        //get macroblocks

        System.out.println("Rendering outlines");

        //draw the original image and the label text onto the new image
        Graphics g = newImage.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        //adjust text position as needed 
        g.drawString(text, 20, outHeight + 30); 
        g.dispose();

        //update the JLabel with the new image
        label.setIcon(new ImageIcon(newImage));
        label.revalidate();
        label.repaint();
        frame.pack();
    }

    public void DisplayFrames(ArrayList<BufferedImage> frames) {
        //frame setup
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

        //display each frame
        for (int i = 0; i < frames.size(); i++) {
            System.out.println("Displaying frame " + i);
            displayLabeledFrame(frames.get(i), i > 0 ? frames.get(i - 1) : null, lbIm1, "Frame " + i);

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
                int blockWidth = macroblockSize;
                if (x + macroblockSize > width) {
                    blockWidth = macroblockSize - (x + macroblockSize - width);
                }
                int blockHeight = macroblockSize;
                if (y + macroblockSize > height) {
                    blockHeight = macroblockSize - (y + macroblockSize - height);
                }
                Macroblock mb = new Macroblock(x, y, blockWidth, blockHeight);

                macroblocks.add(mb);
            }
        }
        return macroblocks;
    }

    //WIP
    public Macroblock[] segmentFrame(RGBFrameData currentFrame, RGBFrameData previousFrame, int frameWidth,
            int frameHeight, int macroBlocksPerFrame) {
        int width = frameWidth;
        int height = frameHeight;
        int macroblockSize = 16;

        Macroblock[] macroblocks = new Macroblock[macroBlocksPerFrame];
        int macroblockIndex = 0;

        double[] globalMotion = new double[2];

        //calculate motion vectors for all macroblocks
        for (int y = 0; y < height; y += macroblockSize) {
            for (int x = 0; x < width; x += macroblockSize) {
                int blockWidth = Math.min(macroblockSize, width - x);
                int blockHeight = Math.min(macroblockSize, height - y);

                Macroblock mb = new Macroblock(x, y, blockWidth, blockHeight);

                if (previousFrame != null) {
                    int[] motionVector = calculateMotionVector(currentFrame, previousFrame, frameWidth, frameHeight, x,
                            y, macroblockSize);
                    mb.motion_vec = motionVector;

                    //calculate global motion
                    if (motionVector != null) {
                        globalMotion[0] += motionVector[0];
                        globalMotion[1] += motionVector[1];
                    }
                }

                macroblocks[macroblockIndex++] = mb;
            }
        }
        // if no previous frame, just mark all as foreground and return the macroblocks,
        // this is the first frame
        if (previousFrame == null) {
            for (Macroblock mb : macroblocks) {
                mb.isForeground = true;
            }
            return macroblocks;
        }

        //estimate global motion (average of all motion vectors)
        globalMotion[0] /= macroblocks.length;
        globalMotion[1] /= macroblocks.length;

        // System.out.printf("Global Motion Vector: (%f, %f)\n", globalMotion[0],
        // globalMotion[1]);

        //classify macroblocks as foreground or background
        for (Macroblock mb : macroblocks) {
            if (mb.motion_vec != null) {
                double relativeMotionX = mb.motion_vec[0] - globalMotion[0];
                double relativeMotionY = mb.motion_vec[1] - globalMotion[1];
                double relativeMagnitude = Math
                        .sqrt(relativeMotionX * relativeMotionX + relativeMotionY * relativeMotionY);

                mb.isForeground = relativeMagnitude > 3.5;
            } else {
                //default to background if no motion vector
                mb.isForeground = false;
            }
        }

        mergeForegroundRegions(macroblocks, frameWidth, frameHeight, macroblockSize);

        return macroblocks;
    }

    private void mergeForegroundRegions(Macroblock[] macroblocks, int frameWidth, int frameHeight, int macroblockSize) {
        int blocksPerRow = frameWidth / macroblockSize;

        for (int i = 0; i < macroblocks.length; i++) {
            Macroblock current = macroblocks[i];
            if (!current.isForeground)
                continue;

            int x = current.x / macroblockSize;
            int y = current.y / macroblockSize;

            //check neighbors (up, down, left, right)
            int[][] neighbors = { { x - 1, y }, { x + 1, y }, { x, y - 1 }, { x, y + 1 } };
            boolean hasForegroundNeighbor = false;

            for (int[] neighbor : neighbors) {
                int nx = neighbor[0];
                int ny = neighbor[1];

                if (nx >= 0 && ny >= 0 && nx < blocksPerRow && ny < (frameHeight / macroblockSize)) {
                    int neighborIndex = ny * blocksPerRow + nx;
                    Macroblock neighborBlock = macroblocks[neighborIndex];

                    if (neighborBlock.isForeground) {
                        hasForegroundNeighbor = true;
                        break;
                    }
                }
            }

            //if foreground block has no foreground neighbors, re-classify as background
            if (!hasForegroundNeighbor) {
                current.isForeground = false;
            }
        }
    }

    private int[] calculateMotionVector(RGBFrameData currentFrame, RGBFrameData previousFrame, int frame_width,
            int frame_height, int x, int y, int size) {
        int width = frame_width;
        int height = frame_height;

        int[] bestVector = { 0, 0 };
        double bestMAD = Double.MAX_VALUE;

        int searchRange = 4;

        for (int dy = -searchRange; dy <= searchRange; dy++) {
            for (int dx = -searchRange; dx <= searchRange; dx++) {
                double mad = 0;
                int validPixels = 0;

                for (int yy = 0; yy < size; yy++) {
                    for (int xx = 0; xx < size; xx++) {
                        int cx = x + xx;
                        int cy = y + yy;
                        int px = cx + dx;
                        int py = cy + dy;

                        //out-of-bound indices
                        px = Math.max(0, Math.min(px, width - 1));
                        py = Math.max(0, Math.min(py, height - 1));

                        if (cx < 0 || cx >= width || cy < 0 || cy >= height) {
                            continue;
                        }

                        //use luminance
                        int rCurrent = currentFrame.R[cy * width + cx];
                        int gCurrent = currentFrame.G[cy * width + cx];
                        int bCurrent = currentFrame.B[cy * width + cx];

                        int rPrevious = previousFrame.R[py * width + px];
                        int gPrevious = previousFrame.G[py * width + px];
                        int bPrevious = previousFrame.B[py * width + px];

                        int luminanceCurrent = (int) (0.299 * rCurrent + 0.587 * gCurrent + 0.114 * bCurrent);
                        int luminancePrevious = (int) (0.299 * rPrevious + 0.587 * gPrevious + 0.114 * bPrevious);

                        int diff = Math.abs(luminanceCurrent - luminancePrevious);
                        mad += diff;
                        validPixels++;
                    }
                }

                //normalize MAD
                if (validPixels > 0) {
                    mad /= validPixels;
                }

                //apply a penalty
                double penalty = (Math.abs(dx) + Math.abs(dy)) * 0.2;
                mad += penalty;

                if (mad < bestMAD) {
                    bestMAD = mad;
                    bestVector[0] = dx;
                    bestVector[1] = dy;
                }
            }
        }
        return bestVector;
    }

    public void applyDCTAndQuantize(Macroblock mb, RGBFrameData frame, int width, int height, int n) {
        int macroblockSize = 16;
        double[][] blockR = new double[8][8];
        double[][] blockG = new double[8][8];
        double[][] blockB = new double[8][8];

        int dctBlockCount = 0;
        for (int i = 0; i < macroblockSize; i += 8) {
            for (int j = 0; j < macroblockSize; j += 8) {
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        int px = mb.x + i + x;
                        int py = mb.y + j + y;
                        if (px >= width || py >= height)
                            continue;
                        int idx = py * width + px;
                        //store pixel values in 8x8 block, shift by -128 to center around 0
                        blockR[y][x] = frame.R[idx] - 128;
                        blockG[y][x] = frame.G[idx] - 128;
                        blockB[y][x] = frame.B[idx] - 128;
                    }
                }
                mb.dctCoefficientsR[dctBlockCount] = applyDCT(blockR);
                mb.dctCoefficientsG[dctBlockCount] = applyDCT(blockG);
                mb.dctCoefficientsB[dctBlockCount] = applyDCT(blockB);
                quantize(mb.dctCoefficientsR[dctBlockCount], n);
                quantize(mb.dctCoefficientsG[dctBlockCount], n);
                quantize(mb.dctCoefficientsB[dctBlockCount], n);
                dctBlockCount++;
            }
        }
    }

    //method to apply DCT to an 8x8 block
    private double[][] applyDCT(double[][] block) {

        double[][] dctBlock = new double[8][8];

        for (int u = 0; u < 8; u++) {
            for (int v = 0; v < 8; v++) {
                double sum = 0.0;
                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 8; y++) {
                        // TODO: Bitshift by 2 to divide by 2*8
                        sum += block[x][y] *
                                cosTableX[u][x] * cosTableY[v][y];
                    }
                }
                double cu = (u == 0) ? oneOverRoot2 : 1.0;
                double cv = (v == 0) ? oneOverRoot2 : 1.0;
                //scaling factor set at 1/4 for DCT
                dctBlock[u][v] = 0.25 * cu * cv * sum; 
            }
        }
        return dctBlock;
    }

    //method for power of 2
    private long TwotoPowerOfX(int x) {
        return (long) 1 << x;
    }

    private void quantize(double[][] block, int n) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                block[y][x] = Math.round(block[y][x] / TwotoPowerOfX(n));
            }
        }
    }

    private void applyIDCTAndDequantize(Macroblock mb, RGBFrameData frame, int width, int height, int n) {
        int macroblockSize = 16;
        double[][] blockR = new double[8][8];
        double[][] blockG = new double[8][8];
        double[][] blockB = new double[8][8];
        int dctBlockCount = 0;
        for (int i = 0; i < macroblockSize; i += 8) {
            for (int j = 0; j < macroblockSize; j += 8) {
                //dequantize coefficients
                dequantize(mb.dctCoefficientsR[dctBlockCount], n);
                dequantize(mb.dctCoefficientsG[dctBlockCount], n);
                dequantize(mb.dctCoefficientsB[dctBlockCount], n);

                //perform IDCT
                blockR = applyInverseDCT(mb.dctCoefficientsR[dctBlockCount]);
                blockG = applyInverseDCT(mb.dctCoefficientsG[dctBlockCount]);
                blockB = applyInverseDCT(mb.dctCoefficientsB[dctBlockCount]);
                dctBlockCount++;

                //store reconstructed pixel values back into the frame
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        int px = mb.x + i + x;
                        int py = mb.y + j + y;
                        if (px >= width || py >= height)
                            continue;
                        int idx = py * width + px;
                        frame.R[idx] = clipToByte(blockR[y][x] + 128);
                        frame.G[idx] = clipToByte(blockG[y][x] + 128);
                        frame.B[idx] = clipToByte(blockB[y][x] + 128);
                    }
                }
            }
        }
    }

    public double[][] applyInverseDCT(double[][] dctBlock) {
        int N = 8;
        double[][] reconstructedBlock = new double[N][N];
        for (int x = 0; x < N; x++) {
            for (int y = 0; y < N; y++) {
                double sum = 0.0;
                for (int u = 0; u < N; u++) {
                    for (int v = 0; v < N; v++) {
                        // Calculatscaling factors
                        double cu = (u == 0) ? oneOverRoot2 : 1.0;
                        double cv = (v == 0) ? oneOverRoot2 : 1.0;
                        sum += cu * cv * dctBlock[u][v] * cosTableX[u][x] * cosTableY[v][y];
                    }
                }
                reconstructedBlock[x][y] = 0.25 * sum;
            }
        }
        return reconstructedBlock;
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
                block[y][x] *= TwotoPowerOfX(n);
            }
        }
    }

    private int clipToByte(double value) {
        return (int) Math.max(0, Math.min(255, Math.round(value)));
    }
}