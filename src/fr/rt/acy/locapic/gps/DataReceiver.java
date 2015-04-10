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
