package fr.rt.acy.locapic;

//import fr.rt.acy.locapic.gps.DataReceiver.UiUpdater;
import fr.rt.acy.locapic.camera.CameraActivity;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

/**
 * MainActivity etendue de TabActivity pour les onglets
 */
public class MainActivity extends TabActivity 
{
	// Variables pour changer facilement le type de bouton lors du developpement
	// Concerne le bouton pour lancer l'enregistrement d'itineraire
	public final static int TRACKING_BUTTON_ACTIVITY_TEXT = 0; // Bouton texte dans l'activite (fond clair)
	public final static int TRACKING_BUTTON_ACTIVITY_ICON = 1; // Bouton image dans l'activite (fond clair)
	public final static int TRACKING_BUTTON_ACTIONBAR_ICON = 2; // Bouton dans l'action bar (fond sombre)
	// Permet de savoir le type de bouton "in Activity" (0 ou 1)
	public final static int TRACKING_BUTTON_ACTIVITY_TYPE = TRACKING_BUTTON_ACTIVITY_ICON;
	// Permet de savoir le type de bouton actuellement utilise (0, 1 ou 2), ici "in actionBar"
	public final static int TRACKING_BUTTON_TYPE = TRACKING_BUTTON_ACTIONBAR_ICON;
	
	private final String DEBUG_AUTO_START_INTENT = ""; // "camera" pour lancer automatiquement la camera
	private Intent prefIntent; // Intent utilise pour lance la page de parametres (preferences)
	private final String TAG = "HOME"; // TAG pour les logs
	private SharedPreferences pref; // SharedPreferences pour recuperer les preferences utilisateurs
	private SharedPreferences.Editor prefEditor; // Editor pour modifier les preferences utilisateurs
	// Les trois type differents de boutons pour lancer l'enregistrement d'itineraire
	private Button button_tracking = null; // bouton normal (pas utilise actuellement)
	private ImageButton image_button_tracking = null; // bouton image (pas utilise actuellement)
	private static MenuItem menu_button_tracking; // celui ci est utilise (item de l'action bar)
	// Boolean permettant de savoir si on est en train d'enregistrer un itineraire, utilise avec les preferences
	private boolean tracking = false;
	private LocationManager lm = null; // LocationManager pour gerer tout ce qui est GPS (Provider allume ou pas)
	// Ci dessous : ancienne chaine utilise pour creer le fichier de base pour l'enregistrement d'itineraire
	//private final String GPX_BASE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n<gpx version=\"1.1\">\n\t<metadata>\n\t\t<name>Android GPS receiver track</name>\n\t\t<desc>GPS track logged on an Android device with an application from a project by Samuel Beaurepaire &amp; Virgile Beguin for IUT of Annecy (Fr), RT departement.</desc>\n\t\t<time></time>\n\t\t<author>\n\t\t\t<name>Samuel Beaurepaire</name>\n\t\t\t<email id=\"sjbeaurepaire\" domain=\"orange.fr\" />\n\t\t</author>\n\t\t<keywords></keywords>\n\t</metadata>\n\n\t<trk>\n\t</trk>\n</gpx>";
	
	/**
	 * Methode onCreate(Bundle) du lifecycle de l'activite
	 * - Creation de la vue
	 * - Creation des preferences par defaut si elle n'existe pas
	 * - Creation des onglets et de leurs contenus
	 */
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
    	// Super constructeur + creation de la vue a partir du layout activity_main(.xml)
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v(TAG, "Lifecycle - onCreate");
        // Pour lancer la camera au demarrage (lors du developpement)
        if (DEBUG_AUTO_START_INTENT.equals("camera")) 
        	startCamera(null);
        
        /* Gestion des preferences : si LOCATION_INTERVAL et LOCATION_DISTANCE
         * alors on les creer avec comme valeurs par defaut 2(secondes) et 0(metres)
         * Ces preferences sont les intervalles de temps et de distance pour le tracking
         */
        prefEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        pref = PreferenceManager.getDefaultSharedPreferences(this);
     	tracking = pref.getBoolean("TRACKING", false);
     	if(!pref.contains("LOCATION_INTERVAL"))
     		prefEditor.putString("LOCATION_INTERVAL", "2");
     	if(!pref.contains("LOCATION_DISTANCE"))
     		prefEditor.putString("LOCATION_DISTANCE", "0");
     	// On met a jour les preferences avec un commit sur notre editeur contenant potentiellement les 2 chaines precedentes
     	prefEditor.commit();
     	
