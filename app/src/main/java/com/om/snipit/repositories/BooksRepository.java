package com.om.snipit.repositories;

import com.om.snipit.models.Book;
import java.util.List;

public interface BooksRepository {

  List<Book> getBooks();

}
