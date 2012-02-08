package com.fanfou.app.hd;

import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;

import com.astuetz.viewpager.extensions.FixedTabsView;
import com.astuetz.viewpager.extensions.SwipeyTabButton;
import com.astuetz.viewpager.extensions.SwipeyTabsView;
import com.astuetz.viewpager.extensions.TabsAdapter;
import com.fanfou.app.App;
import com.fanfou.app.App.ApnType;
import com.fanfou.app.R;
import com.fanfou.app.SearchPage;
import com.fanfou.app.SettingsPage;
import com.fanfou.app.cache.ImageLoader;
import com.fanfou.app.dialog.ConfirmDialog;
import com.fanfou.app.hd.UIBase;
import com.fanfou.app.hd.ui.ConversationListFragment;
import com.fanfou.app.hd.ui.HomeTimelineFragment;
import com.fanfou.app.hd.ui.MentionTimelineFragment;
import com.fanfou.app.hd.ui.PublicTimelineFragment;
import com.fanfou.app.hd.ui.PullToRefreshListFragment;
import com.fanfou.app.hd.ui.UserTimelineFragment;
import com.fanfou.app.service.Constants;
import com.fanfou.app.service.NotificationService;
import com.fanfou.app.ui.ActionBar;
import com.fanfou.app.ui.ActionManager;
import com.fanfou.app.ui.viewpager.TitlePageIndicator;
import com.fanfou.app.ui.viewpager.TitleProvider;
import com.fanfou.app.ui.viewpager.ViewPagerTabs;
import com.fanfou.app.util.IntentHelper;
import com.fanfou.app.util.OptionHelper;
import com.fanfou.app.util.ShakeListener;
import com.fanfou.app.util.ShakeListener.OnShakeListener;
import com.fanfou.app.util.SoundManager;
import com.fanfou.app.util.Utils;

/**
 * @author mcxiaoke
 * @version 1.0 2011.05.31
 * @version 2.0 2011.07.16
 * @version 3.0 2011.09.24
 * @version 3.2 2011.10.25
 * @version 3.3 2011.10.27
 * @version 3.5 2011.10.29
 * @version 3.6 2011.11.02
 * @version 3.7 2011.11.04
 * @version 4.0 2011.11.04
 * @version 4.1 2011.11.07
 * @version 4.2 2011.11.08
 * @version 4.3 2011.11.09
 * @version 4.4 2011.11.11
 * @version 4.5 2011.11.16
 * @version 4.6 2011.11.21
 * @version 4.7 2011.11.22
 * @version 4.8 2011.11.30
 * @version 4.9 2011.12.02
 * @version 5.0 2011.12.05
 * @version 5.1 2011.12.06
 * @version 5.2 2011.12.09
 * @version 5.3 2011.12.13
 * @version 6.0 2011.12.19
 * @version 6.1 2011.12.23
 * @version 6.2 2011.12.26
 * @version 7.0 2012.01.30
 * @version 7.1 2012.01.31
 * @version 8.0 2012.02.06
 * @version 8.1 2012.02.07
 * 
 */
public class UIHome extends UIBase {

	public static final String TAG = UIHome.class.getSimpleName();
//	public static final String[] PAGE_TITLES = new String[] { "我的消息", "随便看看",
//			"我的主页", "提到我的", "我的私信" };

	private static enum Page {
		ME, PUBLIC, HOME, MENTION, DM
	};

	public static final int NUMS_OF_PAGE = Page.values().length;
	private static final HashMap<Page, String> sTitles = new HashMap<UIHome.Page, String>(NUMS_OF_PAGE);

	static {
		sTitles.put(Page.ME, "我的消息");
		sTitles.put(Page.PUBLIC, "随便看看");
		sTitles.put(Page.HOME, "我的主页");
		sTitles.put(Page.MENTION, "提到我的");
		sTitles.put(Page.DM, "我的私信");
	}

	private ActionBar mActionBar;
	private ViewPager mViewPager;
	private PagesAdapter mPagesAdapter;
	private SwipeyTabsView mTabsView;

