package com.example.catprovider.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.catprovider.data.HotelContract.GuestEntry;


public class GuestProvider extends ContentProvider {

    public static final String TAG = GuestProvider.class.getSimpleName();

    /**
     * URI matcher code for the content URI for the guests table
     */
    private static final int GUESTS = 100;

    /**
     * URI matcher code for the content URI for a single guest in the guests table
     */
    private static final int GUEST_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // The content URI of the form "content://com.example.android.guests/guests" will map to the
        // integer code {@link #GUESTS}. This URI is used to provide access to MULTIPLE rows
        // of the guests table.
        sUriMatcher.addURI(HotelContract.CONTENT_AUTHORITY, HotelContract.PATH_GUESTS, GUESTS);

        // The content URI of the form "content://com.example.android.guests/guests/#" will map to the
        // integer code {@link #GUEST_ID}. This URI is used to provide access to ONE single row
        // of the guests table.
        //
        // In this case, the "#" wildcard is used where "#" can be substituted for an integer.
        // For example, "content://com.example.android.guests/guests/3" matches, but
        // "content://com.example.android.guests/guests" (without a number at the end) doesn't match.
        sUriMatcher.addURI(HotelContract.CONTENT_AUTHORITY, HotelContract.PATH_GUESTS + "/#", GUEST_ID);
    }

    /**
     * Database helper object
     */
    private HotelDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new HotelDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Получим доступ к базе данных для чтения
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // Курсор, содержащий результат запроса
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case GUESTS:
                // For the GUESTS code, query the guests table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the guests table.
                cursor = database.query(GuestEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case GUEST_ID:
                // For the GUEST_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.guests/guests/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = GuestEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // This will perform a query on the guests table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(GuestEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case GUESTS:
                return GuestEntry.CONTENT_LIST_TYPE;
            case GUEST_ID:
                return GuestEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case GUESTS:
                return insertGuest(uri, values);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case GUESTS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(GuestEntry.TABLE_NAME, selection, selectionArgs);
                //return database.delete(GuestEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case GUEST_ID:
                // Delete a single row given by the ID in the URI
                selection = GuestEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(GuestEntry.TABLE_NAME, selection, selectionArgs);
                //return database.delete(GuestEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case GUESTS:
                return updateGuest(uri, values, selection, selectionArgs);
            case GUEST_ID:
                // For the GUEST_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = GuestEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateGuest(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Insert a guest into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertGuest(Uri uri, ContentValues values) {
        // Check that the name is not null
        String name = values.getAsString(GuestEntry.COLUMN_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Guest requires a name");
        }

        // Check that the gender is valid
        Integer gender = values.getAsInteger(GuestEntry.COLUMN_GENDER);
        if (gender == null || !GuestEntry.isValidGender(gender)) {
            throw new IllegalArgumentException("Guest requires valid gender");
        }

        // If the age is provided, check that it's greater than or equal to 0 kg
        Integer age = values.getAsInteger(GuestEntry.COLUMN_AGE);
        if (age != null && age < 0) {
            throw new IllegalArgumentException("Guest requires valid age");
        }

        // No need to check the city, any value is valid (including null).

        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new guest with the given values
        long id = database.insert(GuestEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(TAG, "Failed to insert row for " + uri);
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    /**
     * Update guests in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more guests).
     * Return the number of rows that were successfully updated.
     */
    private int updateGuest(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // If the {@link GuestEntry#COLUMN_NAME} key is present,
        // check that the name value is not null.
        if (values.containsKey(GuestEntry.COLUMN_NAME)) {
            String name = values.getAsString(GuestEntry.COLUMN_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Guest requires a name");
            }
        }

        // If the {@link GuestEntry#COLUMN_GENDER} key is present,
        // check that the gender value is valid.
        if (values.containsKey(GuestEntry.COLUMN_GENDER)) {
            Integer gender = values.getAsInteger(GuestEntry.COLUMN_GENDER);
            if (gender == null || !GuestEntry.isValidGender(gender)) {
                throw new IllegalArgumentException("Guest requires valid gender");
            }
        }

        // If the {@link GuestEntry#COLUMN_AGE} key is present,
        // check that the age value is valid.
        if (values.containsKey(GuestEntry.COLUMN_AGE)) {
            // Check that the age is greater than or equal to 0
            Integer age = values.getAsInteger(GuestEntry.COLUMN_AGE);
            if (age != null && age < 0) {
                throw new IllegalArgumentException("Guest requires valid age");
            }
        }

        // No need to check the breed, any value is valid (including null).

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Otherwise, get writeable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(GuestEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated
        return rowsUpdated;
    }
}
