package com.bogdanorzea.inventoryapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bogdanorzea.inventoryapp.data.InventoryContract.ProductEntry;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ProductEditorActivity extends AppCompatActivity {
    private static final String LOG_TAG = ProductEditorActivity.class.getSimpleName();
    private static final int REQUEST_CODE = 1;
    private static final int LOADER_INITIALIZE = 0;
    private Uri mCurrentUri;

    private EditText nameEditText;
    private EditText descriptionEditText;
    private TextView quantityTextView;
    private EditText priceEditText;
    private EditText supplierEditText;
    private EditText supplierEmailEditText;
    private ImageView productImage;

    // Touch listener for the changes to the product
    private boolean mChanged = false;
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mChanged = true;
            return false;
        }
    };

    // Loader to fill the EditText fields
    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            // Projection for Cursor
            String[] projection = new String[]{
                    ProductEntry.COLUMN_PRODUCT_NAME,
                    ProductEntry.COLUMN_DESCRIPTION,
                    ProductEntry.COLUMN_QUANTITY,
                    ProductEntry.COLUMN_PRICE,
                    ProductEntry.COLUMN_SUPPLIER,
                    ProductEntry.COLUMN_SUPPLIER_EMAIL,
                    ProductEntry.COLUMN_IMAGE
            };

            // Return a Cursor for the data to be displayed
            return new CursorLoader(getBaseContext(), mCurrentUri, projection, null, null, null);
        }

        @Override
        public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor data) {
            // Get column indexes
            int nameColumnIndex = data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int descriptionColumnIndex = data.getColumnIndex(ProductEntry.COLUMN_DESCRIPTION);
            int quantityColumnIndex = data.getColumnIndex(ProductEntry.COLUMN_QUANTITY);
            int priceColumnIndex = data.getColumnIndex(ProductEntry.COLUMN_PRICE);
            int supplierColumnIndex = data.getColumnIndex(ProductEntry.COLUMN_SUPPLIER);
            int supplierEmailColumnIndex = data.getColumnIndex(ProductEntry.COLUMN_SUPPLIER_EMAIL);
            int photoColumnIndex = data.getColumnIndex(ProductEntry.COLUMN_IMAGE);

            if (data.moveToFirst()) {
                // Get data from cursor
                String name = data.getString(nameColumnIndex);
                String description = data.getString(descriptionColumnIndex);
                int quantity = data.getInt(quantityColumnIndex);
                double price = data.getDouble(priceColumnIndex);
                String supplier = data.getString(supplierColumnIndex);
                String supplierEmail = data.getString(supplierEmailColumnIndex);
                byte[] imgByte = data.getBlob(photoColumnIndex);


                // Set data to the corresponding EditText
                nameEditText.setText(name);
                descriptionEditText.setText(description);
                quantityTextView.setText(Integer.toString(quantity));
                productImage.setImageBitmap(BitmapFactory.decodeByteArray(imgByte, 0, imgByte.length));
                priceEditText.setText(Double.toString(price));
                supplierEditText.setText(supplier);
                supplierEmailEditText.setText(supplierEmail);
            }
        }

        @Override
        public void onLoaderReset(android.content.Loader<Cursor> loader) {
            // Clear the EditText inputs
            nameEditText.setText("");
            descriptionEditText.setText("");
            quantityTextView.setText("");
            priceEditText.setText("");
            productImage.setImageResource(R.drawable.image_not_found);
            supplierEditText.setText("");
            supplierEmailEditText.setText("");
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            new LoadImageAsyncTask().execute(data);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_editor);

        // Name
        nameEditText = (EditText) findViewById(R.id.edit_name);
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(nameEditText.getText().toString().trim())) {
                    nameEditText.setError(getString(R.string.editor_name_required));
                } else {
                    nameEditText.setError(null);
                }
            }
        });
        nameEditText.setOnTouchListener(mTouchListener);

        // Price
        priceEditText = (EditText) findViewById(R.id.edit_price);
        priceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(priceEditText.getText().toString().trim())) {
                    priceEditText.setError(getString(R.string.editor_price_required));
                } else {
                    priceEditText.setError(null);
                }
            }
        });
        priceEditText.setOnTouchListener(mTouchListener);

        // Quantity
        quantityTextView = (TextView) findViewById(R.id.edit_quantity);
        findViewById(R.id.decrease_quantity_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChanged = true;
                String currentQuantityString = quantityTextView.getText().toString();
                int currentQuantity = Integer.parseInt(currentQuantityString);
                if (currentQuantity > 0) {
                    quantityTextView.setText(Integer.toString(currentQuantity - 1));
                } else {
                    Toast.makeText(ProductEditorActivity.this, R.string.quantity_negative_error, Toast.LENGTH_SHORT).show();
                }
            }
        });
        findViewById(R.id.increase_quantity_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChanged = true;
                String currentQuantityString = quantityTextView.getText().toString();
                int currentQuantity = Integer.parseInt(currentQuantityString);
                if (currentQuantity < 999) {
                    quantityTextView.setText(Integer.toString(currentQuantity + 1));
                } else {
                    Toast.makeText(ProductEditorActivity.this, R.string.quantity_maximum_error, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Description
        descriptionEditText = (EditText) findViewById(R.id.edit_description);
        descriptionEditText.setOnTouchListener(mTouchListener);

        // Supplier
        supplierEditText = (EditText) findViewById(R.id.edit_supplier);
        supplierEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(supplierEditText.getText().toString().trim())) {
                    supplierEditText.setError(getString(R.string.editor_supplier_required));
                } else {
                    supplierEditText.setError(null);
                }
            }
        });
        supplierEditText.setOnTouchListener(mTouchListener);

        // Supplier e-mail
        supplierEmailEditText = (EditText) findViewById(R.id.edit_supplier_email);
        supplierEmailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(supplierEmailEditText.getText().toString().trim())) {
                    supplierEmailEditText.setError(getString(R.string.editor_supplier_email_required));
                } else if (!validEmail(supplierEmailEditText.getText().toString().trim())) {
                    supplierEmailEditText.setError(getString(R.string.editor_supplier_email_invalid));
                } else {
                    supplierEditText.setError(null);
                }
            }

            boolean validEmail(String email) {
                String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
                java.util.regex.Matcher m = p.matcher(email);
                return m.matches();
            }
        });
        supplierEmailEditText.setOnTouchListener(mTouchListener);

        // Order button
        Button orderButton = (Button) findViewById(R.id.order_button);
        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChanged = true;
                if (supplierEmailEditText.getError() == null && supplierEditText.getError() == null && nameEditText.getError() == null) {
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("text/plain");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{supplierEmailEditText.getText().toString()});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Order request");

                    StringBuilder emailBody = new StringBuilder();
                    emailBody.append("Hello ");
                    emailBody.append(supplierEditText.getText().toString());
                    emailBody.append(",");
                    emailBody.append('\n');
                    emailBody.append('\n');
                    emailBody.append("I would like to place an order for your product: ");
                    emailBody.append(nameEditText.getText().toString());
                    emailBody.append(".");

                    emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody.toString());
                    try {
                        startActivity(Intent.createChooser(emailIntent, "Send e-mail..."));
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(v.getContext(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(v.getContext(), "Please input valid supplier e-mail address", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Image
        productImage = (ImageView) findViewById(R.id.product_image);
        Button addImageButton = (Button) findViewById(R.id.add_image_button);
        addImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChanged = true;
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });

        // Check if the Editor was started with an Uri intent
        mCurrentUri = getIntent().getData();
        if (mCurrentUri != null) {
            setTitle(getString(R.string.editor_title_edit));
            getLoaderManager().initLoader(LOADER_INITIALIZE, null, mLoaderCallbacks);
        } else {
            setTitle(getString(R.string.editor_title_add));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // When inserting a new product, hide the "Delete" menu item.
        if (mCurrentUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        // Confirm discard changes dialog if product was modified
        if (mChanged) {
            askDiscardChanges();
        } else {
            super.onBackPressed();
            exitActivityWithAnimation();
        }
    }

    /**
     * Exits the EditorActivity with a sliding animation
     */
    private void exitActivityWithAnimation() {
        finish();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_insert:
                new InsertProductAsyncTask().execute();
                return true;
            case R.id.action_delete:
                askDeleteProduct();
                return true;
            case android.R.id.home:
                if (mChanged) {
                    askDiscardChanges();
                } else {
                    exitActivityWithAnimation();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Prompts user to confirm delete action
     */
    private void askDeleteProduct() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_message);
        builder.setPositiveButton(R.string.alert_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User confirmed product deletion
                if (dialog != null) {
                    deleteProduct();
                    finish();
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

    /**
     * Prompts user to confirm discarding changes
     */
    private void askDiscardChanges() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.discard_message);
        builder.setPositiveButton(R.string.alert_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User confirmed product update
                if (dialog != null) {
                    exitActivityWithAnimation();
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

    private void deleteProduct() {
        int deletedRows = getContentResolver().delete(mCurrentUri, null, null);

        if (deletedRows == 0) {
            Toast.makeText(this, R.string.editor_delete_failed, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.editor_delete_successful, Toast.LENGTH_SHORT).show();
        }
    }

    private class LoadImageAsyncTask extends AsyncTask<Intent, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Intent... params) {
            try {
                InputStream stream = getContentResolver().openInputStream(params[0].getData());
                return BitmapFactory.decodeStream(stream);
            } catch (FileNotFoundException e) {
                Log.e(LOG_TAG, "Error getting the image", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bmp) {
            productImage.setImageBitmap(bmp);
        }
    }

    private class InsertProductAsyncTask extends AsyncTask<Void, Integer, Boolean> {
        private String nameString;
        private String descriptionString;
        private String quantityString;
        private String priceString;
        private Bitmap bitmap;
        private String supplierString;
        private String supplierEmailString;

        @Override
        protected void onPreExecute() {
            // Get the strings from EditTexts
            nameString = nameEditText.getText().toString().trim();
            descriptionString = descriptionEditText.getText().toString().trim();
            quantityString = quantityTextView.getText().toString().trim();
            priceString = priceEditText.getText().toString().trim();
            supplierString = supplierEditText.getText().toString().trim();
            supplierEmailString = supplierEmailEditText.getText().toString().trim();

            // Get the picture bitmap
            bitmap = ((BitmapDrawable) productImage.getDrawable()).getBitmap();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                exitActivityWithAnimation();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Toast.makeText(ProductEditorActivity.this, values[0], Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Prevent adding products that do not have a valid name, quantity or price
            if (TextUtils.isEmpty(nameString) || TextUtils.isEmpty(quantityString) || TextUtils.isEmpty(priceString) ||
                    TextUtils.isEmpty(supplierString) || TextUtils.isEmpty(supplierEmailString)) {
                publishProgress(R.string.editor_insert_incomplete);
                return false;
            }

            // Convert Strings to the corresponding data types for price and quantity
            int quantity = 0;
            if (!TextUtils.isEmpty(quantityString)) {
                quantity = Integer.parseInt(quantityString);
            }
            double price = 0.0;
            if (!TextUtils.isEmpty(priceString)) {
                price = Double.parseDouble(priceString);
            }

            // Convert Bitmap to byte array and reduce quality of image to < 1MB
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            byte[] img = outputStream.toByteArray();

            // Map the values to the corresponding columns
            ContentValues values = new ContentValues();
            values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
            values.put(ProductEntry.COLUMN_DESCRIPTION, descriptionString);
            values.put(ProductEntry.COLUMN_QUANTITY, quantity);
            values.put(ProductEntry.COLUMN_PRICE, price);
            values.put(ProductEntry.COLUMN_IMAGE, img);
            values.put(ProductEntry.COLUMN_SUPPLIER, supplierString);
            values.put(ProductEntry.COLUMN_SUPPLIER_EMAIL, supplierEmailString);

            if (mCurrentUri == null) {
                // Insert the new row, returning the URI of the new row
                Uri newRowUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

                // Inform the user about the insertion status
                if (newRowUri != null) {
                    publishProgress(R.string.editor_insert_successfully);
                } else {
                    publishProgress(R.string.editor_insert_error);
                }
            } else {
                // Update the current product based on the URI
                int updatedRows = getContentResolver().update(mCurrentUri, values, null, null);

                // Inform the user about the update status
                if (updatedRows > 0) {
                    publishProgress(R.string.editor_update_successfully);
                } else {
                    publishProgress(R.string.editor_update_error);
                }
            }

            return true;
        }
    }
}
