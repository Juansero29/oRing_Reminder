package com.lubenard.oring_reminder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

public class DebugFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.debug_menu, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.about_toolbar);
        Button buttonSendNotif = view.findViewById(R.id.debug_send_notif);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get back to last fragment in the stack
                getActivity().getSupportFragmentManager().popBackStackImmediate();
            }
        });

        buttonSendNotif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.sendNotificationWithQuickAnswer(getContext(), "This is a test notification",
                        "No entry is affected by this notification",
                        R.drawable.baseline_done_24, -1);
                Toast.makeText(getContext(), "Just sent a manual notif. Do not worry, no entry will be affected.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