        /* Version texte du boutton itineraire
         * button_tracking = (Button) findViewById(R.id.main_button_tracking);
         * button_tracking.setOnClickListener(trackingButtonListener);*/
        /* Version avec un icone du boutton itineraire
         * image_button_tracking = (ImageButton) findViewById(R.id.main_button_tracking);
         * image_button_tracking.setOnClickListener(trackingButtonListener); */
        // On utilise la version actionBar, gere dans le onCreateOptionsMenu() et onOptionsItemSelected()
        
        /** Gestion des onglets */
		// Creation d'un TabHost qui accueillera tous nos onglets
		TabHost tabHost = (TabHost)findViewById(android.R.id.tabhost);
		// Creation d'onglets (TabSpec)
		TabSpec tab1 = tabHost.newTabSpec("First Tab");
		TabSpec tab2 = tabHost.newTabSpec("Second Tab");
		
		// Parametrage du nom (Indicator, une chaine) et des activites (Content, un intent vers l'activite) pour les differents onglets
		tab1.setIndicator(getResources().getString(R.string.tab_locstats)).setContent(new Intent(this, fr.rt.acy.locapic.gps.LocStatsActivity.class));
		tab2.setIndicator(getResources().getString(R.string.tab_trackstats)).setContent(new Intent(this, fr.rt.acy.locapic.gps.TrackStatsActivity.class));
		
