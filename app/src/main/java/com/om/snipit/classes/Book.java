package com.om.snipit.classes;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;


@DatabaseTable(tableName = "Book")

public class Book {

    @DatabaseField (generatedId = true)
    int id;
    @DatabaseField
    String title;
    @DatabaseField
    String author;
    @DatabaseField
    String image_path;
    @DatabaseField
    String date_added;
    @DatabaseField
    int pages_count;
    @DatabaseField
    int page_reached;
    @DatabaseField
    int color_code;
    @DatabaseField
    int order;

    public Book() {
    }

    public Book(int id, String title, String author, String image_path, String date_added, int pages_count, int page_reached, int color_code, int order) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.image_path = image_path;
        this.date_added = date_added;
        this.pages_count = pages_count;
        this.page_reached = page_reached;
        this.color_code = color_code;
        this.order = order;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getImagePath() {
        return image_path;
    }

    public void setImagePath(String image_path) {
        this.image_path = image_path;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate_added() {
        return date_added;
    }

    public void setDate_added(String date_added) {
        this.date_added = date_added;
    }

    public int getColorCode() {
        return color_code;
    }

    public void setColorCode(int color_code) {
        this.color_code = color_code;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getPages_count() {
        return pages_count;
    }

    public int getPage_reached() {
        return page_reached;
    }

    public void setPage_reached(int page_reached) {
        this.page_reached = page_reached;
    }

    public void setPages_count(int pages_count) {
        this.pages_count = pages_count;
    }
}
