package com.ttco.bookmarker.classes;

public class Param {

	private int number;
	private String value;
	private int id;

	public Param() {
		super();
	}

	public Param(int number, String value, int id) {
		super();
		this.id = id;
		this.number = number;
		this.value = value;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
