package com.om.snipit.activities;

import com.om.snipit.models.Book;
import com.om.snipit.repositories.BooksRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class BooksActivityPresenterTest {

  @Test public void shouldPassBooksToView() {

    // given
    BooksActivityView view = new MockView();
    BooksRepository booksRepository = new MockBooksRepository(true);

    // when
    BooksActivityPresenter presenter = new BooksActivityPresenter(view, booksRepository);
    presenter.loadBooks();

    // then
    Assert.assertEquals(true, ((MockView) view).displayBooksWithBooksCalled);
  }

  @Test public void shouldHandleNoBooksFound() {
    BooksActivityView view = new MockView();
    BooksRepository booksRepository = new MockBooksRepository(false);

    BooksActivityPresenter presenter = new BooksActivityPresenter(view, booksRepository);
    presenter.loadBooks();

    Assert.assertEquals(true, ((MockView) view).displayBooksWithNoBooksCalled);
  }

  private class MockView implements BooksActivityView {

    boolean displayBooksWithBooksCalled;
    boolean displayBooksWithNoBooksCalled;

    @Override public void displayBooks(List<Book> bookList) {
      if (bookList.size() == 3) displayBooksWithBooksCalled = true;
    }

    @Override public void displayNoBooks() {
      displayBooksWithNoBooksCalled = true;
    }
  }

  private class MockBooksRepository implements BooksRepository {

    private boolean returnSomeBooks;

    public MockBooksRepository(boolean returnSomeBooks) {
      this.returnSomeBooks = returnSomeBooks;
    }

    @Override public List<Book> getBooks() {

      if (returnSomeBooks) {
        return Arrays.asList(new Book(), new Book(), new Book());
      } else {
        return Collections.emptyList();
      }
    }
  }
}