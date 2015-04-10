/**
 * LocaPic
 * Copyright (C) 2015  Virgile Beguin and Samuel Beaurepaire
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
