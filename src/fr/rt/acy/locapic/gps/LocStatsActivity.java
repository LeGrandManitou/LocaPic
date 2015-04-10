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

import java.text.SimpleDateFormat;
import java.util.Calendar;
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

public class LocStatsActivity extends Activity implements LocationListener, NmeaListener
{
	// TAG pour les logs
	public final static String TAG ="LocStatsActivity";
	// Notre dataReceiver
	private DataReceiver dataReceiver;
	// LocationManager pour la gestion de la localisation fourni par le systeme
	private LocationManager lm;
	// SharedPreferences pour recup les preferences utilisateur
	private SharedPreferences pref;
	// Chaine GSA
	private String GSA;
	// Nos TextView (interface)
	private TextView tv_locLat;
	private TextView tv_locLon;
	private TextView tv_locAlt;
	private TextView tv_locSpeed;
	private TextView tv_locDate;
	private TextView tv_locTime;
	private TextView tv_locHdop;
	private TextView tv_locVdop;
	private TextView tv_locPdop;
	// Notre bouton
	private Button refreshButton;
	
	/**
	 * Met a jour l'interface en fonction des donnees recues
	 * @param data - toutes les donnees de loc
	 */
	private void refreshUi(Bundle data) {
		// Mutliplicateur pour passer de m/s en km/h
		float ratio = (float) 3.6;
		float speed = ratio*Float.parseFloat(data.getString("LOC_SPEED", "0"));
		String lat = data.getString("LOC_LAT");
		// Limitation du nombre de decimal de la lattitude et de la longitude (8)
		int i;
		if(lat.indexOf(".")+9 > lat.length())
			i = lat.length();
		else
			i = lat.indexOf(".")+9;
		// 1+nombre de chiffre apres la virgule
		lat = lat.substring(0, i);
		String lon = data.getString("LOC_LON");
		if(lon.indexOf(".")+9 > lon.length())
			i = lon.length();
		else
			i = lon.indexOf(".")+9;
		lon = lon.substring(0, i);
		
		// MaJ des textes des TextView
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
	
	/*
	 * onCreate - Activity lifecycle
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locstats);
        // Initialisation preference manager et locationManager
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        
        // Recuperation des TextView
        tv_locLat = (TextView) findViewById(R.id.loc_lat);
        tv_locLon = (TextView) findViewById(R.id.loc_lon);
        tv_locAlt = (TextView) findViewById(R.id.loc_alt);
        tv_locSpeed = (TextView) findViewById(R.id.loc_speed);
        tv_locDate = (TextView) findViewById(R.id.loc_date);
        tv_locTime = (TextView) findViewById(R.id.loc_time);
        tv_locHdop = (TextView) findViewById(R.id.loc_hdop);
        tv_locVdop = (TextView) findViewById(R.id.loc_vdop);
        tv_locPdop = (TextView) findViewById(R.id.loc_pdop);
        
        // Recup du bouton et mise d'un listener pour actualiser la position
        refreshButton = (Button) findViewById(R.id.loc_refresh);
        refreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Si GPS ON
				if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					// Demande d'update unique de la position et d'ecoute des chaines NMEA
					lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, LocStatsActivity.this, null);
					lm.addNmeaListener(LocStatsActivity.this);
				} else if(!pref.getBoolean("USE_NETWORK_LOCATION_PROVIDER", false))
					Toast.makeText(getApplicationContext(), R.string.toast_gps_off, Toast.LENGTH_SHORT).show();
				// Si Networks Ok (user pref) => request d'updates
				if(pref.getBoolean("USE_NETWORK_LOCATION_PROVIDER", false))
					lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, LocStatsActivity.this, null);
				// Toast pour dire en attente
				if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || pref.getBoolean("USE_NETWORK_LOCATION_PROVIDER", false))
					Toast.makeText(getApplicationContext(), R.string.toast_gps_waiting, Toast.LENGTH_SHORT).show();
			}
		});
	}

	// Lifecycle de l'activite
	@Override
	protected void onRestart() {
		super.onRestart();
	}
	
	// Lifecycle de l'activite
	@Override
	protected void onStart() {
		super.onStart();
	}
	
	// Lifecycle de l'activite
	@Override
	protected void onResume() {
		super.onResume();
		
		/** Gestion du BroadcastReceiver pour l'affichage des infos provenant du Service */
		dataReceiver = new DataReceiver() {
			@Override
			public void uiUpdateCallback(Intent intent) {
				Log.v(TAG, "uiUpdateCallback");
		    	Bundle data = intent.getExtras();
		    	refreshUi(data);
			}
		};
		// Creation d'un filtre d'intent avec notre Action (chaine attribut statique de notre service)
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(TrackService.INTENT_ACTION);
		// Enregistrement de notre DataReceiver pour recevoir ces intents
		registerReceiver(dataReceiver, intentFilter);
	}

