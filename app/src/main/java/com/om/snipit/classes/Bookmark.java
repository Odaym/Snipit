package com.om.snipit.classes;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "Bookmark")

public class Bookmark {

    @DatabaseField(generatedId = true)
    private int id;
    @DatabaseField(foreign = true)
    private int book_id;
    @DatabaseField
    private String name;
    @DatabaseField
    private int page_number;
    @DatabaseField
    private String image_path;
    @DatabaseField
    private String date_added;
    @DatabaseField
    private int order;
    @DatabaseField
    private boolean favorite;
    @DatabaseField
    private int views;
    @DatabaseField
    private String note;
    @DatabaseField
    private int times_painted;
    @DatabaseField
    private int isNoteShowing;
    @DatabaseField
    private boolean deleted;

    public Bookmark() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPage_number() {
        return page_number;
    }

    public void setPage_number(int page_number) {
        this.page_number = page_number;
    }

    public String getImage_path() {
        return image_path;
    }

    public void setImage_path(String image_path) {
        this.image_path = image_path;
    }

    public String getDate_added() {
        return date_added;
    }

    public void setDate_added(String date_added) {
        this.date_added = date_added;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public int getBookId() {
        return book_id;
    }

    public void setBookId(int book_id) {
        this.book_id = book_id;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getIsNoteShowing() {
        return isNoteShowing;
    }

    public void setIsNoteShowing(int isNoteShowing) {
        this.isNoteShowing = isNoteShowing;
    }

    public int getTimes_painted() {
        return times_painted;
    }

    public void setTimes_painted(int times_painted) {
        this.times_painted = times_painted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
