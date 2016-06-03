package com.htmlhifive.pitalium.explorer.image;


import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.htmlhifive.pitalium.common.exception.TestRuntimeException;
import com.htmlhifive.pitalium.image.model.DiffPoints;


/**
 * In this class, I changed some methods in com.htmlhifive.pitalium.image.util
 */
public final class ImageUtils2 {

	/**
	 * Constructor
	 */
	private ImageUtils2() {
	}



	/**
	 * get subimage from given image and rectangle
	 * @param image
	 * @param rectangle 
	 * @return subimage of given area
	 */
	private static BufferedImage getSubImage(BufferedImage image, Rectangle rectangle) {
		// Initialize variables
		int width= (int)rectangle.getWidth(), height = (int)rectangle.getHeight();
		int x = (int)rectangle.getX(), y = (int)rectangle.getY();

		return image.getSubimage(x, y, width, height);
	}

	/**
	 * find dominant offest between two images
	 * @param expectedImage
	 * @param actualImage
	 * @param diffThreshold	threshold to ignore small difference
	 * @return	Offset contains offsetX and offsetY
	 */
	public static Offset findDominantOffset(BufferedImage expectedImage, BufferedImage actualImage, double diffThreshold) {
		
		// we don't need to check all elements, only check once at every STEP-st elements 
		int STEP = 10;
		
		// we need to restrict the maximum offset to avoid redundant checking
		int maxOffset = 10;
		
		// initialize size
		int expectedWidth = expectedImage.getWidth(), expectedHeight = expectedImage.getHeight();
		int actualWidth = actualImage.getWidth(), actualHeight = actualImage.getHeight();
		int subWidth = Math.min(expectedWidth, actualWidth), subHeight = Math.min(expectedHeight, actualHeight);
		int xMax = Math.abs(expectedWidth-actualWidth), yMax = Math.abs(expectedHeight-actualHeight);
		xMax = Math.min(xMax,maxOffset);
		yMax = Math.min(yMax,maxOffset);
		
		// check the type of relationship between two image sizes
		// if one of two is contained in the other, it has type 1 or 2.
		// else, it has type 3 or 4.
		int sizeRelationType = getSizeRelationType (expectedWidth, expectedHeight, actualWidth, actualHeight);
		int expectedXOffset=0, expectedYOffset = 0, actualXOffset=0, actualYOffset=0;
		
		// calculation method to find dominant offset depends on the type of this relation.
		switch (sizeRelationType) {
			case 1:
				// for type 1, actualImage is bigger than expectedImage
				// so we need to move a sub-rectangle only in actualImage
				actualXOffset = 1;
				actualYOffset = 1;
				break;
				
			case 2:
				// for type 2, expectedImage is bigger than actualImage
				// so we need to move a sub-rectangle only in expectedImage
				expectedXOffset = 1;
				expectedYOffset = 1;
				break;
				
			case 3:
				// for type 3, the width of expectedImage is larger,
				// and the height of actualImage is larger.
				// so we need to move a sub-rectangle rightward in expectedImage,
				// and downward in actualImage
				expectedXOffset = 1;
				actualYOffset = 1;
				break;
								
			case 4:
				// for type 4, the width of actualImage is larger,
				// and the height of expectedImage is larger.
				// so we need to move a sub-rectangle rightward in actualImage,
				// and downward in expectedImage
				expectedYOffset = 1;
				actualXOffset = 1;
				break;
			}

		// initialize the color array.
		int[] expectedColors = new int[expectedWidth * expectedHeight];
		int[] actualColors = new int[actualWidth * actualHeight];
		expectedImage.getRGB(0, 0, expectedWidth, expectedHeight, expectedColors, 0, expectedWidth);
		actualImage.getRGB(0, 0, actualWidth, actualHeight, actualColors, 0, actualWidth);
		int[] expectedRed = new int[expectedColors.length];
		int[] expectedGreen = new int[expectedColors.length];
		int[] expectedBlue = new int[expectedColors.length];
		int[] actualRed = new int[actualColors.length];
		int[] actualGreen = new int[actualColors.length];
		int[] actualBlue = new int[actualColors.length];

		for (int i = 0; i < expectedColors.length; i++) {
			Color expectedColor = new Color(expectedColors[i]);
			expectedRed[i] = expectedColor.getRed();
			expectedGreen[i] = expectedColor.getGreen();
			expectedBlue[i] = expectedColor.getBlue();
		}

		for (int i = 0; i < actualColors.length; i++) {
			Color actualColor = new Color(actualColors[i]);
			actualRed[i] = actualColor.getRed();
			actualGreen[i] = actualColor.getGreen();
			actualBlue[i] = actualColor.getBlue();
		}

		// the difference of Red, Green, and Blue, respectively.
		int r, g, b, bestX=0, bestY=0;

		// count the number of different points using threshold
		int thresDiffCount, thresDiffMin = -1;

		// Find the dominant offset moving the subimage in the bigger image.
		if (sizeRelationType == 1 || sizeRelationType == 2) {
		// containing case
			
			for (int y = 0; y <= yMax; y++) {
				for (int x = 0; x <= xMax; x++) {
					
					// find dominant offset
					thresDiffCount = 0;	
					for (int i = 0; i < subHeight; i=i+STEP) {
						for (int j = 0; j < subWidth; j=j+STEP) {
							r = expectedRed[expectedWidth*(i+y*expectedYOffset)+(j+x*expectedXOffset)] - 	actualRed[actualWidth*(i+y*actualYOffset)+(j+x*actualXOffset)];
							g = expectedGreen[expectedWidth*(i+y*expectedYOffset)+(j+x*expectedXOffset)]-	actualGreen[actualWidth*(i+y*actualYOffset)+(j+x*actualXOffset)];
							b = expectedBlue[expectedWidth*(i+y*expectedYOffset)+(j+x*expectedXOffset)] - actualBlue[actualWidth*(i+y*actualYOffset)+(j+x*actualXOffset)];
							if (r*r+g*g+b*b > 3*255*255*diffThreshold*diffThreshold)
								thresDiffCount++;
						}
					}
					// Find the minimal number of threshold different pixels.
					if (thresDiffCount < thresDiffMin || thresDiffMin == -1) {
						thresDiffMin = thresDiffCount;
						bestX = x;
						bestY = y;
					}
				}
			}
		} else {
		// not containing case
			
			// move sub-rectangle downward
			int x = 0;
			for (int y = 0; y <= yMax; y++) {
				
				// find dominant offset
				thresDiffCount = 0;	
				for (int i = 0; i < subHeight; i=i+STEP) {
					for (int j = 0; j < subWidth; j=j+STEP) {
						r = expectedRed[expectedWidth*(i+y*expectedYOffset)+(j+x*expectedXOffset)] - 	actualRed[actualWidth*(i+y*actualYOffset)+(j+x*actualXOffset)];
						g = expectedGreen[expectedWidth*(i+y*expectedYOffset)+(j+x*expectedXOffset)]-	actualGreen[actualWidth*(i+y*actualYOffset)+(j+x*actualXOffset)];
						b = expectedBlue[expectedWidth*(i+y*expectedYOffset)+(j+x*expectedXOffset)] - actualBlue[actualWidth*(i+y*actualYOffset)+(j+x*actualXOffset)];
						if (r*r+g*g+b*b > 3*255*255*diffThreshold*diffThreshold)
							thresDiffCount++;
					}
				}
					// Find the minimal number of threshold different pixels.
					if (thresDiffCount < thresDiffMin || thresDiffMin == -1) {
						thresDiffMin = thresDiffCount;
						bestX = x;
						bestY = y;
					}
			}
			
			// move sub-rectangle rightward
			int y = 0;
			for (x = 1; x <= xMax; x++) {

				// find dominant offset
				thresDiffCount = 0;	
				for (int i = 0; i < subHeight; i=i+STEP) {
					for (int j = 0; j < subWidth; j=j+STEP) {
						r = expectedRed[expectedWidth*(i+y*expectedYOffset)+(j+x*expectedXOffset)] - 	actualRed[actualWidth*(i+y*actualYOffset)+(j+x*actualXOffset)];
						g = expectedGreen[expectedWidth*(i+y*expectedYOffset)+(j+x*expectedXOffset)]-	actualGreen[actualWidth*(i+y*actualYOffset)+(j+x*actualXOffset)];
						b = expectedBlue[expectedWidth*(i+y*expectedYOffset)+(j+x*expectedXOffset)] - actualBlue[actualWidth*(i+y*actualYOffset)+(j+x*actualXOffset)];
						if (r*r+g*g+b*b > 3*255*255*diffThreshold*diffThreshold)
							thresDiffCount++;
					}
				}

				// Find the minimal number of threshold different pixels.
				if (thresDiffCount < thresDiffMin || thresDiffMin == -1) {
					thresDiffMin = thresDiffCount;
					bestX = x;
					bestY = y;
				}
			}
		}

		return new Offset(bestX, bestY);
	}
	
