package com.example.sea3;

import java.nio.ByteBuffer;

import android.os.Bundle;
import android.os.Message;

public class lcrMeter {
	
	//Constant variables for locations in Input and Output Data Arrays
    private final static byte R_Byte1_Pos = 0;
    private final static byte R_Scale_Pos = 4;
    
    private final static byte pR_Byte1_Pos = 5;
    private final static byte pR_Scale_Pos = 9;
    
    private final static byte X_Byte1_Pos  = 10;
    private final static byte X_Scale1_Pos = 14;
    private final static byte X_Scale2_Pos = 15;
    
    private final static byte pX_Byte1_Pos	 = 16;
    private final static byte pX_Scale1_Pos = 20;
    private final static byte pX_Scale2_Pos = 21;
    
    private final static byte Q_Byte1_Pos = 22;
    private final static byte D_Byte1_Pos = 26;
    
    private final static byte Mag_Byte1_Pos = 30;
    private final static byte Mag_SCALE_Pos = 34;
    private final static byte Phi_Byte1_Pos = 35;
    
    private final static byte status_pos = 39;
    
    // Constants for output buffer array
    private final static byte Freq_Byte1_Position = 0;
	private final static byte Freq_Byte2_Position = 1;
    private final static byte Freq_Byte3_Position = 2;
    private final static byte Freq_Byte4_Position = 3;
    private final static byte Calibrate_Position = 4;
    
    // Constants for LCR Meter Status
    private final static byte OKAY 			= 0x01;
	private final static byte CLIPPING 		= 0x02;
    private final static byte AUTO_RANGE 	= 0x04;
    private final static byte OVER_RANGE 	= 0x08;
    private final static byte UNDER_RANGE 	= 0x10;
    private final static byte CALIBRATING 	= 0x20;
    
    public final static int aFreq[] =
    	{100,200,500,1000,2000,5000,10000,20000,50000,94000};
    
    public final static int aDivider[] 	= 
    	{12670,6340,2535,1267,633,253,124,62,24,12};

    public static volatile byte lcr_status = 0;
    
	public static volatile float resistance, reactance, D, Q;
	
	public static volatile float pResistance, pReactance, mag, phi;
	
	public static volatile String Scale_R, Scale_PR, Scale_F, Scale_X, Scale_pX, Scale_Mag;
	
	public static volatile String status = "";
	
	// Pulse width modulator value to send to LCR Meter
	public static volatile int PWM = 922; // 1kHz
	
	public static volatile int frequency = 48000000/((PWM + 1) * 52);
	
	// 2^15 scaling constant
	public static final int POW2_15 = 32768;
	
	public static Object syncObject = new Object();
	
	// Empty constructor
	public lcrMeter() {
	}
	
	/* ------------------------------------------------------------------------------------------------------------
	 * Accessor functions
	 * ------------------------------------------------------------------------------------------------------------
	 */
	public static String get_Frequency()
	{ 
		return Integer.toString(frequency) + Scale_F; 
	}
	
	// Return numerical RESISTANCE + qualifier as a String
	public static String get_R()
	{ 
		return  String.format("%.2f", resistance) + Scale_R; 
	}
	
	// Return numerical PARALLEL RESISTANCE + qualifier as a String
	public static String get_pR()
	{ 
		return  String.format("%.2f", pResistance) + Scale_PR; 
	}
	
	// Return numerical REACTANCE + qualifier as a String
	public static String get_X()
	{
		return String.format("%.2f", reactance) + Scale_X;
	}
	
	// Return numerical PARALLEL REACTANCE + qualifier as a String
	public static String get_pX()
	{
		return String.format("%.2f", pReactance) + Scale_pX;
	}
	
	// Return QUALITY FACTOR as a String
	public static String get_Q()
	{
		return String.format("%.3f", Q);
	}
	
	// Return DISSIPATION FACTOR as a String
	public static String get_D()
	{
		return String.format("%.3f", D);
	}
	
	// Return DISSIPATION FACTOR as a String
	public static String get_mag()
	{
		return String.format("%.2f", mag) + Scale_Mag;
	}
	
	// Return DISSIPATION FACTOR as a String
	public static String get_phi()
	{
		return String.format("%.2f°", phi);
	}
	
	// Return an identifier for an image of series circuit
	public static int get_sImage()
	{
		return isCapacitive() ? R.raw.srl181x144 : R.raw.src181x144;
	}
	
	// Return an identifier for an image of parallel circuit
	public static int get_pImage()
	{
		return isCapacitive() ? R.raw.prl181x144 : R.raw.prc181x144;
	}
	
	
	/* 
	 * ------------------------------------------------------------------------------------------------------------
	 * Manipulator functions
	 * ------------------------------------------------------------------------------------------------------------
	 */
	
