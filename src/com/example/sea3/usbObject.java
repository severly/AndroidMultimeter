package com.example.sea3;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;


import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

public class usbObject {
	private static final int PRODUCT_ID = 57720;
	private static final int NUM_BYTES_IN = 40;
	private static final int NUM_BYTES_OUT = 4;
	
	protected static final String TAG = "USB";

	// Pass a handler from the main activity in order to update data in the GUI
	public static volatile Handler mainHandler = null;
	
	public static UsbManager usbManager = null;
    private static UsbDevice usbDevice = null;
    private static UsbDeviceConnection usbConnection = null;
    private static UsbInterface usbInterface = null;
    private static UsbEndpoint epOut = null, epIn = null;
    
    private static HashMap<String, UsbDevice> deviceList;
	private static Iterator<UsbDevice> deviceIterator;
	
	private static volatile boolean stopThread = false;
	
	private static final Object threadLock = new Object();
	
	public static volatile boolean isConnected = false;
	
	/*
	 * ---------------------------------------------------------------------------------
	 * Constructor 
	 *  ---------------------------------------------------------------------------------
	 */
	public usbObject(Context context){
	}
	
	public static void setUsbService(Context context){
		usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
	}
	
	public static boolean deviceIsNull(){
		return usbDevice == null ? true : false;
	}
	
	/*
	 * ---------------------------------------------------------------------------------
	 * Attach main handler to USB Object
	 * ---------------------------------------------------------------------------------
	 */
	public static void setMainHandler(Handler handler){
		mainHandler = handler;
	}
	
	/*
	 * ---------------------------------------------------------------------------------
	 * Core thread of this class, facilitates all USB transfers and updates main thread 
	 * handler
	 * ---------------------------------------------------------------------------------
	 */
	private static Thread usbThread = new Thread() {
		@Override
		public void run(){
			// Initialize IN bound USB request packet
			ByteBuffer inBuff = ByteBuffer.allocate(NUM_BYTES_IN);
			UsbRequest inRequest = new UsbRequest();
			inRequest.initialize(usbConnection, epIn);
			
			
			// Initialize OUT bound USB request packet
			int outBuff_maxLength = epOut.getMaxPacketSize();
		 	ByteBuffer outBuff = ByteBuffer.allocate(NUM_BYTES_OUT);
			UsbRequest outRequest = new UsbRequest();
			outRequest.initialize(usbConnection, epOut);
			
			
			// Set stopThread to false so thread will run continuously 
			stopThread = false;
			// Report back to main thread that the device is connected
			isConnected = true;
			
			// USB worker thread
			while( !stopThread ){
				
				
				// Load PWM data into the out buffer
				lcrMeter.sendData(outBuff);
				// Queue buffer for sending
				outRequest.queue(outBuff, NUM_BYTES_OUT);
				
				if( usbConnection.requestWait() == outRequest ) { }

				// queue inRequest on the input interrupt endpoint
				inRequest.queue(inBuff, NUM_BYTES_IN);
				
				// Check if request was successful
				if( usbConnection.requestWait() == inRequest ){
					// Then retrieve & decode data from the USB buffer inBuff
					lcrMeter.retreiveData(inBuff); 
					
					// Send data to main thread handler to update UI
					Message msg = lcrMeter.bundleData();
					if(mainHandler!= null)
						mainHandler.sendMessage( msg );
						
					try{
						Thread.sleep(100);
					}catch(InterruptedException e){ }
				}else{
					Log.e(TAG, "requestWait false, exiting");
					break;
				}	
			}
		}
	};
	
	public static void stopThread(){
		synchronized(threadLock){
			stopThread = true;
			isConnected = false;
		}
	}
	
		
	/*
	 * ---------------------------------------------------------------------------------
	 * Methods to locate the correct device and connect to it
	 * ---------------------------------------------------------------------------------
	 */
	
	// findDevice() acquires a list of attached devices then cycles through them searching
	// for the correct product ID and making sure we can make a connection to said device
	private static UsbDevice findDevice(){
		
		UsbDevice tempDevice;
		UsbDeviceConnection tempConnection;
		
		deviceList = usbManager.getDeviceList();
		deviceIterator = deviceList.values().iterator();
		
		while(deviceIterator.hasNext()){
			tempDevice = deviceIterator.next();
			
			if(tempDevice.getProductId() == PRODUCT_ID){
				
				if( usbManager.hasPermission(tempDevice) ){
					
					tempConnection = usbManager.openDevice(tempDevice);
					
					// Success!
					if( tempConnection != null ) return tempDevice;	
				}
			}
		}
		return null;
	}
	
