package fr.rt.acy.locapic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import fr.rt.acy.locapic.gps.TrackService;
import fr.rt.acy.locapic.camera.CameraActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity 
{
	private final String DEBUG_AUTO_START_INITENT = ""; // "camera" pour lancer automatiquement la camera
	private Intent prefIntent;
	private boolean recording2 = false;
	private LocationManager lm = null;
	private final String GPX_BASE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n<gpx version=\"1.1\">\n\t<metadata>\n\t\t<name>Android GPS receiver track</name>\n\t\t<desc>GPS track logged on an Android device with an application from a project by Samuel Beaurepaire &amp; Virgile Beguin for IUT of Annecy (Fr), RT departement.</desc>\n\t\t<time></time>\n\t\t<author>\n\t\t\t<name>Samuel Beaurepaire</name>\n\t\t\t<email id=\"sjbeaurepaire\" domain=\"orange.fr\" />\n\t\t</author>\n\t\t<keywords></keywords>\n\t</metadata>\n\n\t<trk>\n\t</trk>\n</gpx>";
	private final String FILES_DIR = Environment.getExternalStorageDirectory().getPath() + "/TracesGPS/";
	private final String TAG = "GPS";
	private SharedPreferences pref;
	private Button goiti2 = null;
	private boolean useNetworkPref = false;
	private boolean notifPref = false;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        if (DEBUG_AUTO_START_INITENT.equals("camera")) 
        	startCamera(null);
            
        goiti2 = (Button) findViewById(R.id.main_button_itineraire);
        goiti2.setOnClickListener(goiti2Listener);
    }
    
    @Override
	protected void onResume() {
		super.onResume();
		Log.v(TAG, "onResume");
		// LocationManager pour toute l'activite
		lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		// Gestion des preferences
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		recording2 = pref.getBoolean("RECORDING2", false);
		useNetworkPref = pref.getBoolean("USE_NETWORK_LOCATION_PROVIDER", false);
		notifPref = pref.getBoolean("ENABLE_NOTIFICATIONS", false);
		
		if(recording2)
			goiti2.setText("Arreter l'itinerance (Service)");
	}
    
    /**
	 * Creer un nom de fichier de type trace-[date]-[n+1].gpx en fonction des fichiers deja presents dans le repertoire $dir
	 * Avec date, une chaine (ici utilise sous la forme aaaammdd) et trace-[date]-[n].gpx un fichier existant (n le plus grand possible pour la date choisie
	 * @param dir - Repertoire ou sont stocker les fichiers
	 * @param date - Date pour laquelle creer le fichier (communement la date instantanee)
	 * @return name - Nom du fichier
	 */
	private String createFilename (String dir, String date) { /// date format : aaaammdd (comme les fichiers)
		int nMax = 0;
		Log.v(TAG, "Path: " + dir);
		//File f = new File(dir);
		//File file[] = f.listFiles();
		// Listing des fichiers du repertoire $dir dans un tableau
		File file[] = new File(dir).listFiles();
		Log.v(TAG, "Size: "+ file.length);
		
		for (int i=0; i < file.length; i++) {
			Log.v(TAG, "FileName:" + file[i].getName());
			/*
			 * Si le nom du fichier correspond a la regex suivante :
			 * "trace-aaaammdd-n.gpx"
			 * avec aaaammdd = $date et n, un nombre (au moins un chiffre)
			 */
			if (file[i].getName().matches("trace-"+date+"-\\d+\\.gpx")) {
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
				Log.v(TAG, "Match, index of '.gpx' : "+index2+" ; index of '-' : "+index1+" ; numS : "+numS);
			} else {
				Log.v(TAG, "Not match.");
			}
		}
		// Creation de la chaine dans un Builder avec nMax+1, nMax etant le numero de fichier le plus grand pour la date passe en parametre 
		StringBuilder fileNameBuilder = new StringBuilder("trace-"+date+"-"+(nMax+1)+".gpx");
		String name = fileNameBuilder.toString();
		Log.v(TAG, "New File Name => "+name);
		return name;
	}
    
    /**
	 * Listener du bouton "Enregistrer itineraire" avec Service
	 * (ou "Arreter l'itineraire en cours") 
	 */
	private OnClickListener goiti2Listener = new OnClickListener() {
		/**
		 * Lors du clique sur le bouton
		 */
		@Override
		public void onClick(View v) {
			Intent serviceIntent = new Intent(MainActivity.this, TrackService.class);
			/**
			 * Si pas deja en train d'enregistrer un itineraire
			 */
			if(!recording2) {
				/**
				 * Si GPS on
				 */
				if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					/**
					 * Creation du fichier
					 */
					// Variables qui seront donnees au Service par l'intent, modifiee plus loin en fonction du support de stockage disponible
					String filesDir = null;
					String fileName = null;
					boolean ext = true;
	
					/**
					 * Creation du contenu du fichier a partir de la base et ajout d'un "segment" de trace (n�1)
					 * TODO JAXP
					 */
					int index = GPX_BASE.indexOf("</trk>", 0)-2;
					String gpxFile = new StringBuilder(GPX_BASE).insert(index, "\n\t\t<number>1</number>\n\t\t<trkseg>\n\t\t</trkseg>").toString();
					
					// Date actuelle
					Date now = new Date();
					// Format de la date
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.FRENCH);
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
					try {
						// Si il y a un media externe et si le media externe n'est pas en lecture seule
						if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
								&& !Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState()) && ext == true) {
							// Creation du repertoire (si c'est la premiere fois par exemple)
							File traceFile = new File(FILES_DIR);
							traceFile.mkdir();
							// Affectation du chemin du repertoire a la variable filesDir qui sera envoye au Service
							filesDir = traceFile.getAbsolutePath();
							// Creation du nom de fichier a utilise en fonction du contenu du repertoire
							// + Affectation du nouveau nom a la variable fileName qui sera aussi envoyee au service
							fileName = createFilename(filesDir, dateNow);
							// Reutilisation de traceFil pour creer le fichier (objet java)
							traceFile = new File(FILES_DIR+fileName);
							// Creation du fichier
							traceFile.createNewFile();
							// Ouverture d'un flux sortant vers le fichier
							FileOutputStream output = new FileOutputStream(traceFile);
							// Ecriture dans le fichier avec le flux et les octets de la chaine gpxFile
							output.write(gpxFile.getBytes());
							// fermeture du flux d'ecriture si il existe
							if(output != null)
								output.close();
						} else {
							/* Sinon utilisation du stockage interne */
							// Recuperation du chemin du stockage interne dedie a l'appli et affectation a filesDir qui sera envoye au Service
							filesDir = getFilesDir().getAbsolutePath();
							// Variable ext qui sera donnee au Service mise a false
							ext = false;
							// Creation du nom de fichier a utilise en fonction du contenu du repertoire
							// + Affectation du nouveau nom a la variable fileName qui sera aussi envoyee au service
							fileName = createFilename(filesDir, dateNow);
							// Ouverture d'un flux sortant vers le fichier - Fichier lisible par tout le monde car fichier pour l'utilisateur
							FileOutputStream output = openFileOutput(fileName, MODE_WORLD_READABLE);
							// Ecriture dans le fichier avec le flux et les octets de la chaine gpxFile
							output.write(gpxFile.getBytes());
							// fermeture du flux d'ecriture si il existe
							if(output != null)
								output.close();
						}
						Log.v(TAG, "Files dir : "+filesDir);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					/**
					 * Preparation et demarrage du service
					 * + enregistrement en preference du debut du recording
					 * + changement du texte du bouton
					 */
					// Intent et Service
					serviceIntent.putExtra("directory", filesDir);
					serviceIntent.putExtra("isExt", ext);
					serviceIntent.putExtra("fileName", fileName);
					startService(serviceIntent);
					/*
					 * Notification et foreground service
					 */
			        /*Notification notification = new Notification(R.drawable.ic_action_refresh, "tickerText", System.currentTimeMillis());
			        Intent notificationIntent = new Intent(MainActivity.this, MainActivity.class);
			        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, notificationIntent, 0);
			        notification.setLatestEventInfo(MainActivity.this, "Titre", "Message", pendingIntent);*/
			        //startForeground("1", notification);
					/*
					 * Fin Notification et foreground service
					 */
					// Gestion de la preference (pour savoir si record en cours ou pas)
					recording2 = true;
					SharedPreferences.Editor prefEditor = pref.edit();
					prefEditor.putBoolean("RECORDING2", recording2);
					prefEditor.commit();
					// Changement du bouton
					goiti2.setText("Stopper l'itinerance (service)");
				} else {
					/**
					 * Si GPS pas actif
					 */
					Toast.makeText(MainActivity.this, "Vous devez activer le GPS pour enregistrer un itineraire.", Toast.LENGTH_SHORT).show();
				}
			} else {
				/**
				 * Si deja en train d'enregistrer un itineraire
				 * + Arret du service
				 * + Changement du bouton
				 * + Edition de la preference
				 */
				// Arret du service
				stopService(serviceIntent);
				// Gestion de la preference (pour savoir si record en cours ou pas)
				recording2 = false;
				SharedPreferences.Editor prefEditor = pref.edit();
				prefEditor.putBoolean("RECORDING2", recording2);
				prefEditor.commit();
				// Changement du bouton
				goiti2.setText("Lancer l'itinerance (service)");
			}
		}
	};
    
    /**
     * Lance un intent de la camera
     */
    public void startCamera(View view)
    {
    	Intent intentCamera = new Intent(this, CameraActivity.class);
    	startActivity(intentCamera);
    	finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (item.getItemId())
        {
			case R.id.action_settings:
				prefIntent = new Intent(this, PreferencesActivity.class);
				startActivity(prefIntent);
				return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
