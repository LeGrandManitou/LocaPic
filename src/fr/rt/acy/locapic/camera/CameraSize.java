package fr.rt.acy.locapic.camera;

public class CameraSize
{
	private int width;
	private int height;
	private float resolution;	// arrondi au 1/10 en Mpixels
	
	public CameraSize(int width, int height) 
	{
		this.setWidth(width);
		this.setHeight(height);
		updateResolution();
	}

	public int getWidth() 
	{
		return width;
	}

	public void setWidth(int width) 
	{
		this.width = width;
		updateResolution();
	}

	public int getHeight() 
	{
		return height;
	}

	public void setHeight(int height) 
	{
		this.height = height;
		updateResolution();
	}

	public float getResolution() 
	{
		return resolution;
	}
	
	private void updateResolution()
	{
		resolution = (int) ((width * height) / 100000);
		resolution = resolution / 10;
	}
	
	@Override
	public String toString()
	{
		return String.valueOf(resolution) + "M (" + String.valueOf(width) + "x" + String.valueOf(height) + ")";
	}
}