	// Adds a value to the device, interface, and endpoints
	public static boolean setDevice() {
		UsbDevice device = null;
		if( deviceIsNull() ){
			device = findDevice();
			if(device == null){
				usbDevice = null;
				epIn = epOut = null;
				return false;
			}
		}
		 
		
        Log.d(TAG, "setDevice " + device);
        if (device.getInterfaceCount() != 1) {
            Log.e(TAG, "could not find interface");
            return false;
        }
        UsbInterface intf = device.getInterface(0);
        // device should have one endpoint
        if (intf.getEndpointCount() != 2) {
            Log.e(TAG, "could not find endpoint");
            return false;
        }
        // endpoint should be of type interrupt
        UsbEndpoint ep0 = intf.getEndpoint(0);
        UsbEndpoint ep1 = intf.getEndpoint(1);
        if (ep0.getType() != UsbConstants.USB_ENDPOINT_XFER_INT) {
            Log.e(TAG, "endpoint0 is not interrupt type");
            return false;
        }
        if (ep1.getType() != UsbConstants.USB_ENDPOINT_XFER_INT) {
            Log.e(TAG, "endpoint1 is not interrupt type");
            return false;
        }
        usbDevice = device;
        usbInterface = intf;
        if(ep0.getDirection() == UsbConstants.USB_DIR_IN){
        	epIn = ep0;
        	epOut = ep1;
        }else if(ep1.getDirection() == UsbConstants.USB_DIR_IN){
        	epIn = ep1;
        	epOut = ep0;
        }
        return true;
    }
	
	// Remove all device associations, usually called when device is detached
	public static void unsetDevice(){
		usbDevice = null;
        usbInterface = null;
    	epIn = null;
    	epOut = null;
	}
	
	// Claim the interface to the device and start communicating
	public static boolean connect2Device(){

		if( deviceIsNull() ){
			if( !setDevice() ){
				return false;
			}
		}
		if(usbDevice == null || usbInterface == null || epOut == null || epIn == null){
			Log.e(TAG, "No USB Device Enumerated");
			return false;
		}
		if( !usbManager.hasPermission(usbDevice) ){
			Log.e(TAG, "No permission for USB device");
			return false;
		}
		
		usbConnection = usbManager.openDevice(usbDevice);
		
		if(usbConnection == null){
			Log.e(TAG, "Unable to create connection");
			return false;
		}
		if( !usbConnection.claimInterface(usbInterface, true) ){
			Log.e(TAG, "Unable to claim interface");
			return false;
		}
		
		usbThread.start();
		
		return true;
	}
	

	/*
	 * ---------------------------------------------------------------------------------
	 * Methods to display information about devices attached to host
	 * ---------------------------------------------------------------------------------
	 */
	public static void displayDeviceInfo(TextView usbText){
		if(usbDevice == null){
			usbText.setText("No device detected");
			return;
		}
		usbText.setText(usbDevice.getDeviceName() +  
						"\nClass: " + usbInterface.getInterfaceClass() +
						"\nProtocol: " + usbInterface.getInterfaceProtocol() +
						"\nSubclass: " + usbInterface.getInterfaceSubclass() +  
						"\nEndpoint Count: " + usbInterface.getEndpointCount() +
						"\nProduct ID: 0x" + Integer.toHexString( usbDevice.getProductId() ) +
						"\nVendor ID: 0x" + Integer.toHexString( usbDevice.getVendorId() ) +
						"\n\nEndpoint 0 Dir: " + epIn.getDirection() +
						"\nEndpoint 1 Dir: " + epOut.getDirection() +
						"\nEndpoint 0 Type: " + epIn.getType() +
						"\nEndpoint 1 Type:" + epOut.getType() );
	}
	
	public static void enumerateDevices(TextView usbText){
		UsbDevice tempDevice;
		deviceList = usbManager.getDeviceList();
		deviceIterator = deviceList.values().iterator();
		usbText.setText("Devices:\n");
		while(deviceIterator.hasNext()){
			tempDevice = deviceIterator.next();
			usbText.append( tempDevice.getDeviceName() + "\n");
			usbText.append("Product ID: " + Integer.toString( tempDevice.getProductId() ) );
			usbText.append("\nVendor ID:" + Integer.toString( tempDevice.getVendorId() ) +"\n\n");
		}
	}

}
