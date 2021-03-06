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
package fr.rt.acy.locapic.gps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import fr.rt.acy.locapic.R;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class TrackService extends Service implements android.location.LocationListener, android.location.GpsStatus.NmeaListener {
	/**
	 * Attributs du service
	 */
	// TAG pour les logs
	private static final String TAG = "trackService";
	// LocationManager pour recuperer la position, enregistrer un ecouteur, etc...
	private LocationManager locationManager = null;
	// Intervalles de prises de position (en millisecondes et en metres)
	private static int LOCATION_INTERVAL = 2000;
	private static float LOCATION_DISTANCE = 0;
	// Variable de type entier pour des tests
	private static int test = 0;
	// Chaine GSA (pour les infos DOP)
	private String GSA = null;
	// SharedPreferences pour recuperer les preferences utilisateurs
	private SharedPreferences pref;
	// Editor pour editer les preferences utilisateurs
	private SharedPreferences.Editor prefEditor;
	// Espace de nom XML utilise par nos fichier GPX
	private Namespace ns = Namespace.getNamespace("http://www.topografix.com/GPX/1/1");
	// TRACKING pour savoir si on track ou pas
	private static boolean TRACKING = false;
	// Nom de notre nouveau fichier
	private static String FILE_NAME;
	// Enregistrement sur media externe (carte SD), mis a true si ce dernier est disponible
	private static boolean FILE_EXT = false;
	// Notre repertoire sur le potentiel media externe
	private final String EXT_FILES_DIR = Environment.getExternalStorageDirectory().getPath() + "/MyTracks/";
	// Action de l'intent envoye pour l'interface graphique
	public static final String INTENT_ACTION = "MY_ACTION";
	
	/**
	 * Methode pour creer le document JDOM de base pour le fichier GPX
	 * Prend en parametre des valeurs pour les metadonnees GPX (contenues dans la balise <metadata>)
	 * @param name - Nom du fichier gpx (pour les metadonnees GPX)
	 * @param desc - Description du fichier gpx (pour les metadonnees GPX)
	 * @param authorsName - Nom de l'autheur de la trace (pour les metadonnees GPX)
	 * @param authorsEmail - Email de l'autheur de la trace (pour les metadonnees GPX)
	 * @param keywords - Mots-cle pour les metadonnees GPX
	 * @return document - Le document JDOM servant de base GPX
	 */
	public Document createGpxDocTree(String name, String desc, String authorsName, String authorsEmail, String keywords) {
		/* Pour la date */
		Calendar calendar = Calendar.getInstance();
		Date date = calendar.getTime();
		// Format de la date
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
		StringBuilder dateBuilder = new StringBuilder(dateFormat.format(date));
		// Pour le format GPX, T apres la date et avant l'heure, et Z a la fin
		dateBuilder.append("Z");
		dateBuilder.insert(10, "T");
		// Conversion builder => string
		String formattedDate = dateBuilder.toString();
		Log.v(TAG, "TIME => "+formattedDate);
		
		/* Pour le reste des metadonnees perso */
		String mailId;
		String mailDomain;
		if(name == null)
			name = "LocaPic track";
		if(desc == null)
			desc = "GPS track logged on an Android device with an application from a project by Samuel Beaurepaire &amp; Virgile Beguin for IUT of Annecy (Fr), RT departement.";
		if(authorsName == null)
			authorsName = "Samuel Beaurepaire";
		if(authorsEmail == null) {
			mailId = "sjbeaurepaire";
			mailDomain = "orange.fr";
		} else {
			String[] mail = authorsEmail.split("@", 2);
			mailId = mail[0];
			mailDomain = mail[1];
		}
		
		// xsi du namespace a indique seulement pour la racine (addNamespaceDeclaratin())
		Namespace XSI = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		// Creation de la racine du fichier gpx (<gpx></gpx>) sous forme d'objet de classe Element avec le namespace gpx
		Element xml_gpx = new Element("gpx", ns);
		// Namespace XSI et attributs
		xml_gpx.addNamespaceDeclaration(XSI);
		xml_gpx.setAttribute(new Attribute("schemaLocation", "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd", XSI));
		xml_gpx.setAttribute(new Attribute("creator", "LocaPic"));
		xml_gpx.setAttribute(new Attribute("version", "1.1"));

		// On cree un nouveau Document JDOM base sur la racine que l'on vient de creer
		Document document = new Document(xml_gpx);
		// ~~~~~~~~~~~~~~~~ <metadata> ~~~~~~~~~~~~~~~~
		Element xml_metadata = new Element("metadata", ns);
		xml_gpx.addContent(xml_metadata);
			// ~~~~~~~~~~~~~~~~ <name> ~~~~~~~~~~~~~~~~
			Element xml_metadata_name = new Element("name", ns);
			xml_metadata_name.addContent(name);
			xml_metadata.addContent(xml_metadata_name);
			// ~~~~~~~~~~~~~~~~ <desc> ~~~~~~~~~~~~~~~~
			Element xml_metadata_desc = new Element("desc", ns);
			xml_metadata_desc.addContent(desc);
			xml_metadata.addContent(xml_metadata_desc);
			// ~~~~~~~~~~~~~~~~ <author> ~~~~~~~~~~~~~~~~
			Element xml_metadata_author = new Element("author", ns);
				// ~~~~~~~~~~~~~~~~ <author> ~~~~~~~~~~~~~~~~
				Element xml_metadata_author_name = new Element("name", ns);
				xml_metadata_author_name.addContent(authorsName);
				xml_metadata_author.addContent(xml_metadata_author_name);
				// ~~~~~~~~~~~~~~~~ <email> ~~~~~~~~~~~~~~~~
				Element xml_metadata_author_email = new Element("email", ns);
				xml_metadata_author_email.setAttribute("id", mailId);
				xml_metadata_author_email.setAttribute("domain", mailDomain);
				xml_metadata_author.addContent(xml_metadata_author_email);
			xml_metadata.addContent(xml_metadata_author);
			// ~~~~~~~~~~~~~~~~ <time> ~~~~~~~~~~~~~~~~
			Element xml_metadata_time = new Element("time", ns);
			xml_metadata_time.addContent(formattedDate);
			xml_metadata.addContent(xml_metadata_time);
			// ~~~~~~~~~~~~~~~~ <keywords> ~~~~~~~~~~~~~~~~
			if (keywords != null) {
				Element xml_keywords = new Element("keywords", ns);
				xml_keywords.addContent(keywords);
				xml_metadata.addContent(xml_keywords);
			}
		// ~~~~~~~~~~~~~~~~ <trk> ~~~~~~~~~~~~~~~~
		Element xml_trk = new Element("trk", ns);
			// ~~~~~~~~~~~~~~~~ <number> ~~~~~~~~~~~~~~~~
			Element xml_trk_number = new Element("number", ns);
			xml_trk_number.addContent("1");
			xml_trk.addContent(xml_trk_number);
			// ~~~~~~~~~~~~~~~~ <trkseg> ~~~~~~~~~~~~~~~~
			Element xml_trk_trkseg = new Element("trkseg", ns);
			xml_trk.addContent(xml_trk_trkseg);
		xml_gpx.addContent(xml_trk);
		
		return document;
	}
	
	/**
	 * Creer le nom du fichier a partir des fichiers du parametre dir et de la date
	 * Le nom du fichier est de la forme : [baseName]-[date]-[n].gpx
	 * Avec baseName, un nom generique, ici track
	 * date au format aaaaMMdd
	 * n, un numero de fichier
	 * @param dir - Repertoire ou sont stocke les fichiers
	 * @param date - date du nom de fichier a creer
	 * @return name - le nom du fichier creer
	 */
	private String createFilename (String dir, String date) { /// date format : aaaammdd (comme les fichiers)
		int nMax = 0;
		String baseName = getResources().getString(R.string.gpx_file_prefix);
		// Listing des fichiers du repertoire $dir dans un tableau
		File file[] = new File(dir).listFiles();
		//Log.v(TAG, "Path: " + dir);
		//Log.v(TAG, "Size: "+ file.length);
		
		for (int i=0; i < file.length; i++) {
			/*
			 * Si le nom du fichier correspond a la regex suivante :
			 * "trace-aaaammdd-n.gpx"
			 * avec aaaammdd = $date et n, un nombre (au moins un chiffre)
			 */
			if (file[i].getName().matches(baseName+"-"+date+"-\\d+\\.gpx")) {
				/*
				 * Index du premier caractere du nombre n dans le nom du fichier
				 * On cherche le 2eme tiret et on ajoute 1
				 * Pour trouver le 2eme tiret : on cherche un tiret a partir de l'index du premier tiret trouve +1
				 */
				int index1 = file[i].getName().indexOf("-", file[i].getName().indexOf("-")+1)+1;
				/*
				 * Index du premier caractere qui correspond a la chaine ".gpx" dans le nom du fichier
				 * Permet de couper le chaine avec substring et de recuperer seulement le nombre en enlevant ce qui reste a partir de cet index
				 */
				int index2 = file[i].getName().indexOf(".gpx");
				// On recupere le numero seul du fichier avec les 2 index precedents
				String numS = file[i].getName().substring(index1, index2);
				// Recuperation du numero et test si il est le plus grand des numeros analyses dans un try/catch au cas ou on recupere une chaine vide
				try {
					int n = Integer.parseInt(numS);
					if (nMax < n)
						nMax = n;
				} catch (NumberFormatException nfe) {
					Log.e(TAG, "Exception catched : "+nfe);
				}
				//Log.v(TAG, "FileName:" + file[i].getName());
				//Log.v(TAG, "Match, index of '.gpx' : "+index2+" ; index of '-' : "+index1+" ; numS : "+numS);
			} else {
				//Log.v(TAG, "Don't match.");
			}
		}
		// Creation de la chaine dans un Builder avec nMax+1, nMax etant le numero de fichier le plus grand pour la date passe en parametre 
		StringBuilder fileNameBuilder = new StringBuilder(baseName+"-"+date+"-"+(nMax+1)+".gpx");
		String name = fileNameBuilder.toString();
		Log.v(TAG, "New File Name => "+name);
		return name;
	}
	
	/**
	 * Methode qui ecrit un fichier a partir du document JDOM doc en utilisant la methode createFilename pour obtenir chemin et nom du fichier
	 * @param doc - Un document JDOM avec lequel creer le fichier
	 * @return true
	 */
	public boolean saveFile(Document doc) {
		/**
		 * Creation du fichier
		 */
		/**
		 * Creation du contenu du fichier a partir de la base et ajout d'un "segment" de trace
		 */
		
		// Date actuelle
		Date now = new Date();
		// Format de la date
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		// Creation de la chaine de la date actuelle a partir de l'objet Date now et du format (SimpleDateFormat).
		String dateNow = new String(sdf.format(now));
		
		/**
		 * Try/Catch pour :
		 * 1 - IOException a cause de :
		 * 		- File.createNewFile()
		 * 		- FileOutputStream.write()
		 * 		- FileOutputStream.close()
		 * 2 - FileNotFoundException a cause de :
		 * 		- FileOutputStream(File)	// Instanciation
		 */
		// Partie enregistrement dans Fichier
		XMLOutputter xmlOutput = new XMLOutputter(Format.getPrettyFormat());
		try {
			// Si il y a un media externe et si le media externe n'est pas en lecture seule
			if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
					&& !Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {
				FILE_EXT = true;
				// Creation du repertoire (pour si c'est la premiere fois par exemple)
				File trackFile = new File(EXT_FILES_DIR);
				trackFile.mkdir();
				// Sauvegarde du repertoire et du nom de fichier dans des attributs de classe
				FILE_NAME = createFilename(EXT_FILES_DIR, dateNow);
				// Creation du nom de fichier a utilise en fonction du contenu du repertoire
				// Reutilisation de trackFile pour creer le fichier (objet java)
				trackFile = new File(EXT_FILES_DIR+FILE_NAME);
				// Creation du fichier
				trackFile.createNewFile();
				// Ouverture d'un flux sortant vers le fichier
				FileOutputStream output = new FileOutputStream(trackFile);
				// Ecriture dans le fichier avec le flux et les octets de la chaine gpxFile
				xmlOutput.output(doc, output);
				// fermeture du flux d'ecriture si il existe
				if(output != null)
					output.close();
			} else {
				FILE_EXT = false;
				/* Sinon utilisation du stockage interne */
				// Sauvegarde du repertoire et du nom de fichier dans des attributs de classe
				FILE_NAME = createFilename(getFilesDir().getAbsolutePath(), dateNow);
				// Creation du nom de fichier a utilise en fonction du contenu du repertoire
				// Ouverture d'un flux sortant vers le fichier - Fichier lisible par tout le monde car fichier pour l'utilisateur
				FileOutputStream output = openFileOutput(FILE_NAME, MODE_WORLD_READABLE);
				// Ecriture dans le fichier avec le flux et les octets de la chaine gpxFile
				xmlOutput.output(doc, output);
				// fermeture du flux d'ecriture si il existe
				if(output != null)
					output.close();
			}
			//Log.v(TAG, "File name : "+FILE_NAME);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	/**
	 * Methode qui creer l'element JDOM pour la nouvelle localisation (<trkpt> pour track point)
	 * Utilise la localisation passe en parametre et l'attribut de classe GSA (String)
	 * @param loc - la nouvelle localisation
	 * @return Element - element JDOM representant la position
	 */
	public Element createLocationElement(Location loc) {
		// Creation de notre objet Element contenant notre nouvelle position
		Element locElement = new Element("trkpt", ns);
		// Ajout des attributs lattitude et longitude
		locElement.setAttribute("lat", String.valueOf(loc.getLatitude()));
		locElement.setAttribute("lon", String.valueOf(loc.getLongitude()));
		
		// Nature du fix (position GPS) : 2d ou 3d, utilise pour l'integration ou non de l'altitude, du vdop (et donc du pdop)
		String locFix = "2d";
		/*
		 * Ajout de l'altitude
		 */
		if (loc.hasAltitude()) {
			Element locAltitude = new Element("ele", ns);
			locAltitude.setText(String.valueOf(loc.getAltitude()));
			locElement.addContent(locAltitude);
			locFix = "3d";
		}
		/*
		 * Creation de la date
		 */
		// Recup de la date au format POSIX (millisecondes depuis 1970)
		long posixTime = (long) loc.getTime();
		// Creation d'un objet Date a partir du temps POSIX
		Date date = new Date(posixTime);
		// Creation d'un format de date
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
		// Creation d'un StringBuilder a partir de notre format et de notre date
		StringBuilder formattedDateBuilder = new StringBuilder(sdf.format(date));
		// On ajoute 2 caractere au builder
		formattedDateBuilder.append("Z"); // Z => indication de fuseau horaire // TODO verif
		// T (pour Time), permet de couper la chaine entre la date et l'heure
		formattedDateBuilder.insert(10, "T");
		// Creation de la chaine a partir du Builder
		String formattedDate = formattedDateBuilder.toString();
		//Log.v(TAG, "TIME => "+formattedDate);

		// Ajout de la date (element XML)
		Element locTime = new Element("time", ns);
		locTime.setText(formattedDate);
		locElement.addContent(locTime);
	
		/*
		 * gestion du fix et de la precision DOP (NMEA, $GPGSA)
		 */
		// Creation d'attributs de type Element, modifie ci apres
		Element locFixElem = new Element("fix", ns);
		Element locSats = new Element("sat", ns);
		Element locHdop = new Element("hdop", ns);
		Element locVdop = new Element("vdop", ns);
		Element locPdop = new Element("pdop", ns);
		// Creation d'attributs de type chaine pour les infos DOP
		String pdop = "";
		String hdop = "";
		String vdop = "";
		
		String nbSat = String.valueOf(loc.getExtras().getInt("satellites"));
		// Si on a une chaine GSA
		// chaine de test : GSA = "$GPGSA,A,2,19,28,14,18,27,22,31,39,,,,,1.7,1.0,1.3*35XX";
		if(GSA != null) {
			String[] gsarray = GSA.split(",");
			pdop = gsarray[gsarray.length - 3];
			hdop = gsarray[gsarray.length - 2];
			vdop = gsarray[gsarray.length - 1].substring(0, gsarray[gsarray.length - 1].length() - 5);
			// Si le fix dans la chaine vaut 2 ET que l'altitude de l'objet loc vaut 0.0 : on met 2d dans notre fix
			// Si le fix vaut 3, on met 3d dans notre fix
			// Tout ca parce que l'attribut hasAltitude de la classe Location n'a pas de valeurs pour les objets location retourne par le systeme de certains telephone (teste avec un Nexus 5)
			if ((gsarray[2].equals("2") && loc.getAltitude() == 0.0) || gsarray[2].equals("3"))
				locFix=gsarray[2]+"d";
			// On fait plus confiance a la chaine NMEA.GSA pour l'utilisation de l'altitude fournit par le locationListener
			// Si le fix dans la chaine GSA vaut 2d, on enleve l'altitude qui a ete prise dans l'objet loc et qui peut-etre une valeur bidon (ex : par defaut = 0m) 
			if (locFix.equals("2d"))
				locElement.removeChild("ele", ns);
		}
		/*
		 * Ajout fix et DOP
		 */
		locFixElem.setText(locFix);
		locElement.addContent(locFixElem);
		// Si nombre de satellite pas nul, on ajoute l'element <sat>n</sat>
		if(nbSat != null && !nbSat.equals("0")) {
			locSats.setText(nbSat);
			locElement.addContent(locSats);
		}
		// Si hdop pas egal a "", on ajoute l'element <hdop>x</hdop>
		if(!hdop.equals("")) {
			locHdop.setText(hdop);
			locElement.addContent(locHdop);
			// Si vdop pas egal a "" ET notre fix vaut 3d, on ajoute l'element <vdop>x</vdop> ...
			if(!vdop.equals("") && locFix.equals("3d")) {
				locVdop.setText(vdop);
				locElement.addContent(locVdop);
				// ... et l'element pdop donne par la chaine GSA ou calcule a partir de hdop et vdop
				if(!pdop.equals("")) {
					locPdop.setText(pdop);
				} else {
					// Si pas de PDOP mais qu'on a V et HDOP alors calcul de PDOP
					double hdopv = Float.parseFloat(hdop);
					double vdopv = Float.parseFloat(vdop);
					locPdop.setText(String.valueOf(Math.sqrt(hdopv*hdopv+vdopv*vdopv)));
				}
				locElement.addContent(locPdop);
			}
		}
		return locElement;
	}
	
	/**
	 * Methode qui ajoute la localisation a partir de l'element JDOM passe en parametre
	 * Utilise les attributs de classe FILE_DIRECTORY et FILE_NAME comme chemin pour le fichier a reecrire
	 * @param loc - Element JDOM representant la nouvelle position
	 * @return true
	 */
	public boolean addLocation(Element loc) {
	    // Parsing
		SAXBuilder saxBuilder = new SAXBuilder();
		Document document = new Document(new Element("temp"));
	    try {
	    	// On creer un nouveau document JDOM a partir du fichier GPX
	    	if (!FILE_EXT)
	    		document = saxBuilder.build(openFileInput(FILE_NAME));
	    	else
	    		document = saxBuilder.build(new FileInputStream(new File(EXT_FILES_DIR+FILE_NAME)));
	        
	    	// On recupere la racine du document
	        Element racine = document.getRootElement();
	        // A partir de la racine, on recupere les tracks (<trk>)
		    List<Element> trkList = racine.getChildren("trk", ns);
		    // A partir de la derniere track, on recupere les segments de track (<trkseg>)
		    List<Element> trksegList = trkList.get(trkList.size()-1).getChildren("trkseg", ns);
		    // On recupere le dernier segment de track (de la derniere track donc)
		    Element trkseg = trksegList.get(trksegList.size()-1);
		    // On y ajoute la nouvelle position (Element loc)
		    trkseg.addContent(loc);
	    } catch(Exception e) {
	    	Log.e(TAG, e.getMessage());
	    }
	    // Partie enregistrement dans le Fichier
	    XMLOutputter xmlOutput = new XMLOutputter(Format.getPrettyFormat());
	    //Log.d(TAG, "addLocation : FILE_EXT = "+FILE_EXT);
	    try {
	    	if (!FILE_EXT)
	    		xmlOutput.output(document, openFileOutput(FILE_NAME, MODE_WORLD_READABLE));
	    	else
	    		xmlOutput.output(document, new FileOutputStream(EXT_FILES_DIR+FILE_NAME));
	    } catch (FileNotFoundException e) {
	    	Log.e(TAG, e.toString());
	    } catch (IOException ioe) {
	    	Log.e(TAG, ioe.toString());
	    }
	    
		return true;
	}
	
	/*
	 * Toutes les methodes abstraites de Service 
	 */
	@Override
	public IBinder onBind(Intent arg0)
	{
		return null;
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.v(TAG, "onStartCommand");
		/*
		 * Partie Gestion de base du Service
		 */
		super.onStartCommand(intent, flags, startId);
		//Bundle extras = intent.getExtras(); //directory = extras.getString("directory"); //fileName = extras.getString("fileName");
		Toast.makeText(this, R.string.toast_tracking_launched, Toast.LENGTH_SHORT).show();
		/*
		 * Partie Notification + foreground
		 */
		// Creation d'un pending intent pour le clique sur la notification
		Intent notificationIntent = new Intent(this, fr.rt.acy.locapic.MainActivity.class);
		notificationIntent.setAction("MainActivity");
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		
		// Creation de la notif avec ses differents texte, son icone et le pendingIntent
        Notification notif = new Notification.Builder(this)
        .setTicker(getResources().getString(R.string.notif_ticker)) // Texte affiche a l'apparition de la notif
        .setContentTitle(getResources().getString(R.string.notif_title)) // Titre de la notif
        .setContentText(getResources().getString(R.string.notif_text)) // Description
        .setSmallIcon(R.drawable.ic_launcher) // Icone de la notif
        .setContentIntent(pendingIntent) // pendingIntent
        .setOngoing(true).getNotification();
        
        // Lancement au premier plan du service avec une priorite de 2 et la notif creer precedemment 
        startForeground(2, notif);
		/*
		 * 
		 */
		return START_REDELIVER_INTENT;
	}
	/**
	 * 
	 */
	@Override
	public void onCreate()
	{
		Log.v(TAG, "onCreate");
		/**
		 * Gestion du Service
		 */
		/*HandlerThread thread = new HandlerThread("trackServiceHandlerThread", Process.THREAD_PRIORITY_DEFAULT);
		thread.start();
		*/
		/**
		 * Gestion de l'itineraire
		 */
		// Recuperation des preferences
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		LOCATION_INTERVAL = Integer.parseInt(pref.getString("LOCATION_INTERVAL", "2"))*1000;
		LOCATION_DISTANCE = Integer.parseInt(pref.getString("LOCATION_DISTANCE", "0"));
		TRACKING = pref.getBoolean("TRACKING", true);
		
		// Creation du document avec la methdoe createGpxDocTree()
		// En parametre son passe les preferences utilisateurs quand au metadonnees GPX
		Document document = createGpxDocTree(pref.getString("TRACK_NAME", null),
				pref.getString("TRACK_DESC", null),
				pref.getString("AUTHORS_NAME", null),
				pref.getString("AUTHORS_EMAIL", null),
				pref.getString("TRACK_KEYWORDS", null));
		// Enregistrement du fichier avec notre methode saveFile()
		saveFile(document);
		
		// Initialisation de notre locationMonager (voir methode plus bas)
		initializeLocationManager();
		try {
			// Demande d'updates aux intervalles recuperer dans les preferences (ou par defaut)
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,	this);//locationListeners
			// Ajout d'un listener pour les chaines NMEA.
			locationManager.addNmeaListener(this);
		} catch (java.lang.SecurityException ex) {
			Log.i(TAG, "fail to request location update, ignore", ex);
		} catch (IllegalArgumentException ex) {
			Log.d(TAG, "gps provider does not exist " + ex.getMessage());
		}
	}
	@Override
	public void onDestroy()
	{
		Log.v(TAG, "onDestroy - 1");
		if (locationManager != null) {
			//Log.v(TAG, "onDestroy => lm != null");
			try {
				// On enleve les listeners
				locationManager.removeUpdates(this);
				locationManager.removeNmeaListener(this);
			} catch (Exception ex) {
				Log.e(TAG, "fail to remove location listners, ignore", ex);
			}
		} else
			Log.w(TAG, "onDestroy => lm == null");
		
		// On met la preference TRACKING a false
		TRACKING = false;
		prefEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		prefEditor.putBoolean("TRACKING", TRACKING);
		prefEditor.commit();
		
		// Petit toast pour l'utilisateur
		Toast.makeText(this, R.string.toast_tracking_stopped, Toast.LENGTH_SHORT).show();
		
		super.onDestroy();
		// Au cas ou...
		stopSelf();
	}
	
	private void initializeLocationManager() {
		//Log.v(TAG, "initializeLocationManager");
		if (locationManager == null) {
			// Recuperation d'un locationManager donne par le systeme
			locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		}
	}
/*	@Override
	protected void onHandleIntent(Intent intent) {
		
	}*/

	@Override
	public void onNmeaReceived(long timestamp, String nmea) {
		//Log.v(TAG, "NMEA beg :=> "+nmea.substring(0, 6));
		// Si la chaine est une chaine GSA ($GPGSA,...) et que son fix ne vaut pas 1 (donc 2 ou 3)
		// Alors on garde la chaine dans l'attribut GSA du service
		if(nmea.substring(0, 6).equals("$GPGSA") && !nmea.split(",")[2].equals("1")) {
			GSA = nmea;
		}		
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.v(TAG, "onLocationChanged: " + location);
		//Log.d(TAG, "onLocationChanged ; test = "+test);
		test++;
		//Log.v(TAG, "NMEA : GSA => "+GSA);
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		if(location != null) {
			//Log.v(TAG, "Latitude " + location.getLatitude() + " et longitude " + location.getLongitude());
			addLocation(createLocationElement(location));
		} else {
			//Log.v(TAG, "Loc == null");
		}
		
		/** Intent pour l'actualisation graphique */
		// Date de la position (format POSIX)
		long locPosixTime = (long) location.getTime();
		// Conversion vers objet de classe Date
		Date locDate = new Date(locPosixTime);
		// Format pour la date (pour l'interface)
		SimpleDateFormat euDateFormat = new SimpleDateFormat("dd/MM/yyyy");
		// Format de l'heure
		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		// Creation des chaines date et heure
		String euDate = euDateFormat.format(locDate);
		String time = timeFormat.format(locDate);
		
		// Creation de chaine modifie ci apres
		String pdop = "";
		String hdop = "";
		String vdop = "";
		// Si on a une chaine dans notre attribut de service, on prend les valeurs DOP
		if(GSA != null) {
			String[] gsarray = GSA.split(",");
			pdop = gsarray[gsarray.length - 3];
			hdop = gsarray[gsarray.length - 2];
			// Recup du dernier champs auxquel on enleve les 5 derniers carac (etoile * + CRC de 2 carac + retour chariot de 2 carac (<CR><LF>))
			vdop = gsarray[gsarray.length - 1].substring(0, gsarray[gsarray.length - 1].length() - 5);
		}
		
		// Creation d'un intent et ajout des extras
		// Les extras sont des donnees sur la nouvelle position pour mettre l'interface graphique a jour
		Intent intent = new Intent();
		// Notre action
		intent.setAction(INTENT_ACTION);
		// Ajout des extras
		intent.putExtra("LOC_LAT", String.valueOf(location.getLatitude()))
			.putExtra("LOC_LON", String.valueOf(location.getLongitude()))
			.putExtra("LOC_ELE", String.valueOf(location.getAltitude()))
			.putExtra("LOC_SPEED", String.valueOf(location.getSpeed()))
			.putExtra("LOC_DATE", euDate)
			.putExtra("LOC_TIME", time)
			.putExtra("LOC_HDOP", hdop)
			.putExtra("LOC_VDOP", vdop)
			.putExtra("LOC_PDOP", pdop);
		// On envoie l'intent en broadcast, pour tout le monde sur le systeme
	    sendBroadcast(intent);
	}

	// Methode de l'interface LocationListener
	@Override
	public void onProviderDisabled(String provider) {
	}

	// Methode de l'interface LocationListener
	@Override
	public void onProviderEnabled(String provider) {
	}

	// Methode de l'interface LocationListener
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}
