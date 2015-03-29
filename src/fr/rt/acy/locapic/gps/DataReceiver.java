package fr.rt.acy.locapic.gps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class DataReceiver extends BroadcastReceiver// implements UiUpdater
{
	public static final String TAG = "DataReceiver";
	private UiUpdater callback;
	
	public interface UiUpdater {
	    public void uiUpdateCallback(Intent intent);
	}

	public DataReceiver(UiUpdater callback) {
		super();
		this.callback = callback;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		callback.uiUpdateCallback(intent);
	}
}
