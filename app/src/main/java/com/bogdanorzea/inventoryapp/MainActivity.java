package com.bogdanorzea.inventoryapp;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bogdanorzea.inventoryapp.data.InventoryContract.ProductEntry;

public class MainActivity extends AppCompatActivity {
    private ProductCursorAdaptor mProductCursorAdaptor;

    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String[] projection = {
                    ProductEntry._ID,
                    ProductEntry.COLUMN_PRODUCT_NAME,
                    ProductEntry.COLUMN_PRICE,
                    ProductEntry.COLUMN_QUANTITY
            };

            // Return a CursorLoader that will take care of creating a Cursor for the data being displayed.
            return new CursorLoader(getBaseContext(), ProductEntry.CONTENT_URI, projection, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mProductCursorAdaptor.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mProductCursorAdaptor.swapCursor(null);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set adaptor for the product list view
        mProductCursorAdaptor = new ProductCursorAdaptor(this, null);

        ListView mProductListView = (ListView) findViewById(R.id.product_list);
        mProductListView.setAdapter(mProductCursorAdaptor);

        // Set empty view for the product list
        TextView emptyView = (TextView) findViewById(R.id.empty_view);
        mProductListView.setEmptyView(emptyView);

        mProductListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent editIntent = new Intent(getBaseContext(), ProductEditorActivity.class);

                Uri data = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);
                editIntent.setData(data);

                startActivity(editIntent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });

        // Prepare the loader.  Either re-connect with an existing one or start a new one.
        getLoaderManager().initLoader(0, null, mLoaderCallbacks);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_all:
                askDeleteAll();
                return true;
            case R.id.action_add:
                startActivity(new Intent(getBaseContext(), ProductEditorActivity.class));
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void askDeleteAll() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_all_message);
        builder.setPositiveButton(R.string.alert_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User confirmed product deletion
                if (dialog != null) {
                    deleteAllProduct();
                }
            }
        });
        builder.setNegativeButton(R.string.alert_no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // The user dismissed the dialog
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    private void deleteAllProduct() {
        int deletedRows = getContentResolver().delete(ProductEntry.CONTENT_URI, null, null);

        if (deletedRows == 0) {
            Toast.makeText(this, R.string.main_delete_all_failed, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.main_delete_all_successful, Toast.LENGTH_SHORT).show();
        }
    }
}
