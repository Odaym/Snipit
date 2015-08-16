package com.om.snipit.classes;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

/**
 * Database helper class used to manage the creation and upgrading of your database. This class also usually provides
 * the DAOs used by the other classes.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String DATABASE_NAME = "Snipit.db";

    private static final int DATABASE_VERSION = 1;

    private RuntimeExceptionDao<Book, Integer> bookRuntimeDAO = null;
    private RuntimeExceptionDao<Bookmark, Integer> bookmarkRuntimeDAO = null;
    private RuntimeExceptionDao<Param, Integer> paramRuntimeDAO = null;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            Log.d("DB", "DatabaseHelper class created, onCreate() called");

            TableUtils.createTable(connectionSource, Book.class);
            TableUtils.createTable(connectionSource, Bookmark.class);
            TableUtils.createTable(connectionSource, Param.class);

            paramRuntimeDAO = getParamDAO();
            paramRuntimeDAO.createIfNotExists(new Param(Constants.SEEN_BOOK_TUTORIAL_DATABASE_VALUE, false));
            paramRuntimeDAO.createIfNotExists(new Param(Constants.SEEN_BOOKMARK_TUTORIAL_DATABASE_VALUE, false));
            paramRuntimeDAO.createIfNotExists(new Param(Constants.SEEN_CREATE_BOOK_TUTORIAL_DATABASE_VALUE, false));
            paramRuntimeDAO.createIfNotExists(new Param(Constants.TUTORIAL_MODE_DATABASE_VALUE, false));
            paramRuntimeDAO.createIfNotExists(new Param(Constants.ANIMATIONS_DATABASE_VALUE, false));

        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
    }

    public RuntimeExceptionDao<Book, Integer> getBookDAO() {
        if (bookRuntimeDAO == null) {
            bookRuntimeDAO = getRuntimeExceptionDao(Book.class);
        }
        return bookRuntimeDAO;
    }

    public RuntimeExceptionDao<Bookmark, Integer> getBookmarkDAO() {
        if (bookmarkRuntimeDAO == null) {
            bookmarkRuntimeDAO = getRuntimeExceptionDao(Bookmark.class);
        }
        return bookmarkRuntimeDAO;
    }

    public RuntimeExceptionDao<Param, Integer> getParamDAO() {
        if (paramRuntimeDAO == null) {
            paramRuntimeDAO = getRuntimeExceptionDao(Param.class);
        }
        return paramRuntimeDAO;
    }

    @Override
    public void close() {
        super.close();
        bookRuntimeDAO = null;
        bookmarkRuntimeDAO = null;
        paramRuntimeDAO = null;
    }
}