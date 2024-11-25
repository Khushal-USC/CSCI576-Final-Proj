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

public class VideoEncoder {

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
		// Read .rgb input
		ArrayList<BufferedImage> frames = readFrames(filePath, width, height);

        // TODO: video segmentation 

        // Write .cmp output
        String outputFilePath = "temp.cmp";
        writeFrames(outputFilePath, frames, width, height);
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
            System.out.println(totalBytes + " " + frameLength);
            // System.out.println(numFrames);

            for (int i = 0; i < numFrames; i++) {
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

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
                        byte g = bytes[ind+(height*width)];
                        byte b = bytes[ind+(height*width*2)];

                        // R.add((int)r);
                        // G.add((int)g);
                        // B.add((int)b);

                        int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                        //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                        //int pix = 0xff32A852;
                        img.setRGB(x,y,pix);
                        ind++;
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

            for (BufferedImage frame : frames) {
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
}