	/**
	 * using dominant offset, extract subImage from given expectedImage
	 * the size of subImage will be the same as the size of intersection of two images.
	 * @param expectedImage	the image which we want to extract sub-image from
	 * @param actualImage	the other image
	 * @param offset dominant offset between two images
	 * @return	subImage of expectedImage at dominant offset
	 */
	public static BufferedImage getDominantImage (BufferedImage expectedImage, BufferedImage actualImage, Offset offset) {
		
		// initialize size
		int expectedWidth = expectedImage.getWidth(), expectedHeight = expectedImage.getHeight();
		int actualWidth = actualImage.getWidth(), actualHeight = actualImage.getHeight();
		
		// dominant frame of expectedImage depends on size-relation type and offset
		int sizeRelationType = getSizeRelationType (expectedWidth, expectedHeight, actualWidth, actualHeight);
		switch (sizeRelationType) {
			case 1:
				return expectedImage;
			case 2:
				return expectedImage.getSubimage(offset.getX(), offset.getY(), actualWidth, actualHeight);
			case 3:
				if (offset.getX() > 0) {
					return expectedImage.getSubimage(offset.getX(), 0, actualWidth, expectedHeight); 
				} else {
					return expectedImage.getSubimage(0, 0, actualWidth, expectedHeight);
				}
			case 4:
				if (offset.getY() > 0) {
					return expectedImage.getSubimage(0, offset.getY(), expectedWidth, actualHeight);
				} else {
					return expectedImage.getSubimage(0,0,expectedWidth, actualHeight);
				}

			// never reach to default case
			default :
				return expectedImage;
		}
	}
	
	
	
