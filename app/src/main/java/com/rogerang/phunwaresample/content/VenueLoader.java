package com.rogerang.phunwaresample.content;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.support.v4.util.LongSparseArray;

/**
 * A custom Loader that loads all of the venue data.
 */
public class VenueLoader extends AsyncTaskLoader<List<Venue>> {
	List<Venue> mVenues;
	private Context mContext;
	
	// Since Venue IDs are long, using recommended LongSparseArray
    public static LongSparseArray<Venue> ITEM_MAP = new LongSparseArray<Venue>();

	public VenueLoader(Context context) {
		super(context);
		mContext = context;
	}	
    
    /**
     * Custom GSON deserializer for Date used in {@link ScheduleItem}.
     */
    private class DateDeserializer implements JsonDeserializer<Date> {
    	private final SimpleDateFormat FORMATTER = new SimpleDateFormat(
    			"yyyy-MM-dd HH:mm:ss Z");
    	
    	public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    			throws JsonParseException {  		
    		try {
				return FORMATTER.parse(json.getAsJsonPrimitive().getAsString());
			} catch (ParseException e) {
				e.printStackTrace();
				throw(new JsonParseException(e));
			}
    	}
    }
    
	@Override
	public List<Venue> loadInBackground() {
		HttpURLConnection urlConnection = null;
		InputStream inputStream = null;
		List<Venue> newData = null;
		
		try {
			URL mURL = new URL("https://s3.amazonaws.com/jon-hancock-phunware/nflapi-static.json");
			
			urlConnection= (HttpURLConnection) mURL.openConnection();
            inputStream = urlConnection.getInputStream();            
			InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
			Type venueType = new TypeToken<List<Venue>>() {}.getType();            
			GsonBuilder gsonBuilder = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
			gsonBuilder.registerTypeAdapter(Date.class, new DateDeserializer());
			Gson gson = gsonBuilder.create();
			newData = gson.fromJson(reader, venueType);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (inputStream != null) 
					inputStream.close();
				if (urlConnection != null)
					urlConnection.disconnect();
			} catch (Exception squish) {
				squish.printStackTrace();
			}
		}

		if (newData == null)
			newData = new ArrayList<Venue>();

		// Done!
		return newData;
	}

	@Override 
	public void deliverResult(List<Venue> venues) {
		if (isReset()) {
			// An async query came in while the loader is stopped.  We
			// don't need the result.
			if (venues != null) {
				onReleaseResources(venues);
			}
		}
		List<Venue> oldVenues = mVenues;
		mVenues = venues;

		ITEM_MAP.clear();
		if (mVenues != null) {
			for (Venue venue : mVenues) {
				ITEM_MAP.put(venue.getId(), venue);
			}
		}

		if (isStarted()) {
			// If the Loader is currently started, we can immediately
			// deliver its results.
			super.deliverResult(venues);
		}

		// At this point we can release the resources associated with
		// old data if needed; now that the new result is delivered we
		// know that it is no longer in use.
		if (oldVenues != null) {
			onReleaseResources(oldVenues);
		}
	}

	@Override 
	protected void onStartLoading() {
		if (mVenues != null) {
			// If we currently have a result available, deliver it
			// immediately.
			deliverResult(mVenues);
		}


		if (takeContentChanged() || mVenues == null) {
			// If the data has changed since the last time it was loaded
			// or is not currently available, start a load.
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		// Attempt to cancel the current load task if possible.
		cancelLoad();
	}


	@Override
	public void onCanceled(List<Venue> venues) {
		super.onCanceled(venues);

		onReleaseResources(venues);
	}

	@Override 
	protected void onReset() {
		super.onReset();

		// Ensure the loader is stopped
		onStopLoading();

		if (mVenues != null) {
			onReleaseResources(mVenues);
			mVenues = null;
		}
	}

	/**
	 * Helper function to take care of releasing resources associated
	 * with an actively loaded data set.
	 */
	protected void onReleaseResources(List<Venue> venus) {
		// For a simple List<> there is nothing to do.  For something
		// like a Cursor, we would close it here.
	}
}