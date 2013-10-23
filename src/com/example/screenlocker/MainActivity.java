package com.example.screenlocker;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends DeviceAdminReceiver {
	
	public static final String PREF_REQUIRE_REENTRY_PWD = "com.screenlocker.reentry";
	
	@Override
	public void onPasswordSucceeded(Context context, Intent intent) {
		SharedPreferences prefs = context.getSharedPreferences(Controller.sharedprefs_key, Controller.pref_mode);
		DevicePolicyManager DPM = (DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE);
		
		Log.i("Password Succeeded", "It was successful");
		
		if (prefs.getBoolean(PREF_REQUIRE_REENTRY_PWD, false)) {
			Log.i("Lock Screen", "Screen unlocked");
			NotificationManager notiMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			notiMan.cancel(ScreenLockerService.notiId);
			DPM.resetPassword("", 0);
			showToast(context, "Device unlocked");
			Editor editor = prefs.edit();
			editor.putBoolean(PREF_REQUIRE_REENTRY_PWD, false);
			editor.commit();
		}
		
		super.onPasswordSucceeded(context, intent);
	}

    void showToast(Context context, CharSequence msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEnabled(final Context context, Intent intent) {
        showToast(context, "Screen Locker Device Admin: enabled");
    }
    
    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        showToast(context, "pw changed");
    }

    /**
     * <p>UI control for the sample device admin.  This provides an interface
     * to enable, disable, and perform other operations with it to see
     * their effect.</p>
     *
     * <p>Note that this is implemented as an inner class only keep the sample
     * all together; typically this code would appear in some separate class.
     */
    public static class Controller extends Activity implements LocationListener {
        static final int RESULT_ENABLE = 1;
        
        private ZoneSQLiteAdder Zadder;
    	private LocationManager locationManager;
    	private SharedPreferences prefs;
    	private String provider;
    	private TextView mTextv;
    	private TextView numZones;
    	private TextView mBSSID;
    	private Button addZone;
    	public static final String sharedprefs_key = "com.screenlocker.sharedprefs";
    	public static final String pwd_key = "com.screenlocker.bluefishswim.underseacavern";
    	public static final String one_time_key = "com.screenlocker.firsttimesetpassword";
    	public static final int pref_mode = Context.MODE_PRIVATE;
    	
    	private List<Zone> curZones;

        public static DevicePolicyManager mDPM;
        public static ComponentName mDeviceAdminSample;
        
        private boolean createCalled = false;
        private boolean resumeCalled = false;

        Button mEnableButton;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            prefs = getSharedPreferences(sharedprefs_key, pref_mode);
            
            Zadder = new ZoneSQLiteAdder(Controller.this);
			Zadder.open();

            mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
            mDeviceAdminSample = new ComponentName(Controller.this, MainActivity.class);
            
            if (!mDPM.isAdminActive(mDeviceAdminSample)) {
	
	            setContentView(R.layout.device_admin);
	
	            // Watch for button click
	            mEnableButton = (Button)findViewById(R.id.enable);
	            mEnableButton.setOnClickListener(mEnableListener);
            } else {
            	
            	if (prefs.getBoolean(one_time_key, true)) {
                	Editor editor = prefs.edit();
                	editor.putBoolean(one_time_key, false);
                	editor.commit();
                	
                	AlertDialog.Builder builder = new Builder(Controller.this);
                    builder.setTitle("Set Lock Screen Password");
                    final EditText input = new EditText(Controller.this);
            		input.setInputType(InputType.TYPE_NUMBER_VARIATION_PASSWORD | InputType.TYPE_CLASS_NUMBER);
            		input.setHint("numeric password");
            		builder.setView(input);
            		builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            			
            			@Override
            			public void onClick(DialogInterface arg0, int arg1) {
            				SharedPreferences prefs = getSharedPreferences(sharedprefs_key, pref_mode);
            				Editor editor = prefs.edit();
            				editor.putInt(Controller.pwd_key, Integer.valueOf(input.getText().toString()));
            				editor.commit();
            				Log.i("Password", "Password changed");
            				mDPM.resetPassword(input.getText().toString(), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
            			}
            		});
            		
            		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            			
            			@Override
            			public void onClick(DialogInterface dialog, int which) {
            				return;
            			}
            		});
            		final AlertDialog dialog = builder.show();
            		
            		input.setOnKeyListener(new OnKeyListener() {
            			
            			@Override
            			public boolean onKey(View v, int keyCode, KeyEvent event) {
            				if ((event.getAction() == KeyEvent.ACTION_UP) && 
            						(keyCode == KeyEvent.KEYCODE_ENTER)) {
            					SharedPreferences prefs = getSharedPreferences(sharedprefs_key, pref_mode);
            					Editor editor = prefs.edit();
            					editor.putInt(Controller.pwd_key, Integer.valueOf(input.getText().toString()));
            					editor.commit();
            					Log.i("Password", "Password changed");
            					dialog.dismiss();
            					return true;
            				}
            				return false;
            			}
            		});
                }
            	
            	Intent service = new Intent(Controller.this, ScreenLockerService.class);
    			startService(service);
    			
    			mDPM.setPasswordQuality(mDeviceAdminSample, DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
    			mDPM.setPasswordMinimumLength(mDeviceAdminSample, 0);
    			
    			setContentView(R.layout.activity_main);
    			
    			locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    			
    			Criteria criteria = new Criteria();
    			provider = locationManager.getBestProvider(criteria, false);
    			Location location = locationManager.getLastKnownLocation(provider);
    			
    			mTextv = (TextView) findViewById(R.id.Router_Name);
    			numZones = (TextView) findViewById(R.id.Number_Zones);
    			mBSSID = (TextView) findViewById(R.id.Router_BSSID);
    			addZone = (Button) findViewById(R.id.Add_Unlock_Zone_Button);
    			
    			curZones = Zadder.getZones();
    			numZones.setText(String.valueOf(curZones.size()));
    			
    			if(location != null){
	    			mTextv.setText(String.valueOf(location.getLatitude()) + " " + String.valueOf(location.getLongitude()));
	    			mBSSID.setText(provider);
	    			
	    			if(Zadder.zoneExists(location)) {
	    				addZone.setText("Zone Exists");
	    				addZone.setEnabled(false);
	    			} else {
		    			addZone.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View v) {
								Location clickedLocation = locationManager.getLastKnownLocation(provider);
								Zone zone = new Zone();
								zone.setLatitude(clickedLocation.getLatitude());
								zone.setLongitude(clickedLocation.getLongitude());
								Zadder.addZone(zone);
							}
						});
	    			}
    			} else
    				mTextv.setText("Location not available");
    			
    			createCalled = true;
            }

        }
        
        @Override
    	public boolean onCreateOptionsMenu(Menu menu) {
    		getMenuInflater().inflate(R.menu.main, menu);
    		return true;
    	}
        
        @Override
        public boolean onMenuItemSelected(int featureId, MenuItem item) {
        	switch (item.getItemId()) {
    		case R.id.change_password :
    			AlertDialog.Builder builder = new AlertDialog.Builder(Controller.this);
    			builder.setMessage("Enter new Password").setTitle("Screen Locker");
    			final EditText input = new EditText(Controller.this);
    			input.setInputType(InputType.TYPE_NUMBER_VARIATION_PASSWORD | InputType.TYPE_CLASS_NUMBER);
    			input.setHint("numeric password");
    			builder.setView(input);
    			builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						prefs = getSharedPreferences(sharedprefs_key, pref_mode);
						Editor editor = prefs.edit();
						editor.putInt(pwd_key, Integer.valueOf(input.getText().toString()));
						editor.commit();
						Log.i("Password", "Password changed");
					}
				});
    			
    			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				});
    			final AlertDialog dialog = builder.show();
    			
    			input.setOnKeyListener(new OnKeyListener() {
					
					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						if ((event.getAction() == KeyEvent.ACTION_UP) && 
								(keyCode == KeyEvent.KEYCODE_ENTER)) {
							prefs = getSharedPreferences(sharedprefs_key, pref_mode);
							Editor editor = prefs.edit();
							editor.putInt(pwd_key, Integer.valueOf(input.getText().toString()));
							editor.commit();
							Log.i("Password", "Password changed");
							dialog.dismiss();
							return true;
						}
						return false;
					}
				});
    			
    			return true;
    			
    		case R.id.delete_zone :
        	}
        	return super.onMenuItemSelected(featureId, item);
        }
        
        @Override
        protected void onDestroy() {
        	super.onDestroy();
        	Zadder.close();
        }

        @Override
        protected void onResume() {
            super.onResume();
            if (createCalled) {
            	locationManager.requestLocationUpdates(provider, 400, 1, Controller.this);
            	resumeCalled = true;
            	
            }
        }
        
        @Override
        protected void onPause() {
        	super.onPause();
        	if (resumeCalled)
        		locationManager.removeUpdates(this);
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case RESULT_ENABLE:
                    if (resultCode == Activity.RESULT_OK) {
                        Log.i("DeviceAdminSample", "Admin enabled!");
                        Intent intent = new Intent(getApplicationContext(), Controller.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    } else {
                        Log.i("DeviceAdminSample", "Admin enable FAILED!");
                    }
                    return;
            }

            super.onActivityResult(requestCode, resultCode, data);
        }

        private OnClickListener mEnableListener = new OnClickListener() {
            public void onClick(View v) {
                // Launch the activity to have the user enable our admin.
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                        mDeviceAdminSample);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Screen Locker needs permission to set and remove passwords");
                startActivityForResult(intent, RESULT_ENABLE);
            }
        };

		@Override
		public void onLocationChanged(Location location) {
			double lat = location.getLatitude();
			double lng = location.getLongitude();
			mTextv.setText(String.valueOf(lat) + " " + String.valueOf(lng));
			mBSSID.setText(provider);
			if(Zadder.zoneExists(location)) {
				addZone.setText("Zone Exists");
				addZone.setEnabled(false);
			} else {
				addZone.setText("Add Zone");
				addZone.setEnabled(true);
    			addZone.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Location clickedLocation = locationManager.getLastKnownLocation(provider);
						Zone zone = new Zone();
						zone.setLatitude(clickedLocation.getLatitude());
						zone.setLongitude(clickedLocation.getLongitude());
						Zadder.addZone(zone);
						Intent addedIntent = new Intent("com.screenlocker.addedZone");
						sendBroadcast(addedIntent);
					}
				});
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}

    }
}
