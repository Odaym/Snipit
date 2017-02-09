package com.om.snipit.activities;

import com.om.snipit.models.Book;
import com.om.snipit.repositories.BooksRepository;

import java.util.List;

public class BooksActivityPresenter {

    private BooksActivityView view;
    private BooksRepository booksRepository;

    public BooksActivityPresenter(BooksActivityView view, BooksRepository booksRepository) {
        this.view = view;
        this.booksRepository = booksRepository;
    }

    public void loadBooks() {
        List<Book> bookList;
        try {
            bookList = booksRepository.getBooks();
            if (bookList.isEmpty()) {
                view.displayNoBooks();
            } else {
                view.displayBooks(bookList);
            }
        } catch (Exception e) {
            view.displayError();
        }
    }
}
