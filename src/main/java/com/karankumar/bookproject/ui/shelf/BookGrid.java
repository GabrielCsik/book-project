/*
    The book project lets a user keep track of different books they would like to read, are currently
    reading, have read or did not finish.
    Copyright (C) 2020  Karan Kumar

    This program is free software: you can redistribute it and/or modify it under the terms of the
    GNU General Public License as published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT ANY
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
    PURPOSE.  See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with this program.
    If not, see <https://www.gnu.org/licenses/>.
 */

package com.karankumar.bookproject.ui.shelf;

import com.karankumar.bookproject.backend.entity.Author;
import com.karankumar.bookproject.backend.entity.Book;
import com.karankumar.bookproject.backend.entity.PredefinedShelf;
import com.karankumar.bookproject.backend.service.BookService;
import com.karankumar.bookproject.backend.utils.CustomShelfUtils;
import com.karankumar.bookproject.backend.utils.PredefinedShelfUtils;
import com.karankumar.bookproject.ui.book.BookForm;
import com.karankumar.bookproject.ui.shelf.component.BookGridColumn;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import lombok.extern.java.Log;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.karankumar.bookproject.backend.utils.ShelfUtils.isAllBooksShelf;

@Log
public class BookGrid {
    private final Grid<Book> bookGrid;
    
    private final PredefinedShelfUtils predefinedShelfUtils;
    private final CustomShelfUtils customShelfUtils;
    private final BookService bookService;
    private String chosenShelf;

    BookGrid(PredefinedShelfUtils predefinedShelfUtils, CustomShelfUtils customShelfUtils, BookService bookService) {
        this.bookService = bookService;
        this.bookGrid = new Grid<>(Book.class);
        this.predefinedShelfUtils = predefinedShelfUtils;
        this.customShelfUtils = customShelfUtils;
        configure();
    }

    public void configure() {
        bookGrid.setHeightByRows(true);
        new BookGridColumn(bookGrid).addColumns();
    }

    void bind(BookForm bookForm) {
        bookGrid.asSingleSelect().addValueChangeListener(event -> {
            Book book = event.getValue();

            if (book != null && bookForm != null) {
                bookForm.setBook(book);
                bookForm.openForm();
            }
        });
    }

    public void toggleColumnVisibility(String columnKey, boolean showColumn) {
        if (bookGrid.getColumnByKey(columnKey) == null) {
            LOGGER.log(Level.SEVERE, "Key is null: " + columnKey);
        } else {
            bookGrid.getColumnByKey(columnKey)
                    .setVisible(showColumn);
        }
    }

    public Grid<Book> get() {
        return bookGrid;
    }

    void update(String chosenShelf, BookFilters bookFilters) {
        if (chosenShelf == null) {
            LOGGER.log(Level.FINEST, "Chosen shelf is null");
            return;
        }

        Set<Book> books = getBooks(chosenShelf);
        for (int i = 0; i < 150; i++) {
            books.add(new Book("title" + i, new Author("firstname" + i, "lastname" + i), new PredefinedShelf(PredefinedShelf.ShelfName.DID_NOT_FINISH)));
        }
        populateGridWithBooks(books, bookFilters);
    }

    private Set<Book> getBooks(String chosenShelf) {
        if (isAllBooksShelf(chosenShelf)) {
            return predefinedShelfUtils.getBooksInAllPredefinedShelves();
        }

        if (PredefinedShelfUtils.isPredefinedShelf(chosenShelf)) {
            return predefinedShelfUtils.getBooksInChosenPredefinedShelf(chosenShelf);
        }

        return customShelfUtils.getBooksInCustomShelf(chosenShelf);
    }

    private void populateGridWithBooks(Set<Book> books, BookFilters bookFilters) {
//        List<Book> items = filterShelf(books, bookFilters);
        DataProvider<Book, Void> dataProvider =
                DataProvider.fromFilteringCallbacks(
                        query -> {
                            int indexOfFirstItemToLoad = query.getOffset();
                            int itemsToLoad = query.getLimit();
                            List<Book> allBooks = bookService.findAll(
                                    PageRequest.of(indexOfFirstItemToLoad, itemsToLoad)
                            ).toList();
                            return allBooks.stream();
                        },
                        // TODO: handle filtering
                        query -> Math.toIntExact(bookService.count()));
        bookGrid.setDataProvider(dataProvider);
//        bookGrid.setItems(items);
    }

    private List<Book> filterShelf(List<Book> books, BookFilters bookFilters) {
        return books.stream()
                    .filter(bookFilters::apply)
                    .collect(Collectors.toList());
    }

    public void setChossenShelf(String chosenShelf) {
        chosenShelf = chosenShelf;
    }
}
