package com.om.snipit.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "Param")

public class Param {

  @DatabaseField(id = true) private int id;
  @DatabaseField private boolean enabled;

  public Param() {
    super();
  }

  public Param(int id, boolean enabled) {
    this.id = id;
    this.enabled = enabled;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
