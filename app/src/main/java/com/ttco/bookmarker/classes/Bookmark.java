package com.ttco.bookmarker.classes;

import android.os.Parcel;
import android.os.Parcelable;

public class Bookmark implements Parcelable {
    public static final Parcelable.Creator<Bookmark> CREATOR = new Parcelable.Creator<Bookmark>() {
        public Bookmark createFromParcel(Parcel in) {
            return new Bookmark(in);
        }

        public Bookmark[] newArray(int size) {
            return new Bookmark[size];
        }
    };

    private int id;
    private String name;
    private int page_number;
    private String image_path;
    private String date_added;
    private int order;
    private int views;
    private int book_id;

    public Bookmark() {
    }

    public Bookmark(Parcel in) {
        id = in.readInt();
        name = in.readString();
        page_number = in.readInt();
        image_path = in.readString();
        date_added = in.readString();
        order = in.readInt();
        views = in.readInt();
        book_id = in.readInt();
    }


    public Bookmark(int id, String name, int page_number, String image_path, String date_added, int order, int book_id, int views) {
        this.id = id;
        this.name = name;
        this.page_number = page_number;
        this.image_path = image_path;
        this.date_added = date_added;
        this.order = order;
        this.views = views;
        this.book_id = book_id;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeInt(page_number);
        dest.writeString(image_path);
        dest.writeString(date_added);
        dest.writeInt(order);
        dest.writeInt(views);
        dest.writeInt(book_id);
    }
}
