package com.pagecall.sample;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.pagecall.PagecallWebView;

public class HomeFragment extends Fragment {
    private EditText roomIdInput;
    private EditText accessTokenInput;
    private EditText queryInput;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        roomIdInput = view.findViewById(R.id.room_id_input);
        accessTokenInput = view.findViewById(R.id.access_token_input);
        queryInput = view.findViewById(R.id.query_input);
        roomIdInput.setText("65f2abebd8de5b3269c0e6b2");
        accessTokenInput.setText("lPamRwagouVXtjMsDidjwkzZXLgC1MoV");
        queryInput.setText("logLevel=0");

        view.findViewById(R.id.meet_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchPagecall(PagecallWebView.PagecallMode.MEET);
            }
        });

        view.findViewById(R.id.replay_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchPagecall(PagecallWebView.PagecallMode.REPLAY);
            }
        });
    }

    private void launchPagecall(PagecallWebView.PagecallMode mode) {
        String roomId = roomIdInput.getText().toString();
        String accessToken = accessTokenInput.getText().toString();
        String query = queryInput.getText().toString();

        HomeFragmentDirections.ActionHomeFragmentToPagecallFragment action =
                HomeFragmentDirections.actionHomeFragmentToPagecallFragment(mode.toString(), roomId, accessToken, query);
        NavHostFragment.findNavController(HomeFragment.this).navigate(action);
    }
}