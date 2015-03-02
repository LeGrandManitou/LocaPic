
package fr.rt.acy.locapic.camera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fr.rt.acy.locapic.R;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ZoomControls;

//on supprime les warnings a cause de la classe Camera depreciee a partir de l'api 21
@SuppressWarnings("deprecation")
public class CameraActivity extends Activity implements SensorEventListener
{
	// tag pour debugage
	private final static String TAG = CameraActivity.class.getName(); 
	// tag pour metadonnees exif. L'azimute et l'orientation y seront enregistres
	public final static String TAG_USER_COMMENT = "UserComment";
	// tag pour startActivityForResult
	private final static int REQUEST_CODE_POPUP_FAST_SETTINGS = 100;
	// pause durant laquelle le preview se fige entre chaque photo (en ms)
	public final static int pauseEntrePhoto = 1000 ;

	private String cheminPhoto = null;      // Chemin de la dernier photo enregistree
	private Camera camera;                  // Camera utillisee
	private PreviewCamera previewCamera;    // preview de la camera
	private ImageButton fastSettings;       // bouton fastSettings (activation du flash, retardateur, ...)
    private ZoomControls zoomControls;		// bouton de control du zoom
    private int currentZoom = 0;			// niveau de zoom actuel
    private int maxZoom = 0;				// niveau de zoom maximum. Affecte dans le onCreate
    private boolean isZoomSupported = false;
    
	// Capteurs
	private SensorManager sensorManager;    // gere les capteurs du telephone
	private Sensor accelSensor;             // accelerometre
	private Sensor magnSensor;              // capteur de champ magnetique(boussole)
	
	// Vecteur utillise par getRotationMatrix dans onSensorChanged pour obtenir une matrisse de rotation.
	private float[] accelVector = new float[3]; // vecteur gravite
	private float[] magnVector = new float[3]; 	// vecteur champ magnetique
	
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
	
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Mettre l'activite en plein ecran
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

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
        isZoomSupported = params.isZoomSupported();
        supportedPictureSizes = params.getSupportedPictureSizes();		// Recuperer les taille de camera supporte
        camera.setParameters(params);
        
        previewCamera = new PreviewCamera(this, camera);
        previewCamera.setKeepScreenOn(true);                // Garder l'ecran allume
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(previewCamera);

        fastSettings = (ImageButton) findViewById(R.id.fastSettings);

