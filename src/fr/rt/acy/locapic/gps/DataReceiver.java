package fr.rt.acy.locapic.gps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @author Samuel
 * BroadcastReceiver utilise pour recevoir des infos a afficher sur la page principal
 * infos recus de la part du service (stats sur position et itineraire)
 */
public abstract class DataReceiver extends BroadcastReceiver
{
	// Tag pour les potentiels logs
	public static final String TAG = "DataReceiver";
	
	// A la reception de l'intent, on appelle la methode abstraite uiUpdateCallback()
	// Cette derniere est definie dans l'activite qui utilise ce receiver
	// Elle permet de faire des MaJ d'interface graphique (affichage des stats)
	// On passe l'intent pour recuperer les extras
	@Override
	public void onReceive(Context context, Intent intent) {
		uiUpdateCallback(intent);
	}

	public abstract void uiUpdateCallback(Intent intent);
}
