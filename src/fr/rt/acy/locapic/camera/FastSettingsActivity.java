package fr.rt.acy.locapic.camera;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import fr.rt.acy.locapic.R;
import fr.rt.acy.locapic.camera.CameraActivity.Orientation;

/**
 * Popup avec parametre rapide (flash ...)
 */
public class FastSettingsActivity extends Activity
{
	// taille du popup
	private final int WIDTH = 400;
	private final int HEIGHT = 300;
	
	// Mode de flash : ON, OFF ou AUTO
	private Flash flashMode = Flash.AUTO;
	// Retardateur en seconde
	private int retardateur = 0;
	// resolution de camera supporté par la camera
	private ArrayList<CameraSize> supportedPictureSizes = new ArrayList<>();
	private int indexCameraSizeSelected = 0;
	
	// Indique le reglage du retardateur
	private TextView retardateurTexte;
	// Bar selection du retardateur
	private SeekBar seekBar;

	private Spinner spinnerFlash;
	private Spinner spinnerResolution;

	
	// Intent retourne a l'activite principal
	private Intent returnIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fast_settings);
		
		// initialiser flash, retardateur et resolution
		flashMode = (Flash) getIntent().getExtras().get("flashMode");
		retardateur = getIntent().getExtras().getInt("retardateur");
		indexCameraSizeSelected = getIntent().getIntExtra("indexCameraSizeSelected", 0);
		LinearLayout fastSettingsLayout = (LinearLayout) findViewById(R.id.fastSettingsAll);
		
		// orientation du telephone
		Orientation orientation = Orientation.PORTRAIT;
		orientation = (Orientation) getIntent().getSerializableExtra("orientation"); // une enumeration est serialisable
		
		int[] tmpSizes = getIntent().getIntArrayExtra("supportedPictureSizes");
		
		for (int i = 0; i < tmpSizes.length; i+=2) 
		{
			if (tmpSizes[i]!= 0 && tmpSizes[i+1] != 0)
				supportedPictureSizes.add(new CameraSize(tmpSizes[i], tmpSizes[i+1]));
		}
		
		// on initialise la valeur par defaut du texte du seekbar
		retardateurTexte = (TextView) findViewById(R.id.retardateurText);
		retardateurTexte.setText(String.valueOf(retardateur) + " sec");
		
		spinnerFlash = (Spinner) findViewById(R.id.spinnerFlash);
		spinnerResolution = (Spinner) findViewById(R.id.spinnerResolution);
		
		ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(this, R.array.flashMode, android.R.layout.simple_spinner_item);
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerFlash.setAdapter(arrayAdapter);
		spinnerFlash.setSelection(flashMode.getIndex());
		spinnerFlash.setOnItemSelectedListener(new OnItemSelectedListener() 
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) 
			{
				if(position == Flash.AUTO.getIndex())
					flashMode = Flash.AUTO;
				else if(position == Flash.ON.getIndex())
					flashMode = Flash.ON;
				else if(position == Flash.OFF.getIndex())
					flashMode = Flash.OFF;
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		ArrayAdapter<CameraSize> arrayAdapter2 = new ArrayAdapter<>(this,
								android.R.layout.simple_spinner_item, supportedPictureSizes);
		spinnerResolution.setAdapter(arrayAdapter2);
		spinnerResolution.setSelection(indexCameraSizeSelected);
		spinnerResolution.setOnItemSelectedListener(new OnItemSelectedListener() 
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) 
			{
				indexCameraSizeSelected = position;
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		
		seekBar = (SeekBar) findViewById(R.id.retardateur);
		// listener du seekBar
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() 
		{
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
			{
				retardateurTexte.setText(String.valueOf(progress) + " sec");
				retardateur = progress;
			}
		});
		
		// on initialise la valeur par defaut du seekbar
		seekBar.setProgress(retardateur);
		
		returnIntent = new Intent();
		setReturnData();
		
		if (orientation == Orientation.PORTRAIT)
		{
			fastSettingsLayout.getLayoutParams().width = WIDTH;
			fastSettingsLayout.getLayoutParams().height = HEIGHT;
		}
		else
		{
			fastSettingsLayout.getLayoutParams().width = WIDTH;
			fastSettingsLayout.getLayoutParams().height = WIDTH;
			fastSettingsLayout.setRotation(orientation.getRotation());
		}
		fastSettingsLayout.requestLayout();
	}
	
	/**
	 * Met a jour les valeur retourner par l'activite.
	 */
	private void setReturnData() 
	{
		// On met en extra les differents parametres
		returnIntent.putExtra("flashMode", flashMode);
		returnIntent.putExtra("retardateur", retardateur);
		returnIntent.putExtra("indexCameraSizeSelected", indexCameraSizeSelected);
		setResult(Activity.RESULT_OK, returnIntent);
	}
	
	@Override
	public void onUserInteraction() 
	{
		//On met a jour les valeur retourner par l'activite a chaque interaction de l'utilisateur
		setReturnData();
		super.onUserInteraction();
	}
}
