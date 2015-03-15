package fr.rt.acy.locapic.camera;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

// on supprime les warnings a cause de classe depreciee (nottament Camera)
@SuppressWarnings("deprecation")
public class PreviewCamera extends SurfaceView implements SurfaceHolder.Callback
{
	//tag pour debugage
	private final static String TAG = PreviewCamera.class.getName(); 
	
	private Camera camera;
	private SurfaceHolder holder;
		
	public PreviewCamera(Context context, Camera camera) 
	{
		super(context);
		
		this.camera = camera;
		holder = getHolder();
		holder.addCallback(this);
		
		// pour version android inferieur a 3.0 
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) 
	{
		try 
		{
			// on lance le preview
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            camera.setDisplayOrientation(90);
        }
		catch (IOException e) 
		{
            Log.e(TAG, "Erreur surfaceCreated : " + e.getMessage());
        }
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) 
	{
		Log.v(TAG, "mise a jour surface");
		
		if (holder.getSurface() == null)
		{
			Log.e(TAG, "surfaceChanged : holder n'existe pas");
		}
		else
		{
			try 
			{
				camera.stopPreview();
			} 
			catch (Exception e){ 
				/* le preview est deja arrete */ }
            Camera.Parameters params = camera.getParameters();
            List<Camera.Size> sizes = params.getSupportedPreviewSizes();

            Camera.Size optimalPreviewSize = getOpitmalPreviewSize(sizes, width, height);
            params.setPreviewSize(optimalPreviewSize.width, optimalPreviewSize.height);
            camera.setParameters(params);

			try 
			{
				camera.setPreviewDisplay(holder);
				camera.startPreview();
			} 
			catch (IOException e) 
			{
				Log.e(TAG, "Impossible de redemarer la camera : " + e.getMessage());
			}
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) 
	{
		/*if(camera != null)
		{
			camera.stopPreview();
			camera.release();
			camera = null;
		}*/
	}

    /**
     * Retourne la taille la mieux adapte au preview parmi une liste de taille
     * @param sizes liste des tailles supporte par l'appareil
     * @param targetWidth largeur de la cible
     * @param targetHeight longueur de la cible
     * @return the optimal preview size
     */
    private Camera.Size getOpitmalPreviewSize(List<Camera.Size> sizes, int targetWidth, int targetHeight)
    {
        final double ASPECT_TOLERANCE = 0.1;                        // Tolerence
        Camera.Size optimalPreviewSize = null;                      // Taille optimale
        double minDiff = Double.MAX_VALUE;                          // Difference minimal trouve avec la taille du preview
        double targetRatio = (double) targetHeight / targetWidth;   // Ratio recherche

        if (sizes != null)
        {
            for (Camera.Size size : sizes)
            {
                double ratio = (double) size.width / size.height;
                if (!(Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)) // si le ratio est tolerable, continuer
                {
                    if (Math.abs(size.height - targetHeight) < minDiff)
                    {
                        optimalPreviewSize = size;
                        minDiff = Math.abs(size.height - targetHeight);
                    }
                }
            }

            // Si aucune taille optimal n'a ete trouve, on tolere les ratios differents
            if (optimalPreviewSize == null)
            {
                minDiff = Double.MAX_VALUE;
                for (Camera.Size size : sizes)
                {
                    if (Math.abs(size.height - targetHeight) < minDiff)
                    {
                        optimalPreviewSize = size;
                        minDiff = Math.abs(size.height - targetHeight);
                    }
                }
            }
        }
        return  optimalPreviewSize;
    }

}
