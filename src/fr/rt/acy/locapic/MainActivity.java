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

public class MainActivity extends TabActivity 
{
	public final static int TRACKING_BUTTON_ACTIVITY_TEXT = 0;
	public final static int TRACKING_BUTTON_ACTIVITY_ICON = 1;
	public final static int TRACKING_BUTTON_ACTIONBAR_ICON = 2;
	
	private final static int TRACKING_BUTTON_ACTIVITY_TYPE = TRACKING_BUTTON_ACTIVITY_ICON;
	private final static int TRACKING_BUTTON_TYPE = TRACKING_BUTTON_ACTIONBAR_ICON;
	private final String DEBUG_AUTO_START_INTENT = ""; // "camera" pour lancer automatiquement la camera
	private Intent prefIntent;
	private final String TAG = "HOME";
	private SharedPreferences pref;
	private SharedPreferences.Editor prefEditor;
	private boolean useNetworkPref = false;
	private boolean notifPref = false;
	private Button button_tracking = null;
	private ImageButton image_button_tracking = null;
	private static MenuItem menu_button_tracking;
	private boolean tracking = false;
	private LocationManager lm = null;
	//private final String GPX_BASE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n<gpx version=\"1.1\">\n\t<metadata>\n\t\t<name>Android GPS receiver track</name>\n\t\t<desc>GPS track logged on an Android device with an application from a project by Samuel Beaurepaire &amp; Virgile Beguin for IUT of Annecy (Fr), RT departement.</desc>\n\t\t<time></time>\n\t\t<author>\n\t\t\t<name>Samuel Beaurepaire</name>\n\t\t\t<email id=\"sjbeaurepaire\" domain=\"orange.fr\" />\n\t\t</author>\n\t\t<keywords></keywords>\n\t</metadata>\n\n\t<trk>\n\t</trk>\n</gpx>";
	//private final String FILES_DIR = Environment.getExternalStorageDirectory().getPath() + "/TracesGPS/";
	
	public class MyDataReceiver extends BroadcastReceiver {
		
		public static final String TAG = "MyDataReceiver";
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String test = intent.getExtras().getString("DATA");
			Log.i(TAG, "BLBL2 : "+test);
		}
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v(TAG, "Lifecycle - onCreate");
        if (DEBUG_AUTO_START_INTENT.equals("camera")) 
        	startCamera(null);
        
        // Gestion des preferences
        prefEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        pref = PreferenceManager.getDefaultSharedPreferences(this);
     	tracking = pref.getBoolean("TRACKING", false);
     	if(!pref.contains("LOCATION_INTERVAL"))
     		prefEditor.putString("LOCATION_INTERVAL", "2");
     	if(!pref.contains("LOCATION_DISTANCE"))
     		prefEditor.putString("LOCATION_DISTANCE", "0");
     	prefEditor.commit();
     	
        /* Version texte du boutton itineraire
         * button_tracking = (Button) findViewById(R.id.main_button_tracking);
         * button_tracking.setOnClickListener(trackingButtonListener);*/
        /* Version avec un icone du boutton itineraire
         * image_button_tracking = (ImageButton) findViewById(R.id.main_button_tracking);
         * image_button_tracking.setOnClickListener(trackingButtonListener); */
        // On utilise la version actionBar
        
        /** Gestion des onglets */
		// Creation d'un TabHost qui accueillera tous nos onglets
		TabHost tabHost = (TabHost)findViewById(android.R.id.tabhost);
		
		// Creation d'onglets
		TabSpec tab1 = tabHost.newTabSpec("First Tab");
		TabSpec tab2 = tabHost.newTabSpec("Second Tab");
		TabSpec tab3 = tabHost.newTabSpec("Third tab");
		
		// Parametrage du nom et des activites pour les differents onglets
		tab1.setIndicator(getResources().getString(R.string.tab_locstats)).setContent(new Intent(this, fr.rt.acy.locapic.gps.LocStatsActivity.class));
		tab2.setIndicator(getResources().getString(R.string.tab_trackstats)).setContent(new Intent(this, fr.rt.acy.locapic.gps.TrackStatsActivity.class));
		tab3.setIndicator(getResources().getString(R.string.tab_map));//.setContent(new Intent(this,fr.rt.acy.locapic.gps.TrackStatsActivity.class));
		
		// Ajout des onglets au TabHost
		tabHost.addTab(tab1);
		tabHost.addTab(tab2);
		//tabHost.addTab(tab3);
		
    }
    
    @Override
	protected void onResume() {
		super.onResume();
		Log.v(TAG, "Lifecycle - onResume");
		// LocationManager pour toute l'activite
		lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		// Gestion des preferences
		tracking = pref.getBoolean("TRACKING", false);
		useNetworkPref = pref.getBoolean("USE_NETWORK_LOCATION_PROVIDER", false);
		
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
     * Lance ou arrete le service qui enregistre l'itineraire
     * Change une preference TRACKING (boolean pour savoir si on enregistre ou pas)
     * Change aussi le texte ou l'image du boutton clique en fonction de l'entier passe en parametre :
     * @param button_type - 0 pour un boutton texte dans l'activite (fond blanc) ; 1 pour une image boutton dans l'activite (fond blanc) ; 2 pour une image boutton dans l'actionBar (fond noir)
     */
    private void switchTracking(int button_type) {
    	Intent serviceIntent = new Intent(MainActivity.this, fr.rt.acy.locapic.gps.TrackService.class);
		/** Si pas deja en train d'enregistrer un itineraire */
		if(!tracking) {
			/** Si GPS on */
			if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				startService(serviceIntent);
				// Gestion de la preference (pour savoir si record en cours ou pas)
				tracking = true; //PreferenceManager.getDefaultSharedPreferences(getBaseContext())
				SharedPreferences.Editor prefEditor = pref.edit();
				prefEditor.putBoolean("TRACKING", tracking);
				prefEditor.commit();
				// Changement du bouton
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
				/**
				 * Si GPS pas actif
				 */
				Toast.makeText(MainActivity.this, R.string.toast_gps_off, Toast.LENGTH_SHORT).show();
			}
		} else {
			/** Si deja en train d'enregistrer un itineraire : Arret du service + Changement du bouton + Edition de la preference */
			// Arret du service
			stopService(serviceIntent);
			// Gestion de la preference (pour savoir si record en cours ou pas)
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
	 * lance switchTracking(int)
	 */
	private OnClickListener trackingButtonListener = new OnClickListener() {
		/** Lors du clique sur le bouton */
		@Override
		public void onClick(View v) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
    	Log.v(TAG, "Lifecycle - onCreateOptionsMenu");
		// Inflate the menu; this adds items to the action bar if it is present.
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		menu_button_tracking = menu.findItem(R.id.action_track);
		// Gestion de la preference tracking pour savoir quel icone afficher pour le bouton de marche/arret de l'itineraire
        pref = PreferenceManager.getDefaultSharedPreferences(this);
     	tracking = pref.getBoolean("TRACKING", false);
		if(tracking) {
			menu_button_tracking.setIcon(R.drawable.ic_action_location_off_white);
			menu_button_tracking.setTitle(R.string.button_tracking_on);
		}
		return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId())
        {
			case R.id.action_settings:
				prefIntent = new Intent(this, PreferencesActivity.class);
				startActivity(prefIntent);
				break;
			case R.id.action_camera:
		    	startActivity(new Intent(this, CameraActivity.class));
				break;
			case R.id.action_track:
				switchTracking(TRACKING_BUTTON_ACTIONBAR_ICON);
				break;
        }
        return super.onOptionsItemSelected(item);
    }
}
