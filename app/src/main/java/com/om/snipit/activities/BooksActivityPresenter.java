package com.om.snipit.activities;

import com.om.snipit.models.Book;
import com.om.snipit.repositories.BooksRepository;

import java.util.List;

import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class BooksActivityPresenter {

    private BooksActivityView view;
    private BooksRepository booksRepository;
    private final Scheduler mainScheduler;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public BooksActivityPresenter(BooksActivityView view, BooksRepository booksRepository, Scheduler mainScheduler) {
        this.view = view;
        this.booksRepository = booksRepository;
        this.mainScheduler = mainScheduler;
    }

    public void loadBooks() {

        compositeDisposable.add(booksRepository.getBooks()
                .subscribeOn(Schedulers.io())
                .observeOn(mainScheduler)
                .subscribeWith(new DisposableSingleObserver<List<Book>>() {
                    @Override
                    public void onSuccess(List<Book> bookList) {
                        System.out.println("Thread subscribe(): " + Thread.currentThread().getId());
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
