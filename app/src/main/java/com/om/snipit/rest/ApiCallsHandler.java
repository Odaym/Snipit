package com.om.snipit.rest;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApiCallsHandler {

  public static void uploadBook(UserObject userObj, String book_title,
      List<SnippetObject> snippetObjs, final Callback<DefaultGetResponse> callback) {
    Call<DefaultGetResponse> apiCall = new RestClient().getApiService()
        .uploadBook(
            new JSONBookRequestParent(new UploadBookRequest(userObj, book_title, snippetObjs)));

    apiCall.enqueue(new Callback<DefaultGetResponse>() {
      @Override
      public void onResponse(Call<DefaultGetResponse> call, Response<DefaultGetResponse> response) {
        callback.onResponse(call, response);
      }

      @Override public void onFailure(Call<DefaultGetResponse> call, Throwable t) {
        callback.onFailure(call, t);
      }
    });
  }

  public static class JSONBookRequestParent {
    UploadBookRequest json_book;

    public JSONBookRequestParent(UploadBookRequest json_book) {
      this.json_book = json_book;
    }
  }

  public static class UploadBookRequest {
    final UserObject user;
    final String book_title;
    final List<SnippetObject> snippets;

    UploadBookRequest(UserObject user, String book_title, List<SnippetObject> snippets) {
      this.user = user;
      this.book_title = book_title;
      this.snippets = snippets;
    }
  }

  public static class UserObject {
    final String email;
    final String full_name;

    public UserObject(String email, String full_name) {
      this.email = email;
      this.full_name = full_name;
    }
  }

  public static class SnippetObject {
    final String name;
    final int page_number;
    final String note;
    final String ocr_content;
    final String aws_image_path;

    public SnippetObject(String name, int page_number, String note, String ocr_content,
        String aws_image_path) {
      this.name = name;
      this.page_number = page_number;
      this.note = note;
      this.ocr_content = ocr_content;
      this.aws_image_path = aws_image_path;
    }
  }
}
