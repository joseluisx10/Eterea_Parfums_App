package com.example.etereatesis.models;

// Clase para representar cada elemento de los spinners
public class ComboItem {
    private int id;
    private String name;
    public ComboItem(int id, String name) {
        this.id = id;
        this.name = name;
    }
    public int getId() { return id; }
    public String getName() { return name; }
    @Override
    public String toString() { return name; }
}
