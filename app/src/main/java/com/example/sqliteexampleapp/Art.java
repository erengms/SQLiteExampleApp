package com.example.sqliteexampleapp;

import android.graphics.Bitmap;

public class Art {
    int id;
    String name;
    Bitmap bitmap;

    public Art(int id, String name, Bitmap bitmap) {
        this.id = id;
        this.name = name;
        this.bitmap = bitmap;
    }
}
