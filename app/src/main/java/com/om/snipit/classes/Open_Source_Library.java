package com.om.snipit.classes;

public class Open_Source_Library {
    private String name;
    private String website;
    private String description;

    public Open_Source_Library(String name, String website, String description) {
        this.name = name;
        this.website = website;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
