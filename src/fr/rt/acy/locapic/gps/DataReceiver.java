package fr.rt.acy.locapic.gps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class DataReceiver extends BroadcastReceiver
{
	public static final String TAG = "DataReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		uiUpdateCallback(intent);
	}

	public abstract void uiUpdateCallback(Intent intent);
}
