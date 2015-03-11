package fr.rt.acy.locapic;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class PreferencesActivity extends Activity {
	
	public static class SettingsFragment extends PreferenceFragment {
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);

	        // Load the preferences from an XML resource
	        addPreferencesFromResource(R.layout.preference_fragment);
	    }
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
    
    /**
     * Quand le boutton retour arriere est presse
     * Verification des valeurs entrees pour LOCATION_DISTANCE et LOCATION_INTERVAL
     * Si non convenable(s) : boite de dialogue qui indique le probleme
     * 		Un bouton pour quitter quand meme (et conserver les anciennes valeurs)
     * 		Un bouton pour rester
     */
    @Override
    public void onBackPressed() {
    	// TODO Check des valeurs LOCATION_DISTANCE et LOCATION_INTERVAL
    	super.onBackPressed();
    }
}