	/**
	 * In order to find dominant offset, we have to consider the relationship between two image sizes.
	 * @param expectedWidth		width of expectedImage
	 * @param expectedHeight	height of expectedImage
	 * @param actualWidth		width of actualImage
	 * @param actualHeight		height of actualImage
	 * @return 	the number of type of size-relationship
	 * 			1 when actualImage is bigger than expectedImage
	 * 			2 when expectedImage is bigger than actualImage
	 * 			3 when the width of expectedImage is larger and the height of actualImage is larger.
	 *			4 when the width of actualImage is larger and the height of expectedImage is larger.
	 */
	private static int getSizeRelationType (int expectedWidth, int expectedHeight, int actualWidth, int actualHeight){
		if (expectedWidth <= actualWidth && expectedHeight <= actualHeight) {
			return 1;
		} else if (expectedWidth >= actualWidth && expectedHeight >= actualHeight) {
			return 2;
		} else if (expectedWidth >= actualWidth && expectedHeight < actualHeight) {
			return 3;
		} else {
			return 4;
		}
	}
	
	/***********************************************************************************/
	/** The below methods are from pitalium, but I needed to modify some part of them **/
	/** Before sending pull request, I use them here for the fast application 		  **/
	/**												- 2016.06.04. Yeongjin - 		  **/
	
	
	/***** com.htmlhifive.pitalium.image.util.ImageComparator *****/