	private void log(String message) {
		Log.d(TAG, message);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (App.DEBUG)
			log("onCreate()");
	}

	@Override
	protected void initialize() {
		ImageLoader.getInstance();
	}

	@Override
	protected void setLayout() {

		setContentView(R.layout.ui_home);

		mActionBar = (ActionBar) findViewById(R.id.actionbar);
		mActionBar.setTitle("测试版");
		mActionBar.setRightAction(new ActionBar.WriteAction(this, null));

		mPagesAdapter = new PagesAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.viewpager);
		mViewPager.setAdapter(mPagesAdapter);
		mViewPager.setCurrentItem(Page.HOME.ordinal());

		mTabsView = (SwipeyTabsView) findViewById(R.id.viewindicator);
		mTabsView.setAdapter(new PageTabsAdapter(this));
		mTabsView.setViewPager(mViewPager);

	}

	@Override
	protected IntentFilter getIntentFilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_STATUS_SENT);
		filter.addAction(Constants.ACTION_DRAFTS_SENT);
		filter.addAction(Constants.ACTION_NOTIFICATION);
		return filter;
	}

	private void onActionSent() {
		if (App.DEBUG) {
			log("onBroadcastReceived ACTION_STATUS_SENT");
		}

		int curPage = mViewPager.getCurrentItem();
		if (curPage == Page.HOME.ordinal()) {
			boolean needRefresh = OptionHelper.readBoolean(this,
					R.string.option_refresh_after_send, false);
			if (needRefresh) {
				startRefresh(curPage);
			}
		}
	}

	@Override
	protected boolean onBroadcastReceived(Intent intent) {
		String action = intent.getAction();
		if (action.equals(Constants.ACTION_STATUS_SENT)
				|| action.equals(Constants.ACTION_DRAFTS_SENT)) {
			onActionSent();
		} else if (action.equals(Constants.ACTION_NOTIFICATION)) {
			if (App.DEBUG) {
				log("onBroadcastReceived ACTION_NOTIFICATION");
			}
			int type = intent.getIntExtra(Constants.EXTRA_TYPE, -1);
			int count = intent.getIntExtra(Constants.EXTRA_COUNT, 0);
			switch (type) {
			case NotificationService.NOTIFICATION_TYPE_HOME:
				if (count > 0) {
					mPagesAdapter.updateUI(Page.HOME.ordinal());
					Utils.notify(this, count + "条新消息");
				}
				break;
			case NotificationService.NOTIFICATION_TYPE_MENTION:
				if (count > 0) {
					mPagesAdapter.updateUI(Page.MENTION.ordinal());
					Utils.notify(this, count + "条新消息");
				}

				break;
			case NotificationService.NOTIFICATION_TYPE_DM:
				if (count > 0) {
					mPagesAdapter.updateUI(Page.DM.ordinal());
					Utils.notify(this, count + "条新私信");
				}
				break;
			default:
				break;
			}
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (App.DEBUG) {
			Log.d(TAG, "onResume()");
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (App.DEBUG) {
			Log.d(TAG, "onPause()");
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (App.DEBUG) {
			Log.d(TAG, "onStop()");
		}
		if (App.getApnType() != ApnType.WIFI) {
			App.getImageLoader().clearQueue();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (App.DEBUG) {
			Log.d(TAG, "onDestroy()");
		}
		App.getImageLoader().shutdown();
	}

	@Override
	protected int getPageType() {
		return PAGE_HOME;
	}

	@Override
	protected boolean isHomeScreen() {
		return true;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		int page = getIntent().getIntExtra(Constants.EXTRA_PAGE,
				Page.HOME.ordinal());
		mViewPager.setCurrentItem(page);
		if (App.DEBUG) {
			log("onNewIntent page=" + page);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.home_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.menu_option:
			onMenuOptionClick();
			return true;
		case R.id.menu_profile:
			onMenuProfileClick();
			return true;
		case R.id.menu_search:
			onMenuSearchClick();
			return true;
		case R.id.menu_logout:
			onMenuLogoutClick();
			return true;
		case R.id.menu_about:
			onMenuAboutClick();
			return true;
		case R.id.menu_feedback:
			onMenuFeedbackClick();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void onMenuOptionClick() {
		Intent intent = new Intent(this, SettingsPage.class);
		startActivity(intent);
	}

	private void onMenuProfileClick() {
		ActionManager.doMyProfile(this);
	}

	private void onMenuSearchClick() {
		Intent intent = new Intent(this, SearchPage.class);
		startActivity(intent);
	}

	private void onMenuAboutClick() {
		Utils.goAboutPage(this);
	}

	private void onMenuFeedbackClick() {
		ActionManager.doWrite(this, getString(R.string.config_feedback_account)
				+ " (" + Build.MODEL + "-" + Build.VERSION.RELEASE + " "
				+ App.appVersionName + ") ");
	}

	private void onMenuLogoutClick() {
		final ConfirmDialog dialog = new ConfirmDialog(this, "注销",
				"确定注销当前登录帐号吗？");
		dialog.setClickListener(new ConfirmDialog.AbstractClickHandler() {

			@Override
			public void onButton1Click() {
				App.setOAuthToken(null);
				IntentHelper.goLoginPage(mContext);
				finish();
			}
		});
		dialog.show();
	}

	@Override
	public void onClick(View v) {
	}

	private void checkRefresh(int page) {
		if (App.DEBUG) {
			Log.d(TAG, "checkRefresh page=" + page);
		}
		PullToRefreshListFragment fragment = mPagesAdapter.getItem(page);
		BaseAdapter adapter = fragment.getAdapter();
		if (App.DEBUG) {
			Log.e(TAG, "fragment=" + fragment);
			Log.e(TAG, "adapter=" + adapter);
		}
		if (adapter.isEmpty()) {
			fragment.startRefresh();
		}
	}

	private void startRefresh(int page) {
		mPagesAdapter.getItem(page).startRefresh();
	}

	private static class PagesAdapter extends FragmentPagerAdapter implements
			TitleProvider {

		private final PullToRefreshListFragment[] fragments = new PullToRefreshListFragment[NUMS_OF_PAGE];

		public PagesAdapter(FragmentManager fm) {
			super(fm);
			fragments[Page.ME.ordinal()] = UserTimelineFragment
					.newInstance(null);
			fragments[Page.PUBLIC.ordinal()] = PublicTimelineFragment
					.newInstance(0);
			fragments[Page.HOME.ordinal()] = HomeTimelineFragment
					.newInstance(0);
			fragments[Page.MENTION.ordinal()] = MentionTimelineFragment
					.newInstance(0);
			fragments[Page.DM.ordinal()] = ConversationListFragment
					.newInstance(0);
		}

		@Override
		public PullToRefreshListFragment getItem(int position) {
			return fragments[position % NUMS_OF_PAGE];
		}

		@Override
		public int getCount() {
			return NUMS_OF_PAGE;
		}

		public void updateUI(int position) {
			getItem(position).updateUI();
		}

		@Override
		public String getTitle(int position) {
			return sTitles.get(Page.values()[position % NUMS_OF_PAGE]);
//			return PAGE_TITLES[position % NUMS_OF_PAGE];
		}

	}

	private static class PageTabsAdapter implements TabsAdapter {

		private Activity mContext;

		public PageTabsAdapter(Activity ctx) {
			this.mContext = ctx;
		}

		@Override
		public View getView(int position) {
			LayoutInflater inflater = mContext.getLayoutInflater();
			SwipeyTabButton tab = (SwipeyTabButton) inflater.inflate(
					R.layout.tab_swipey, null);
			if (position < NUMS_OF_PAGE)
//				tab.setText(PAGE_TITLES[position]);
			tab.setText(sTitles.get(Page.values()[position]));
			return tab;
		}

	}

}