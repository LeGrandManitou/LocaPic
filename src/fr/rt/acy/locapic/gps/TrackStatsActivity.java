package fr.rt.acy.locapic.gps;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;
import fr.rt.acy.locapic.*;

/**
 * Deuxieme onglet de la page principale
 * Servira a afficher des stats sur l'itineraire en cours
 */
public class TrackStatsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Creation d'un textView "Work in progress..."
		TextView  tv=new TextView(this);
        tv.setTextSize(25);
        tv.setGravity(Gravity.CENTER);
        tv.setText("Work in progress...");
        // Affichage du texte
        setContentView(tv);
        //setContentView(R.layout.activity_trackstats);
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
	}

	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
