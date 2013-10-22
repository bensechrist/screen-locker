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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
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
    public static class Controller extends Activity {
        static final int RESULT_ENABLE = 1;
        
        private ZoneSQLiteAdder Zadder;
    	private String macAddr;
    	private String SSID;
    	private WifiManager wm;
    	private ConnectivityManager cm;
    	private SharedPreferences prefs;
    	public static final String sharedprefs_key = "com.screenlocker.sharedprefs";
    	public static final String pwd_key = "com.screenlocker.bluefishswim.underseacavern";
    	public static final String one_time_key = "com.screenlocker.firsttimesetpassword";
    	public static final int pref_mode = Context.MODE_PRIVATE;
    	
    	private List<Zone> curZones;

        public static DevicePolicyManager mDPM;
        public static ComponentName mDeviceAdminSample;

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
    			
    			cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    			NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    			
    			wm = (WifiManager) getSystemService(WIFI_SERVICE);
    			SSID = "Wifi Disabled";
    			if (wm.isWifiEnabled()) {
	    			WifiInfo winfo = wm.getConnectionInfo();
	    			if (wifiInfo.isConnected()){
	    				macAddr = winfo.getMacAddress();
	    				Log.i("Wifi State", "Enabled");
	    			}
	    			else
	    				macAddr = null;
	    			SSID = winfo.getSSID();
    			}
    			
    			TextView mTextv = (TextView) findViewById(R.id.Router_Name);
    			TextView numZones = (TextView) findViewById(R.id.Number_Zones);
    			TextView mBSSID = (TextView) findViewById(R.id.Router_BSSID);
    			Button addZone = (Button) findViewById(R.id.Add_Unlock_Zone_Button);
    			
    			curZones = Zadder.getZones();
    			numZones.setText(String.valueOf(curZones.size()));
    			
    			if(macAddr != null){
    				Zone temp = new Zone();
    				temp.setMacAddr(macAddr);
	    			if (Zadder.zoneExists(temp)){
	    				addZone.setText("Zone Already Exists");
	    				addZone.setEnabled(false);
	    			}
	    			mTextv.setText(SSID);
	    			mBSSID.setText(macAddr);
	    			
	    			addZone.setOnClickListener(new OnClickListener() {
	    				
	    				@Override
	    				public void onClick(View arg0) {
	    					Log.i("Mac Address", macAddr);
	    					Zone newZone = new Zone();
	    					newZone.setMacAddr(macAddr);
	    					newZone.setSSID(SSID);
	    					if(!Zadder.zoneExists(newZone)) {
	    						Zadder.addZone(newZone);
	    						Intent addedIntent = new Intent("com.screenlocker.addedZone");
	    						sendBroadcast(addedIntent);
	    						Intent i = getBaseContext().getPackageManager()
	    					             .getLaunchIntentForPackage( getBaseContext().getPackageName() );
	    						i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    						startActivity(i);
	    					}
	    				}
	    			});
    			} else
    				mTextv.setText("No Wifi Network Detected");
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
    			final List<Zone> zones = Zadder.getZones();
    			String[] ssids = new String[zones.size()];
    			for (int i=0; i<zones.size(); i++) {
    				ssids[i] = zones.get(i).getSSID();
    				ssids[i] += " - " + zones.get(i).getMacAddr();
    				Log.i("Zone", zones.get(i).getSSID());
    			}
    			final ArrayList<Integer> selectedZones = new ArrayList<Integer>();;
    			AlertDialog.Builder deleteBuilder = new Builder(Controller.this);
    			deleteBuilder.setTitle("Select Zones to Delete");
    			deleteBuilder.setMultiChoiceItems(ssids, null, new DialogInterface.OnMultiChoiceClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						if (!isChecked) {
							selectedZones.remove(which);
							Toast.makeText(Controller.this, zones.get(which).getSSID() + " unselected", Toast.LENGTH_SHORT).show();
						} else if (isChecked){
							selectedZones.add(which);
							Toast.makeText(Controller.this, zones.get(which).getSSID() + " selected to delete", Toast.LENGTH_SHORT).show();
						}
					}
				}).setPositiveButton(R.string.delete_button, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						for (int i=0; i<selectedZones.size(); ++i) {
							Zadder.removeZone(zones.get(selectedZones.get(i)));
							Intent intent = getBaseContext().getPackageManager()
   					             .getLaunchIntentForPackage( getBaseContext().getPackageName() );
	   						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	   						startActivity(intent);
						}
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						selectedZones.clear();
					}
				});
    			
    			deleteBuilder.show();
        	}
        	return super.onMenuItemSelected(featureId, item);
        }
        
        @Override
        protected void onDestroy() {
        	Zadder.close();
        	super.onDestroy();
        }

        @Override
        protected void onResume() {
            super.onResume();
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

    }
}
