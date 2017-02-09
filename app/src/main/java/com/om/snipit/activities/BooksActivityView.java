package com.om.snipit.activities;

import com.om.snipit.models.Book;

import java.util.List;

public interface BooksActivityView {

    void displayBooks(List<Book> bookList);

    void displayNoBooks();

    void displayError();
}
