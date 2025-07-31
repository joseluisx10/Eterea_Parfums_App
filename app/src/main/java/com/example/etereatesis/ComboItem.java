package com.example.etereatesis;

public class ComboItem {
    private int id;
    private String name;

    public ComboItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // Se muestra el nombre en el Spinner
    @Override
    public String toString() {
        return name;
    }
}
