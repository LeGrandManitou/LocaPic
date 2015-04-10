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
 * Mode de declanchement du flash de l'apareil photo : automatique, allume ou eteint
 */
public enum Flash 
{
	AUTO("AUTO", 0), ON("ON", 1), OFF("OFF", 2);
	private String nom;
	private int index;
	
	Flash(String nom, int index)
	{
		this.nom = nom;
		this.index = index;
	}
	
	public int getIndex()
	{
		return index;
	}
	
	public String toString()
	{
		return nom;
	}
}
