package fr.rt.acy.locapic.gps;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import fr.rt.acy.locapic.*;
import fr.rt.acy.locapic.gps.DataReceiver.UiUpdater;


public class LocStatsActivity extends Activity implements LocationListener, NmeaListener
{
	public final static String TAG ="LocStatsActivity";
	private DataReceiver dataReceiver;
	private LocationManager lm;
	private SharedPreferences pref;
	private String GSA = null;
	private TextView tv_locLat;
	private TextView tv_locLon;
	private TextView tv_locAlt;
	private TextView tv_locSpeed;
	private TextView tv_locDate;
	private TextView tv_locTime;
	private TextView tv_locHdop;
	private TextView tv_locVdop;
	private TextView tv_locPdop;
	private Button refreshButton;
	
	private void refreshUi(Bundle data) {
		float ratio = (float) 3.6;
		float speed = ratio*Float.parseFloat(data.getString("LOC_SPEED", "0"));
		String lat = data.getString("LOC_LAT");
		int i;
		if(lat.indexOf(".")+9 > lat.length())
			i = lat.length();
		else
			i = lat.indexOf(".")+9;
		lat = lat.substring(0, i); // 1+nombre de chiffre apres la virgule
		String lon = data.getString("LOC_LON");
		if(lon.indexOf(".")+9 > lon.length())
			i = lon.length();
		else
			i = lon.indexOf(".")+9;
		lon = lon.substring(0, i);
		
		tv_locLat.setText(lat);
    	tv_locLon.setText(lon);
    	tv_locAlt.setText(data.getString("LOC_ALT", "0")+" m");
    	tv_locSpeed.setText(String.valueOf(speed)+" km/h");
    	tv_locDate.setText(data.getString("LOC_DATE"));
    	tv_locTime.setText(data.getString("LOC_TIME"));
    	tv_locHdop.setText(data.getString("LOC_HDOP", ""));
    	tv_locVdop.setText(data.getString("LOC_VDOP", ""));
    	tv_locPdop.setText(data.getString("LOC_PDOP", ""));
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locstats);
        
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        
        tv_locLat = (TextView) findViewById(R.id.loc_lat);
        tv_locLon = (TextView) findViewById(R.id.loc_lon);
        tv_locAlt = (TextView) findViewById(R.id.loc_alt);
        tv_locSpeed = (TextView) findViewById(R.id.loc_speed);
        tv_locDate = (TextView) findViewById(R.id.loc_date);
        tv_locTime = (TextView) findViewById(R.id.loc_time);
        tv_locHdop = (TextView) findViewById(R.id.loc_hdop);
        tv_locVdop = (TextView) findViewById(R.id.loc_vdop);
        tv_locPdop = (TextView) findViewById(R.id.loc_pdop);
        
        refreshButton = (Button) findViewById(R.id.loc_refresh);
        refreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Si GPS ON
				if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					// Demande d'update unique de la position et d'ecoute des chaines NMEA
					lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, LocStatsActivity.this, null);
					lm.addNmeaListener(LocStatsActivity.this);
				}
				if(pref.getBoolean("USE_NETWORK_LOCATION_PROVIDER", false))
					lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, LocStatsActivity.this, null);
				if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || pref.getBoolean("USE_NETWORK_LOCATION_PROVIDER", false))
					Toast.makeText(getApplicationContext(), "En attente des donnees de localisation...", Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		/** Gestion du BroadcastReceiver pour l'affichage des infos provenant du Service */
		dataReceiver = new DataReceiver(new UiUpdater() {
		    @Override
		    public void uiUpdateCallback(Intent intent) {
		    	Log.v(TAG, "uiUpdateCallback");
		    	Bundle data = intent.getExtras();
		    	refreshUi(data);
		    }     
		});
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(TrackService.INTENT_ACTION);
		registerReceiver(dataReceiver, intentFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(dataReceiver);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if(lm != null) {
			lm.removeUpdates(LocStatsActivity.this);
			lm.removeNmeaListener(LocStatsActivity.this);
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onLocationChanged(Location loc) {
		if(loc != null) {
			String pdop = "";
			String hdop = "";
			String vdop = "";
			if(GSA != null && loc.getProvider().equals(LocationManager.GPS_PROVIDER)) {
				String[] gsarray = GSA.split(",");
				pdop = gsarray[gsarray.length - 3];
				hdop = gsarray[gsarray.length - 2];
				vdop = gsarray[gsarray.length - 1].substring(0, gsarray[gsarray.length - 1].length() - 5);
			}
			// Formatage de la date et de l'heure
			long locPosixTime = (long) loc.getTime();
			Date locDate = new Date(locPosixTime);
			SimpleDateFormat euDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
			SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.FRENCH);
			euDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+1"));
			timeFormat.setTimeZone(TimeZone.getTimeZone("GMT+1"));
			String euDate = euDateFormat.format(locDate);
			String time = timeFormat.format(locDate);
			// Ajout des donnees a un bundle et lancement de refreshUi pour actualiser l'interface
			Bundle locData = new Bundle();
			locData.putString("LOC_LAT", String.valueOf(loc.getLatitude()));
			locData.putString("LOC_LON", String.valueOf(loc.getLongitude()));
			locData.putString("LOC_ALT", String.valueOf(loc.getAltitude()));
			locData.putString("LOC_SPEED", String.valueOf(loc.getSpeed()));
			locData.putString("LOC_DATE", euDate);
			locData.putString("LOC_TIME", time);
			locData.putString("LOC_HDOP", hdop);
			locData.putString("LOC_VDOP", vdop);
			locData.putString("LOC_PDOP", pdop);
			refreshUi(locData);
		}
		// Arret des listeners
		lm.removeUpdates(LocStatsActivity.this);
		lm.removeNmeaListener(LocStatsActivity.this);
	}

	@Override
	public void onProviderDisabled(String provider) { }

	@Override
	public void onProviderEnabled(String provider) { }

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) { }

	@Override
	public void onNmeaReceived(long timestamp, String nmea) {
		if(nmea.substring(0, 6).equals("$GPGSA")) {
			GSA = nmea;
		}		
	}
	
}