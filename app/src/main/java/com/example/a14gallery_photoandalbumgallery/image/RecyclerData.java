package com.example.a14gallery_photoandalbumgallery.image;

public class RecyclerData {
    public enum Type {
        Label,
        Image
    }

    public final Type type;
    public String labelData;
    public final Image imageData;
    public final int index;

    public RecyclerData(Type type, String labelData, Image imageData, int index) {
        this.type = type;
        this.labelData = labelData;
        this.imageData = imageData;
        this.index = index;
    }
}
