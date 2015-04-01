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