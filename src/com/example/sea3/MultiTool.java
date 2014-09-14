package com.example.sea3;

import java.util.Locale;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MultiTool extends FragmentActivity implements ActionBar.TabListener {
	
	/*
	 * The PagerAdapter that will provide fragments for each of the sections. We use a
	 * FragmentPagerAdapter derivative, which will keep every loaded fragment in memory. 
	 * If this becomes too memory intensive, it may be best to switch to a
	 * FragmentStatePagerAdapter.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	//The ViewPager that will host the section contents.
	ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_multi_tool);
		
		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter( getSupportFragmentManager() );

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() 
			{
				@Override
				public void onPageSelected(int position) {
					actionBar.setSelectedNavigationItem(position);
				}
			});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
		
		lcrMeter.set_frequency(1000);
	}
	
	// The bridge between the USB thread and the main thread
	static Handler mainHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			Bundle b = msg.getData();
			if( lcrSeriesFragment.isVisible ){
				if( lcrSeriesFragment.imageId != b.getInt("sImage") ){
					lcrSeriesFragment.imageId = b.getInt("sImage");
					lcrSeriesFragment.lcrImage.setImageResource( lcrSeriesFragment.imageId );
				}
				lcrSeriesFragment.resText.setText( b.getString("R") );
				lcrSeriesFragment.xText.setText( b.getString("X") );
				lcrSeriesFragment.usbText.setText( b.getString("status") );
			}
			
			
			if( lcrParallelFragment.isVisible ){
				if( lcrParallelFragment.imageId != b.getInt("pImage") ){
					lcrParallelFragment.imageId = b.getInt("pImage");
					lcrParallelFragment.lcrImage.setImageResource( lcrParallelFragment.imageId );
				}
				lcrParallelFragment.resText.setText( b.getString("pR") );
				lcrParallelFragment.pxText.setText( b.getString("pX") );
				
			}
			
			if( advancedFragment.isVisible ){
				advancedFragment.qText.setText( b.getString("Q") );
				advancedFragment.dText.setText( b.getString("D") );
				advancedFragment.magText.setText( b.getString("mag") );
				advancedFragment.phiText.setText( b.getString("phi") );
			}
			
		}
	};
	
	
	@Override
	public void onResume(){
		super.onResume();
		
		// Register device detached receiver, if not already registered
		try{
			registerReceiver(mUsbReceiver, new IntentFilter (UsbManager.ACTION_USB_DEVICE_DETACHED) );
		} 
		catch(IllegalArgumentException e) {}
		
		synchronized(this)
		{
			
		if( usbObject.usbManager == null)
			usbObject.setUsbService( getApplicationContext() );
		
		if( usbObject.mainHandler == null)
			usbObject.setMainHandler( mainHandler );
		
		if( !usbObject.isConnected )
			usbObject.connect2Device();
			//if( lcrSeriesFragment.isVisible )
			//	lcrSeriesFragment.usbText.setText( usbObject.connect2Device() );
		
		}
	}
	
	public void onPause(){
		super.onPause();
		unregisterReceiver(mUsbReceiver);
		usbObject.stopThread();
	}

	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.multi_tool, menu);
		return true;
	}
	
	// Set up options menu
	@Override
    public boolean onOptionsItemSelected(MenuItem item){    
        switch ( item.getItemId() ) {
        
        case R.id.action_connect:
        	usbObject.unsetDevice();
			usbObject.connect2Device();
        	return true;
        
        case R.id.action_settings:
        	//usbText.setText("Settings");
        	return true;
        	
        case R.id.action_setDevice:
        	usbObject.unsetDevice();
        	if( !usbObject.setDevice() ){
        		Toast.makeText( this, "Set device failed", Toast.LENGTH_SHORT ).show();
        	}
        	return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    } 

	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		mViewPager.setCurrentItem(tab.getPosition());
	}

	/*
	 * A FragmentPagerAdapter that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			Fragment fragment = null;
			switch(position){
			
			case 0: fragment = new lcrSeriesFragment();
				break;
			
			case 1: fragment = new lcrParallelFragment();
				break;
				
			case 2: fragment = new advancedFragment();
				break;
			}
				
			Bundle args = new Bundle();
			args.putInt( lcrSeriesFragment.ARG_SECTION_NUMBER, position + 1 );
			fragment.setArguments( args );
			return fragment;
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_series).toUpperCase(l);
			case 1:
				return getString(R.string.title_parallel).toUpperCase(l);
			case 2:
				return getString(R.string.title_advanced).toUpperCase(l);
			}
			return null;
		}
		
		// Hopefully allows use to see two fragment panes at once
		@Override
	    public float getPageWidth(int position) {
			if( screenIsLarge() )
				return(1/3f);
			else return(1f);
	    }
	}
	
	private boolean screenIsLarge(){
		int screenMask = getResources().getConfiguration().screenLayout;
		if ( ( screenMask & Configuration.SCREENLAYOUT_SIZE_MASK) == 
				Configuration.SCREENLAYOUT_SIZE_LARGE) {
		    return true;
		}
		
		if ( (screenMask & Configuration.SCREENLAYOUT_SIZE_MASK) == 
		        Configuration.SCREENLAYOUT_SIZE_XLARGE) {
				    return true;
		}
		
		return false;
		
	}

	/*
	 * ---------------------------------------------------------------------------------
	 * USB Device Broadcast Receiver 
	 * ---------------------------------------------------------------------------------
	 */

	// USB Broadcast receiver notifies when the device is connected or disconnected
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {

	        String action = intent.getAction();
	        
	        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
	            synchronized (this) {
	            	Toast.makeText( context, "Device attached", Toast.LENGTH_SHORT ).show();

	            	if( !usbObject.connect2Device() ){
	            		Log.e("USB","Could not connect to device");
	            		return;
	            	}

	            }
	            
	        }else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
	            synchronized (this) {
	            	usbObject.stopThread();
	            	usbObject.unsetDevice();
	            	Toast.makeText( context, "Device detached", Toast.LENGTH_SHORT ).show();
	            	
	            	if( lcrSeriesFragment.isVisible )
	            		lcrSeriesFragment.setConnectText(false);
	            	
	            	if( lcrParallelFragment.isVisible )
	            		lcrParallelFragment.setConnectText(false);
	            	
	            	if( advancedFragment.isVisible )
	            		advancedFragment.setConnectText(false);
	            	
	            	// Insert code to check if this was our device
	            }
	        }
	    }
	};

	/**
	 * ---------------------------------------------------------------------------------
	 * ---------------------------------------------------------------------------------
	 * FRAGMENT SECTION
	 * ---------------------------------------------------------------------------------
	 * ---------------------------------------------------------------------------------
	 */
	
	
	/*
	 * ---------------------------------------------------------------------------------
	 * SERIES FRAGMENT:
	 * The Series Circuit Configuration Fragment
	 * ---------------------------------------------------------------------------------
	 */
	public static class lcrSeriesFragment extends Fragment {

		public static final String ARG_SECTION_NUMBER = "section_number";
		public static boolean isVisible = false;
		
		public static TextView resText, xText, connectText;
		public static TextView usbText;
		public static EditText freqInput;
		public static ImageView lcrImage;
		public static SeekBar freqSeek;
		public static int imageId = 0;
		
		/*
		 * ---------------------------------------------------------------------------------
		 * SERIES FRAGMENT:
		 * Fragment Constructor
		 * ---------------------------------------------------------------------------------
		 */
		public lcrSeriesFragment() {
			
		}

		/*
		 * ---------------------------------------------------------------------------------
		 * SERIES FRAGMENT:
		 * Native fragment methods
		 * ---------------------------------------------------------------------------------
		 */
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			
			View rootView = inflater.inflate( R.layout.fragment_lcr_series, container, false );
			
			findViews(rootView);
			addSeekBarListener();
			//addTextListener();
			setConnectText(false);
			
			freqInput.setText( lcrMeter.get_Frequency() );
			freqSeek.setProgress( lcrMeter.frequency );
			
			return rootView;
		}
		
		// On resume sets the series handler and detects whether the device is connected
		@Override 
		public void onResume(){
			super.onResume();

			//setConnectText( usbObject.isConnected );
			setConnectText( true );
			usbText.setText(" ");
		}
		
		@Override
		public void onPause(){
			super.onPause();

		}
		
		@Override
		public void setUserVisibleHint(boolean isVisibleToUser) {
		    super.setUserVisibleHint(isVisibleToUser);
		    if (isVisibleToUser) {isVisible = true; }
		    else { isVisible = false;  }
		}
		
		
		/*
		 * ---------------------------------------------------------------------------------
		 * SERIES FRAGMENT:
		 * GUI Set Up Methods
		 * ---------------------------------------------------------------------------------
		 */
		
		// Correlates GUI Ids to local variables
		private static void findViews(View rootView){
			lcrImage = (ImageView) rootView.findViewById(R.id.RCImage);
			
			resText		= (TextView) rootView.findViewById(R.id.resistance);
			xText 		= (TextView) rootView.findViewById(R.id.reactance);
			connectText = (TextView) rootView.findViewById(R.id.connectText);
			usbText 	= (TextView) rootView.findViewById(R.id.usbText);
			usbText.setMovementMethod(new ScrollingMovementMethod());
			
			freqInput 	= (EditText) rootView.findViewById(R.id.freqInput);
			
			freqSeek 	= (SeekBar) rootView.findViewById(R.id.freqBar);
		}
		
		
		// Initializes the response of the frequency seek bar
		private static void addSeekBarListener(){
			freqSeek.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if(progress == 0){ return; }
					lcrMeter.set_frequency(progress);
					freqInput.setText( lcrMeter.get_Frequency() );
				}
				@Override
				public void onStartTrackingTouch(SeekBar arg0) {
					
				}
				@Override
				public void onStopTrackingTouch(SeekBar arg0) {
					
				}
				
			});
		}
		
		private void addTextListener(){
			freqInput.setOnFocusChangeListener(new OnFocusChangeListener() {
			    public void onFocusChange(View v, boolean hasFocus) {
			    	String freq = freqInput.getText().toString();
			    	if( freq.isEmpty() ) return;
			    	freq = freq.substring(0, freq.lastIndexOf(" ") );
			        lcrMeter.set_frequency( Integer.parseInt(freq) );
			        Log.d("KEY", "Lost focus");
			    }
			});
		}
		
		
		/*
		 * ---------------------------------------------------------------------------------
		 * SERIES FRAGMENT:
		 * GUI Manipulation Methods
		 * ---------------------------------------------------------------------------------
		 */
		public static void setConnectText(boolean connected){
			if( connected ){
				connectText.setText("Connected");
				connectText.setTextColor(Color.GREEN);
			}else{
				connectText.setText("Disconnected");
				connectText.setTextColor(Color.RED);
			}
		}
		
	}
	
	
	/*
	 * ---------------------------------------------------------------------------------
	 * PARALLEL FRAGMENT:
	 * The Parallel Circuit Configuration Fragment
	 * ---------------------------------------------------------------------------------
	 */
	public static class lcrParallelFragment extends Fragment {

		public static final String ARG_SECTION_NUMBER = "section_number";
		public static boolean isVisible = false;
		
		public static TextView resText, pxText, connectText;
		public static TextView usbText;
		public static EditText freqInput;
		public static ImageView lcrImage;
		public static SeekBar freqSeek;
		public static int imageId = 0;
		
		/*
		 * ---------------------------------------------------------------------------------
		 * PARALLEL FRAGMENT:
		 * Fragment Constructor
		 * ---------------------------------------------------------------------------------
		 */
		public lcrParallelFragment() {
		}

		/*
		 * ---------------------------------------------------------------------------------
		 * PARALLEL FRAGMENT:
		 * Native fragment methods
		 * ---------------------------------------------------------------------------------
		 */
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate( R.layout.fragment_lcr_parallel, container, false );
			
			findViews(rootView);
			addSeekBarListener();
			//addTextListener();
			setConnectText(false);
			
			freqInput.setText( lcrMeter.get_Frequency() );
			freqSeek.setProgress( lcrMeter.frequency );

			return rootView;
		}
		
		@Override 
		public void onResume(){
			super.onResume();

			setConnectText( usbObject.isConnected );
			usbText.setText(" ");
		}
		
		@Override
		public void onPause(){
			super.onPause();

		}
		
		@Override
		public void setUserVisibleHint(boolean isVisibleToUser) {
		    super.setUserVisibleHint(isVisibleToUser);
		    if (isVisibleToUser) {isVisible = true; }
		    else { isVisible = false;  }
		}
		
		
		/*
		 * ---------------------------------------------------------------------------------
		 * PARALLEL FRAGMENT:
		 * GUI Set Up Methods
		 * ---------------------------------------------------------------------------------
		 */
		
		// Correlates GUI Ids to local variables
		private void findViews(View rootView){
			lcrImage = (ImageView) rootView.findViewById(R.id.RCImage);
			
			resText		= (TextView) rootView.findViewById(R.id.resistance);
			pxText 		= (TextView) rootView.findViewById(R.id.reactance);
			connectText = (TextView) rootView.findViewById(R.id.connectText);
			usbText 	= (TextView) rootView.findViewById(R.id.usbText);
			
			freqInput 	= (EditText) rootView.findViewById(R.id.freqInput);
			freqSeek 	= (SeekBar) rootView.findViewById(R.id.freqBar);
		}
		
		// Initializes the response of the frequency seek bar
		private static void addSeekBarListener(){
			freqSeek.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
					if(progress == 0){ return; }
					lcrMeter.set_frequency(progress);
					freqInput.setText( lcrMeter.get_Frequency() );
				}
				@Override
				public void onStartTrackingTouch(SeekBar arg0) {
					
				}
				@Override
				public void onStopTrackingTouch(SeekBar arg0) {
					
				}
				
			});
		}
		
		private void addTextListener(){
			freqInput.setOnFocusChangeListener(new OnFocusChangeListener() {
			    public void onFocusChange(View v, boolean hasFocus) {
			    	String freq = freqInput.getText().toString();
			    	if( freq.isEmpty() ) return;
			    	freq = freq.substring(0, freq.lastIndexOf(" ") );
			        lcrMeter.set_frequency( Integer.parseInt(freq) );
			        Log.d("KEY", "Lost focus");
			    }
			});
		}
		
		
		/*
		 * ---------------------------------------------------------------------------------
		 * PARALLEL FRAGMENT:
		 * GUI Manipulation Methods
		 * ---------------------------------------------------------------------------------
		 */
		public static void setConnectText(boolean connected){
			if( connected ){
				connectText.setText("Connected");
				connectText.setTextColor(Color.GREEN);
			}else{
				connectText.setText("Disconnected");
				connectText.setTextColor(Color.RED);
			}
		}
	}
	
	/*
	 * ---------------------------------------------------------------------------------
	 * ADVANCED FRAGMENT:
	 * The Parallel Circuit Configuration Fragment
	 * ---------------------------------------------------------------------------------
	 */
	public static class advancedFragment extends Fragment {
		
		public static final String ARG_SECTION_NUMBER = "section_number";
		public static boolean isVisible = false;
		
		public static TextView qText;
		public static TextView dText;
		public static TextView magText;
		public static TextView phiText;
		public static TextView connectText;
		
		public advancedFragment() { }
		
		
		/*
		 * ---------------------------------------------------------------------------------
		 * ADVANCED FRAGMENT:
		 * Native fragment methods
		 * ---------------------------------------------------------------------------------
		 */
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate( R.layout.fragment_advanced, container, false );
			
			findViews(rootView);
			return rootView;
		}
		
		@Override 
		public void onResume(){
			super.onResume();
			
			setConnectText( usbObject.isConnected );
		}
		
		@Override
		public void onPause(){
			super.onPause();

		}
		
		@Override
		public void setUserVisibleHint(boolean isVisibleToUser) {
		    super.setUserVisibleHint(isVisibleToUser);
		    if (isVisibleToUser)
		    	isVisible = true; 
		    else 
		    	isVisible = false;  
		}
		
		/*
		 * ---------------------------------------------------------------------------------
		 * ADVANCED FRAGMENT:
		 * GUI Set Up Methods
		 * ---------------------------------------------------------------------------------
		 */
		private void findViews(View rootView){
			//lcrImage = (ImageView) rootView.findViewById(R.id.RCImage);
			
			qText	= (TextView) rootView.findViewById(R.id.qualityfactor);
			dText 	= (TextView) rootView.findViewById(R.id.dissipationfactor);
			magText = (TextView) rootView.findViewById(R.id.magText);
			phiText = (TextView) rootView.findViewById(R.id.phiText);
			
			connectText = (TextView) rootView.findViewById(R.id.connectText);
		}
		
		/*
		 * ---------------------------------------------------------------------------------
		 * ADVANCED FRAGMENT:
		 * GUI Manipulation Methods
		 * ---------------------------------------------------------------------------------
		 */
		public static void setConnectText(boolean connected){
			if( connected ){
				connectText.setText("Connected");
				connectText.setTextColor(Color.GREEN);
			}else{
				connectText.setText("Disconnected");
				connectText.setTextColor(Color.RED);
			}
		}
	}
	
}
