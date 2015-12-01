package com.example.mysensorapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@SuppressLint({ "ShowToast", "SimpleDateFormat" })
public class MainActivity extends Activity implements SensorEventListener, LocationListener {

	private Date date;//計測開始日時．ファイル名に使用．
	private SimpleDateFormat simpleDateFormat;//日付形式指定用変数．
	private long startTime;//startボタンを押した時の時刻［ミリ秒単位］．

	private boolean measureStart = false;//startボタンを押すとtrue．計測中状態．
	private boolean titleAcc = false;//加速度データCSVファイルの第一行を書いたらtrue.
	private boolean titleLac = false;//線形加速度データCSVファイルの第一行を書いたらtrue.
	private boolean titleGyr = false;//角速度データCSVファイルの第一行を書いたらtrue.
	private boolean titleMag = false;//地磁気データCSVファイルの第一行を書いたらtrue.
	private boolean titlePrs = false;//気圧データCSVファイルの第一行を書いたらtrue.
	private boolean titleOri = false;//姿勢データCSVファイルの第一行を書いたらtrue.
	private boolean titleLgt = false;//照度データCSVファイルの第一行を書いたらtrue
	private boolean titleLoc = false;//位置データCSVファイルの第一行を書いたらtrue.

	//センサデータ格納用配列
	private float[] accValues = new float[3];
	private float[] lacValues = new float[3];
	private float[] gyrValues = new float[3];
	private float[] magValues = new float[3];
	private float[] prsValues = new float[3];
	private float[] oriValues = new float[3];
	private float[] lgtValues = new float[3];

	//姿勢計算用行列
	private static final int MATRIX_SIZE = 16;
    float[]  inR = new float[MATRIX_SIZE];
    float[] outR = new float[MATRIX_SIZE];
    float[]    I = new float[MATRIX_SIZE];

	private SensorManager sManager;//センサーマネージャー
	private LocationManager lManager;//ロケーションマネージャ（GPS）
	private File directory = Environment.getExternalStorageDirectory();//アンドロイド内蔵ストレージの「/sdcard」フォルダの場所を取得

	@Override
	protected void onStart() {
		// TODO 自動生成されたメソッド・スタブ
		super.onStart();
	}

	@Override
	protected void onStop() {
		// TODO 自動生成されたメソッド・スタブ
		lManager.removeUpdates(this);
		super.onStop();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.sManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		this.lManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //ロケーションサービスの設定
		Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(true);
        criteria.setBearingRequired(true);
        criteria.setCostAllowed(false);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setSpeedRequired(true);
        String provider = lManager.getBestProvider(criteria, true);
		lManager.requestLocationUpdates(provider,
                1000,    //リスナーに通知する最小時間間隔
                1,         //リスナーに通知する最小距離間隔
                this );    //リスナー
  	}

	public void onButtonClick(View v) {
		TextView buttonTextView = (TextView) findViewById(R.id.textStatDisp);

		//
		switch (v.getId()) {

		case R.id.startButton:
			buttonTextView.setText("start");
			measureStart = true;

			///sdcardの下に出力先のフォルダ(SampleDirフォルダ)を作成してここにデータファイルを保存
			if (directory.exists()){
			    if(directory.canWrite()){
			        File file = new File(directory.getAbsolutePath() + "/SampleDir");
			        file.mkdir();
			    }
			}
			startTime = System.currentTimeMillis();//計測開始時刻をミリ秒で取得
			date = new Date(startTime);//計測開始時刻をミリ秒から日付に変換
			simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");//計測開始時刻のフォーマット指定
			//ここからServiceの開始
			Intent intentStart = new Intent(getApplication(),SensorService.class);
			intentStart.putExtra("measureStart",measureStart);
			startService(intentStart);
			break;

		case R.id.stopButton:
			buttonTextView.setText("stop");
			measureStart = false;
			//ここからServiceの終了
			Intent intentStop = new Intent(getApplication(),SensorService.class);
			stopService(intentStop);
			break;
		}
	}

	@Override
	protected void onPause() {
		// TODO 自動生成されたメソッド・スタブ
		super.onPause();
		//センサ解放
		this.sManager.unregisterListener(this);
	}

