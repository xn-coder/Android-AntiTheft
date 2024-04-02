package com.xncoder.advanceprotection;

public class CustomItems {
    private String title;
    private String description;
    private boolean isSelected;

    public CustomItems(String title, String description, boolean isSelected) {
        this.title = title;
        this.description = description;
        this.isSelected = isSelected;
    }

    public String getName() { return title; }

    public String getNumber() { return description; }

    public boolean getSelected() { return isSelected; }

    public void setSelected(boolean isSelected) { this.isSelected = isSelected; }
}
