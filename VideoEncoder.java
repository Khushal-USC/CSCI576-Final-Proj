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

            for (int i = 0; i < 10; i++) {
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
                        byte g = bytes[ind+(height*width)];
                        byte b = bytes[ind+(height*width*2)];

                        // R.add((int)r);
                        // G.add((int)g);
                        // B.add((int)b);

                        int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                        //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                        //int pix = 0xff32A852;
                        frameData.addPixel((int)r, (int)g, (int)b);
                        ind++;
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



    public void displayLabeledFrame(BufferedImage img, JLabel label, String text) {
		int outWidth = img.getWidth();
		int outHeight = img.getHeight();
	
		// Create a new BufferedImage with space for the label text
		BufferedImage newImage = new BufferedImage(outWidth, outHeight + 50, BufferedImage.TYPE_INT_RGB);
	
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
            displayLabeledFrame(frames.get(i), lbIm1, "Frame " + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}