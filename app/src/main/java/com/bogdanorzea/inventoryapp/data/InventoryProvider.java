package com.bogdanorzea.inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.bogdanorzea.inventoryapp.data.InventoryContract.ProductEntry;

/**
 * ContentProvider for Inventory database
 */
public class InventoryProvider extends ContentProvider {
    private static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    private static final int PRODUCTS = 100;
    private static final int PRODUCT_ID = 101;

    // UriMatcher object to match a content URI to a corresponding code.
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The content URI of the form "content://com.bogdanorzea.inventoryapp/products" will map
        // to the integer code {@link #PRODUCTS}.
        // This URI is used to provide access to MULTIPLE rows of the products table.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCTS, PRODUCTS);

        // The content URI of the form "content://com.bogdanorzea.inventoryapp/products/#" will map
        // to the integer code {@link #PRODUCT_ID}. T
        // his URI is used to provide access to ONE single row of the products table.
        // In this case, the "#" wildcard is used where "#" can be  substituted for an integer.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCTS + "/#", PRODUCT_ID);
    }

    // Database helper object handle
    private InventoryDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new InventoryDbHelper(getContext());

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final int match = sUriMatcher.match(uri);

        // Create and/or open a database that will be used for reading
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor;
        switch (match) {
            case PRODUCTS:
                cursor = db.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case PRODUCT_ID:
                // For the PRODUCT_ID code, extract out the ID from the URI.
                selection = ProductEntry._ID + "=?";

                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                cursor = db.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI to the Cursor, so we know what content URI the Cursor was created for.
        // If the data at this URI changes, we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor containing the queried data
        return cursor;
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return ProductEntry.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return ProductEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return insertProduct(uri, values);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    private Uri insertProduct(Uri uri, ContentValues values) {
        // Check that the name is not null
        String name = values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Product requires a name");
        }

        // No need to check the description, any value is valid (including null).

        // Check that the price is not null and positive
        Double price = values.getAsDouble(ProductEntry.COLUMN_PRICE);
        if (price == null || price < 0) {
            throw new IllegalArgumentException("Invalid product price");
        }

        // Check that the quantity is not null and positive
        Integer quantity = values.getAsInteger(ProductEntry.COLUMN_QUANTITY);
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Invalid product quantity");
        }

        // No need to check the photo, any value is valid (including null).

        // Check that the supplier is not null
        String supplier = values.getAsString(ProductEntry.COLUMN_SUPPLIER);
        if (supplier == null) {
            throw new IllegalArgumentException("Product requires a supplier name");
        }

        // Check that the supplier is not null
        String supplierEmail = values.getAsString(ProductEntry.COLUMN_SUPPLIER_EMAIL);
        if (supplierEmail == null) {
            throw new IllegalArgumentException("Product requires a supplier e-mail");
        }

        // Create and/or open a database that will be used for reading and writing
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Insert a new product into the products database table with the given ContentValues
        long id = db.insert(ProductEntry.TABLE_NAME, null, values);

        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the products content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID appended to the end of it
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Create and/or open a database that will be used for reading and writing
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        switch (match) {
            case PRODUCTS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PRODUCT_ID:
                // Delete a single row given by the ID in the URI
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalStateException("Deletion is not supported for " + uri);
        }

        // Notify all listeners that the data has changed for the product content URI
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                // Update all rows that match the selection and selection args
                return updateProducts(uri, values, selection, selectionArgs);
            case PRODUCT_ID:
                // Update a single row given by the ID in the URI
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateProducts(uri, values, selection, selectionArgs);
            default:
                throw new IllegalStateException("Update is not supported for " + uri);
        }
    }

    private int updateProducts(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // If the COLUMN_PRODUCT_NAME key is present, check that the name value is not null.
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_NAME)) {
            String name = values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Product requires a name");
            }
        }

        // If the COLUMN_PRICE key is present, check that the value is not null and positive.
        if (values.containsKey(ProductEntry.COLUMN_PRICE)) {
            Double price = values.getAsDouble(ProductEntry.COLUMN_PRICE);
            if (price == null || price < 0) {
                throw new IllegalArgumentException("Invalid product price");
            }
        }

        // If the COLUMN_PRICE key is present, check that the value is not null and positive.
        if (values.containsKey(ProductEntry.COLUMN_QUANTITY)) {
            Integer quantity = values.getAsInteger(ProductEntry.COLUMN_QUANTITY);
            if (quantity == null || quantity < 0) {
                throw new IllegalArgumentException("Invalid product quantity");
            }
        }

        // If the COLUMN_SUPPLIER key is present, check that the supplier is not null
        if (values.containsKey(ProductEntry.COLUMN_SUPPLIER)) {
            String supplier = values.getAsString(ProductEntry.COLUMN_SUPPLIER);
            if (supplier == null) {
                throw new IllegalArgumentException("Product requires a supplier name");
            }
        }

        // If the COLUMN_SUPPLIER_EMAIL key is present, check that the email is not null
        if (values.containsKey(ProductEntry.COLUMN_SUPPLIER_EMAIL)) {
            String supplierEmail = values.getAsString(ProductEntry.COLUMN_SUPPLIER_EMAIL);
            if (supplierEmail == null) {
                throw new IllegalArgumentException("Product requires a supplier e-mail");
            }
        }

        // Create and/or open a database that will be used for reading and writing
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Update a product into the database with the given content values
        int count = database.update(ProductEntry.TABLE_NAME, values, selection, selectionArgs);

        if (count != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }
}
