package fr.rt.acy.locapic.camera;

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
