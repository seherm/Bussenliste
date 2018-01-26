package com.hermann.bussenliste;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * An activity representing a list of Fines. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link FineDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class FineListActivity extends AppCompatActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private DataSourceFine dataSourceFine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fine_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCreateNewFineDialog();
            }
        });

        dataSourceFine = new DataSourceFine(this);

        if (findViewById(R.id.fine_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        View recyclerView = findViewById(R.id.fine_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(this, dataSourceFine.getAllFines(), mTwoPane));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    public static class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final FineListActivity mParentActivity;
        private final List<Fine> mValues;
        private final boolean mTwoPane;
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fine item = (Fine) view.getTag();
                if (mTwoPane) {
                    Bundle arguments = new Bundle();
                    arguments.putString(FineDetailFragment.ARG_ITEM_ID, Long.toString(item.getId()));
                    FineDetailFragment fragment = new FineDetailFragment();
                    fragment.setArguments(arguments);
                    mParentActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fine_detail_container, fragment)
                            .commit();
                } else {
                    Context context = view.getContext();
                    Intent intent = new Intent(context, FineDetailActivity.class);
                    intent.putExtra(FineDetailFragment.ARG_ITEM_ID, Long.toString(item.getId()));

                    context.startActivity(intent);
                }
            }
        };

        SimpleItemRecyclerViewAdapter(FineListActivity parent,
                                      List<Fine> fines,
                                      boolean twoPane) {
            mValues = fines;
            mParentActivity = parent;
            mTwoPane = twoPane;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fine_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mIdView.setText(mValues.get(position).getDescription());
            holder.mContentView.setText(String.format("%d .-", mValues.get(position).getAmount()));

            holder.itemView.setTag(mValues.get(position));
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mIdView;
            final TextView mContentView;

            ViewHolder(View view) {
                super(view);
                mIdView = (TextView) view.findViewById(R.id.id_text);
                mContentView = (TextView) view.findViewById(R.id.content);
            }
        }
    }

    private void showCreateNewFineDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater layoutInflater = LayoutInflater.from(FineListActivity.this);
        View promptView = layoutInflater.inflate(R.layout.input_dialog_new_fine, null);
        builder.setTitle(R.string.create_fine);
        builder.setView(promptView);
        final EditText descriptionBox = promptView.findViewById(R.id.fine_description);
        final EditText amountBox = promptView.findViewById(R.id.fine_amount);

        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fineDescription = String.valueOf(descriptionBox.getText());
                String fineAmount = String.valueOf(amountBox.getText());
                Boolean isEmptyDescriptionText = (descriptionBox.getText().toString().trim().isEmpty());
                Boolean isEmptyFineAmountText = (amountBox.getText().toString().trim().isEmpty());

                if (!dataSourceFine.hasFine(fineDescription) && !isEmptyDescriptionText && !isEmptyFineAmountText) {
                    dataSourceFine.createFine(fineDescription, Integer.parseInt(fineAmount));
                    View recyclerView = findViewById(R.id.fine_list);
                    assert recyclerView != null;
                    setupRecyclerView((RecyclerView) recyclerView);
                    dialog.dismiss();
                } else if (isEmptyDescriptionText) {
                    Toast.makeText(getApplicationContext(), R.string.empty_fine_description, Toast.LENGTH_LONG).show();
                } else if (isEmptyFineAmountText) {
                    Toast.makeText(getApplicationContext(), R.string.empty_fine_amount, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.already_added_fine, Toast.LENGTH_LONG).show();
                }
            }
        });

    }
}
