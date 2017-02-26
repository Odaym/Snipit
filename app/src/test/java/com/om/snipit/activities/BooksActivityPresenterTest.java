package com.om.snipit.activities;

import com.om.snipit.models.Book;
import com.om.snipit.repositories.BooksRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BooksActivityPresenterTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    BooksRepository booksRepository;

    @Mock
    BooksActivityView view;

    BooksActivityPresenter presenter;
    private final List<Book> MANY_BOOKS = Arrays.asList(new Book(), new Book(), new Book());

    @Before
    public void setUp() {
        presenter = new BooksActivityPresenter(view, booksRepository, Schedulers.trampoline());
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
    }

    @After
    public void cleanUp() {
        RxJavaPlugins.reset();
    }

    @Test
    public void shouldPassBooksToView() {
        when(booksRepository.getBooks()).thenReturn(Single.just(MANY_BOOKS));

        presenter.loadBooks();

        verify(view).displayBooks(MANY_BOOKS);
    }

    @Test
    public void shouldHandleNoBooksFound() throws InterruptedException {
        when(booksRepository.getBooks()).thenReturn(Single.just(Collections.emptyList()));

        presenter.loadBooks();

        verify(view).displayNoBooks();
    }

    @Test
    public void shouldHandleError() {
        when(booksRepository.getBooks()).thenReturn(Single.<List<Book>>error(new Throwable("boom")));

        presenter.loadBooks();

        verify(view).displayError();
    }
}