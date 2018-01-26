package com.hermann.bussenliste;

import android.app.Activity;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A fragment representing a single Fine detail screen.
 * This fragment is either contained in a {@link FineListActivity}
 * in two-pane mode (on tablets) or a {@link FineDetailActivity}
 * on handsets.
 */
public class FineDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private Fine mItem;
    private DataSourceFine dataSourceFine;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FineDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dataSourceFine = new DataSourceFine(getContext());

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            String argument = getArguments().getString(ARG_ITEM_ID);
            mItem = dataSourceFine.getFine(Long.parseLong(argument));

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                appBarLayout.setTitle(getString(R.string.edit_fine));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fine_detail, container, false);

        if (mItem != null) {
            ((EditText) rootView.findViewById(R.id.fine_description)).setText(mItem.getDescription());
            ((EditText) rootView.findViewById(R.id.fine_description)).setSelection(mItem.getDescription().length());
            ((EditText) rootView.findViewById(R.id.fine_amount)).setText(Integer.toString(mItem.getAmount()));
        }

        return rootView;
    }
}
