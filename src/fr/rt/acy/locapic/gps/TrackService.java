package fr.rt.acy.locapic.gps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import fr.rt.acy.locapic.*;

public class TrackService extends Service
{
	/*public TrackService(String name) {
		super("TrackService");
	}
	public TrackService() {
		super("TrackService");
	}*/
	private static final String TAG = "trackService";
	private LocationManager mLocationManager = null;
	private static int LOCATION_INTERVAL = 2000;
	private static float LOCATION_DISTANCE = 0; //10f
	private static int test = 0;
	private String directory = null;
	private String fileName = null;
	private String GSA = null;
	private String TEMP = null;
	private String pos = null;
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private SharedPreferences pref;
	
	/**
	 * 
	 * @author Samuel
	 *
	 */
	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		@Override
		public void handleMessage(Message msg) {
			long endTime = System.currentTimeMillis() + 30*1000;
			while (System.currentTimeMillis() < endTime) {
				Log.v(TAG, "Service thread handler : bllbl");
				synchronized (this) {
					try {
						//wait(endTime - System.currentTimeMillis());
						wait(1000);
					} catch (Exception e) {
					}
				}
			}
			Log.v(TAG, "Fin service thread handler");
			//stopSelf(msg.arg1);
		}
	}
	
	/**
	 * 
	 * @author Samuel
	 *
	 */
	private class NmeaGpgsaListener implements android.location.GpsStatus.NmeaListener {
		@Override
		public void onNmeaReceived(long timestamp, String nmea) {
			Log.v(TAG, "NMEA beg :=> "+nmea.substring(0, 6));
			if(nmea.substring(0, 6).equals("$GPGSA")) {
				GSA = nmea;
			}
		}
	}
	
	/**
	 * 
	 * @author Samuel
	 *
	 */
	private class LocationListener implements android.location.LocationListener, android.location.GpsStatus.NmeaListener{
		Location mLastLocation;
		public LocationListener(String provider)
		{
			Log.v(TAG, "LocationListener " + provider);
			mLastLocation = new Location(provider);
		}
		@Override
		public void onLocationChanged(Location location)
		{
			Log.v(TAG, "onLocationChanged: " + location);
			mLastLocation.set(location);
			Log.i(TAG, "onLocationChanged ; test = "+test);
			test++;
			
			/*
			 * Save
			 */
			Log.v(TAG, "Extra : directory => "+directory+" ; file name : "+fileName);
			Log.v(TAG, "NMEA : GSA => "+GSA);
			mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			//mLocationManager.addNmeaListener(this);
			if(location != null) {
				Log.v(TAG, "Latitude " + location.getLatitude() + " et longitude " + location.getLongitude());
				File gpxFile = null;
				FileOutputStream output = null;
				StringBuffer lu = null;
				try {
					gpxFile = new File(directory+"/"+fileName);
					FileInputStream input = new FileInputStream(gpxFile);
					int value;
					lu = new StringBuffer();
					while((value = input.read()) != -1) {
						lu.append((char)value);
					}
					if(input != null)
						input.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				/*
				 * Creation de la date
				 */
				long posixTime = (long) location.getTime();
				Date date = new Date(posixTime); /// *1000 pour passer le temps en millisecondes //*1000L ???
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss", Locale.FRENCH); /// Format de la date
				sdf.setTimeZone(TimeZone.getTimeZone("GMT+1")); /// Give a timezone reference for formating
				StringBuilder formattedDateBuilder = new StringBuilder(sdf.format(date));
				formattedDateBuilder.append("Z");
				formattedDateBuilder.insert(10, "T");
				String formattedDate = formattedDateBuilder.toString();
				Log.v(TAG, "TIME => "+formattedDate);

				/*
				 * gestion du fix et de la precision DOP (NMEA, $GPGSA)
				 */
				String nbSat = String.valueOf(location.getExtras().getInt("satellites"));
				String posDOP = "";
				if(nbSat != null)
					posDOP += "<sat>"+nbSat+"</sat>";
				String locFix = "2d";
				//GSA = MainActivity.getNMEA();
				if(GSA != null) {
					String[] gsarray = GSA.split(",");
					String pdop = gsarray[gsarray.length - 3];
					String hdop = gsarray[gsarray.length - 2];
					String vdop = gsarray[gsarray.length - 1].substring(0, gsarray[gsarray.length - 1].length() - 5);
					if (gsarray[2] == "2" || gsarray[2] == "3")
						locFix=gsarray[2]+"d";
					if(hdop != "") {
						posDOP += "<hdop>"+hdop+"</hdop>";
						if(vdop != "")
							posDOP += "<vdop>"+vdop+"</vdop><pdop>"+pdop+"</pdop>";
					}
				}
				
				TEMP = lu.toString();
				int index = TEMP.indexOf("</trkseg>", 0)-3;
				String ele = "";
				if(locFix == "3d" || location.getAltitude() != 0.0) {
					ele += "<ele>"+location.getAltitude()+"</ele>";
					locFix = "3d";
				}
				pos = "\n\t\t\t<trkpt lat=\""+location.getLatitude()+"\" lon=\""+location.getLongitude()+"\">"+ele+"<time>"+formattedDate+"</time><fix>"+locFix+"</fix>"; //</trkpt>
				pos += posDOP+"</trkpt>";
				TEMP = new StringBuilder(TEMP).insert(index, pos).toString();

				try {
					output = new FileOutputStream(gpxFile);
					output.write(TEMP.getBytes());
					if(output != null)
						output.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				Log.v(TAG, "Loc == null");
			}
			/*
			 * Fin save
			 */
		}
		@Override
		public void onProviderDisabled(String provider)
		{
			Log.v(TAG, "onProviderDisabled: " + provider);            
		}
		@Override
		public void onProviderEnabled(String provider)
		{
			Log.v(TAG, "onProviderEnabled: " + provider);
		}
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras)
		{
			Log.v(TAG, "onStatusChanged: " + provider);
		}
		@Override
		public void onNmeaReceived(long timestamp, String nmea) {
			Log.v(TAG, "NMEA start :=> "+nmea.substring(0, 6));
			if(nmea.substring(0, 6).equals("$GPGSA")) {
				GSA = nmea;
			}
		}
	} 
	LocationListener mLocationListeners = new LocationListener(LocationManager.GPS_PROVIDER);
	NmeaGpgsaListener mNMEAListener = new NmeaGpgsaListener();
	
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
		
		Bundle extras = intent.getExtras();
		directory = extras.getString("directory");
		fileName = extras.getString("fileName");
		
		Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);
		
		/*
		 * Partie Notification + foreground
		 */
		/*Notification notification = new Notification(R.drawable.ic_action_refresh, "tickerTest", System.currentTimeMillis());
        Intent notificationIntent = new Intent(MainActivity.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(MainActivity.this, "Titre", "Message", pendingIntent);*/
		Log.i(TAG, "Received Start Foreground Intent ");
		
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		
			Intent notificationIntent = new Intent(this, MainActivity.class);
			notificationIntent.setAction("MainActivity");
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
			
	        Notification notif = new Notification.Builder(this)
	        .setTicker("Debut de l'enregistrement de la trace gps...")
	        .setContentTitle("LocaPic - Itinerance")
	        .setContentText("Vos deplacements sont enregistres")
	        .setSmallIcon(R.drawable.ic_launcher)
	        .setContentIntent(pendingIntent)
	        .setOngoing(true).getNotification();
	        
	        startForeground(2, notif);
		/*
		 * 
		 */
		return START_REDELIVER_INTENT;
	}
	@Override
	public void onCreate()
	{
		Log.v(TAG, "onCreate");
		/**
		 * Gestion Service
		 */
		HandlerThread thread = new HandlerThread("trackServiceHandlerThread", Process.THREAD_PRIORITY_DEFAULT);
		thread.start();

		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
		
		/**
		 * Gestion itineraire
		 */
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		LOCATION_INTERVAL = Integer.parseInt(pref.getString("LOCATION_INTERVAL", "2000"))*1000;
		LOCATION_DISTANCE = Integer.parseInt(pref.getString("LOCATION_DISTANCE", "0"));
		initializeLocationManager();
		try {
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,	mLocationListeners);
			mLocationManager.addNmeaListener(mNMEAListener);
		} catch (java.lang.SecurityException ex) {
			Log.i(TAG, "fail to request location update, ignore", ex);
		} catch (IllegalArgumentException ex) {
			Log.d(TAG, "gps provider does not exist " + ex.getMessage());
		}
	}
	@Override
	public void onDestroy()
	{
		Log.v(TAG, "onDestroy");
		super.onDestroy();
		if (mLocationManager != null) {
			try {
				mLocationManager.removeUpdates(mLocationListeners);
				mLocationManager.removeNmeaListener(mNMEAListener);
			} catch (Exception ex) {
				Log.i(TAG, "fail to remove location listners, ignore", ex);
			}
		}
		SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		prefEditor.putBoolean("RECORDING2", false);
		prefEditor.commit();
	}
	private void initializeLocationManager() {
		Log.v(TAG, "initializeLocationManager");
		if (mLocationManager == null) {
			mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		}
	}
/*	@Override
	protected void onHandleIntent(Intent intent) {
		
	}*/
}
