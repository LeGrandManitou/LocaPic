package fr.rt.acy.locapic.camera;

/**
 * Taille et resolution de la camera
 */
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
	
	@Override
	public String toString()
	{
		return String.valueOf(resolution) + "M (" + String.valueOf(width) + "x" + String.valueOf(height) + ")";
	}

	/**
	 * @return la largeur
	 */
	public int getWidth() 
	{
		return width;
	}

	/**
	 * @param width la largeur
	 */
	public void setWidth(int width) 
	{
		this.width = width;
		updateResolution();
	}

	/**
	 * @return la hauteur
	 */
	public int getHeight() 
	{
		return height;
	}

	/**
	 * @param height la hauteur
	 */
	public void setHeight(int height) 
	{
		this.height = height;
		updateResolution();
	}

	/**
	 * @return la resolution
	 */
	public float getResolution() 
	{
		return resolution;
	}
	
	private void updateResolution()
	{
		resolution = (int) ((width * height) / 100000);
		resolution = resolution / 10;
	}
}
