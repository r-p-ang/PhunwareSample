package com.rogerang.phunwaresample;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.rogerang.phunwaresample.content.ScheduleItem;
import com.rogerang.phunwaresample.content.Venue;
import com.rogerang.phunwaresample.content.VenueLoader;
import com.rogerang.phunwaresample.content.VenueImageLoader;

/**
 * A fragment representing a single Venue detail screen.
 * This fragment is either contained in a {@link ItemListActivity}
 * in two-pane mode (on tablets) or a {@link ItemDetailActivity}
 * on handsets.
 * 
 */
public class ItemDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Bitmap> {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The content this fragment is presenting.
     */
    private Venue mItem;

    // venue image
    private ImageView mImageView;
    private View noImageView;
    
    /**
     * Custom list adapter for ScheduleItems.
     * @author Roger
     *
     */
    public class ScheduleAdapter extends ArrayAdapter<ScheduleItem> {
    	private final LayoutInflater mInflater;    
    	private final SimpleDateFormat dateFormat = new SimpleDateFormat("E M/d");
    	private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mma");
    	
    	public ScheduleAdapter(Context context, List<ScheduleItem> venueData) {
    		super(context, R.layout.schedule_list_entry, venueData);
    		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	}

    	public View getView(int position, View convertView, ViewGroup parent) {
    		// Inflate a view template
    		if (convertView == null) {
    			convertView = mInflater.inflate(R.layout.schedule_list_entry, parent, false);
    		}
    		TextView tv = (TextView) convertView;    		
    		ScheduleItem item = getItem(position);    		
    		     		
    		Date startDate = item.getStartDate();
    		Date endDate = item.getEndDate();
    		
    		if (startDate != null && endDate != null) {
    			String startDateStr = dateFormat.format(startDate);
    			String schedule =  startDateStr + " " + timeFormat.format(startDate) + " to ";

    			// don't print end date if the same as start date
    			String endDateStr  = dateFormat.format(endDate);
    			if (!endDateStr.equals(startDateStr)) {
    				schedule += endDateStr + " ";
    			}
    			schedule += timeFormat.format(endDate);
    			
    			tv.setText(schedule);            		
    		}
    		return convertView;
    	}
    }
    
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the content specified by the fragment arguments.
        	// TODO should use different storage (cache?)
            mItem = VenueLoader.ITEM_MAP.get(getArguments().getLong(ARG_ITEM_ID));            
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_item_detail, container, false);

        // Show the content 
        if (mItem != null) {            
        	mImageView = (ImageView) rootView.findViewById(R.id.imageView1);        	
        	noImageView = rootView.findViewById(R.id.noImageText);     
        	
            ((TextView) rootView.findViewById(R.id.venueDetailNameText)).setText(mItem.getName());
            ((TextView) rootView.findViewById(R.id.venueDetailAddressText)).setText(mItem.getAddress());

            // populate schedule list
            List<ScheduleItem> scheduleList = mItem.getSchedule();
            if (scheduleList != null && !scheduleList.isEmpty()) {            	
            	ListView lv = (ListView) rootView.findViewById(R.id.scheduleList);
            	lv.setAdapter(new ScheduleAdapter(getActivity(), scheduleList));
            } 
        }

        return rootView;
    }
    
    
    @Override 
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        // load image 
        if (mItem != null) {          
        	if (mItem.getImageUrl() != null) {
                // Prepare the loader.  Either re-connect with an existing one,
                // or start a new one.
                getLoaderManager().initLoader(0, null, this);
        	}
        } 
    }
    
    public void shareVenue() {
    	if (mItem != null) {
    		String msg = "";
    		
    		if (mItem.getName() != null) {
    			msg += mItem.getName() + ", ";
    		}
    		if (mItem.getAddress() != null) {
    			msg += mItem.getAddress();
    		}    	   	
    		
    		Intent sendIntent = new Intent();
    		sendIntent.setAction(Intent.ACTION_SEND);
    		sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
    		sendIntent.setType("text/plain");
    	    if (sendIntent.resolveActivity(getActivity().getPackageManager()) != null) {
    	        startActivity(sendIntent);
    	    }    		
    	}
    }

	@Override
	public Loader<Bitmap> onCreateLoader(int id, Bundle args) {
		DisplayMetrics metrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return new VenueImageLoader(getActivity(), mItem.getImageUrl(), metrics.widthPixels, metrics.heightPixels); 
	}
	
	private Bitmap scaleDownBitmap(Bitmap bitmap, int reqWidth, int reqHeight) {
		if (reqWidth <= 0 || reqHeight <= 0) {
			return bitmap;
		}
		
	    // scale down to fit within requested dimensions
	    int width = bitmap.getWidth();
	    int height = bitmap.getHeight();
	    if (reqWidth < width ||  reqHeight < height) { 	    		 
	    	float xScale = ((float) reqWidth) / width;
	    	float yScale = ((float) reqHeight) / height;
	    	float scale = (xScale <= yScale) ? xScale : yScale;

	    	// Create a matrix for the scaling and add the scaling data
	    	Matrix matrix = new Matrix();
	    	matrix.postScale(scale, scale);

	    	// Create a new bitmap 
	    	return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
	    } else {
	    	return bitmap;  // no scaling needed
	    }
	}

	@Override
	public void onLoadFinished(Loader<Bitmap> loader, Bitmap data) {
		if (data != null) {
			// scale bitmap because ImageView doesn't properly resize with scaling options
			// TODO this is still buggy, the view sometimes still have no dimensions at this point
			int width = mImageView.getWidth() != 0 ? mImageView.getWidth() : noImageView.getWidth();						
			Bitmap scaledBitmap = scaleDownBitmap(data, width, data.getHeight());
			noImageView.setVisibility(View.GONE);
			mImageView.setImageBitmap(scaledBitmap);
			mImageView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onLoaderReset(Loader<Bitmap> loader) {
		mImageView.setVisibility(View.GONE);
		noImageView.setVisibility(View.VISIBLE);
	}
}