        // Zoom controls
        zoomControls = (ZoomControls) findViewById(R.id.zoomControl);
        if (isZoomSupported)
        {
            zoomControls.setIsZoomInEnabled(true);
            zoomControls.setIsZoomOutEnabled(false);
            zoomControls.setOnZoomInClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    zoomControls.setIsZoomOutEnabled(true);
                    if (currentZoom < maxZoom)
                        currentZoom ++;

                    if (currentZoom >= maxZoom)
                    {
                        zoomControls.setIsZoomInEnabled(false);
                        currentZoom = maxZoom;
                    }

                    Camera.Parameters params = camera.getParameters();
                    params.setZoom(currentZoom);
                    camera.setParameters(params);
                }
            });
            zoomControls.setOnZoomOutClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    zoomControls.setIsZoomInEnabled(true);
                    if (currentZoom > 0)
                        currentZoom --;

                    if (currentZoom <= 0)
                    {
                        zoomControls.setIsZoomOutEnabled(false);
                        currentZoom = 0;
                    }
                    
                    // Affecter le niveau de zoom
                    Camera.Parameters params = camera.getParameters();
                    params.setZoom(currentZoom);
                    camera.setParameters(params);
                }
            });
        }
        else
        {
        	// Si le zoom n'est pas supporté, faire disparaitre les boutons du zoom
            zoomControls.setVisibility(View.GONE);
            Toast.makeText(this, R.string.zoomNotSupported, Toast.LENGTH_LONG).show();
        }
        
        // Initialiser les variables des capteurs
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        
        // Commencer l'ecoute des capteurs
    	sensorManager.registerListener( this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
    	sensorManager.registerListener( this, magnSensor, SensorManager.SENSOR_DELAY_NORMAL);
    	
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
    			camera.release(); // On libere la camera
                camera = null;
			} 
    		catch (Exception e)
			{
				Log.e(TAG, "onPause impossible de liberer la camera : " + e.getMessage());
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
    		
    		camera.setParameters(param);
    	}
    	super.onActivityResult(requestCode, resultCode, data);
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
			z = (z / SensorManager.GRAVITY_EARTH); // z de 1 a -1 (0 = telephone vertical)
			
			// z de 180 a 0 : 90 = telephone vertical, 0 horizontal(ecran vers le haut)
			// et 180 horizontal (ecran vers le bas)
			orientation = (z * -90) + 90; 	
			//Log.v(TAG, "Orientation : " + String.valueOf(orientation) + "°"); // DEBUG
			//sensorManager.unregisterListener(this); // on arrete l'ecoute des capteurs apres avoir obtenu une valeur
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
        String nomPhoto = "IMG_" + dateTime;

        // On recupere le repertoire photo du telephone
        File rep = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File photo = File.createTempFile(nomPhoto, ".jpg", rep); // creation du fichier vide

        // On met dans l'atribut cheminPhoto le chemin du fichier precedement cree
        cheminPhoto = photo.getAbsolutePath();
        return photo;
    }

    /**
     * Prend une photo
     */
    public void prendrePhoto(View view)
    {
        if(retardateur > 0)
        {
            SystemClock.sleep(retardateur * 1000); // on marque une pause (= retardateur) TODO utiliser AsyncTask
			/*for(int i = retardateur; i > 0; i--)
			{
				// TODO BUG: ne s'affiche pas avant la fin de la boucle
				Toast.makeText(getApplicationContext(), String.valueOf(i) + " !!!", Toast.LENGTH_SHORT).show();
				SystemClock.sleep(1000); // on marque une pause de 1 seconde
			}*/
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

    //TODO BUG: mode focus non reinitialise apres prise de photo (lorsque zoom est utilise ? )
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
                    fos.write(data); // On ecrit la photo dans le fichier
                    fos.close(); // On ferme le fichier
                    writeMetadata(); // on ecrit les metadonnees
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

            // On redemarre le preview apres un courte pause
            //pour permetre de voir la photo prise
            SystemClock.sleep(pauseEntrePhoto); // on marque une pause
            //camera.startPreview(); // On redemarre le preview
            resetCamera(camera);    // On redemarre le preview
        }
    };
    
    private void setZoom(int zoom)
    {
    	if (isZoomSupported)
        {
    		if (zoom >= maxZoom)
    		{
    			zoom = maxZoom;
    		}

			currentZoom = zoom;
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
     * Fonction permetant d'appele showFastSettingsDialog() depuis xml (onClick)
     */
    public void showFastSettingsDialog(View view)
    {
        showFastSettingsDialog();
    }

    /**
     * Affiche la boite de dialogue permetent de modifier les parametres de base
     */
    private void showFastSettingsDialog() //TODO selection de la scene
    {
        Intent fastSettingsIntent = new Intent(this, FastSettingsActivity.class);
        // On passe la valeur actuel des parametres en extra
        fastSettingsIntent.putExtra("flashMode", flashMode);
        fastSettingsIntent.putExtra("retardateur", retardateur);
        //fastSettingsIntent.putExtra("supportedPictureSizes", supportedPictureSizes.toArray());

        startActivityForResult(fastSettingsIntent, REQUEST_CODE_POPUP_FAST_SETTINGS);
    }

    public void showZoomPopup(View view)
    {
        showZoomPopup();
    }

    private void showZoomPopup()
    {
        if (zoomControls.getVisibility() == View.INVISIBLE)
        {
            zoomControls.setVisibility(View.VISIBLE);
        }
        else if (zoomControls.getVisibility() == View.VISIBLE)
        {
            zoomControls.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * ecrit les metadonnees dans la derniere photo prise
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

            // valeur du tag exif a ecrire
            String tagPerso = "";
            tagPerso += "Orientation:" + String.valueOf(orientation) + "\n";
            tagPerso += "Azimut:" + String.valueOf(azimute);

            // On affecte le tag userComment
            ei.setAttribute(TAG_USER_COMMENT, tagPerso);

            // fixe l'orientation de la photo  TODO a amelierer : lorsque le telephone est completement retourne
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                ei.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(ExifInterface.ORIENTATION_ROTATE_90));
            else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                ei.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(ExifInterface.ORIENTATION_NORMAL));
            else
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
        Log.v(TAG, "Lecture des metadonnees ...");

        File f = new File(cheminPhoto);

        if (f.exists())
        {
            // on recupere les metaDonnees exif
            ExifInterface ei;
            try
            {
                ei = new ExifInterface(cheminPhoto);

                Log.v(TAG, TAG_USER_COMMENT + " -> " + ei.getAttribute(TAG_USER_COMMENT));
            }
            catch (IOException e)
            {
                Log.e(TAG, "Erreur lecture metadonnees : " + e.getMessage());
                e.printStackTrace();
            }
        }
        else
        {
            Log.w(TAG, "Le fichier " + cheminPhoto + " n'existe pas. Impossible de creer les metadonnees.");
        }
    }

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

    // TODO Recuperation flash et retardateur
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
    	//outState.putString("cheminPhoto", cheminPhoto);

    	super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);

    	//cheminPhoto = savedInstanceState.getString("cheminPhoto");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }
}
