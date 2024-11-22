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

public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;
	int N = 8; // Block size is 8x8
	double piOver2N = Math.PI / (2 * N);
	
	//Hope we dont have to zigzag order for anything other than 8x8 lol
	int[] zigzag8x8rows = {
		0, 0, 1, 2, 1, 0, 0, 1, 2, 3, 4, 3, 2, 1, 0, 0, 1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1, 0, 0, 1, 2, 3, 4, 5, 6, 7, 7, 6, 5, 4, 3, 2, 1, 2, 3, 4, 5, 6, 7, 7, 6, 5, 4, 3, 4, 5, 6, 7, 7, 6, 5, 6, 7, 7
	};

	int[] zigzag8by8cols = {
		0, 1, 0, 0, 1, 2, 3, 2, 1, 0, 0, 1, 2, 3, 4, 5, 4, 3, 2, 1, 0, 0, 1, 2, 3, 4, 5, 6, 7, 6, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 6, 7, 7, 6, 5, 4, 3, 2, 3, 4, 5, 6, 7, 7, 6, 5, 4, 5, 6, 7, 7, 6, 7
	};

	private static final double[][] cosTableX = new double[8][8];
	private static final double[][] cosTableY = new double[8][8];

	double oneOverRoot2 = 1 / Math.sqrt(2);

	//REAL MAIN
	public static void main(String[] args) {
		// Check if the correct number of arguments is passed
        if (args.length != 2) {
            System.out.println("Incorrect Param Length: See README for usage instructions.");
			System.out.println(args.length);
			for (String arg : args) {
				System.out.println(arg);
			}
            return;
        }

        // Parse command-line arguments
        String filePath = args[0];
        int width = 512;
        int height = 512;
		int n = 0;

        try {
            n = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Error: h1, h2 should be integers.");
            return;
        }

        System.out.println("File Path: " + filePath);
        System.out.println("Width: " + width);
        System.out.println("Height: " + height);
		System.out.println("Coefficient: " + n);

		ImageDisplay ren = new ImageDisplay();
		int outWidth = width;
		int outHeight = height;

		ren.showIms(filePath, width, height, outWidth, outHeight, n);
		System.out.println();

	}

	public void showIms(String filePath, int width, int height, int outWidth, int outHeight, int n) {
		precomputeCosTables();

		// Get the RGB values of the image
		ArrayList<Integer> R = new ArrayList<Integer>();
		ArrayList<Integer> G = new ArrayList<Integer>();
		ArrayList<Integer> B = new ArrayList<Integer>();

		// Read in the specified image
		BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, filePath, res, R, G, B);

		// Fix the negative values of the R, G, B channels
		fixNegativeValues(R, G, B);

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

		if(n <= 0) {
			// Display the single image
			displayLabeledImage(res, lbIm1, "Original Image");
		} else {
			// Generate the DCT image
			BufferedImage imgDCT = generateDCTImage(width, height, outWidth, outHeight, n, R, G, B);

			// Display the DCT image
			displayLabeledImage(imgDCT, lbIm1, "DCT Image");
		}
	}
	// Helper method to display combined DCT and DWT images with labels
	private void displayCombinedImage(BufferedImage imgDCT, BufferedImage imgDWT, int iteration, JLabel label, int dctNum, int dwtNum, int dctMax, int dwtMax, int dctCoefficients, int dwtCoefficients) {
		int outWidth = imgDCT.getWidth();
		int outHeight = imgDCT.getHeight();

		// Create a combined image to display DCT and DWT side by side with labels
		BufferedImage combinedImage = new BufferedImage(outWidth * 2, outHeight + 150, BufferedImage.TYPE_INT_RGB);
		Graphics g = combinedImage.getGraphics();

		g.drawImage(imgDCT, 0, 0, null); 
		g.drawImage(imgDWT, outWidth, 0, null); 

		// Add labels below the images
		g.setColor(Color.WHITE);
		g.setFont(new Font("Arial", Font.BOLD, 20));
		g.drawString("DCT Coefficents: " + dctCoefficients, 100, outHeight + 50);
		g.drawString( dctNum +"/" + dctMax, 100, outHeight + 100);
		g.drawString("DWT Coefficents: " + dwtCoefficients, outWidth + 100, outHeight + 50);
		g.drawString( dwtNum +"/" + dwtMax, outWidth + 100, outHeight + 100);
		g.dispose();

		// Update the JLabel with the new combined image
		label.setIcon(new ImageIcon(combinedImage));
		label.revalidate();
		label.repaint();
		frame.pack();
	}

	// Method to display just single image
	public void displayLabeledImage(BufferedImage img, JLabel label, String text) {
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


	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img, ArrayList<Integer> R, ArrayList<Integer> G, ArrayList<Integer> B)
	{
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

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

					R.add((int)r);
					G.add((int)g);
					B.add((int)b);

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					//int pix = 0xff32A852;
					img.setRGB(x,y,pix);
					ind++;
				}
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
	}

	// Method to apply DCT to an 8x8 block
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
                dctBlock[u][v] = 0.25 * cu * cv * sum; // Scaling factor 1/4 for DCT
            }
        }
        return dctBlock;
    }

	// Method to shift the values of the 1D Arraylist down by 128
	public ArrayList<Double> shiftDown128(ArrayList<Double> arr) {
		ArrayList<Double> centeredArr = new ArrayList<>(arr);
		for (int i = 0; i < centeredArr.size(); i++) {
			centeredArr.set(i, centeredArr.get(i) - 128);
		}
		return centeredArr;
	}

	// Method to shift the values of the 1D Arraylist up by 128
	public ArrayList<Double> shiftUp128(ArrayList<Double> arr) {
		ArrayList<Double> centeredArr = new ArrayList<>(arr);
		for (int i = 0; i < centeredArr.size(); i++) {
			centeredArr.set(i, centeredArr.get(i) + 128);
		}
		return centeredArr;
	}

	// Method to apply DCT across the entire channel stored as a 1D ArrayList
    public ArrayList<Double> applyDCTandEncodeChannel(ArrayList<Double> channel, int m) {
		
        ArrayList<Double> dctChannel = new ArrayList<>(channel);
        
        for (int i = 0; i < 512; i += 8) {
            for (int j = 0; j < 512; j += 8) {
                // Extract the 8x8 block
                double[][] block = new double[8][8];
                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 8; y++) {
                        int index = (i + x) * 512 + (j + y);
                        block[x][y] = channel.get(index);
                    }
                }

                // Perform DCT on the block
                double[][] dctBlock = applyDCT(block);

				//Encode the DCT block using zigzag order
				encodeDCTBlock(dctBlock, m);

                // Store the DCT coefficients back in the 1D ArrayList
                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 8; y++) {
                        int index = (i + x) * 512 + (j + y);
                        dctChannel.set(index, dctBlock[x][y]);
                    }
                }
            }
        }
        return dctChannel;
    }

	// Method to apply Inverse DCT across the entire channel stored as a 1D ArrayList
	public ArrayList<Double> applyInverseDCTOnChannel(ArrayList<Double> dctChannel) {
		ArrayList<Double> channel = new ArrayList<>(dctChannel);
		
		for (int i = 0; i < 512; i += 8) {
			for (int j = 0; j < 512; j += 8) {
				// Extract the 8x8 block
				double[][] dctBlock = new double[8][8];
				for (int x = 0; x < 8; x++) {
					for (int y = 0; y < 8; y++) {
						int index = (i + x) * 512 + (j + y);
						dctBlock[x][y] = dctChannel.get(index);
					}
				}

				// Perform Inverse DCT on the block
				double[][] block = applyInverseDCT(dctBlock);

				// Store the pixel values back in the 1D ArrayList
				for (int x = 0; x < 8; x++) {
					for (int y = 0; y < 8; y++) {
						int index = (i + x) * 512 + (j + y);
						channel.set(index, block[x][y]);
					}
				}
			}
		}
		return channel;
	}

	public void selectCoefficients(double[][] channel, int numCoefficients) {
		int retainSize = (int) Math.sqrt(numCoefficients); // Size of lower-left square (e.g., 64x64 for 4096)
		for (int i = 0; i < 512; i++) {
			for (int j = 0; j < 512; j++) {
				if (i >= retainSize || j >= retainSize) {
					channel[i][j] = 0;
				}
			}
		}
	}

	//Encode the DCT coefficients using zigzag order
	public double[][] encodeDCTBlock(double[][] dctBlock, int m) {
		int count = 0;
		//Interate through 8x8 block
		for (int i = 0; i < 8; i++) {
			for(int j = 0; j < 8; j++) {
				if (count >= m) {
					dctBlock[zigzag8x8rows[count]][zigzag8by8cols[count]] = 0;
				}
				count++;
			}
		}
		return dctBlock;
	}

	public double[][] applyInverseDCT(double[][] dctBlock) {
		
		double[][] reconstructedBlock = new double[N][N];
		for (int x = 0; x < N; x++) {
			for (int y = 0; y < N; y++) {
				double sum = 0.0;
				for (int u = 0; u < N; u++) {
					for (int v = 0; v < N; v++) {
						// Calculatscaling factors
						double cu = (u == 0) ? oneOverRoot2 : 1.0;
						double cv = (v == 0) ? oneOverRoot2 : 1.0;
						sum += cu * cv * dctBlock[u][v] * cosTableX[u][x] * cosTableY[v][y];;
					}
				}
				reconstructedBlock[x][y] = 0.25 * sum;
			}
		}
		return reconstructedBlock;
	}

	public void precomputeCosTables(){
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


	private int getR(int color){
		return ((color & 0xff0000) >> 16);
	}
	private int getG(int color){
		return ((color & 0xff00) >> 8);
	}
	private int getB(int color){
		return (color & 0xff);
	}

	
	// Method to generate the DCT image
	public BufferedImage generateDCTImage(int width, int height, int outWidth, int outHeight, int n, ArrayList<Integer> R, ArrayList<Integer> G, ArrayList<Integer> B){
		// Apply DCT to the R, G, B channels
		ArrayList<Double> R_dct = new ArrayList<Double>();
		ArrayList<Double> G_dct = new ArrayList<Double>();
		ArrayList<Double> B_dct = new ArrayList<Double>();

		//Shift the values of the R, G, B channels down by 128
		for (int i = 0; i < R.size(); i++) {
			R_dct.add((double)R.get(i) - 128);
			G_dct.add((double)G.get(i) - 128);
			B_dct.add((double)B.get(i) - 128);
		}
		//m is the number of coefficients to keep per block for DCT encoding 
		int m = Math.round(n/4096);
		R_dct = applyDCTandEncodeChannel(R_dct, m);
		G_dct = applyDCTandEncodeChannel(G_dct, m);
		B_dct = applyDCTandEncodeChannel(B_dct, m);

		// Apply Inverse DCT
		ArrayList<Integer> R_out_DCT = new ArrayList<Integer>();
		ArrayList<Integer> G_out_DCT = new ArrayList<Integer>();
		ArrayList<Integer> B_out_DCT = new ArrayList<Integer>();

		R_dct = applyInverseDCTOnChannel(R_dct);
		G_dct = applyInverseDCTOnChannel(G_dct);
		B_dct = applyInverseDCTOnChannel(B_dct);

		for (int i = 0; i < R_dct.size(); i++) {
			R_out_DCT.add((int)R_dct.get(i).doubleValue());
			G_out_DCT.add((int)G_dct.get(i).doubleValue());
			B_out_DCT.add((int)B_dct.get(i).doubleValue());
		}

		//Shift the values of the R, G, B channels up by 128
		for (int i = 0; i < R_out_DCT.size(); i++) {
			R_out_DCT.set(i, R_out_DCT.get(i) + 128);
			G_out_DCT.set(i, G_out_DCT.get(i) + 128);
			B_out_DCT.set(i, B_out_DCT.get(i) + 128);
		}

		// Clamp the values to the range [0, 255]
		for (int i = 0; i < R_out_DCT.size(); i++) {
			R_out_DCT.set(i, ImageHelper.clamp(R_out_DCT.get(i), 0, 255));
			G_out_DCT.set(i, ImageHelper.clamp(G_out_DCT.get(i), 0, 255));
			B_out_DCT.set(i, ImageHelper.clamp(B_out_DCT.get(i), 0, 255));
		}

		// Create the output image
		BufferedImage imgDCTOut = new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < outHeight; y++) {
			for (int x = 0; x < outWidth; x++) {
				int index = y * outWidth + x;
				int r = R_out_DCT.get(index);
				int g = G_out_DCT.get(index);
				int b = B_out_DCT.get(index);
				int rgb = (r << 16) | (g << 8) | b;
				imgDCTOut.setRGB(x, y, rgb);
			}
		}
		return imgDCTOut;
	}

	// Method to fix the negative values of the R, G, B channels
	public void fixNegativeValues(ArrayList<Integer> R, ArrayList<Integer> G, ArrayList<Integer> B) {
		//Fix negative values of R, G, B channels
		for (int i = 0; i < R.size(); i++) {
			//Get the R, G, B values
			int r = R.get(i);
			int g = G.get(i);
			int b = B.get(i);

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

			//Set the R, G, B values
			R.set(i, r);
			G.set(i, g);
			B.set(i, b);
		}
	}

}
