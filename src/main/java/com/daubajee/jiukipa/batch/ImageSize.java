package com.daubajee.jiukipa.batch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageSize {

	private final int width;
	
	private final int height;
	
	private final boolean landscape;
	
	public static final ImageSize THUMBSNAIL = new ImageSize(80, 60); 
	public static final ImageSize SCREEN = new ImageSize(1024, 768); 
	public static final ImageSize FHD = new ImageSize(1920, 1440); 
	public static final ImageSize THUMBSNAIL_P = new ImageSize(60, 80); 
	public static final ImageSize SCREEN_P = new ImageSize(768, 1024); 
	public static final ImageSize FHD_P = new ImageSize(1440, 1920); 
	
	public ImageSize(int width, int height) {
		this.width = width;
		this.height = height;
		landscape = width > height;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public boolean isLandscape() {
		return landscape;
	}
	
	public static List<ImageSize> getProfiles(int width, int height) {
		if (width > height) {
			if (width > FHD.width) {
				return Arrays.asList(FHD, SCREEN, THUMBSNAIL);
			}
			if (width > SCREEN.width) {
				return Arrays.asList(SCREEN, THUMBSNAIL);
			}
			if (width > THUMBSNAIL.width) {
				return Arrays.asList(THUMBSNAIL);
			}
			return new ArrayList<>(0);
		}
		else {
			if (width > FHD_P.width) {
				return Arrays.asList(FHD_P, SCREEN_P, THUMBSNAIL_P);
			}
			if (width > SCREEN_P.width) {
				return Arrays.asList(SCREEN_P, THUMBSNAIL_P);
			}
			if (width > THUMBSNAIL_P.width) {
				return Arrays.asList(THUMBSNAIL_P);
			}
			return new ArrayList<>(0);
		}
	}

    public static ImageSize getLeastSmallestStdSize(int width, int height) {
        if (width > height) {
            if (width >= FHD.width) {
                return FHD;
            }
            if (width > SCREEN.width) {
                return SCREEN;
            }
            return THUMBSNAIL;
        } else {
            if (width >= FHD_P.width) {
                return FHD_P;
            }
            if (width > SCREEN_P.width) {
                return SCREEN_P;
            }
            return THUMBSNAIL_P;
        }
    }
	@Override
	public String toString() {
		return "ImageSize [width=" + width + ", height=" + height + ", landscape=" + landscape + "]";
	}
	
	public static void main(String[] args) {
		System.out.println(ImageSize.THUMBSNAIL);
		System.out.println(ImageSize.SCREEN);
		System.out.println(ImageSize.FHD);
		System.out.println(ImageSize.THUMBSNAIL_P);
		System.out.println(ImageSize.SCREEN_P);
		System.out.println(ImageSize.FHD_P);
	}
}

