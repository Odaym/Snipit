package com.om.snipit.activities;

import com.om.snipit.models.Book;
import com.om.snipit.repositories.BooksRepository;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;

import static java.util.Collections.EMPTY_LIST;
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
    presenter = new BooksActivityPresenter(view, booksRepository);
  }

  @Test public void shouldPassBooksToView() {
    when(booksRepository.getBooks()).thenReturn(MANY_BOOKS);

    presenter.loadBooks();

    verify(view).displayBooks(MANY_BOOKS);
  }

  @Test public void shouldHandleNoBooksFound() {
    when(booksRepository.getBooks()).thenReturn(EMPTY_LIST);

    presenter.loadBooks();

    verify(view).displayNoBooks();
  }

  @Test public void shouldHandleError() {
    when(booksRepository.getBooks()).thenThrow(new RuntimeException("boom"));

    presenter.loadBooks();

    verify(view).displayError();
  }
}