	// Lifecycle de l'activite
	@Override
	protected void onPause() {
		super.onPause();
		// On arrete le receiver
		unregisterReceiver(dataReceiver);
	}
	
	// Lifecycle de l'activite 
	@Override
	protected void onStop() {
		super.onStop();
		// Suppression des updates
		if(lm != null) {
			lm.removeUpdates(LocStatsActivity.this);
			lm.removeNmeaListener(LocStatsActivity.this);
		}
	}
	
	// Lifecycle de l'activite
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onLocationChanged(Location loc) {
		// Toast pour l'utilisateur, indique quel provider a ete utilise
		if(loc.getProvider().equals(LocationManager.GPS_PROVIDER))
			Toast.makeText(this, R.string.toast_loc_gps_found, Toast.LENGTH_LONG).show();
		else
			Toast.makeText(this, R.string.toast_loc_network_found, Toast.LENGTH_LONG).show();
		// Si Loc pas nul
		if(loc != null) {
			String pdop = "";
			String hdop = "";
			String vdop = "";
			//Log.d(TAG, "Location received, NMEA : "+GSA);
			if(GSA != null && loc.getProvider().equals(LocationManager.GPS_PROVIDER)) {
				String[] gsarray = GSA.split(",");
				pdop = gsarray[gsarray.length - 3];
				hdop = gsarray[gsarray.length - 2];
				vdop = gsarray[gsarray.length - 1].substring(0, gsarray[gsarray.length - 1].length() - 5);
			}
			// Creation d'un objet de classe Date a partir du temps POSIX de la position
			long locPosixTime = (long) loc.getTime();
			Date locDate = new Date(locPosixTime);
			// Format pour la date
			SimpleDateFormat euDateFormat = new SimpleDateFormat("dd/MM/yyyy");
			// Format pour l'heure
			SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
			// Creation des chaines a partir des format et de la date de la localisation
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
			// Lancement de la MaJ de l'interface avec notre methode refreshUi()
			refreshUi(locData);
		}
		// Arret des listeners si on recoit une position GPS (donc arret du listener utilisant le provider NETWORK
		if (loc.getProvider().equals(LocationManager.GPS_PROVIDER) && lm != null) {
			lm.removeUpdates(LocStatsActivity.this);
			lm.removeNmeaListener(LocStatsActivity.this);
		}
	}

	// Methode de l'interface LocationListener
	@Override
	public void onProviderDisabled(String provider) {}
	// Methode de l'interface LocationListener
	@Override
	public void onProviderEnabled(String provider) {}
	// Methode de l'interface LocationListener
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}
	// NmeaReceiver - reception des chaines GSA (GPS)
	@Override
	public void onNmeaReceived(long timestamp, String nmea) {
		// Si la chaine est une chaine GSA ($GPGSA,...) et que son fix ne vaut pas 1 (donc 2 ou 3)
		// Alors on garde la chaine dans l'attribut GSA du service
		if(nmea.substring(0, 6).equals("$GPGSA") && !nmea.split(",")[2].equals("1")) {
			GSA = nmea;
			//Log.v(TAG, "NMEA Received : "+GSA);
		}		
	}
	
}
