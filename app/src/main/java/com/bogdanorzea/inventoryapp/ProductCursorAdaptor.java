package com.bogdanorzea.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.bogdanorzea.inventoryapp.data.InventoryContract.ProductEntry;

class ProductCursorAdaptor extends CursorAdapter {
    private final LayoutInflater inflater;

    public ProductCursorAdaptor(Context context, Cursor c) {
        super(context, c, 0);
        inflater = (LayoutInflater) context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(R.layout.product_row, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        // Get views to display information
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        TextView quantityTextView = (TextView) view.findViewById(R.id.quantity);

        // Get the cursor column names
        int idColumnIndex = cursor.getColumnIndex(ProductEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_QUANTITY);

        // Add information to views
        final int productId = cursor.getInt(idColumnIndex);
        nameTextView.setText(cursor.getString(nameColumnIndex));
        priceTextView.setText(Double.toString(cursor.getDouble(priceColumnIndex)));
        final int productQuantity = cursor.getInt(quantityColumnIndex);
        quantityTextView.setText(Integer.toString(productQuantity));

        // Add button action
        Button sellButton = (Button) view.findViewById(R.id.sell);
        sellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (productQuantity > 0) {
                    // Create Uri for this product
                    Uri uri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, productId);

                    // Add new quantity for product
                    ContentValues values = new ContentValues();
                    values.put(ProductEntry.COLUMN_QUANTITY, productQuantity - 1);

                    // Update the quantity
                    int updatedRows = context.getContentResolver().update(uri, values, null, null);
                    if (updatedRows > 0) {
                        Toast.makeText(context, R.string.sell_decrease_successfully, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, R.string.sell_decrease_error, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, R.string.sell_decrease_negative_error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
