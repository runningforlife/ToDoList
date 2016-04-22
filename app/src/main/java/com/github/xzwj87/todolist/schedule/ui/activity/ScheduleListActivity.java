package com.github.xzwj87.todolist.schedule.ui.activity;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.github.xzwj87.todolist.R;
import com.github.xzwj87.todolist.schedule.data.provider.ScheduleContract;
import com.github.xzwj87.todolist.schedule.interactor.UseCase;
import com.github.xzwj87.todolist.schedule.interactor.mapper.ScheduleSuggestionModelDataMapper;
import com.github.xzwj87.todolist.schedule.interactor.query.GetAllScheduleSuggestion;
import com.github.xzwj87.todolist.schedule.presenter.SearchSuggestionPresenter;
import com.github.xzwj87.todolist.schedule.presenter.SearchSuggestionPresenterImpl;
import com.github.xzwj87.todolist.schedule.ui.SearchSuggestionView;
import com.github.xzwj87.todolist.schedule.ui.adapter.ScheduleAdapter;
import com.github.xzwj87.todolist.schedule.ui.adapter.SearchSuggestionAdapter;
import com.github.xzwj87.todolist.schedule.ui.fragment.ScheduleDetailFragment;
import com.github.xzwj87.todolist.schedule.ui.fragment.ScheduleListFragment;
import com.github.xzwj87.todolist.schedule.ui.model.ScheduleModel;
import com.github.xzwj87.todolist.schedule.ui.model.ScheduleSuggestionModel;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ScheduleListActivity extends AppCompatActivity
        implements ScheduleListFragment.
        Callbacks, NavigationView.OnNavigationItemSelectedListener,
        SearchSuggestionView {
    private static final String LOG_TAG = ScheduleListActivity.class.getSimpleName();

    private static final String DETAIL_FRAGMENT_TAG = "detail_fragment";
    private static final String SEARCH_RESULT_FRAGMENT_TAG = "search_result_fragment";
    private boolean mTwoPane;
    private String mTypeFilter;
    private SearchView mSearchView;
    private SearchSuggestionAdapter mSuggestionAdapter;

    private SearchSuggestionPresenter mPresenter;

    private ScheduleObserver mScheduleObserver;

    @Bind(R.id.fab) FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_list);

        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close){
            @Override
            public void onDrawerOpened (View drawerView){
                super.onDrawerOpened(drawerView);
                Log.v(LOG_TAG, "onDrawerOpened()");
            }
        };

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(0).setChecked(true);

        mTwoPane = findViewById(R.id.schedule_detail_container) != null;
        Log.v(LOG_TAG, "onCreate(): mTwoPane = " + mTwoPane);

        if (savedInstanceState == null) {
            ScheduleListFragment fragment = ScheduleListFragment.newInstanceByType(null);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.schedule_list_container, fragment)
                    .commit();
        }

        handleIntent(getIntent());

        initialize();

        registerObserver();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        unregisterObserver();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_schedule_type_all) {
            Log.v(LOG_TAG, "onNavigationItemSelected(): nav_schedule_type_all");
            mTypeFilter = null;
            replaceScheduleListWithType(null);
        } else if (id == R.id.nav_done) {
            Log.v(LOG_TAG, "onNavigationItemSelected(): nav_done");
            mTypeFilter = ScheduleListFragment.SCHEDULE_TYPE_DONE;
            replaceScheduleListWithType(mTypeFilter);
        } else if (id == R.id.nav_schedule_type_meeting) {
            Log.v(LOG_TAG, "onNavigationItemSelected(): nav_schedule_type_meeting");
            mTypeFilter = ScheduleModel.SCHEDULE_TYPE_MEETING;
            replaceScheduleListWithType(mTypeFilter);
        } else if (id == R.id.nav_schedule_type_entertainment) {
            Log.v(LOG_TAG, "onNavigationItemSelected(): nav_schedule_type_entertainment");
            mTypeFilter = ScheduleModel.SCHEDULE_TYPE_ENTERTAINMENT;
            replaceScheduleListWithType(mTypeFilter);
        } else if (id == R.id.nav_date) {
            Log.v(LOG_TAG, "onNavigationItemSelected(): nav_date");
            mTypeFilter = ScheduleModel.SCHEDULE_TYPE_DATE;
            replaceScheduleListWithType(mTypeFilter);
        } else if (id == R.id.nav_settings) {
            Log.v(LOG_TAG, "onNavigationItemSelected(): nav_settings");
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_schedule_list, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        setupSearchView(searchItem);

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.fab)
    public void pickStartDate(View view) {
        Intent intent = new Intent(ScheduleListActivity.this, AddScheduleActivity.class);
        startActivity(intent);
    }

    @Override
    public void onItemSelected(long id, ScheduleAdapter.ViewHolder vh) {
        Log.v(LOG_TAG, "onItemSelected(): id = " + id);

        if (mTwoPane) {
            ScheduleDetailFragment fragment = ScheduleDetailFragment.newInstance(id);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.schedule_detail_container, fragment, DETAIL_FRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, ScheduleDetailActivity.class);
            intent.putExtra(ScheduleDetailActivity.SCHEDULE_ID, id);
            startActivity(intent);
        }
    }

    @Override
    public void updateSuggestions(List<ScheduleSuggestionModel> suggestions) {
        mSuggestionAdapter.swapSuggestions(suggestions);
    }

    @Override
    public void updateSearchText(String query) {
        mSearchView.setQuery(query, true);
    }

    @Override
    public Context getViewContext() {
        return this;
    }

    private void initialize() {
        UseCase useCase = new GetAllScheduleSuggestion();
        ScheduleSuggestionModelDataMapper mapper = new ScheduleSuggestionModelDataMapper();
        mPresenter = new SearchSuggestionPresenterImpl(useCase, mapper);

        mPresenter.setView(this);
        mPresenter.initialize();
    }

    private void replaceScheduleListWithType(String scheduleType) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);

        ScheduleListFragment fragment = ScheduleListFragment.newInstanceByType(scheduleType);
        ft.replace(R.id.schedule_list_container, fragment).commit();

        mFab.show();
    }

    private void replaceScheduleListWithSearchResult(String query) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);

        ScheduleListFragment fragment = ScheduleListFragment.newInstanceByQuery(query);
        ft.replace(R.id.schedule_list_container, fragment, SEARCH_RESULT_FRAGMENT_TAG).commit();

        mFab.hide();
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.v(LOG_TAG, "handleIntent(): query = " + query);

            mPresenter.saveRecent(query);

            replaceScheduleListWithSearchResult(query);
        }
    }

    private void setupSearchView(MenuItem searchItem) {
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        MenuItemCompat.setOnActionExpandListener(searchItem,
                new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem menuItem) {
                        mFab.hide();
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                        Log.v(LOG_TAG, "onMenuItemActionCollapse()");
                        replaceScheduleListWithType(mTypeFilter);
                        return true;
                    }
                });

        mSuggestionAdapter = new SearchSuggestionAdapter(this, null, 0);
        mSearchView.setSuggestionsAdapter(mSuggestionAdapter);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mPresenter.requestSuggestion(newText);
                return false;
            }
        });

        mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                mPresenter.onSuggestionSelected(position);
                return true;
            }
        });
    }

    public class ScheduleObserver extends ContentObserver{
        public static final String TAG = "ScheduleObserver";

        private ContentResolver mContentResolver;
        private long mTotalSchedule;
        private long mDoneSchedule;
        private String mTotalScheduleMenuTitle = getResources().getString(R.string.schedule_type_all);
        private String mDoneScheduleMenuTitle = getResources().getString(R.string.schedule_done);


        public ScheduleObserver(Context context,Handler handler){
            super(handler);

            mContentResolver = getContentResolver();

            String selection = ScheduleContract.ScheduleEntry.COLUMN_IS_DONE + " = ?";
            String args[] = {ScheduleModel.UNDONE};
            Cursor cursor = mContentResolver.query(ScheduleContract.ScheduleEntry.CONTENT_URI,
                    null,selection,args,null);
            //cursor.moveToFirst();
            mTotalSchedule = cursor.getCount();

            String selection = ScheduleContract.ScheduleEntry.COLUMN_IS_DONE + " = ?";
            String args[] = {ScheduleModel.DONE};
            cursor = mContentResolver.query(ScheduleContract.ScheduleEntry.CONTENT_URI,
                    null,selection,args,null);

            mDoneSchedule = cursor.getCount();

            updateDrawerView();
        }

        @Override
        public void onChange(boolean selfChange){
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange,Uri uri){
            Log.v(TAG, "onChange: uri = " + uri);

            String selection = ScheduleContract.ScheduleEntry.COLUMN_IS_DONE + " = ?";
            String args[] = {ScheduleModel.UNDONE};
            Cursor cursor = mContentResolver.query(ScheduleContract.ScheduleEntry.CONTENT_URI,
                    null,selection,args,null);
            //cursor.moveToFirst();
            mTotalSchedule = cursor.getCount();

            selection = ScheduleContract.ScheduleEntry.COLUMN_IS_DONE + " = ?";
            args[] = {ScheduleModel.DONE};
            cursor = mContentResolver.query(ScheduleContract.ScheduleEntry.CONTENT_URI,
                    null,selection,args,null);

            mDoneSchedule = cursor.getCount();

            updateDrawerView();
        }

        private void updateDrawerView(){
            NavigationView view = (NavigationView)findViewById(R.id.nav_view);
            Menu menu = view.getMenu();

            menu.findItem(R.id.nav_schedule_type_all).setTitle(mTotalScheduleMenuTitle
                    + "(" + mTotalSchedule + ")");
            menu.findItem(R.id.nav_done).setTitle(mDoneScheduleMenuTitle
                    + "(" + mDoneSchedule + ")");
        }
    }

    public void registerObserver(){
        Log.v(LOG_TAG,"registerObserver");

        mScheduleObserver = new ScheduleObserver(this,new Handler());
        getContentResolver().registerContentObserver(ScheduleContract.ScheduleEntry.CONTENT_URI,
                true,mScheduleObserver);
    }

    public void unregisterObserver(){
        Log.v(LOG_TAG, "unregisterObserver");

        getContentResolver().unregisterContentObserver(mScheduleObserver);
    }
}
