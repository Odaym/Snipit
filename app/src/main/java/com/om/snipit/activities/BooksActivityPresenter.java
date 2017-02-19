package com.om.snipit.activities;

import com.om.snipit.models.Book;
import com.om.snipit.repositories.BooksRepository;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;

public class BooksActivityPresenter {

    private BooksActivityView view;
    private BooksRepository booksRepository;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public BooksActivityPresenter(BooksActivityView view, BooksRepository booksRepository) {
        this.view = view;
        this.booksRepository = booksRepository;
    }

    public void loadBooks() {
        compositeDisposable.add(booksRepository.getBooks()
                .subscribeWith(new DisposableSingleObserver<List<Book>>() {
                    @Override
                    public void onSuccess(List<Book> bookList) {
                        if (bookList.isEmpty()) {
                            view.displayNoBooks();
                        } else {
                            view.displayBooks(bookList);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        view.displayError();
                    }
                }));
    }

    public void unsubscribe() {
        compositeDisposable.clear();
    }
}
