package com.om.atomic.classes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String databaseName = "bookmarker.db";
    public static final int version = 1;

    public static final String BOOK_TABLE = "Book";

    public static final String B_ID = "book_id";
    public static final String B_TITLE = "title";
    public static final String B_AUTHOR = "author";
    public static final String B_IMAGE = "image_path";
    public static final String B_DATE_ADDED = "date_added";
    public static final String B_COLOR_CODE = "color_code";
    public static final String B_ORDER = "book_order";

    public static final String BOOKMARK_TABLE = "Bookmark";

    public static final String BM_ID = "bookmark_id";
    public static final String BM_BOOK_FOREIGN_KEY = "book_id";
    public static final String BM_NAME = "name";
    public static final String BM_PAGENUMBER = "page_number";
    public static final String BM_IMAGEPATH = "image_path";
    public static final String BM_DATE_ADDED = "date_added";
    public static final String BM_VIEWS = "views";
    public static final String BM_ORDER = "bookmark_order";
    public static final String BM_NOTE = "note";

    public static final String PARAM_TABLE = "Param";

    public static final String PRM_NUMBER = "number";
    public static final String PRM_STRINGVALUE = "stringValue";

    public DatabaseHelper(Context context) {
        super(context, databaseName, null, version);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @DebugLog
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String CREATE_BOOK_TABLE = "CREATE TABLE IF NOT EXISTS " + BOOK_TABLE
                + " (" + B_ID + " INTEGER PRIMARY KEY, " + B_TITLE + " TEXT, " + B_AUTHOR + " TEXT, " + B_IMAGE + " TEXT, " + B_DATE_ADDED + " TEXT, " + B_COLOR_CODE + " INTEGER, " + B_ORDER + " INTEGER)";

        String CREATE_BOOKMARK_TABLE = "CREATE TABLE IF NOT EXISTS " + BOOKMARK_TABLE
                + " (" + BM_ID + " INTEGER PRIMARY KEY, " + BM_BOOK_FOREIGN_KEY + " INTEGER, " + BM_NAME + " TEXT, " + BM_PAGENUMBER + " INTEGER, " + BM_IMAGEPATH + " TEXT, " + BM_DATE_ADDED + " TEXT, " + BM_ORDER + " INTEGER, " + BM_VIEWS + " INTEGER, " + BM_NOTE + " TEXT, FOREIGN KEY (" + BM_BOOK_FOREIGN_KEY + ") REFERENCES " + BOOK_TABLE + " (" + B_ID + ") ON DELETE CASCADE)";

        String CREATE_PARAM_TABLE = "CREATE TABLE " + PARAM_TABLE
                + " (id INTEGER PRIMARY KEY AUTOINCREMENT, " + PRM_NUMBER
                + " INTEGER, " + PRM_STRINGVALUE + " TEXT)";

        sqLiteDatabase.execSQL(CREATE_BOOK_TABLE);
        sqLiteDatabase.execSQL(CREATE_BOOKMARK_TABLE);
        sqLiteDatabase.execSQL(CREATE_PARAM_TABLE);

        /**
         * Initilize the Seen Books, Bookmarks and Create Book params (for Showcase Views in each)
         */
        initializeParam_ForNullness(sqLiteDatabase, 1);
        initializeParam_ForNullness(sqLiteDatabase, 2);
        initializeParam_ForNullness(sqLiteDatabase, 3);

        /**
         * Initialize the Enable Layout Animations value
         * Values other than the Coachmark Seens will start from 10 onwards
         */
        initializeParamToTrue(sqLiteDatabase, 10);
    }

    @DebugLog
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
    }

    public ArrayList<Book> getAllBooks(String sortBy) {
        SQLiteDatabase dbHandler = this.getReadableDatabase();
//        SQLiteDatabase dbHandler = SQLiteDatabase.openDatabase(Environment.getExternalStorageDirectory().getPath() + File.separator + "bookmarker.db", null, 0);

        ArrayList<Book> books = new ArrayList<>();

        String query;

        if (sortBy == null)
            query = "SELECT * FROM " + BOOK_TABLE + " ORDER BY " + B_ORDER;
        else
            query = "SELECT * FROM " + BOOK_TABLE + " ORDER BY " + sortBy;

        Cursor cursor = dbHandler.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Book book = new Book();
                book.setId(cursor.getInt(0));
                book.setTitle(cursor.getString(1));
                book.setAuthor(cursor.getString(2));
                book.setImagePath(cursor.getString(3));
                book.setDate_added(cursor.getString(4));
                book.setColorCode(cursor.getInt(5));
                book.setOrder(cursor.getInt(6));
                books.add(book);
            } while (cursor.moveToNext());
        }

        cursor.close();

        return books;
    }

    @DebugLog
    public int createBook(Book book) {
        SQLiteDatabase dbHandler = this.getWritableDatabase();
        ContentValues cv;

        cv = new ContentValues();
        cv.putNull(B_ID);
        cv.put(B_TITLE, book.getTitle());
        cv.put(B_AUTHOR, book.getAuthor());
        cv.put(B_IMAGE, book.getImagePath());
        cv.put(B_DATE_ADDED, book.getDate_added());
        cv.put(B_COLOR_CODE, book.getColorCode());
        cv.put(B_ORDER, getMax_BookOrder(dbHandler));

        return (int) dbHandler.insert(BOOK_TABLE, null, cv);
    }

    @DebugLog
    public int createSampleBook(Book book) {
        SQLiteDatabase dbHandler = this.getWritableDatabase();
        ContentValues cv;

        int db_insert_success_status = 0;

        if (checkIfDataExists(dbHandler, BOOK_TABLE, B_ID, String.valueOf(book.getId()))) {
            //Data does exist
            db_insert_success_status = 1;
        } else {
            //Data does not exist
            cv = new ContentValues();
            cv.put(B_ID, book.getId());
            cv.put(B_TITLE, book.getTitle());
            cv.put(B_AUTHOR, book.getAuthor());
            cv.put(B_IMAGE, book.getImagePath());
            cv.put(B_DATE_ADDED, book.getDate_added());
            cv.put(B_COLOR_CODE, book.getColorCode());
            cv.put(B_ORDER, getMax_BookOrder(dbHandler));

            dbHandler.insert(BOOK_TABLE, null, cv);
        }

        return db_insert_success_status;
    }

    public static boolean checkIfDataExists(SQLiteDatabase db, String tableName,
                                            String dbField, String fieldValue) {
        String Query = "SELECT " + dbField + " FROM " + tableName + " WHERE " + dbField + " = " + fieldValue;
        Cursor cursor = db.rawQuery(Query, null);
        return cursor.getCount() > 0;
    }

    @DebugLog
    public void updateBook(Book book) {
        SQLiteDatabase dbHandler = this.getWritableDatabase();

        ContentValues newValues = new ContentValues();

        String[] args = new String[]{String.valueOf(book.getId())};

        newValues.put(B_TITLE, book.getTitle());
        newValues.put(B_AUTHOR, book.getAuthor());
        newValues.put(B_DATE_ADDED, book.getDate_added());
        newValues.put(B_ORDER, book.getOrder());

        dbHandler.update(BOOK_TABLE, newValues, B_ID + "= ?", args);
    }

    @DebugLog
    public void deleteBook(int book_id) {
        SQLiteDatabase dbHandler = this.getWritableDatabase();

        //Remove all the bookmarks that belonged to this book and are still stored on disk as images
        //Do this BEFORE deleting the books because ON DELETE CASCADE
        List<Bookmark> bookmarks = getAllBookmarks(book_id, null);
        for (Bookmark bookmark : bookmarks) {
            Helper_Methods.delete_image_from_disk(bookmark.getImage_path());
        }

        String table = BOOK_TABLE;
        String whereClause = B_ID + " = ?";
        String[] whereArgs = new String[]{String.valueOf(book_id)};
        dbHandler.delete(table, whereClause, whereArgs);

    }

    public ArrayList<Bookmark> getAllBookmarks(int book_id, String sortBy) {
        SQLiteDatabase dbHandler = this.getReadableDatabase();

        ArrayList<Bookmark> bookmarks = new ArrayList<>();

        String query;

        if (sortBy == null)
            query = "SELECT * FROM " + BOOKMARK_TABLE + " WHERE " + BM_BOOK_FOREIGN_KEY + " = " + book_id + " ORDER BY " + BM_ORDER;
        else
            query = "SELECT * FROM " + BOOKMARK_TABLE + " WHERE " + BM_BOOK_FOREIGN_KEY + " = " + book_id + " ORDER BY " + sortBy;

        Cursor cursor = dbHandler.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Bookmark bookmark = new Bookmark();
                bookmark.setId(cursor.getInt(0));
                bookmark.setBookId(cursor.getInt(1));
                bookmark.setName(cursor.getString(2));
                bookmark.setPage_number(cursor.getInt(3));
                bookmark.setImage_path(cursor.getString(4));
                bookmark.setDate_added(cursor.getString(5));
                bookmark.setOrder(cursor.getInt(6));
                bookmark.setViews(cursor.getInt(7));
                bookmark.setNote(cursor.getString(8));
                bookmarks.add(bookmark);
            } while (cursor.moveToNext());
        }

        cursor.close();

        return bookmarks;
    }

    @DebugLog
    public ArrayList<Bookmark> searchAllBookmarks(int book_id, String likeText) {
        SQLiteDatabase dbHandler = this.getReadableDatabase();

        ArrayList<Bookmark> bookmarkResults = new ArrayList<>();

        String query = "SELECT * FROM " + BOOKMARK_TABLE + " WHERE " + BM_BOOK_FOREIGN_KEY + " = ? AND " + BM_NAME + " LIKE ? COLLATE NOCASE";

        Cursor cursor = dbHandler.rawQuery(query, new String[]{String.valueOf(book_id), "%" + likeText + "%"});

        if (cursor.moveToFirst()) {
            do {
                Bookmark bookmark = new Bookmark();
                bookmark.setId(cursor.getInt(0));
                bookmark.setBookId(cursor.getInt(1));
                bookmark.setName(cursor.getString(2));
                bookmark.setPage_number(cursor.getInt(3));
                bookmark.setImage_path(cursor.getString(4));
                bookmark.setDate_added(cursor.getString(5));
                bookmark.setOrder(cursor.getInt(6));
                bookmark.setViews(cursor.getInt(7));
                bookmark.setNote(cursor.getString(8));
                bookmarkResults.add(bookmark);
            } while (cursor.moveToNext());
        }

        cursor.close();

        return bookmarkResults;
    }

    @DebugLog
    public void createBookmark(Bookmark bookmark, int book_id) {
        SQLiteDatabase dbHandler = this.getWritableDatabase();
        ContentValues cv;

        cv = new ContentValues();
        cv.putNull(BM_ID);
        cv.put(BM_BOOK_FOREIGN_KEY, book_id);
        cv.put(BM_NAME, bookmark.getName());
        cv.put(BM_PAGENUMBER, bookmark.getPage_number());
        cv.put(BM_IMAGEPATH, bookmark.getImage_path());
        cv.put(BM_DATE_ADDED, bookmark.getDate_added());
        cv.put(BM_ORDER, getMax_BookmarkOrder(dbHandler));
        cv.put(BM_VIEWS, 0);
        cv.put(BM_NOTE, "");

        dbHandler.insert(BOOKMARK_TABLE, null, cv);
    }

    @DebugLog
    public void createSampleBookmark(Bookmark bookmark, int book_id) {
        SQLiteDatabase dbHandler = this.getWritableDatabase();
        ContentValues cv;

        if (!checkIfDataExists(dbHandler, BOOKMARK_TABLE, BM_ID, String.valueOf(bookmark.getId()))) {
            cv = new ContentValues();
            cv.putNull(BM_ID);
            cv.put(BM_BOOK_FOREIGN_KEY, book_id);
            cv.put(BM_NAME, bookmark.getName());
            cv.put(BM_PAGENUMBER, bookmark.getPage_number());
            cv.put(BM_IMAGEPATH, bookmark.getImage_path());
            cv.put(BM_DATE_ADDED, bookmark.getDate_added());
            cv.put(BM_ORDER, getMax_BookmarkOrder(dbHandler));
            cv.put(BM_VIEWS, 0);
            cv.put(BM_NOTE, "");

            dbHandler.insert(BOOKMARK_TABLE, null, cv);
        }
    }

    @DebugLog
    public void updateBookmark(Bookmark bookmark) {
        SQLiteDatabase dbHandler = this.getWritableDatabase();

        ContentValues newValues = new ContentValues();

        String[] args = new String[]{String.valueOf(bookmark.getId())};

        newValues.put(BM_NAME, bookmark.getName());
        newValues.put(BM_PAGENUMBER, bookmark.getPage_number());
        newValues.put(BM_DATE_ADDED, bookmark.getDate_added());
        newValues.put(BM_IMAGEPATH, bookmark.getImage_path());
        newValues.put(BM_ORDER, bookmark.getOrder());
        newValues.put(BM_VIEWS, bookmark.getViews());
        newValues.put(BM_NOTE, bookmark.getNote());

        dbHandler.update(BOOKMARK_TABLE, newValues, BM_ID + "= ?", args);
    }

    @DebugLog
    public void update_BookmarkImage(int bookmark_id, String bookmarkImage) {
        SQLiteDatabase dbHandler = this.getWritableDatabase();
        ContentValues newValues = new ContentValues();

        String[] args = new String[]{String.valueOf(bookmark_id)};

        newValues.put(BM_IMAGEPATH, bookmarkImage);

        dbHandler.update(BOOKMARK_TABLE, newValues, BM_ID + " = ?", args);
    }

    @DebugLog
    public void update_BookmarkNote(int bookmark_id, String note) {
        SQLiteDatabase dbHandler = this.getWritableDatabase();
        ContentValues newValues = new ContentValues();

        String[] args = new String[]{String.valueOf(bookmark_id)};

        newValues.put(BM_NOTE, note);

        dbHandler.update(BOOKMARK_TABLE, newValues, BM_ID + " = ?", args);
    }

    @DebugLog
    public void deleteBookmark(int bookmark_id) {
        SQLiteDatabase dbHandler = this.getWritableDatabase();

        String table = BOOKMARK_TABLE;
        String whereClause = BM_ID + " = ?";
        String[] whereArgs = new String[]{String.valueOf(bookmark_id)};
        dbHandler.delete(table, whereClause, whereArgs);
    }

    @DebugLog
    public int getMax_BookOrder(SQLiteDatabase dbHandler) {
        Cursor cursor = dbHandler.rawQuery("SELECT MAX(" + B_ORDER + ") FROM " + BOOK_TABLE, null);

        int max_book_order = 0;

        if (cursor.moveToFirst())
            max_book_order = cursor.getInt(0) + 1;

        cursor.close();

        return max_book_order;
    }

    @DebugLog
    public int getMax_BookmarkOrder(SQLiteDatabase dbHandler) {
        Cursor cursor = dbHandler.rawQuery("SELECT MAX(" + BM_ORDER + ") FROM " + BOOKMARK_TABLE, null);

        int max_bookmark_order = 0;

        if (cursor.moveToFirst())
            max_bookmark_order = cursor.getInt(0) + 1;

        cursor.close();

        return max_bookmark_order;
    }

    @DebugLog
    public int getBookmarkViews(int bookmark_id) {
        SQLiteDatabase dbHandler = this.getReadableDatabase();
        Cursor cursor = dbHandler.rawQuery("SELECT " + BM_VIEWS + " FROM " + BOOKMARK_TABLE + " WHERE " + BM_ID + " = " + bookmark_id, null);

        int views = 0;

        if (cursor.moveToFirst())
            views = cursor.getInt(0);

        cursor.close();

        return views;
    }

    @DebugLog
    public String getBookmarkNote(int bookmark_id) {
        SQLiteDatabase dbHandler = this.getReadableDatabase();
        Cursor cursor = dbHandler.rawQuery("SELECT " + BM_NOTE + " FROM " + BOOKMARK_TABLE + " WHERE " + BM_ID + " = " + bookmark_id, null);

        String note = "";

        if (cursor.moveToFirst())
            note = cursor.getString(0);

        cursor.close();

        return note;
    }

    @DebugLog
    public void initializeParam_ForNullness(SQLiteDatabase db, int paramNumber) {
        ContentValues newValues = new ContentValues();

        newValues.putNull("id");
        newValues.put(PRM_NUMBER, paramNumber);
        newValues.putNull(PRM_STRINGVALUE);

        db.insert(PARAM_TABLE, null, newValues);
    }

    @DebugLog
    public void initializeParamToTrue(SQLiteDatabase db, int paramNumber) {
        ContentValues newValues = new ContentValues();

        newValues.putNull("id");
        newValues.put(PRM_NUMBER, paramNumber);
        newValues.put(PRM_STRINGVALUE, "True");

        db.insert(PARAM_TABLE, null, newValues);
    }

    @DebugLog
    public void reverseParamsTruths(int paramNumber, String paramValue) {
        SQLiteDatabase dbHandler = this.getWritableDatabase();
        ContentValues newValues = new ContentValues();

        String[] args = new String[]{String.valueOf(paramNumber)};

        newValues.put(PRM_STRINGVALUE, paramValue);

        dbHandler.update(PARAM_TABLE, newValues, PRM_NUMBER + " = ?", args);
    }

    @DebugLog
    public void updateParam(Param param) {
        SQLiteDatabase dbHandler = this.getWritableDatabase();
        ContentValues newValues = new ContentValues();

        String[] args = new String[]{String.valueOf(param.getNumber())};

        newValues.put(PRM_STRINGVALUE, param.getValue());

        dbHandler.update(PARAM_TABLE, newValues, PRM_NUMBER + " = ?", args);
    }

    @DebugLog
    public boolean getParam(SQLiteDatabase dbHandler, int paramNumber) {
        if (dbHandler == null)
            dbHandler = this.getReadableDatabase();

        String query = "SELECT " + PRM_STRINGVALUE + " FROM " + PARAM_TABLE
                + " WHERE " + PRM_NUMBER + " = " + paramNumber;
        Cursor cursor = dbHandler.rawQuery(query, null);

        boolean seen = false;

        try {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    seen = cursor.getString(0) != null && cursor.getString(0).equals("True");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (cursor != null) {
            cursor.close();
        }

        return seen;
    }
}
