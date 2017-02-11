package com.om.snipit.activities;

import com.om.snipit.models.Book;
import com.om.snipit.repositories.BooksRepository;

import java.util.List;

import io.reactivex.functions.Consumer;

public class BooksActivityPresenter {

    private BooksActivityView view;
    private BooksRepository booksRepository;

    public BooksActivityPresenter(BooksActivityView view, BooksRepository booksRepository) {
        this.view = view;
        this.booksRepository = booksRepository;
    }

    public void loadBooks() {
        booksRepository.getBooks()
                .subscribe(new Consumer<List<Book>>() {
                    @Override
                    public void accept(List<Book> bookList) throws Exception {
                        if (bookList.isEmpty()) {
                            view.displayNoBooks();
                        } else {
                            view.displayBooks(bookList);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        view.displayError();
                    }
                });
    }
}
