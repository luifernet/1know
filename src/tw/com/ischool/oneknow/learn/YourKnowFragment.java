package tw.com.ischool.oneknow.learn;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import tw.com.ischool.oneknow.R;
import tw.com.ischool.oneknow.channel.UpdateChannelService;
import tw.com.ischool.oneknow.main.IReloadable;
import tw.com.ischool.oneknow.main.ISearchable;
import tw.com.ischool.oneknow.main.MainActivity;
import tw.com.ischool.oneknow.model.KnowDataSource;
import tw.com.ischool.oneknow.model.KnowImageTask;
import tw.com.ischool.oneknow.model.KnowImageTask.OnImageCompleteListener;
import tw.com.ischool.oneknow.model.KnowImageTask.OnImageProgresListener;
import tw.com.ischool.oneknow.model.Knowledge;
import tw.com.ischool.oneknow.model.OnKnowledgeReceiveListener;
import tw.com.ischool.oneknow.study.StudyActivity;
import tw.com.ischool.oneknow.util.CircleProgressBar;
import tw.com.ischool.oneknow.util.StringUtil;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class YourKnowFragment extends Fragment implements IReloadable,
		ISearchable {

	private OnSearchListener mSearchListener;
	private String mKeyword;
	private GridView mGridView;
	private LinearLayout mProgress;
	private ArrayList<Knowledge> mKnowList;
	private OnReloadCompletedListener mListener;
	private KnowDataSource mKnows;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		mKnows = new KnowDataSource(getActivity());

		View view = inflater.inflate(R.layout.fragment_knowledge, container,
				false);

		mGridView = (GridView) view.findViewById(R.id.lvKnowledge);
		mProgress = (LinearLayout) view.findViewById(R.id.progressInfo);
		mProgress.setVisibility(View.INVISIBLE);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		mKnows.open();

		if (StringUtil.isNullOrWhitespace(mKeyword)) {
			mKnowList = mKnows.getYourKnowledges();

			if (mKnowList.size() == 0) {
				reload();
			} else {
				bindData();
			}
		} else {
			search(mKeyword);
		}

		IntentFilter filter = new IntentFilter(UpdateChannelService.ACTION);

		this.getActivity().registerReceiver(mReceiver, filter);
	}

	@Override
	public void onStop() {
		mKnows.close();
		this.getActivity().unregisterReceiver(mReceiver);

		super.onStop();
	}

	private void bindData() {
		KnowAdapter adapter = new KnowAdapter();
		mGridView.setAdapter(adapter);
		mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				Intent intent = new Intent(getActivity(), StudyActivity.class);

				Knowledge know = mKnowList.get(position);
				intent.putExtra(StudyActivity.PARAM_KNOW, know);
				startActivity(intent);
			}
		});

		// TODO
		if (mSearchListener != null && StringUtil.isNullOrWhitespace(mKeyword)) {
			mSearchListener.onDataReady();
		}
	}

	private class KnowAdapter extends ArrayAdapter<Knowledge> {

		private LayoutInflater _inflater;

		public KnowAdapter() {
			super(getActivity(), R.layout.item_discover_know, mKnowList);

			_inflater = (LayoutInflater) getActivity().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = _inflater.inflate(R.layout.item_your_know, null);
			}

			final ImageView imgKnow = (ImageView) convertView
					.findViewById(R.id.imgKnowledge);
			final CircleProgressBar progImg = (CircleProgressBar) convertView
					.findViewById(R.id.progress);

			imgKnow.setImageBitmap(null);

			TextView txtKnowName = (TextView) convertView
					.findViewById(R.id.txtKnowName);
			TextView txtLastView = (TextView) convertView
					.findViewById(R.id.txtLastViewTime);
			CircleProgressBar progStudy = (CircleProgressBar) convertView
					.findViewById(R.id.progressStudy);

			Knowledge know = mKnowList.get(position);

			txtKnowName.setText(know.getName());

			handleLastViewTime(know.getLastViewTime(), txtLastView);

			progStudy.setMaxProgressColor(Color.GRAY);
			progStudy.setProgressColor(Color.RED);
			progStudy.setStrokeWidth(10);
			progStudy.setMaxProgress(know.getTotalTime());
			progStudy.setProgress(know.getGainedTime());

			// txtSubCount.setText(String.valueOf(know.getReader()));
			// rating.setRating(know.getRating());

			Bitmap cacheImage = know.getCachedLogoBitmap(getActivity());
			if (cacheImage != null) {
				imgKnow.setImageBitmap(cacheImage);
				return convertView;
			}

			// if (Knowledge.loadLogoImage(getActivity(), know) != null) {
			// imgKnow.setImageBitmap(know.getLogoBitmap());
			// return convertView;
			// }

			KnowImageTask task = new KnowImageTask(getActivity(), know);
			progImg.setVisibility(View.VISIBLE);
			progImg.setMaxProgress(100);

			task.setOnImageCompleteListener(new OnImageCompleteListener() {

				@Override
				public void onImageComplete(Bitmap bitmap) {
					imgKnow.setImageBitmap(bitmap);
					progImg.setVisibility(View.INVISIBLE);
				}
			});

			task.setOnImageProgressListener(new OnImageProgresListener() {

				@Override
				public void onProgress(int progress) {
					progImg.setProgress(progress);
				}
			});
			task.execute();

			return convertView;
		}
	}

	private void handleLastViewTime(String lastViewTime, TextView textView) {
		if (StringUtil.isNullOrWhitespace(lastViewTime)) {
			textView.setText(R.string.New);
			textView.setTextColor(Color.RED);
			textView.setTypeface(null, Typeface.BOLD_ITALIC);
			return;
		}

		textView.setTextColor(Color.DKGRAY);
		textView.setTypeface(Typeface.DEFAULT);

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
				Locale.getDefault());
		Date d = null;
		try {
			d = format.parse(lastViewTime);
		} catch (ParseException e) {
			String error = e.getMessage();
			Log.w(MainActivity.TAG, error);
		}

		if (d == null) {
			textView.setText(StringUtil.EMPTY);
			return;
		}

		Calendar cal = Calendar.getInstance();
		cal.setTime(d);

		Calendar now = Calendar.getInstance();

		String str = StringUtil.EMPTY;
		long t = (now.getTimeInMillis() - cal.getTimeInMillis()) / 1000;
		if (t < 60) {
			str = getString(R.string.seconds_ago);
			str = String.format(str, String.valueOf(t));
		} else if (t < 3600) {
			str = getString(R.string.minutes_ago);
			str = String.format(str, String.valueOf(t / 60));
		} else if (t < 86400) {
			str = getString(R.string.hours_ago);
			str = String.format(str, String.valueOf(t / 3600));
		} else if (t < 2592000) {
			str = getString(R.string.days_ago);
			str = String.format(str, String.valueOf(t / 86400));
		} else {
			SimpleDateFormat f = new SimpleDateFormat(
					getString(R.string.date_formater), Locale.getDefault());
			str = f.format(d);
		}

		textView.setText(str);
	}

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mKnowList = mKnows.getYourKnowledges();
			bindData();
		}

	};

	@Override
	public void reload() {
		mProgress.setVisibility(View.VISIBLE);
		YourKnowledgeTask task = new YourKnowledgeTask(mKnows);
		task.setOnKnowledgeReceivedListener(new OnKnowledgeReceiveListener() {

			@Override
			public void onReceived(ArrayList<Knowledge> knowledges) {
				mKnowList = knowledges;
				bindData();
				mProgress.setVisibility(View.GONE);
				if (mListener != null)
					mListener.onCompleted();
			}
		});

		task.execute();
	}

	@Override
	public void setOnReloadCompletedListener(OnReloadCompletedListener listener) {
		mListener = listener;
	}

	@Override
	public void search(String keyword) {
		mKeyword = keyword.toLowerCase(Locale.getDefault());

		ArrayList<Knowledge> knows = new ArrayList<Knowledge>();

		for (Knowledge know : mKnows.getYourKnowledges()) {
			String name = know.getName().toLowerCase(Locale.getDefault());

			if (name.contains(mKeyword)) {
				knows.add(know);
			}
		}

		mKnowList = knows;
		bindData();

		if (mSearchListener != null)
			mSearchListener.onSearchCompleted(knows.size());
	}

	@Override
	public void setOnSearchListener(OnSearchListener listener) {
		mSearchListener = listener;
	}
}
