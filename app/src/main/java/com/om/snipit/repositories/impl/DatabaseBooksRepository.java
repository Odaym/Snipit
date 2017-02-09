package com.om.snipit.repositories.impl;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.om.snipit.classes.DatabaseHelper;
import com.om.snipit.models.Book;
import com.om.snipit.repositories.BooksRepository;

import android.content.Context;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;


public class DatabaseBooksRepository implements BooksRepository {

    private final DatabaseHelper databaseHelper;

    public DatabaseBooksRepository(Context context) {
        databaseHelper = OpenHelperManager.getHelper(context, DatabaseHelper.class);
    }

    @Override
    public List<Book> getBooks() {
        try {
            return databaseHelper.getBookDAO().queryBuilder()
                    .orderBy("order", true)
                    .query();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
}
