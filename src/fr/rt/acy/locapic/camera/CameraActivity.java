
package fr.rt.acy.locapic.camera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import fr.rt.acy.locapic.R;

//on supprime les warnings a cause de la classe Camera depreciee a partir de l'api 21
@SuppressWarnings("deprecation")
public class CameraActivity extends Activity implements SensorEventListener, LocationListener
{
	// tag pour debugage
	private final static String TAG = CameraActivity.class.getName(); 
	private final static String TAG_METADONNEES = "metadonnees"; 

	// tag pour metadonnees exif. L'azimute et l'orientation y seront enregistres
	public final static String TAG_USER_COMMENT = "UserComment";
	// tag pour startActivityForResult
	private final static int REQUEST_CODE_POPUP_FAST_SETTINGS = 100;
	// pause durant laquelle le preview se fige pour afficher un apercu de la photo prise entre chaque photo (en ms)
	public final static int pauseEntrePhoto = 0;

	private String cheminPhoto = null;      // Chemin de la dernier photo enregistree
	private Camera camera;                  // Camera utillisee
	
	/// Boutons ///
	private PreviewCamera previewCamera;    // preview de la camera
	private ImageButton fastSettingsButton; // bouton fastSettings (activation du flash, retardateur, ...)
	private ImageButton prendrePhotoButton;	// bouton prendre une photo
	private ImageButton retourButton;		// bouton retour au menu principal
    private int maxZoom = 0;				// niveau de zoom maximum. Initialisé dans le onCreate
    private boolean isZoomSupported = false;// Le zoom est supporté (true) ou non (false)
    
    private Orientation orientationEcran = Orientation.PORTRAIT; // indique l'orientation (portrait ou paysage)
    
	/// Capteurs ///
	private SensorManager sensorManager;    // gere les capteurs du telephone
	private Sensor accelSensor;             // accelerometre
	private Sensor magnSensor;              // capteur de champ magnetique(boussole)
	
	// Vecteur utillise par getRotationMatrix dans onSensorChanged pour obtenir une matrisse de rotation.
	private float[] accelVector = new float[3]; // vecteur gravite
	private float[] magnVector = new float[3]; 	// vecteur champ magnetique
	
	// LocationManager pour les mises a jours de position
	private LocationManager locationManager;
	// Pour recuperer les preferences utilisateurs
	// (utilise pour savoir si utilisation du provider NETWORK ou non pour la geolocalisation)
	private SharedPreferences pref;
	
	/*
	 * Orientation du telephone en degree. De 0 a 180 -> 90 = telephone vertical, 
	 * 0 = horizontal(ecran vers le haut) et 180 = horizontal (ecran vers le bas)
	 * A mettre a jour automatiquement
	 */
	private float orientation;
	
	// Azimute du telephone en degree. Mis a jour automatiquement
	private float azimute;
	
	// Options camera :
	private Flash flashMode = Flash.AUTO;   	// Mode du flash et valeur par defaut
	private int retardateur = 0;            	// Retardateur en secondes (0 par defaut)
	private List<Size> supportedPictureSizes; 	// Taille de l'appareil photo supporte
	private int indexCameraSizeSelected = 1;	// index dans supportedPictureSizes de la taille selectionn�
	
