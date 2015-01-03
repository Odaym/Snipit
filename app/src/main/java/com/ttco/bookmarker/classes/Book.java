package com.ttco.bookmarker.classes;

import android.os.Parcel;
import android.os.Parcelable;

public class Book implements Parcelable {
    public static final Parcelable.Creator<Book> CREATOR = new Parcelable.Creator<Book>() {
        public Book createFromParcel(Parcel in) {
            return new Book(in);
        }

        public Book[] newArray(int size) {
            return new Book[size];
        }
    };

    private int id;
    private String title;
    private String author;
    private String image_path;
    private String date_added;
    private int color_code;
    private int order;

    public Book() {
    }

    public Book(Parcel in) {
        id = in.readInt();
        title = in.readString();
        author = in.readString();
        image_path = in.readString();
        date_added = in.readString();
        color_code = in.readInt();
        order = in.readInt();
    }

    public Book(int id, String title, String author, String image_path, String date_added, int color_code, int order) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.image_path = image_path;
        this.date_added = date_added;
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

    @Override
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(author);
        dest.writeString(image_path);
        dest.writeString(date_added);
        dest.writeInt(color_code);
        dest.writeInt(order);
    }
}
