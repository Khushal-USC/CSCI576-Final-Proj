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

public class ImageHelper {

	public static void print2DArray(double[][] arr){
		for (int i = 0; i < arr.length; i++) {
			for (int j = 0; j < arr[i].length; j++) {
				System.out.print(arr[i][j] + " ");
			}
			System.out.println();
		}
	}

    //Method to convert integer 2D array to double 2D array
	public static double[][] convertIntToDouble(int[][] arr){
		double[][] doubleArr = new double[arr.length][arr[0].length];
		for (int i = 0; i < arr.length; i++) {
			for (int j = 0; j < arr[i].length; j++) {
				doubleArr[i][j] = (double)arr[i][j];
			}
		}
		return doubleArr;
	}

    
	 public static void matrixTransposeInPlace(double[][] matrix) {
		int n = matrix.length; 
		for (int row = 0; row < n; row++) {
			for (int column = row + 1; column < n; column++) { 
				double temp = matrix[row][column];
				matrix[row][column] = matrix[column][row];
				matrix[column][row] = temp;
			}
		}
	}
    
	//Method to duplicate the 1D double array
	public static double[] duplicateArray(double[] arr){
		double[] newArr = new double[arr.length];
		for (int i = 0; i < arr.length; i++) {
			newArr[i] = arr[i];
		}
		return newArr;
	}

    public static int clamp(int value, int min, int max) {
		return (int)Math.max(min, Math.min(max, value));
	}

	public static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	//Method to normalize the vector array
	private double[] normalizeVector(double dx, double dy) {
		int Dy = (int)dy;
		int Dx = (int)dx;
		double magnitude = Math.sqrt(dx * dx + dy * dy);
		if (magnitude == 0) {
			return new double[]{0.0, 0.0}; // Handle zero motion
		}
		return new double[]{dx / magnitude, dy / magnitude};
	}
	
}