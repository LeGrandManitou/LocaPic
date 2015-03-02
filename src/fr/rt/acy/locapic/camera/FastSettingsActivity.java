package fr.rt.acy.locapic.camera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import fr.rt.acy.locapic.R;

/**
 * Popup avec parametre rapide (flash ...)
 */
public class FastSettingsActivity extends Activity
{
	// Mode de flash : ON, OFF ou AUTO
	private Flash flashMode = Flash.AUTO;
	// Retardateur en seconde
	private int retardateur = 0;
	
	// Indique le reglage du retardateur
	private TextView retardateurTexte;
	// Bar selection du retardateur
	private SeekBar seekBar;

	private Spinner spinnerFlash;
	
	// Intent retourne a l'activite principal
	private Intent returnIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fast_settings);
		
		// initialiser flash et retardateur
		flashMode = (Flash) getIntent().getExtras().get("flashMode");
		retardateur = getIntent().getExtras().getInt("retardateur");
		//Size [] supportedPictureSizes = (Size[]) getIntent().getExtras().get("supportedPictureSizes");
		
		// on initialise la valeur par defaut du texte du seekbar
		retardateurTexte = (TextView) findViewById(R.id.retardateurText);
		retardateurTexte.setText(String.valueOf(retardateur) + " sec");
		
		spinnerFlash = (Spinner) findViewById(R.id.spinnerFlash);
		
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
	}
	
	/**
	 * Met a jour les valeur retourner par l'activite.
	 */
	private void setReturnData() 
	{
		// On met en extra les differents parametres
		returnIntent.putExtra("flashMode", flashMode);
		returnIntent.putExtra("retardateur", retardateur);
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
