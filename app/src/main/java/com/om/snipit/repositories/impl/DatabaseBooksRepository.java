package com.om.snipit.repositories.impl;

import com.om.snipit.classes.DatabaseHelper;
import com.om.snipit.models.Book;
import com.om.snipit.repositories.BooksRepository;

import java.util.List;

import io.reactivex.Single;


public class DatabaseBooksRepository implements BooksRepository {

    private final DatabaseHelper databaseHelper;

    public DatabaseBooksRepository(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    @Override
    public Single<List<Book>> getBooks() {
        return Single.fromCallable(() -> {
            try {
                System.out.println("Thread db: " + Thread.currentThread().getId());
                return databaseHelper.getBookDAO().queryBuilder()
                        .orderBy("order", true)
                        .query();
            } catch (Exception e) {
                throw new RuntimeException();
            }
        });
    }
}