	/// Multitouch ///
	// Coordonnées du pointeur initial
	private float downX;
	private float downY;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        
        //TODO ne fonctionne pas completement: ne masque pas completement la bar de bouton systeme
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE); 

        // On active la camera par defaut
        try 
        {
            camera = null;
            camera = Camera.open();
		} 
        catch (Exception e) 
		{
			Log.e(TAG, "Impossible d'activer la camera : " + e.getMessage());
		}
        
        // Parametrage de la camera
        Parameters params = camera.getParameters();
        params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        maxZoom = params.getMaxZoom();                                  // Recuperer le zoom maximum
        isZoomSupported = params.isZoomSupported();						// Le zoom est il suporte ?
        supportedPictureSizes = params.getSupportedPictureSizes();		// Recuperer les taille de camera supporte
        camera.setParameters(params);
        
        previewCamera = new PreviewCamera(this, camera);	// Creation du preview
        previewCamera.setKeepScreenOn(true);                // Garder l'ecran allume
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(previewCamera);						// affichage du preview
        
        // Initialisation des variables des Boutons
        prendrePhotoButton = (ImageButton) findViewById(R.id.prendrePhoto);
        fastSettingsButton = (ImageButton) findViewById(R.id.fastSettings);
        retourButton = (ImageButton) findViewById(R.id.retour);
        
        // listener des boutons
        prendrePhotoButton.setOnClickListener(new OnClickListener() 
        {
			@Override
			public void onClick(View v)
			{
				prendrePhoto();
			}
		});
        fastSettingsButton.setOnClickListener(new OnClickListener() 
        {
			@Override
			public void onClick(View v)
			{
				showFastSettingsDialog();
			}
		});
        retourButton.setOnClickListener(new OnClickListener() 
        {
			@Override
			public void onClick(View v) 
			{
				finish();
			}
		});
        
        // Initialiser les variables des capteurs
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        
        // Commencer l'ecoute des capteurs
    	sensorManager.registerListener( this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
    	sensorManager.registerListener( this, magnSensor, SensorManager.SENSOR_DELAY_NORMAL);
    	
    	// Enregistrement de l'activite (qui implemente l'interface LocationListener) comme LocationListener
    	// Mise a jour de la position toutes les 30 secondes
    	locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    	pref = PreferenceManager.getDefaultSharedPreferences(this);
    	try {
    		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 0, this, null);
    		if (pref.getBoolean("USE_NETWORK_LOCATION_PROVIDER", false))
    			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000, 0, this, null);
    	} catch (Exception e) {
    		Log.e(TAG, e.getLocalizedMessage());
    	}
    }
    
    @Override
    protected void onDestroy() 
    {
    	Log.v(TAG, "on libere la camera");
    	// on libere la camera
    	if (camera != null)
    	{
    		try 
    		{
    			camera.release();	// On libere la camera
                camera = null;
			} 
    		catch (Exception e)
			{
				Log.e(TAG, "onDestroy impossible de liberer la camera : " + e.getMessage());
			}
        }
    	
    	// on arrete l'ecoute des capteurs
    	sensorManager.unregisterListener(this);
    	
    	super.onDestroy();
    }
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
    	if (requestCode == REQUEST_CODE_POPUP_FAST_SETTINGS)
    	{
    		indexCameraSizeSelected = data.getIntExtra("indexCameraSizeSelected", 0);
    		Size cameraSizeSelected = supportedPictureSizes.get(indexCameraSizeSelected);
    		retardateur = data.getIntExtra("retardateur", 0);
    		flashMode = (Flash) data.getExtras().get("flashMode");
    		
    		Parameters param = camera.getParameters();
    		if(flashMode == Flash.AUTO)
    		{
    			// Flash automatique
    			param.setFlashMode(Parameters.FLASH_MODE_AUTO);
    		}
    		else if(flashMode == Flash.ON)
	    	{
	    		// flash On
	    		param.setFlashMode(Parameters.FLASH_MODE_ON);
	    	}
	    	else if(flashMode == Flash.OFF)
	    	{
	    		// flash OFF
	    		param.setFlashMode(Parameters.FLASH_MODE_OFF);
	    	}
    		// Changement de la resolution
    		param.setPictureSize(cameraSizeSelected.width, cameraSizeSelected.height);
    		
    		// Aplication des nouveaux parametres
    		camera.setParameters(param);
    	}
    	
    	super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) 
    {
    	final float sensitivity = 0.08f;	// sensibilité du multitouch
    	
    	switch (event.getAction()) 
		{
			case MotionEvent.ACTION_DOWN:
				// Si l'ecran est touché
				
		    	// Enregistrer les coordonnées du 1er pointeur
		    	downX = event.getX();
		    	downY = event.getY();
				break;
				
			case MotionEvent.ACTION_MOVE:
				// Si le(s) pointeur(s) a bougé 
				
				float x, y;					// difference entre le touché initial et le pointeur actuel
				float curentX, currentY; 	// Coordonnée du pointeur actuel
				
				// Recuperer les coordonnées actuel du pointeur
				curentX = event.getX();
				currentY = event.getY();
				
				// Recuperer le difference entre le pointeur initial et le pointeur actuel
				x = curentX - downX;
				y = currentY - downY;
				
				// Le pointeur actuel de vient le pointeur initial
				downX = curentX;
				downY = currentY;
				
				float move = (float) x-y;
		    	
		    	if (event.getPointerCount() > 1)	// si au moins 2 pointeurs sont detectés :
		    	{
		    		int currentZoom = getZoom();	// niveau de zoom actuel
		    		int maxZoom = getMaxZoom();		// niveau maximum de zoom supporté par la camera
			    	
		    		// niveau de zoom a affecter a la camera
		    		int zoom = (int) ((currentZoom + (move * sensitivity)));
			    	
			    	// Controler que le niveau de zoom ne depasse pas les limites de la camera
			    	if (zoom > maxZoom)
			    	{
			    		zoom = maxZoom;
			    	}
			    	else if (zoom < 0)
			    	{
			    		zoom = 0;
			    	}
			    	// Affecter le zoom a la camera
			    	setZoom(zoom);	
			    	//Log.v(TAG, "-> " + String.valueOf(zoom)); //Debug
		    	}
		    	break;
			default:
				break;
		}

    	return true;
    }
    
	@Override
	public void onSensorChanged(SensorEvent event) 
	{
		float[] rotMatrix = new float[9]; //matrisse de rotation
		float[] res = new float[3]; // orientation : azimute, pitch, roll
		
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			accelVector = event.values; // Mis a jour resultat de l'accelerometre
			
			float z = event.values[2]; // rotation autour de z. valeur de 9.81 a -9.81 (0 = telephone vertical)
			z = (z / SensorManager.GRAVITY_EARTH); // z est ammene autour de 1 a -1 (0 = telephone vertical)
			
			// z de 180 a 0 : 90 = telephone vertical, 0 horizontal(ecran vers le haut)
			// et 180 horizontal (ecran vers le bas)
			orientation = (z * -90) + 90; 	
			//Log.v(TAG, "Orientation : " + String.valueOf(orientation) + " �"); // DEBUG
			
			
			// Orientation ecran
			Orientation orientationMesured;
			float y = event.values[1];
			float x = event.values[0];
			y = (y / SensorManager.GRAVITY_EARTH);
			if(y < 0.5 && x != 0)
			{
    			if (x < 0)
    				orientationMesured = Orientation.PAYSAGE_180;
    			else
    				orientationMesured = Orientation.PAYSAGE_0;
			}
			else
				orientationMesured = Orientation.PORTRAIT;
			
			if (orientationMesured != orientationEcran)
			{
				orientationEcran = orientationMesured;
				rotateScreen(orientationMesured);// mettre a jour orientation camera et bouton
			}		
		}
		else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
		{
			magnVector = event.values; // Mis a jour du champ magnetique
		}
		
		// permet d'obtenir une matrisse de rotation
		SensorManager.getRotationMatrix(rotMatrix, null, accelVector, magnVector); 
		
		/*
		 *  Met l'orientation de l'appareil dans le tableau res.
		 *  L'utilisation de la matrisse de rotation permet de corriger les erreurs de mesure losque
		 *  le telephone n'est pas a plat.
		 */
		SensorManager.getOrientation(rotMatrix, res);
		
		azimute = (float) Math.toDegrees(res[0]); // res[0} = azimute, res[1} = tangage er res[2] = assiette
		//Log.v(TAG, "azimut : " + String.valueOf(azimute)); // DEBUG
	}

    /**
     * Creer un fichier vide pour la photo
     * @return un fichier vide destine a etre utilise pour enregistrer une photo
     * @throws IOException
     */
    private File creerFichierPhoto() throws IOException
    {
        // On creer le fichier
        String dateTime =  new SimpleDateFormat(getString(R.string.format_date), Locale.FRENCH).format(new Date());
        String nomPhoto = "Locapic_" + dateTime;

        // On recupere le repertoire photo du telephone
        File rep = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File photo = File.createTempFile(nomPhoto, ".jpg", rep); // creation du fichier vide

        // On met dans l'atribut cheminPhoto le chemin du fichier precedement cree
        cheminPhoto = photo.getAbsolutePath();
        return photo;
    }
    
	/**
	 * Convertir des coordonnees GPS du format degres au format degres, minute, seconde (DMS)
	 * @param loc Les coordonnees GPS a convertir
	 * @return Les coordonnees GPS convertis
	 */
	private String decimalDegreesToDMS(double loc) 
	{
		/*  Format des coordonn�es GPS dans les metadonn�es
		 * 	num1/denom1,num2/denom2,num3/denom3
		 * 	num1/denom1 = degres
		 *	num2/denom2 = minutes
		 *	num3/denom3 = secondes
		 */
		
		String str = Integer.toString((int) loc) + "/1,";   // 105/1,
		loc = (loc % 1) * 60;         						// .987654321 * 60 = 59.259258
		str = str + Integer.toString((int) loc) + "/1,";	// 105/1,59/1,
		loc = (loc % 1) * 60000;							// .259258 * 60000 = 15555
		str = str + Integer.toString((int) loc) + "/1000";	// 105/1,59/1,15555/1000
		
		return str;
	}
    
	/**
	 * Retourne les es coordonnees actuelle GPS au format DMS
	 * @return Les coordonnees actuelle GPS au format DMS. Le 1er element est la latitude. Le 2e la longitude
	 */
	private String[] getLastLoc()
	{
    	Location loc = locationManager.getLastKnownLocation("gps");
    	double[] locDouble = {0, 0};
    	String[] locStr = {null, null};
    	
    	if (loc != null) 
    	{
    		locDouble[0] = loc.getLatitude();
    		locDouble[1] = loc.getLongitude();
    	}
    	
    	// Convertir les coordonn�es au bon format (degrees minutes seconde)
    	locStr[0] = String.valueOf(decimalDegreesToDMS(locDouble[0]));
    	locStr[1] = String.valueOf(decimalDegreesToDMS(locDouble[1]));
    	
    	return locStr;
    }
    
	/**
	 * @return Le niveau de zoom maximum supporte par la camera. Retourne -1 si la camera ne supporte pas le zoom
	 */
    private int getMaxZoom() 
    {
    	if (isZoomSupported)
        {
    		return camera.getParameters().getMaxZoom();
        }
        else
        {
            Toast.makeText(this, R.string.zoomNotSupported, Toast.LENGTH_LONG).show();
            return -1;
        }
	}
    
	/**
	 * @return Le niveau de zoom actuel de la camera. Retourne -1 si la camera ne supporte pas le zoom
	 */
    private int getZoom()
    {
    	if (isZoomSupported)
        {
    		Camera.Parameters params = camera.getParameters();
    		return params.getZoom();
        }
        else
        {
            Toast.makeText(this, R.string.zoomNotSupported, Toast.LENGTH_LONG).show();
            return -1;
        }
    }

    /**
     * Prend une photo
     */
    private void prendrePhoto()
    {
    	try {
    		locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
	    	if (pref.getBoolean("USE_NETWORK_LOCATION_PROVIDER", false))
	    		locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
    	} catch (Exception e) {
    		Log.e(TAG, e.getLocalizedMessage());
    	}
        if(retardateur > 0)
        {
        	//Toast.makeText(this, "Retardateur : " + String.valueOf(retardateur * 1000) + " seconde", Toast.LENGTH_LONG).show();
            SystemClock.sleep(retardateur * 1000); // on marque une pause (= retardateur)
        }
        // On met la camera en mode auto focus (sinon camera.autoFocus() ne marche pas)
        Parameters params = camera.getParameters();
        params.setFocusMode(Parameters.FOCUS_MODE_AUTO);
        camera.setParameters(params);

        camera.autoFocus(new Camera.AutoFocusCallback()
        {
            @Override
            public void onAutoFocus(boolean success, Camera camera)
            {
                if (success)
                {
                    camera.takePicture(null, null, pictureCallback);    // On enregistre la photo
                    camera.autoFocus(null);                 // on arrete l'ecoute de l'auto focus
                }
            }
        });
    }

    //TODO BUG: apres 2 prises de photos,impossble d'en reprendre une nouvelle. Seulement sur galaxy S4.
    // Enregistrement de la photo. Appelle lors de takePicture(null, null, pictureCallback)
    private PictureCallback pictureCallback = new PictureCallback()
    {
        @Override
        public void onPictureTaken(byte[] data, Camera camera)
        {
            Log.v(TAG, "prise de photo ...");

            File photo = null;
            try
            {
                photo = creerFichierPhoto(); // Creation d'un fichier vide
            }
            catch (IOException e)
            {
                Log.e(TAG, "ERREUR creerFichierPhoto : " + e.toString());
            }
            // si le fichier vide existe
            if (photo != null)
            {
                // on enregistre la photo
                try
                {
                    // On ouvre le fichier de la photo
                    FileOutputStream fos = new FileOutputStream(photo);
                    fos.write(data); 	// On ecrit la photo dans le fichier
                    fos.close(); 		// On ferme le fichier
                    writeMetadata(); 	// on ecrit les metadonnees
                    Toast.makeText(getApplicationContext(), "Photo enregistre sous " + cheminPhoto , Toast.LENGTH_LONG).show();
                }
                catch (IOException e)
                {
                    Log.e(TAG, "Erreur enregistrement photo : " + e.getMessage());
                }
            }
            else
            {
                Log.v(TAG, "Erreur lors de la creation du fichier");
            }

            /* On redemarre le preview apres un courte pause
             * pour permetre de voir la photo prise*/
            SystemClock.sleep(pauseEntrePhoto); // on marque une pause
            resetCamera(camera);    // On redemarre le preview
        }
    };
    
    /**
     * Redemarre la camera en reinitialisant l'auto focus
     * @param camera la camera a redemarrer
     */
    private void resetCamera(Camera camera)
    {
        Log.v(TAG, "ResetCamera ...");

        Parameters params = camera.getParameters();
        params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(params);
        camera.startPreview(); // On redemarre le preview
    }
    
    /**
     * Tourne les bouton de l'ecran
     */
    private void rotateScreen(Orientation orientation)
    {
        //Log.v(TAG, "rotateScreen -> " + orientation);
        
        //int rotation = orientation.getRotation();
        //prendrePhotoButton.setRotation(rotation);
        //fastSettingsButton.setRotation(rotation);
        //retourButton.setRotation(rotation);
    }
    
    /**
     * @param zoom le niveau de zoom a affecte a la camera
     */
    private void setZoom(int zoom)
    {
    	// Si le zoom est supporte par le telephone :
    	if (isZoomSupported)
        {
    		// Empecher le niveau de zoom de depasser le maximum et le minimum
    		if (zoom > maxZoom)
    		{
    			zoom = maxZoom;
    		}
    		else if (zoom < 0)
    		{
    			zoom = 0;
    		}

    		// Affectation du nouveau niveau de zoom a la camera
			Camera.Parameters params = camera.getParameters();
    		params.setZoom(zoom);
    		camera.setParameters(params);
        }
        else
        {
            Toast.makeText(this, R.string.zoomNotSupported, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Affiche la boite de dialogue permettent de modifier les parametres de base de la camera (flash, retardateur, ...)
     */
    private void showFastSettingsDialog()
    {
        Intent fastSettingsIntent = new Intent(this, FastSettingsActivity.class);
        
        // On passe la valeur actuel des parametres en extra
        // Flash
        fastSettingsIntent.putExtra("flashMode", flashMode);
        // Retardateur
        fastSettingsIntent.putExtra("retardateur", retardateur);
		
        // Resolution suporte par la camera
		int[] sizes = new int[supportedPictureSizes.size()*2];
		for (int i = 0; i < supportedPictureSizes.size(); i+=2) 
		{
			sizes[i] = supportedPictureSizes.get(i).width;
			sizes[i+1] = supportedPictureSizes.get(i).height;
		}
		fastSettingsIntent.putExtra("supportedPictureSizes", sizes);
		
		// Index de la resolution actiellement selectionne
		fastSettingsIntent.putExtra("indexCameraSizeSelected", indexCameraSizeSelected);
		// Orientation actuel du telephone
		fastSettingsIntent.putExtra("orientation", orientationEcran);
		
		// Demarrer le popup
        startActivityForResult(fastSettingsIntent, REQUEST_CODE_POPUP_FAST_SETTINGS);
    }

    /**
     * Ecrit les metadonnees dans la derniere photo prise
     * @throws IOException
     */
    private void writeMetadata() throws IOException
    {
        Log.v(TAG, "Creation des metadonnees ...");

        File f = new File(cheminPhoto); // On recupere le fichier de la photo

        if (f.exists())
        {
            // on recupere les metaDonnees exif
            ExifInterface ei = new ExifInterface(cheminPhoto);
            
            /// GPS ///
            String[] loc = getLastLoc();
            String latitude = loc[0];
            String longitude = loc[1];
            
            ei.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latitude);
            ei.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, longitude);
            
            if (Integer.valueOf(latitude.substring(0, latitude.indexOf("/"))) > 0)
            {
            	// Si la latitude est superieur a 0,  nous sommes dans l'emisphere nord
            	ei.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
            }
            else
            {
            	// sinon mettre le referentiel de la latitude a "sud"
            	ei.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
            }
            
            if (Integer.valueOf(longitude.substring(0, longitude.indexOf("/"))) > 0)
            {
            	// Si la longitude est superieur a 0, nous sommes a l'est du meridien de greenwich
            	ei.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
            }
            else
            {
            	// sinon nous sommes a l'ouest du meridien de greenwich
            	ei.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
            }

            /// ORIENTATION et AZIMUT ///
            // valeur du tag exif a ecrire
            String tagPerso = "";
            tagPerso += "Orientation:" + String.valueOf(orientation) + "\n";
            tagPerso += "Azimut:" + String.valueOf(azimute);

            // On affecte le tag userComment
            ei.setAttribute(TAG_USER_COMMENT, tagPerso);

            if (orientationEcran == Orientation.PORTRAIT)
                ei.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(ExifInterface.ORIENTATION_ROTATE_90));
            else if (orientationEcran == Orientation.PAYSAGE_0)
                ei.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(ExifInterface.ORIENTATION_NORMAL));
            else if (orientationEcran == Orientation.PAYSAGE_180)
            	ei.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(ExifInterface.ORIENTATION_ROTATE_180));

            ei.saveAttributes();	// Enregistrer les metadonnees
            readMetadata(); 		// Afficher les metadonnees precedement creer pour le debugage
        }
        else
        {
            Log.w(TAG, "Le fichier " + cheminPhoto + " n'existe pas. Impossible de creer les metadonnees.");
        }
    }

    /**
     * Lit les metadonnees de la photo qui viens d'etre cree (pour debugage)
     */
    private void readMetadata()
    {
        Log.v(TAG_METADONNEES, "Lecture des metadonnees ...");

        File f = new File(cheminPhoto);

        if (f.exists())
        {
            // on recupere les metaDonnees exif
            ExifInterface ei;
            try
            {
                ei = new ExifInterface(cheminPhoto);
                Log.v(TAG_METADONNEES, TAG_USER_COMMENT + " -> " + ei.getAttribute(TAG_USER_COMMENT));

                Log.v(TAG_METADONNEES, ExifInterface.TAG_GPS_LATITUDE + " -> " + ei.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                Log.v(TAG_METADONNEES, ExifInterface.TAG_GPS_LONGITUDE + " -> " + ei.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
            }
            catch (IOException e)
            {
                Log.e(TAG_METADONNEES, "Erreur lecture metadonnees : " + e.getMessage());
                e.printStackTrace();
            }
        }
        else
        {
            Log.w(TAG, "Le fichier " + cheminPhoto + " n'existe pas. Impossible de creer les metadonnees.");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

	@Override
	public void onLocationChanged(Location loc) {
		// TODO Message pour dire que la loc est a jour (si y'a le temps)
	}

	@Override
	public void onProviderDisabled(String arg0) {}

	@Override
	public void onProviderEnabled(String arg0) {}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
}
