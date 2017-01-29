package com.om.snipit.abbyy_ocr;

import java.io.Reader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Task {
  public TaskStatus Status = TaskStatus.Unknown;
  public String Id;
  public String DownloadUrl;
  public Task(Reader reader) throws Exception {
    // Read all text into string
    // String data = new Scanner(reader).useDelimiter("\\A").next();
    // Read full task information from xml
    InputSource source = new InputSource();
    source.setCharacterStream(reader);
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = builder.parse(source);

    NodeList taskNodes = doc.getElementsByTagName("task");
    Element task = (Element) taskNodes.item(0);

    parseTask(task);
  }

  public Boolean isTaskActive() {
    if (Status == TaskStatus.Queued || Status == TaskStatus.InProgress) {
      return true;
    }

    return false;
  }

  private void parseTask(Element taskElement) {
    Id = taskElement.getAttribute("id");
    Status = parseTaskStatus(taskElement.getAttribute("status"));
    if (Status == TaskStatus.Completed) {
      DownloadUrl = taskElement.getAttribute("resultUrl");
    }
  }

  private TaskStatus parseTaskStatus(String status) {
    if (status.equals("Submitted")) {
      return TaskStatus.Submitted;
    } else if (status.equals("Queued")) {
      return TaskStatus.Queued;
    } else if (status.equals("InProgress")) {
      return TaskStatus.InProgress;
    } else if (status.equals("Completed")) {
      return TaskStatus.Completed;
    } else if (status.equals("ProcessingFailed")) {
      return TaskStatus.ProcessingFailed;
    } else if (status.equals("Deleted")) {
      return TaskStatus.Deleted;
    } else if (status.equals("NotEnoughCredits")) {
      return TaskStatus.NotEnoughCredits;
    } else {
      return TaskStatus.Unknown;
    }
  }

  public enum TaskStatus {
    Unknown, Submitted, Queued, InProgress, Completed, ProcessingFailed, Deleted, NotEnoughCredits
  }
}