	@Override
	protected void onResume() {
		// TODO 自動生成されたメソッド・スタブ
		super.onResume();

		// センサの取得
		List<Sensor> sensors = this.sManager.getSensorList(Sensor.TYPE_ALL);

		// センサマネージャヘリスナーを登録する
		// implements SensorEventListenerにより，thisで登録する
		// 念のため使いそうなセンサ―を登録しているが，全部登録するとアプリが重くなる
		// 必要ではないセンサはコメントアウトしておく
		for (Sensor sensor : sensors) {

			// 加速度センサ(重力補償された加速度)を高速サンプリングで登録
			if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				sManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
			}


			// 直線加速度センサ(重力補償された加速度)を高速サンプリングで登録
			if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
				sManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
			}
			// 角速度センサを高速サンプリングで登録
			if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
				sManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
			}


//			// 地磁気センサを高速サンプリングで登録
//			if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//				sManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
//			}

			// 気圧センサを通常速度サンプリングで登録
//			if (sensor.getType() == Sensor.TYPE_PRESSURE) {
//				sManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
//			}
			// 照度センサを通常速度サンプリングで登録
//			if (sensor.getType() == Sensor.TYPE_LIGHT) {
//				sManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
//			}
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO 自動生成されたメソッド・スタブ

		//event.values.clone()は格納されている配列データをコピー（クローン作成）する用意された関数
		//どのセンサイベントかを判別し，センサごとに用意した配列変数にコピーし，dataWriteFile関数でファイルにCSV形式で書き込む
		//ファイルはセンサごとに作成
		if (measureStart) {
			long currentTime = System.currentTimeMillis();
			long sampleTime = currentTime - startTime;

			switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				accValues = event.values.clone();
				dataWriteFile("ACC", sampleTime, accValues, titleAcc);
				titleAcc = true;
				break;
			case Sensor.TYPE_LINEAR_ACCELERATION:
				lacValues = event.values.clone();
				dataWriteFile("LAC", sampleTime, lacValues, titleLac);
				titleLac = true;
				break;
			case Sensor.TYPE_GYROSCOPE:
				gyrValues = event.values.clone();
				dataWriteFile("GYR", sampleTime, gyrValues, titleGyr);
				titleGyr = true;
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				magValues = event.values.clone();
				dataWriteFile("MAG", sampleTime, magValues, titleMag);
				titleMag = true;
				break;
			case Sensor.TYPE_PRESSURE:
				prsValues[0] = event.values[0];
				dataWriteFile("PRS", sampleTime, prsValues, titlePrs);
				titlePrs = true;
				break;
			case Sensor.TYPE_LIGHT:
				lgtValues[0] = event.values[0];
				dataWriteFile("LGT", sampleTime, lgtValues, titleLgt);
				titleLgt = true;
				break;
			}

			//TYPE_ORIENTATIONが非推奨となったので以下で計算．以下URLより拝借
			//http://techbooster.org/android/ui/443/
			if (accValues != null && magValues != null) {
				SensorManager.getRotationMatrix(inR, I, accValues, magValues);
				SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
				SensorManager.getOrientation(outR, oriValues);
				//ラジアンから度への変換
				for (int axis = 0; axis < 3; axis++){
					oriValues[axis] = radianToDegree(oriValues[axis]);
				}
				dataWriteFile("ORI", sampleTime, oriValues, titleOri);
				titleOri = true;
			}

		}
	}

	float radianToDegree(float rad){
	    return (float) Math.floor( Math.toDegrees(rad) ) ;
	}

	//センサデータをファイルに書き込む関数
	public void dataWriteFile(String sensorType, float timeStamp, float[] data, boolean title) {
		//
		//スタートボタンを押した時刻+センサ名.csvをファイル名とし
		//出力先ファイルのフルパスを作成
		//
		StringBuilder str = new StringBuilder(); //１行のデータを格納する文字列
		String filepath = directory.getAbsolutePath() + "/SampleDir/" + simpleDateFormat.format(date) + sensorType + ".csv";

		// try {...} catch (IOException e) {...}はファイルを取り扱うときのお決まりの手続き
		try {
			//ファイル書き込み識別子作成
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, true), "UTF-8"));

			//ファイルに書き込むデータ文字列作成
			if (sensorType == "PRS") {
				//気圧センサのデータは１つ
				//カンマつなぎのCSV形式
				//計測時刻,気圧データ
				if (!title) {
					str.append("Time, Pressure");
					//ファイルへ書き込み
					bw.write(str.toString());
					//改行
					bw.newLine();
					//ファイル閉じる
					bw.close();
				} else {
					str.append(timeStamp + "," + data[0]);
					//ファイルへ書き込み
					bw.write(str.toString());
					//改行
					bw.newLine();
					//ファイル閉じる
					bw.close();
				}
			} else if (sensorType == "LGT") {
				//照度センサのデータは１つ
				//カンマつなぎのCSV形式
				//計測時刻,照度データ
				if (!title) {
					str.append("Time, Brightness");
					//ファイルへ書き込み
					bw.write(str.toString());
					//改行
					bw.newLine();
					//ファイル閉じる
					bw.close();
				} else {
					str.append(timeStamp + "," + data[0]);
					//ファイルへ書き込み
					bw.write(str.toString());
					//改行
					bw.newLine();
					//ファイル閉じる
					bw.close();
				}
			}
			else {
				//加速度，直線加速度，角速度，地磁気，姿勢は３つ（３軸）
				//カンマつなぎのCSV形式
				//計測時刻,X軸データ,Y軸データ,Z軸データ
				if (!title) {
					str.append("Time, X, Y, Z");
					//ファイルへ書き込み
					bw.write(str.toString());
					//改行
					bw.newLine();
					//ファイル閉じる
					bw.close();
				} else {
					str.append(timeStamp + "," + data[0] + "," + data[1] + "," + data[2]);
					//ファイルへ書き込み
					bw.write(str.toString());
					//改行
					bw.newLine();
					//ファイル閉じる
					bw.close();
				}
			}
		} catch(IOException e) {
			System.out.println(e);
		}

	}
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO 自動生成されたメソッド・スタブ
		//未使用
	}

	@Override
	public void onLocationChanged(Location paramLocation) {
		// TODO 自動生成されたメソッド・スタブ
		if (measureStart) {
			long currentTime = System.currentTimeMillis();
			long sampleTime = currentTime - startTime;

			//スタートボタンを押した時刻+センサ名.csvをファイル名とし
			//出力先ファイルのフルパスを作成
			//
			StringBuilder str = new StringBuilder(); //１行のデータを格納する文字列
			String filepath = directory.getAbsolutePath() + "/SampleDir/" + simpleDateFormat.format(date) + "LOC" + ".csv";

			// try {...} catch (IOException e) {...}はファイルを取り扱うときのお決まりの手続き
			try {
				//ファイル書き込み識別子作成
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, true), "UTF-8"));

				if (!titleLoc) {
					str.append("Time, Latitude, Longitude, Altitude, Bearing, Speed, Time, Accuracy");
					//ファイルへ書き込み
					bw.write(str.toString());
					//改行
					bw.newLine();
					//ファイル閉じる
					bw.close();
					//タイトル行書き込み済み
					titleLoc = true;
				} else {
					str.append(sampleTime + "," + Double.toString(paramLocation.getLatitude()));
					str.append("," + Double.toString(paramLocation.getLongitude()));
					str.append("," + Double.toString(paramLocation.getAltitude()));
					str.append("," + Double.toString(paramLocation.getBearing()));
					str.append("," + Double.toString(paramLocation.getSpeed()));
					str.append("," + Double.toString(paramLocation.getTime()));
					str.append("," + Double.toString(paramLocation.getAccuracy()));
					//ファイルへ書き込み
					bw.write(str.toString());
					//改行
					bw.newLine();
					//ファイル閉じる
					bw.close();
				}
			} catch(IOException e) {
				System.out.println(e);
			}

		}

	}

	@Override
	public void onProviderDisabled(String paramString) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void onProviderEnabled(String paramString) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void onStatusChanged(String paramString, int paramInt,
			Bundle paramBundle) {
		// TODO 自動生成されたメソッド・スタブ

	}

}
