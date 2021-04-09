package com.lubenard.oring_reminder.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lubenard.oring_reminder.custom_components.CustomListAdapter;
import com.lubenard.oring_reminder.DbManager;
import com.lubenard.oring_reminder.R;
import com.lubenard.oring_reminder.custom_components.RingModel;
import com.lubenard.oring_reminder.utils.Utils;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

public class MainFragment extends Fragment {

    public static final String TAG = "MainFragment";

    // We can set thoses variables as static, because we know the view is going to be created
    private static ArrayList<RingModel> dataModels;
    private static DbManager dbManager;
    private static CustomListAdapter adapter;
    private static ListView listView;
    private static Context context;
    private static boolean orderEntryByDesc = true;
    private static TextView statLastDayTextview;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FloatingActionButton fab = view.findViewById(R.id.fab);
        listView = view.findViewById(R.id.main_list);
        Toolbar toolbar = view.findViewById(R.id.main_toolbar);

        dataModels = new ArrayList<>();

        dbManager = new DbManager(getContext());
        context = getContext();

        Log.d(TAG, "DB version is: " + dbManager.getVersion());

        statLastDayTextview = view.findViewById(R.id.header_last_day);
        //statLastDayTextview;

        fab.setOnClickListener(view12 -> actionOnPlusButton(false));

        fab.setOnLongClickListener(view1 -> {
            actionOnPlusButton(true);
            return true;
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                RingModel dataModel= dataModels.get(i);
                Log.d(TAG, "Element " + dataModel.getId());
                EntryDetailsFragment fragment = new EntryDetailsFragment();
                Bundle bundle = new Bundle();
                bundle.putLong("entryId", dataModel.getId());
                fragment.setArguments(bundle);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(android.R.id.content, fragment, null)
                        .addToBackStack(null).commit();
            }
        });

        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_settings:
                    // Navigate to settings screen
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(android.R.id.content, new SettingsFragment(), null)
                            .addToBackStack(null).commit();
                    return true;
                case R.id.action_reload_datas:
                    updateElementList();
                    return true;
                case R.id.action_sort_entrys:
                    orderEntryByDesc = !orderEntryByDesc;
                    Toast.makeText(context, context.getString((orderEntryByDesc) ? R.string.ordered_by_desc : R.string.not_ordered_by_desc),Toast.LENGTH_SHORT).show();
                    updateElementList();
                    return true;
                default:
                    return false;
            }
        });
    }

    private void actionOnPlusButton(boolean isLongClick) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String action = sharedPreferences.getString("ui_action_on_plus_button", "default");

        if (isLongClick) {
            if (action.equals("default")) {
                createNewEntry();
            } else {
                Toast.makeText(getContext(), "Session started at: " + Utils.getdateFormatted(new Date()), Toast.LENGTH_SHORT).show();
                EditEntryFragment.setUpddateMainList(true);
                new EditEntryFragment(getContext()).insertNewEntry(Utils.getdateFormatted(new Date()), false);
            }
        } else {
            if (action.equals("default")) {
                Toast.makeText(getContext(), "Session started at: " + Utils.getdateFormatted(new Date()), Toast.LENGTH_SHORT).show();
                EditEntryFragment.setUpddateMainList(true);
                new EditEntryFragment(getContext()).insertNewEntry(Utils.getdateFormatted(new Date()), false);
            } else {
                createNewEntry();
            }
        }
    }

    /**
     * Update the listView by fetching all elements from the db
     */
    public static void updateElementList() {
        Log.d(TAG, "updated main Listview");
        dataModels.clear();
        LinkedHashMap<Integer, RingModel> entrysDatas = dbManager.getAllDatasForMainList(orderEntryByDesc);
        for (LinkedHashMap.Entry<Integer, RingModel> oneElemData : entrysDatas.entrySet())
            dataModels.add(oneElemData.getValue());
        adapter = new CustomListAdapter(dataModels, context);
        listView.setAdapter(adapter);
        recomputeLastWearingTime();
    }

    private static void recomputeLastWearingTime() {
        int totalTimeLastDay = 0;
        Calendar calendar = Calendar.getInstance();
        String todayDate = Utils.getdateFormatted(calendar.getTime());
        calendar.add(Calendar.HOUR_OF_DAY, -24);
        String last24Hours = Utils.getdateFormatted(calendar.getTime());
        Log.d(TAG, "Computing last 24 hours: interval is between: " + last24Hours + " and " + todayDate);
        for (int i = 0; i != dataModels.size(); i++) {
            if (dataModels.get(i).getIsRunning() == 0) {
                // If the session is inside the born
                if (Utils.getDateDiff(last24Hours, dataModels.get(i).getDatePut(), TimeUnit.MINUTES) > 0 &&
                        Utils.getDateDiff(dataModels.get(i).getDateRemoved(), todayDate, TimeUnit.MINUTES) > 0) {
                    Log.d(TAG, "entry at index " + i + " is added: " + dataModels.get(i).getTimeWeared());
                    totalTimeLastDay += dataModels.get(i).getTimeWeared();
                } else if (Utils.getDateDiff(last24Hours, dataModels.get(i).getDatePut(), TimeUnit.MINUTES) <= 0 &&
                        Utils.getDateDiff(last24Hours, dataModels.get(i).getDateRemoved(),  TimeUnit.MINUTES) > 0) {
                    Log.d(TAG, "entry at index " + i + " is between the born: " + Utils.getDateDiff(last24Hours, dataModels.get(i).getDateRemoved(), TimeUnit.MINUTES));
                    totalTimeLastDay += Utils.getDateDiff(last24Hours, dataModels.get(i).getDateRemoved(), TimeUnit.MINUTES);
                }
            } else {
                if (Utils.getDateDiff(last24Hours, dataModels.get(i).getDatePut(), TimeUnit.MINUTES) > 0) {
                    Log.d(TAG, "running entry at index " + i + " is added: " +Utils.getDateDiff(dataModels.get(i).getDatePut(), todayDate, TimeUnit.MINUTES));
                    totalTimeLastDay += Utils.getDateDiff(dataModels.get(i).getDatePut(), todayDate, TimeUnit.MINUTES);
                } else if (Utils.getDateDiff(last24Hours, dataModels.get(i).getDatePut(), TimeUnit.MINUTES) <= 0) {
                    Log.d(TAG, "running entry at index " + i + " is between the born: " + Utils.getDateDiff(last24Hours, Utils.getdateFormatted(new Date()), TimeUnit.MINUTES));
                    totalTimeLastDay += Utils.getDateDiff(last24Hours, Utils.getdateFormatted(new Date()), TimeUnit.MINUTES);
                }
            }
        }
        Log.d(TAG, "Computed last 24 hours is: " + totalTimeLastDay + "mn");
        statLastDayTextview.setText(context.getString(R.string.last_day_string_header) + String.format("%dh%02dm", totalTimeLastDay / 60, totalTimeLastDay % 60));
    }

    /**
     * Launch the new Entry fragment, and specify we do not want to update a entry
     */
    private void createNewEntry() {
        EditEntryFragment fragment = new EditEntryFragment(getContext());
        Bundle bundle = new Bundle();
        bundle.putLong("entryId", -1);
        fragment.setArguments(bundle);
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment, null)
                .addToBackStack(null).commit();
    }

    /**
     * Each time the app is resumed, fetch new entry
     */
    @Override
    public void onResume() {
        super.onResume();
        updateElementList();
    }
}