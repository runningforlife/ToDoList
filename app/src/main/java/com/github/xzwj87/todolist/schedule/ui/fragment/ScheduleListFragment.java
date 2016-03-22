package com.github.xzwj87.todolist.schedule.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.xzwj87.todolist.R;
import com.github.xzwj87.todolist.schedule.interactor.GetScheduleList;
import com.github.xzwj87.todolist.schedule.interactor.QueryUseCase;
import com.github.xzwj87.todolist.schedule.interactor.SearchSchedule;
import com.github.xzwj87.todolist.schedule.interactor.mapper.ScheduleModelDataMapper;
import com.github.xzwj87.todolist.schedule.presenter.ScheduleListPresenter;
import com.github.xzwj87.todolist.schedule.presenter.ScheduleListPresenterImpl;
import com.github.xzwj87.todolist.schedule.ui.ScheduleListView;
import com.github.xzwj87.todolist.schedule.ui.adapter.ScheduleAdapter;
import com.github.xzwj87.todolist.schedule.ui.model.ScheduleModel;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ScheduleListFragment extends Fragment implements
        ScheduleAdapter.DataSource, ScheduleListView {
    private static final String LOG_TAG = ScheduleListFragment.class.getSimpleName();

    private static final String SCHEDULE_TYPE = "schedule_type";
    private static final String QUERY = "query";

    private String mScheduleType;
    private Callbacks mCallbacks = sDummyCallbacks;
    private ScheduleAdapter mScheduleAdapter;
    private ScheduleListPresenter mScheduleListPresenter;
    private boolean mIsSearchMode = false;
    private String mQuery;

    @Bind(R.id.rv_schedule_list) RecyclerView mRvScheduleList;

    public interface Callbacks {
        void onItemSelected(long id, ScheduleAdapter.ViewHolder vh);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(long id, ScheduleAdapter.ViewHolder vh) { }
    };

    public ScheduleListFragment() {}

    public static ScheduleListFragment newInstanceByType(String scheduleType) {
        ScheduleListFragment fragment = new ScheduleListFragment();

        Bundle args = new Bundle();
        args.putString(SCHEDULE_TYPE, scheduleType);
        fragment.setArguments(args);

        return fragment;
    }

    public static ScheduleListFragment newInstanceByQuery(String query) {
        ScheduleListFragment fragment = new ScheduleListFragment();

        Bundle args = new Bundle();
        args.putString(QUERY, query);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_schedule_list, container, false);
        ButterKnife.bind(this, rootView);

        Bundle arguments = getArguments();
        if (arguments != null) {
            if (arguments.containsKey(SCHEDULE_TYPE)) {
                mScheduleType = arguments.getString(SCHEDULE_TYPE);
                Log.v(LOG_TAG, "onCreateView(): mScheduleType = " + mScheduleType);
            } else  if (arguments.containsKey(QUERY)) {
                mIsSearchMode = true;
                mQuery = arguments.getString(QUERY);
                Log.v(LOG_TAG, "onCreateView(): mQuery = " + mQuery);
            }

        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initialize();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!(context instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onResume() {
        super.onResume();
        mScheduleListPresenter.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScheduleListPresenter.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mScheduleListPresenter.destroy();
    }

    @Override
    public ScheduleModel getItemAtPosition(int position) {
        return mScheduleListPresenter.getScheduleAtPosition(position);
    }

    @Override
    public int getItemCount() {
        return mScheduleListPresenter.getScheduleItemCount();
    }

    @Override
    public void renderScheduleList() {
        Log.v(LOG_TAG, "renderScheduleList()");
        mScheduleAdapter.notifyDataSetChanged();
    }

    private void initialize() {
        QueryUseCase useCase;
        if (mIsSearchMode) {
            useCase = new SearchSchedule(mQuery);
        } else {
            if (mScheduleType != null) {
                useCase = new GetScheduleList(GetScheduleList.SORT_BY_START_DATE_ASC,
                        mScheduleType);
            } else {
                useCase = new GetScheduleList(GetScheduleList.SORT_BY_START_DATE_ASC);
            }
        }

        ScheduleModelDataMapper mapper = new ScheduleModelDataMapper();
        mScheduleListPresenter = new ScheduleListPresenterImpl(useCase, mapper);
        mScheduleListPresenter.setView(this);

        setupRecyclerView();

        loadScheduleListData();
    }

    private void loadScheduleListData() {
        mScheduleListPresenter.initialize();
    }

    private void setupRecyclerView() {
        mScheduleAdapter = new ScheduleAdapter(this);
        mScheduleAdapter.setOnItemClickListener(new ScheduleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, ScheduleAdapter.ViewHolder vh) {
                long id = mScheduleListPresenter.getScheduleAtPosition(position).getId();
                Log.v(LOG_TAG, "onItemClick(): position = " + position + ", id = " + id);

                mCallbacks.onItemSelected(id, vh);
            }
        });
        mRvScheduleList.setAdapter(mScheduleAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRvScheduleList.setLayoutManager(layoutManager);

        mRvScheduleList.setHasFixedSize(true);
    }


}
