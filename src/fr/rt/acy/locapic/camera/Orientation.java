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
 * Orientation du telephone : potrait, paysage ou paysage inverse
 */
public enum Orientation 
{
	PORTRAIT("Portrait", 90, 0), PAYSAGE_0("Paysage 0", 0, 90), PAYSAGE_180("Paysage 180", 180, -90);
	
	private String nom;
	private int degree;
	/* Rotation a effectuer pour passer une View creee
	 * en paysage dans la nouvelle orientation d'ecran */
	private int rotation;
	
	Orientation(String nom, int degree, int rotation)
	{
		this.nom = nom;
		this.degree = degree;
		this.rotation = rotation;
	}
	
	@Override
	public String toString() 
	{
		return nom;
	}
	
	/**
	 * @return orientation en degre par rapport a PAYSAGE_0
	 */
	public int getDegree()
	{
		return degree;
	}
	
	/**
	 * @return la rotation a effectuer pour passer dans cette 
	 * orientation a partir de l'orientation portrait
	 */
	public int getRotation()
	{
		return rotation;
	}
}
