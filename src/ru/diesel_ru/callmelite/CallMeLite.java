package ru.diesel_ru.callmelite;

import ru.diesel_ru.callmelite.Abaut;
import ru.diesel_ru.callmelite.FavContList;
import ru.diesel_ru.callmelite.PrefActivity;
import ru.diesel_ru.callmelite.R;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class CallMeLite extends Activity {
	protected static final int PICK_RESULT = 0;
	static final private int PHONE_NUMBER = 3;
	
	private String strOprator = "";
	private boolean blClose = false;
	private SharedPreferences sp;
	private int iMount = 1;
	private int iSMSCount = 0;
	
	private ImageButton buttonSelectContact;
	private ImageButton buttonSelectFavoritesContact;
	private ImageButton buttonSend;
	
	private TextView txtPhoneNumber;
	private TextView txtCount;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_call_me_lite);
		
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		strOprator = sp.getString("defaultOperator","0");
		blClose = sp.getBoolean("CloseApp", false);
		iMount = sp.getInt("iMount", 1);
		iSMSCount = sp.getInt("iSMSCount", 0);
        
		txtPhoneNumber = (TextView) findViewById(R.id.etPhone);
		txtCount = (TextView) findViewById(R.id.tvCount);
		buttonSelectContact = (ImageButton) findViewById(R.id.btnContacts);
        buttonSelectFavoritesContact = (ImageButton) findViewById(R.id.btnFContacts);
        buttonSend = (ImageButton) findViewById(R.id.btnSend);
        
        txtCount.setText("Отправлено " + iSMSCount + " из 15.");
        
        Intent localIntent = getIntent();
        if (localIntent.getAction().contains("android.intent.action.SENDTO")){
        	String strNum = "";
        	strNum = Uri.decode(localIntent.getData().toString()).replace("smsto:", "").replace("sms:", "").replace("+7", "8").replace("-", "").replace(" ", "");
        	//tv.setText(strNum);

        	Intent _intent = new Intent(Intent.ACTION_CALL);
        	
        	if (strOprator.compareToIgnoreCase("1") == 0) {
        		//"MTS":
				_intent.setData(Uri.fromParts("tel", "*110*" + strNum + "#", "#"));				
        	}else if (strOprator.compareToIgnoreCase("2") == 0) {				
        		//"Beeline":
				_intent.setData(Uri.fromParts("tel", "*144#" + strNum + "#", "#"));			
        	}else if (strOprator.compareToIgnoreCase("3") == 0) {
        		//"Megafon":
				_intent.setData(Uri.fromParts("tel", "*144*" + strNum + "#", "#"));
        	}else if (strOprator.compareToIgnoreCase("4") == 0) {
        		//"Tele2":
				_intent.setData(Uri.fromParts("tel", "*118*" + strNum + "#", "#"));
        	}else if (strOprator.compareToIgnoreCase("5") == 0) {
        		//"BWC":
				_intent.setData(Uri.fromParts("tel", "*141*" + strNum + "#", "#"));
        	}else if (strOprator.compareToIgnoreCase("6") == 0) {
        		//"Other":
        		_intent.setData(Uri.fromParts("tel", sp.getString("otherStartUSSD","") + strNum + "#", "#"));
			}else if (strOprator.compareToIgnoreCase("0") == 0) {
				Toast.makeText(getApplicationContext(), "Выберите оператора связи!", Toast.LENGTH_SHORT).show();
				return;
			}

        	if(AcceptSend()){
        		startActivity(_intent);
        	}
        	
			finish();
        }        
        
        // Обработчик выбора контакта
        buttonSelectContact.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		// Выбор только контактов звонков (без почтовых)
                Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
                pickIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                startActivityForResult(pickIntent, PICK_RESULT);
        	}
        });
        
        // Обработчик выбора избранного контакта
        buttonSelectFavoritesContact.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(CallMeLite.this, FavContList.class);
			    startActivityForResult(intent, PHONE_NUMBER);
        	}
        });
        
        // Обработчик нажатиЯ на кнопку отправить
        buttonSend.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		if (txtPhoneNumber.length() < 1)
        		{
        			Toast.makeText(getApplicationContext(), "Введите номер телефона!", Toast.LENGTH_SHORT).show();
        			return;
        		}
        		
        		sendUSSD(txtPhoneNumber.getText().toString());
        	}
        });
        
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
	      MenuItem mi = menu.add(0, 1, 0, "Настройки");
	      mi.setIntent(new Intent(this, PrefActivity.class));
	      mi = menu.add(0, 1, 0, "О программе");
	      mi.setIntent(new Intent(this, Abaut.class));
	      
	      return super.onCreateOptionsMenu(menu);
    }

    // Обработка выбора контакта
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	// Получаем номер из избранных контактов
    	if (requestCode == PHONE_NUMBER) {
    		if (resultCode == RESULT_OK) {
    			String thiefname = data.getStringExtra(FavContList.PHONE_NUMBER);
    			txtPhoneNumber.setText(thiefname.replace("+7", "8").replace("-", "").replace(" ", ""));
    		}else {
    			txtPhoneNumber.setText(""); // стираем текст
    		}
    	}
    	// Получаем номер из активити контактов
        if (data != null) {
            Uri uri = data.getData();

            if (uri != null) {
                Cursor c = null;
                try {
                	c = getContentResolver().query(uri, new String[]{ 
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.TYPE},
                        null, null, null);

                    if (c != null && c.moveToFirst()) {
                        String number = c.getString(0);
                        int type = c.getInt(1);
                        showSelectedNumber(type, number);
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }
    }

    // присваивание значениЯ номера телефона 
    private void showSelectedNumber(int type, String number) {
    	txtPhoneNumber.setText(number.replace("+7", "8").replace("-", "").replace(" ", ""));      
    }
    
    private void sendUSSD(String strNum){
    	
    	strNum = strNum.replace("smsto:", "").replace("sms:", "").replace("+7", "8").replace("-", "").replace(" ", "");
    	
    	Intent _intent = new Intent(Intent.ACTION_CALL);
    	
    	if (strOprator.compareToIgnoreCase("1") == 0) {
    		//"MTS"
			_intent.setData(Uri.fromParts("tel", "*110*" + strNum + "#", "#"));				
    	}else if (strOprator.compareToIgnoreCase("2") == 0) {				
    		//"Beeline"
			_intent.setData(Uri.fromParts("tel", "*144#" + strNum + "#", "#"));			
    	}else if (strOprator.compareToIgnoreCase("3") == 0) {
    		//"Megafon"
			_intent.setData(Uri.fromParts("tel", "*144*" + strNum + "#", "#"));
    	}else if (strOprator.compareToIgnoreCase("4") == 0) {
    		//"Tele2"
			_intent.setData(Uri.fromParts("tel", "*118*" + strNum + "#", "#"));
    	}else if (strOprator.compareToIgnoreCase("5") == 0) {
    		//"BWC"
			_intent.setData(Uri.fromParts("tel", "*141*" + strNum + "#", "#"));
    	}else if (strOprator.compareToIgnoreCase("6") == 0) {
    		//"Other"
    		_intent.setData(Uri.fromParts("tel", sp.getString("otherStartUSSD","") + strNum + "#", "#"));
		}else if (strOprator.compareToIgnoreCase("0") == 0) {
			Toast.makeText(getApplicationContext(), "Выберите оператора связи!", Toast.LENGTH_SHORT).show();
			return;
		}

    	if(AcceptSend()){
    		startActivity(_intent);
    	}
    	
		if(blClose)     
	        finish();
    }
    
    // Закрытие приложениЯ
	@Override
    protected void onStop(){
       super.onStop();
    }
    
	@Override
    protected void onResume() {
		strOprator = sp.getString("defaultOperator","0");
		blClose = sp.getBoolean("CloseApp", false);
		iMount = sp.getInt("iMount", 1);
		iSMSCount = sp.getInt("iSMSCount", 0);
		super.onResume();
    }

    @Override
    protected void onPause() {
		//Log.d(LOG_TAG, "onPause");
		super.onPause();
    }
    
    @Override
    protected void onStart() {
    	strOprator = sp.getString("defaultOperator","0");
    	blClose = sp.getBoolean("CloseApp", false);
    	iMount = sp.getInt("iMount", 1);
    	iSMSCount = sp.getInt("iSMSCount", 0);
		super.onStart();
    }
    
    private boolean AcceptSend(){
    	java.util.Calendar calendar = java.util.Calendar.getInstance(java.util.TimeZone.getDefault(), java.util.Locale.getDefault());
    	calendar.setTime(new java.util.Date());
    	//int currentYear = calendar.get(java.util.Calendar.YEAR);
    	int currentMount = calendar.get(java.util.Calendar.MONTH);
    	if(currentMount == iMount && iSMSCount <= 15){
    		sp.edit().putInt("iMount", currentMount);
    		sp.edit().apply();
    		sp.edit().putInt("iSMSCount", iSMSCount - 1);
    		sp.edit().apply();
    		txtCount.setText("Отправлено " + iSMSCount + " из 15.");
    		
    		return true;
    	} else {
    		Toast.makeText(getApplicationContext(), "Вы израсходовали все СМС!\n Приобретите полную версию.", Toast.LENGTH_SHORT).show();
    		return false;
    	}
    }
}