	// Sets the frequency to the nearest possible option to freq
	public static void set_frequency(int freq)
	{
		
			PWM = (48000000 / (freq * 52)) - 1;
			frequency = 48000000/((PWM + 1)*52);

			if(frequency <= 1000) 
				Scale_F = " Hz";
			else
			{
				Scale_F = " kHz";
				frequency /= 1000;
			}
		
	}
	
	private static boolean set_status(byte lcr_status){
		switch(lcr_status){
		case(OKAY): status = " ";
			return true;

		case(CLIPPING) : 	status = "Clipping";
			return false;
			
		case(AUTO_RANGE) : 	status = "Auto Ranging";
			return false;
			
		case(OVER_RANGE) : 	status = "Over range";
			return false;
			
		case(UNDER_RANGE) : status = "Under range";
			return false;
			
		case(CALIBRATING) : status = "Open circuit calibration";
			return false;
		}
		return false;
	}
	
	/*
	 *  ------------------------------------------------------------------------------------------------------------
	 * Miscellaneous 
	 * ------------------------------------------------------------------------------------------------------------
	 */
	
	// Return TRUE if the circuit is in PARALLEL configuration
	public static boolean isCapacitive()
	{
		return Scale_R.contains("F");
	}
	
	public static String get_xLabel(){
		return Scale_X.contains("F") ? "C" : "L";
	}
	
	public static String get_pxLabel(){
		return Scale_pX.contains("F") ? "C" : "L";
	}
	
	public static synchronized Message bundleData(){
		Bundle b = new Bundle();
		Message msg = new Message();
		
		b.putString("pR", lcrMeter.get_pR() );
		b.putString("pX", lcrMeter.get_pX() );
		b.putString("pxLabel", lcrMeter.get_pxLabel() );
		b.putInt("pImage", lcrMeter.get_pImage() );

		b.putString("R", lcrMeter.get_R() );
		b.putString("X", lcrMeter.get_X() );
		b.putString("xLabel", lcrMeter.get_xLabel() );
		b.putInt("sImage", lcrMeter.get_sImage() );

		b.putString("Q", lcrMeter.get_Q() );
		b.putString("D", lcrMeter.get_D() );
		
		b.putString("mag", lcrMeter.get_mag() );
		b.putString("phi", lcrMeter.get_phi() );
		
		b.putString("status", lcrMeter.status );
		
		msg.setData(b);
		
		return msg;
	}
	
	/* ------------------------------------------------------------------------------------------------------------
	 * USB Transfer Methods
	 * ------------------------------------------------------------------------------------------------------------
	 */
	public static void retreiveData(ByteBuffer buff)
	{
		lcr_status = buff.get( status_pos );
		
		if( !set_status( lcr_status ) )
			return;
		
		
		resistance 	= ( (float) buff.getInt(R_Byte1_Pos) ) / POW2_15;
		pResistance = ( (float) buff.getInt(pR_Byte1_Pos) ) / POW2_15;
		reactance 	= ( (float) buff.getInt(X_Byte1_Pos) ) / POW2_15;
		pReactance	= ( (float) buff.getInt(pX_Byte1_Pos) ) / POW2_15;
		
		Q = ( (float) buff.getInt(Q_Byte1_Pos) ) / POW2_15;
		D = ( (float) buff.getInt(D_Byte1_Pos) ) / POW2_15;
		
		mag = ( (float) buff.getInt(Mag_Byte1_Pos) ) / POW2_15;
		phi = ( (float) ( buff.getInt(Phi_Byte1_Pos) ) / POW2_15 ) - 180;
		
		Scale_R = "" + (char) buff.get(R_Scale_Pos) + 'Ω';
		Scale_PR = "" + (char)buff.get(pR_Scale_Pos) + 'Ω';
		Scale_X = "" + (char)buff.get(X_Scale1_Pos) + (char)buff.get(X_Scale2_Pos);
		Scale_pX = "" + (char)buff.get(pX_Scale1_Pos) + (char)buff.get(pX_Scale2_Pos);
		Scale_Mag = "" + (char)buff.get(Mag_SCALE_Pos);
		
		
	
	}
	
	public static void sendData(ByteBuffer buff)
	{
		
		byte b1 = (byte) (0x000000FF & (PWM >> 24) );
		byte b2 = (byte) (0x000000FF & (PWM >> 16));
		byte b3 = (byte) (0x000000FF & (PWM >> 8));
		byte b4 = (byte) (0x000000FF & (PWM));
		
		
		buff.put( Freq_Byte1_Position, b1 );
		buff.put( Freq_Byte2_Position, b2 );
		buff.put( Freq_Byte3_Position, b3 );
		buff.put( Freq_Byte4_Position, b4 );

		//buff.putInt(Freq_Byte1_Position, PWM);
	}
}
