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
	private final String TAG = "HOME";
	private SharedPreferences pref;
	private boolean useNetworkPref = false;
	private boolean notifPref = false;
	private Button button_tracking = null;
	private boolean tracking = false;
	private LocationManager lm = null;
	private final String GPX_BASE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n<gpx version=\"1.1\">\n\t<metadata>\n\t\t<name>Android GPS receiver track</name>\n\t\t<desc>GPS track logged on an Android device with an application from a project by Samuel Beaurepaire &amp; Virgile Beguin for IUT of Annecy (Fr), RT departement.</desc>\n\t\t<time></time>\n\t\t<author>\n\t\t\t<name>Samuel Beaurepaire</name>\n\t\t\t<email id=\"sjbeaurepaire\" domain=\"orange.fr\" />\n\t\t</author>\n\t\t<keywords></keywords>\n\t</metadata>\n\n\t<trk>\n\t</trk>\n</gpx>";
	private final String FILES_DIR = Environment.getExternalStorageDirectory().getPath() + "/TracesGPS/";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        if (DEBUG_AUTO_START_INITENT.equals("camera")) 
        	startCamera(null);
            
        button_tracking = (Button) findViewById(R.id.main_button_tracking);
        button_tracking.setOnClickListener(trackingButtonListener);
    }
    
    @Override
	protected void onResume() {
		super.onResume();
		Log.v(TAG, "onResume");
		// LocationManager pour toute l'activite
		lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		// Gestion des preferences
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		tracking = pref.getBoolean("TRACKING", false);
		useNetworkPref = pref.getBoolean("USE_NETWORK_LOCATION_PROVIDER", false);
		
		if(tracking)
			button_tracking.setText(R.string.button_tracking_on);
	}
    
    /**
	 * Listener du bouton "Enregistrer itineraire" avec Service
	 * (ou "Arreter l'itineraire en cours") 
	 */
	private OnClickListener trackingButtonListener = new OnClickListener() {
		/**
		 * Lors du clique sur le bouton
		 */
		@Override
		public void onClick(View v) {
			Intent serviceIntent = new Intent(MainActivity.this, fr.rt.acy.locapic.gps.TrackService.class);
			/**
			 * Si pas deja en train d'enregistrer un itineraire
			 */
			if(!tracking) {
				/**
				 * Si GPS on
				 */
				if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					startService(serviceIntent);
					// Gestion de la preference (pour savoir si record en cours ou pas)
					tracking = true; //PreferenceManager.getDefaultSharedPreferences(getBaseContext())
					SharedPreferences.Editor prefEditor = pref.edit();
					prefEditor.putBoolean("TRACKING", tracking);
					prefEditor.commit();
					// Changement du bouton
					button_tracking.setText("Stopper l'itinerance (service)");
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
				//serviceIntent.putExtra("STOP", true);
				serviceIntent.setAction("fr.rt.acy.locapic.STOP");
				sendBroadcast(serviceIntent);
				stopService(serviceIntent);
				// Gestion de la preference (pour savoir si record en cours ou pas)
				tracking = false;
				SharedPreferences.Editor prefEditor = pref.edit();
				prefEditor.putBoolean("TRACKING", tracking);
				prefEditor.commit();
				// Changement du bouton
				button_tracking.setText("Lancer l'itinerance (service)");
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
