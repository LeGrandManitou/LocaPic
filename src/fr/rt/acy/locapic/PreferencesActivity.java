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
package fr.rt.acy.locapic;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.Menu;

/**
 * Activite de parametrage des preferences utilisateurs
 */
public class PreferencesActivity extends Activity {
	
	/**
	 * Creation d'un fragment perso a partir de PreferenceFragment et qui utilise notre layout
	 */
	public static class SettingsFragment extends PreferenceFragment {
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        // Charge les preferences depuis le layout preference_fragment
	        // Ce dernier contient pour chaque champ, sa clé (chaine de caracteres) pour les preferences
	        addPreferencesFromResource(R.layout.preference_fragment);
	    }
	}
	
	/**
	 * onCreate - activity lifecycle
	 * On remplace le contenu de l'activite par un fragment settingsFragment
	 */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Change le contenu (designe par l'id generique android.R.id.content)
        // par notre fragment
        getFragmentManager().beginTransaction()
        	.replace(android.R.id.content, new SettingsFragment())
            .commit();
    }
    
    /**
     * onCreateOptionsMenu pour la gestion de la creation du menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	// Ici on active juste le logo (celui de l'application dans l'action bar)
    	// comme bouton pour un retour arriere (Activite parent defini dans le Manifest) 
    	getActionBar().setDisplayHomeAsUpEnabled(true);
    	return super.onCreateOptionsMenu(menu);
    }
    
    /**
     * Quand le boutton retour arriere est presse
     * Pour plus tard : verificiations des entrees utilisateurs ?
     */
    @Override
    public void onBackPressed() {
    	super.onBackPressed();
    }
}
