package com.om.snipit.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "Snippet")

public class Snippet implements Parcelable {

    public static final String BOOK_ID_FIELD_NAME = "book_id";

    @DatabaseField(generatedId = true)
    int id;
    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = BOOK_ID_FIELD_NAME)
    private Book book;
    @DatabaseField
    String name;
    @DatabaseField
    int page_number;
    @DatabaseField
    String image_path;
    @DatabaseField
    String aws_image_path;
    @DatabaseField
    String ocr_content;
    @DatabaseField
    String date_added;
    @DatabaseField
    int order;
    @DatabaseField
    int views;
    @DatabaseField
    String note;
    @DatabaseField
    int isNoteShowing;
    @DatabaseField
    String screen_name;

    public Snippet() {
    }

    private Snippet(Parcel in) {
        id = in.readInt();
        name = in.readString();
        page_number = in.readInt();
        image_path = in.readString();
        aws_image_path = in.readString();
        ocr_content = in.readString();
        date_added = in.readString();
        order = in.readInt();
        views = in.readInt();
        note = in.readString();
        isNoteShowing = in.readInt();
        screen_name = in.readString();
    }

    public static final Parcelable.Creator<Snippet> CREATOR = new Parcelable.Creator<Snippet>() {
        public Snippet createFromParcel(Parcel in) {
            return new Snippet(in);
        }

        public Snippet[] newArray(int size) {
            return new Snippet[size];
        }
    };

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

    public String getAws_image_path() {
        return aws_image_path;
    }

    public void setAws_image_path(String aws_image_path) {
        this.aws_image_path = aws_image_path;
    }

    public String getOcr_content() {
        return ocr_content;
    }

    public void setOcr_content(String ocr_content) {
        this.ocr_content = ocr_content;
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

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
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

    public String getScreen_name() {
        return screen_name;
    }

    public void setScreen_name(String screen_name) {
        this.screen_name = screen_name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(id);
        out.writeString(name);
        out.writeInt(page_number);
        out.writeString(image_path);
        out.writeString(aws_image_path);
        out.writeString(ocr_content);
        out.writeString(date_added);
        out.writeInt(order);
        out.writeInt(views);
        out.writeString(note);
        out.writeInt(isNoteShowing);
        out.writeString(screen_name);
    }
}
