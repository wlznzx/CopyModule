package cn.wl.utilView;

import java.io.File;

import cn.wl.copy.CopyPasteFilesTask;
import cn.wl.utils.SDCardUtils;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {
	private final static String TAG = "wlutils";
	private BroadcastReceiver mReceiver;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.v(TAG,"SDUtils.isSDMounted() = " + SDCardUtils.checkFsWritable("/storage/sdcard1"));
		
		mReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.i(TAG,"BroadcastReceiver: "+intent.getAction());
		        //intent.getData().getPath());获取存储设备路径
		        Log.i(TAG,"path: "+intent.getData().getPath());
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);//表明sd对象是存在并具有读/写权限
	    filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);//SDCard已卸掉,如果SDCard是存在但没有被安装
	    filter.addDataScheme("file"); // 必须要有此行，否则无法收到广播  
	    registerReceiver(mReceiver, filter);
	    
	    Button btn = (Button) findViewById(R.id.btn);
	    btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				File _file = new File("/storage/sdcard0/mtklog");
				String _destFolder = "/storage/sdcard1/";
				CopyPasteFilesTask _taks = new CopyPasteFilesTask(
						null, MainActivity.this, _file, _destFolder);
				_taks.execute();
			}
		});
	}
	
	
}
