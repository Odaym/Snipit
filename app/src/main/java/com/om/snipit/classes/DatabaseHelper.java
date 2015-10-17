package com.om.snipit.classes;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.om.snipit.models.Book;
import com.om.snipit.models.Channel;
import com.om.snipit.models.Snippet;

import java.sql.SQLException;

/**
 * Database helper class used to manage the creation and upgrading of your database. This class also usually provides
 * the DAOs used by the other classes.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String DATABASE_NAME = "Snipit.db";

    private static final int DATABASE_VERSION = 1;

    private RuntimeExceptionDao<Book, Integer> bookRuntimeDAO = null;
    private RuntimeExceptionDao<Snippet, Integer> snippetRuntimeDAO = null;
    private RuntimeExceptionDao<Channel, Integer> channelRuntimeDAO = null;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, Book.class);
            TableUtils.createTable(connectionSource, Snippet.class);
            TableUtils.createTable(connectionSource, Channel.class);
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

    public RuntimeExceptionDao<Snippet, Integer> getSnippetDAO() {
        if (snippetRuntimeDAO == null) {
            snippetRuntimeDAO = getRuntimeExceptionDao(Snippet.class);
        }
        return snippetRuntimeDAO;
    }

    public RuntimeExceptionDao<Channel, Integer> getChannelDAO() {
        if (channelRuntimeDAO == null) {
            channelRuntimeDAO = getRuntimeExceptionDao(Channel.class);
        }
        return channelRuntimeDAO;
    }

    @Override
    public void close() {
        super.close();
        bookRuntimeDAO = null;
        snippetRuntimeDAO = null;
        channelRuntimeDAO = null;
    }
}