package com.om.snipit.repositories;

import com.om.snipit.models.Book;
import java.util.List;

import io.reactivex.Single;

public interface BooksRepository {

  Single<List<Book>> getBooks();
}
