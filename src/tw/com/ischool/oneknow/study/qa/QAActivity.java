package tw.com.ischool.oneknow.study.qa;

import tw.com.ischool.oneknow.R;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;

public class QAActivity extends Activity {

	private QAFragment mFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_quiz);

		getActionBar().hide();

		FragmentManager fm = getFragmentManager();
		mFragment = new QAFragment();
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		mFragment.setArguments(bundle);
		FragmentTransaction ft = fm.beginTransaction();
		ft.add(R.id.container_quiz, mFragment);
		ft.commitAllowingStateLoss();
	}

	@Override
	public void onBackPressed() {
		Bundle bundle = mFragment.createBundle();
		Intent data = new Intent();
		data.putExtras(bundle);
		setResult(0, data);
		finish();
	}

}