	/**
	 * 2枚の画像を比較し、差分の一覧を取得します。
	 * 
	 * @param img1 画像1
	 * @param img1Area 画像1で比較の対象とする範囲
	 * @param img2 画像2
	 * @param img2Area 画像2で比較の対象とする範囲
	 * @param diffThreshold to extract 'real' difference, we need to use threshold.
	 * @return 比較結果の差分データ
	 */
	public static DiffPoints compare(BufferedImage img1, Rectangle img1Area, BufferedImage img2, Rectangle img2Area, double diffThreshold) {
		if (img1 == null || img2 == null) {
			throw new TestRuntimeException("Both img1 and img2 is required.");
		}
		//LOG.trace("[Compare] image1[w: {}, h: {}; {}]; image2[w: {}, h: {}: {}]", img1.getWidth(), img1.getHeight(),
		//		img1Area, img2.getWidth(), img2.getHeight(), img2Area);

		int offsetX = 0;
		int offsetY = 0;
		BufferedImage image1 = null;
		BufferedImage image2 = null;
		if (img1Area != null) {
			image1 = getSubImage(img1, img1Area);
			offsetX = (int) img1Area.getX();
			offsetY = (int) img1Area.getY();
		} else {
			image1 = img1;
		}
		if (img2Area != null) {
			image2 = getSubImage(img2, img2Area);
		} else {
			image2 = img2;
		}

		// In this comparison, we don't use sizeDiffPoints
		//List<Point> sizeDiffPoints = createSizeDiffPoints(image1, image2, offsetX, offsetY);
		List<Point> sizeDiffPoints = new ArrayList<Point>();
		List<Point> diffPoints = compare(image1, image2, offsetX, offsetY, diffThreshold);
		return new DiffPoints(diffPoints, sizeDiffPoints);
	}


	/***** com.htmlhifive.pitalium.image.util.DefaultImageComparator *****/


	protected static List<Point> compare(BufferedImage image1, BufferedImage image2, int offsetX, int offsetY, double diffThreshold) {
		//LOG.trace("[Compare] image1[w: {}, h: {}], image2[w: {}, h: {}], offset: ({}, {})", image1.getWidth(),
		//		image1.getHeight(), image2.getWidth(), image2.getHeight(), offsetX, offsetY);
		int width = Math.min(image1.getWidth(), image2.getWidth());
		int height = Math.min(image1.getHeight(), image2.getHeight());

		int[] rgb1 = getRGB(image1, width, height);
		int[] rgb2 = getRGB(image2, width, height);
		
		// different of red, green, and blue
		int r, g, b;
		
		List<Point> diffPoints = new ArrayList<Point>();
		for (int i = 0, length = rgb1.length; i < length; i++) {
			
			Color color1 = new Color(rgb1[i]), color2 = new Color(rgb2[i]);
			r = color1.getRed() - color2.getRed();
			g = color1.getGreen() - color2.getGreen();
			b = color1.getBlue() - color2.getBlue();
			if (r*r+g*g+b*b > 3*255*255*diffThreshold*diffThreshold) {
				int x = (i % width) + offsetX;
				int y = (i / width) + offsetY;

				Point diffPoint = new Point(x, y);
				diffPoints.add(diffPoint);
				//LOG.trace("[Compare] Diff found ({}, {}). #{} <=> #{}", diffPoint.x, diffPoint.y,
				//		Integer.toHexString(rgb1[i]), Integer.toHexString(rgb2[i]));
			}
		}

		if (!diffPoints.isEmpty()) {
			//LOG.debug("[Compare] {} diff found.", diffPoints.size());
		}
		return diffPoints;
	}
	/**
	 * 指定した画像のRGBベースのピクセル配列を取得します。
	 * 
	 * @param image 対象の画像
	 * @param width 読み込む幅
	 * @param height 読み込む高さ
	 * @return ピクセル配列
	 */
	private static int[] getRGB(BufferedImage image, int width, int height) {
		return image.getRGB(0, 0, width, height, null, 0, width);
	}

}