		// Ajout des onglets au TabHost
		tabHost.addTab(tab1);
		tabHost.addTab(tab2);
    }
    
    /**
     * onResume() - lifecycle de l'activite
     * - Verification : itineraire en cours d'enregistrement ou pas
     * 		-> grace a la preference "TRACKING" que l'on met a true quand on en lance un
     * - Si tracking alors MaJ du bouton de lancement en bouton d'arret
     * 		(changement d'icone/de texte suivant le type de bouton actuellement utilise)
     */
    @Override
	protected void onResume() {
		super.onResume();
		Log.v(TAG, "Lifecycle - onResume");
		// LocationManager pour toute l'activite
		lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		// Verification de la preference "TRACKING"
		tracking = pref.getBoolean("TRACKING", false);
		// Changement du bouton suivant son type (voir attributs TRACKING_BUTTON_*)
		if(tracking) {
			switch(TRACKING_BUTTON_TYPE) {
				case TRACKING_BUTTON_ACTIVITY_TEXT:
					button_tracking.setText(R.string.button_tracking_on);
					break;
				case TRACKING_BUTTON_ACTIVITY_ICON:
					image_button_tracking.setImageResource(R.drawable.ic_action_location_off);
					break;
				case TRACKING_BUTTON_ACTIONBAR_ICON:
					// Fait dans le onCreateOptionsMenu()
					break;
			}
		}
	}
    
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
    /**
     * Lance ou arrete le service qui enregistre l'itineraire (appele lors de l'appuie sur le bouton de tracking
     * Change une preference TRACKING (boolean pour savoir si on enregistre ou pas)
     * Change aussi le texte ou l'image du boutton, en fonction de l'entier passe en parametre :
     * @param button_type - 0 pour un boutton texte dans l'activite (fond blanc) ; 1 pour une image boutton dans l'activite (fond blanc) ; 2 pour une image boutton dans l'actionBar (fond noir)
     */
    private void switchTracking(int button_type) {
    	Intent serviceIntent = new Intent(MainActivity.this, fr.rt.acy.locapic.gps.TrackService.class);
		/** Si pas deja en train d'enregistrer un itineraire */
		if(!tracking) {
			/** Si GPS on */
			if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				startService(serviceIntent);
				// Mise a true de la preference "TRACKING" (et de l'attribut de l'activite)
				tracking = true;
				SharedPreferences.Editor prefEditor = pref.edit();
				prefEditor.putBoolean("TRACKING", tracking);
				prefEditor.commit();
				// Changement du bouton en fonction de son type actuel
				switch(button_type) {
					case TRACKING_BUTTON_ACTIVITY_TEXT:
						button_tracking.setText(R.string.button_tracking_on);
						break;
					case TRACKING_BUTTON_ACTIVITY_ICON:
						image_button_tracking.setImageResource(R.drawable.ic_action_location_off);
						break;
					case TRACKING_BUTTON_ACTIONBAR_ICON:
						menu_button_tracking.setIcon(R.drawable.ic_action_location_off_white);
						menu_button_tracking.setTitle(R.string.button_tracking_on);
						break;
				}
			} else {
				/* Si GPS pas actif, un ptit Toast */
				Toast.makeText(MainActivity.this, R.string.toast_gps_off, Toast.LENGTH_SHORT).show();
			}
		} else {
			/* Si deja en train d'enregistrer un itineraire :
			 * Arret du service + Changement du bouton + Edition de la preference
			 */
			// Arret du service
			stopService(serviceIntent);
			// Mise a false de la preference "TRACKING" (et de l'attribut de l'activite)
			tracking = false;
			SharedPreferences.Editor prefEditor = pref.edit();
			prefEditor.putBoolean("TRACKING", tracking);
			prefEditor.commit();
			// Changement du bouton
			switch(button_type) {
				case TRACKING_BUTTON_ACTIVITY_TEXT:
					button_tracking.setText(R.string.button_tracking_off);
					break;
				case TRACKING_BUTTON_ACTIVITY_ICON:
					image_button_tracking.setImageResource(R.drawable.ic_action_location_found);
					break;
				case TRACKING_BUTTON_ACTIONBAR_ICON:
					menu_button_tracking.setIcon(R.drawable.ic_action_location_found_white);
					menu_button_tracking.setTitle(R.string.button_tracking_off);
					break;
			}
		}
    }
    /**
	 * Listener du bouton Itineraire (Tracking button)
	 * lance switchTracking(int buttonType)
	 */
	private OnClickListener trackingButtonListener = new OnClickListener() {
		/** Lors du clique sur le bouton */
		@Override
		public void onClick(View v) {
			// Lance/arrete le tracking grace a notre methode switchTracking(buttonType)
			switchTracking(TRACKING_BUTTON_ACTIVITY_TYPE);
		}
	};
    
    /**
     * Lance un intent de la camera
     */
    public void startCamera(View view)
    {
    	Intent intentCamera = new Intent(this, CameraActivity.class);
    	startActivity(intentCamera);
    	//finish();
    }
    
    /**
     * onCreateOptionsMenu(Menu) - lifecycle de l'activite
     * - creation du menu (action bar) avec le layout de menu : main(.xml)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
    	Log.v(TAG, "Lifecycle - onCreateOptionsMenu");
    	// Recuperation du MenuInflater pour ajouter nos item au menu (action bar si presente)
		MenuInflater inflater = getMenuInflater();
		// Inflation du menu, ajout des items a l'action bar si elle est presente
		inflater.inflate(R.menu.main, menu);
		// On recupere l'item d'id action_track, c'est le bouton pour lancer/arreter le tracking
		menu_button_tracking = menu.findItem(R.id.action_track);
		// Gestion de la preference "TRACKING" pour savoir quel icone afficher (marche ou arret)
        pref = PreferenceManager.getDefaultSharedPreferences(this);
     	tracking = pref.getBoolean("TRACKING", false);
     	// Changement de l'icone (et du texte) si tracking en cours
		if(tracking) {
			menu_button_tracking.setIcon(R.drawable.ic_action_location_off_white);
			menu_button_tracking.setTitle(R.string.button_tracking_on);
		}
		return true;
    }

    /**
     * onOptionsItemSelected(MenuItem)
     * appele lors de l'appuie sur un des boutons (item) du menu (ici de l'action bar) 
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	// En fonction de l'id de l'item appuye :
        switch (item.getItemId())
        {
			case R.id.action_settings:
				// Lance l'activite des preferences
				prefIntent = new Intent(this, PreferencesActivity.class);
				startActivity(prefIntent);
				break;
			case R.id.action_camera:
				// Lance l'appareil photo
		    	startActivity(new Intent(this, CameraActivity.class));
				break;
			case R.id.action_track:
				// Lance/arrete le tracking GPS
				switchTracking(TRACKING_BUTTON_ACTIONBAR_ICON);
				break;
        }
        return super.onOptionsItemSelected(item);
    }
}
