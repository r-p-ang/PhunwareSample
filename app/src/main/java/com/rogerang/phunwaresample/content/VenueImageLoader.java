package com.rogerang.phunwaresample.content;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Loader to handle getting Venue images
 *
 */
public class VenueImageLoader extends AsyncTaskLoader<Bitmap> {
	private Context mContext;
	private String urlStr;
	private int reqWidth;
	private int reqHeight;
	
	private static final String FILENAME = "dl_bitmap"; // temp. download file name
	
	/**
	 * New venue image loader.
	 * @param context
	 * @param URL URL to download image from
	 * @param width requested width of final bitmap
	 * @param height requested width of final bitmap
	 */
	public VenueImageLoader(Context context, String URL, int width, int height) {
		super(context);
		mContext = context;
		this.urlStr = URL;
		this.reqHeight = height;
		this.reqWidth = width;
	}

	@Override
	public Bitmap loadInBackground() {	
		HttpURLConnection urlConnection = null;
		InputStream inputStream = null;
		FileOutputStream fos = null;

		try {
			URL mURL = new URL(urlStr);
			
			// download to local file 
			urlConnection= (HttpURLConnection) mURL.openConnection();
            inputStream = urlConnection.getInputStream();
            fos = mContext.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            
            byte data[] = new byte[4096];
            int count;
            while ((count = inputStream.read(data)) != -1) {
            	fos.write(data, 0, count);
            }
            inputStream.close();
            fos.close();
		
			inputStream = mContext.openFileInput(FILENAME);
		
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(inputStream, null, options);
			int imageHeight = options.outHeight;
			int imageWidth = options.outWidth;
			
			// restart file
			inputStream.close();
			inputStream = mContext.openFileInput(FILENAME);
			if (imageHeight <= reqHeight && imageWidth <= reqWidth) {
				options.inJustDecodeBounds = false;
				return BitmapFactory.decodeStream(inputStream, null, options);
			} else {
				return decodeSampledBitmap(inputStream);
			}

		} catch (IOException e) {
			e.printStackTrace();
	     } finally {
             try {
                 if (inputStream != null) 
                	 inputStream.close();
                 if (fos != null) 
                	 fos.close();
                 if (urlConnection != null)
                	 urlConnection.disconnect();
             } catch (Exception e) {
            	 e.printStackTrace();
             }
		}
		return null;
	}
	
	/**
	 * Calculate sample size needed for bitmap.
	 * @param options
	 * @return
	 */
	private int calculateInSampleSize(BitmapFactory.Options options) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	/**
	 * Decode bitmap for input stream.  Tries to decode a scaled down version into memory.  
	 * @param is inputstream for bitmap
	 * @return bitmap both dimensions will be equal to or less than requested dimensions.
	 * @throws IOException 
	 */
	private Bitmap decodeSampledBitmap(InputStream is) throws IOException {
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(is, null, options);

	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options);

		// restart file
	    is.close();
	    is = mContext.openFileInput(FILENAME);
	    
	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);	     
	    return bitmap;
	}
	

	@Override 
	public void deliverResult(Bitmap bitmap) {
		if (isReset()) {
			// An async query came in while the loader is stopped.  We
			// don't need the result.
			if (bitmap != null) {
				onReleaseResources(bitmap);
			}
		}
			
		if (isStarted()) {
			// If the Loader is currently started, we can immediately
			// deliver its results.
			super.deliverResult(bitmap);
		}
		
		// TODO cache image by URL
	}

	@Override 
	protected void onStartLoading() {
		// TODO check if image cached, return it
		
		forceLoad();
	}

	@Override
	protected void onStopLoading() {
		// Attempt to cancel the current load task if possible.
		cancelLoad();
	}


	@Override
	public void onCanceled(Bitmap bitmap) {
		super.onCanceled(bitmap);

		onReleaseResources(bitmap);
	}

	@Override 
	protected void onReset() {
		super.onReset();

		// Ensure the loader is stopped
		onStopLoading();		
	}

	/**
	 * Helper function to take care of releasing resources associated
	 * with an actively loaded data set.
	 */
	protected void onReleaseResources(Bitmap bitmap) {
		if (bitmap != null)
			bitmap.recycle();
	}
}