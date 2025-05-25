package com.example.diplom_staff_application;

public class CartItem {
    private String id;
    private Product product;
    private int count;
    private String size;
    private int availableQuantity;
    private int productSizeId;

    public CartItem(String id, Product product, int count, String size, int availableQuantity, int productSizeId) {
        this.id = id;
        this.product = product;
        this.count = count;
        this.size = size;
        this.availableQuantity = availableQuantity;
        this.productSizeId = productSizeId;
    }

    public String getId() {
        return id;
    }

    public int getProductSizeId() {
        return productSizeId;
    }

    public Product getProduct() {
        return product;
    }

    public int getCount() {
        return count;
    }

    public String getSize() {
        return size;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}