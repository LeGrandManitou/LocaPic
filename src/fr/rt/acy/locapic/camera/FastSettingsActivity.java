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
package fr.rt.acy.locapic.camera;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import fr.rt.acy.locapic.R;

/**
 * Popup avec parametre (flash, retardateur, resolution, ...)
 */
public class FastSettingsActivity extends Activity
{
	// taille du popup en dp
	private final int WIDTH = 300;
	private final int HEIGHT = 200;
	
	// Mode de flash : ON, OFF ou AUTO
	private Flash flashMode = Flash.AUTO;
	// Retardateur en seconde
	private int retardateur = 0;
	// resolution de camera supporte par la camera
	private ArrayList<CameraSize> supportedPictureSizes = new ArrayList<>();
	// index de la resolution actuelle de la photo dans le tableau supportedPictureSizes
	private int indexCameraSizeSelected = 0;
	
	// Indique le reglage du retardateur
	private TextView retardateurTexte;
	// Bar selection du retardateur
	private SeekBar seekBar;
	// Menu deroulant selection du flash
	private Spinner spinnerFlash;
	// Menu deroulant selection de la resolution de la photo
	private Spinner spinnerResolution;

	// Intent pour retourne des variable a l'activite camera
	private Intent returnIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fast_settings);
		
		/// Recuperation valeur passe en parametre a l'activite ///
		// initialiser flash, retardateur et resolution
		flashMode = (Flash) getIntent().getExtras().get("flashMode");
		retardateur = getIntent().getExtras().getInt("retardateur");
		indexCameraSizeSelected = getIntent().getIntExtra("indexCameraSizeSelected", 0);
		LinearLayout fastSettingsLayout = (LinearLayout) findViewById(R.id.fastSettingsAll);
		
		// orientation du telephone
		Orientation orientation = Orientation.PORTRAIT;
		orientation = (Orientation) getIntent().getSerializableExtra("orientation"); // une enumeration est serialisable
		
		// Recuperation des resolution suporte par la camera
		int[] tmpSizes = getIntent().getIntArrayExtra("supportedPictureSizes");
		
		for (int i = 0; i < tmpSizes.length; i+=2) 
		{
			if (tmpSizes[i]!= 0 && tmpSizes[i+1] != 0)
				supportedPictureSizes.add(new CameraSize(tmpSizes[i], tmpSizes[i+1]));
		}
		
		// on initialise la valeur par defaut du texte du seekbar
		retardateurTexte = (TextView) findViewById(R.id.retardateurText);
		retardateurTexte.setText(String.valueOf(retardateur) + " sec");
		
		/// Creation des listes deroulante ///
		spinnerFlash = (Spinner) findViewById(R.id.spinnerFlash);
		spinnerResolution = (Spinner) findViewById(R.id.spinnerResolution);
		
		// Spinner flash
		ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(this, R.array.flashMode, android.R.layout.simple_spinner_item);
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerFlash.setAdapter(arrayAdapter);
		spinnerFlash.setSelection(flashMode.getIndex()); // Element de la liste selectionne
		spinnerFlash.setOnItemSelectedListener(new OnItemSelectedListener() 
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) 
			{
				// Lorsque l'on selectionne un mode de flash, enregistrer le mode de flash
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
		
		// Spinner resolution
		ArrayAdapter<CameraSize> arrayAdapter2 = new ArrayAdapter<>(this,
								android.R.layout.simple_spinner_item, supportedPictureSizes);
		spinnerResolution.setAdapter(arrayAdapter2);
		spinnerResolution.setSelection(indexCameraSizeSelected); // Element de la liste selectionne
		spinnerResolution.setOnItemSelectedListener(new OnItemSelectedListener() 
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) 
			{
				// Lorsque l'on selectionne un element, enregistrer son indice dans une variable
				indexCameraSizeSelected = position;
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		// seekBar (utiliser pour la selection du retardateur)
		seekBar = (SeekBar) findViewById(R.id.retardateur);
		// Sistener du seekBar
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() 
		{
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
			{
				retardateurTexte.setText("+" + String.valueOf(progress) + " sec");
				retardateur = progress;
			}
		});
		
		// on initialise la valeur par defaut du seekbar
		seekBar.setProgress(retardateur);
		
		/// Valeur a retourner a l'activite camera
		returnIntent = new Intent();
		setReturnData();
		
		int widthDp = dpsToPx(WIDTH);
		int heightDp = dpsToPx(HEIGHT);

		// Recuperation de l'orientation du telephone qui a ete passe en parametre
		if (orientation == Orientation.PORTRAIT)
		{
			// affectation au layout de la taille desire
			fastSettingsLayout.getLayoutParams().width = widthDp;
			fastSettingsLayout.getLayoutParams().height = heightDp;
		}
		else
		{
			// affectation au layout de la taille desire
			fastSettingsLayout.getLayoutParams().width = widthDp;
			fastSettingsLayout.getLayoutParams().height = widthDp;
			
			// Retouner le layout
			fastSettingsLayout.setRotation(orientation.getRotation());
		}
		fastSettingsLayout.requestLayout(); // TODO verifier utilite
	}
	
	/**
	 * Convertir des dps en pixel
	 * @param dps les dps a convertir
	 * @return les dps en pixel
	 */
	private int dpsToPx(int dps)
	{
		final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
		return (int) (dps * scale + 0.5f);
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
