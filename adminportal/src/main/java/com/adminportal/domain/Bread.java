package com.adminportal.domain;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;

@Entity
public class Bread {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String title;  
    private String baker; 
    private String type;  
    private String bakeDate; 
    private String ingredients; 
    private String category;
    private double weight; 
    private String size; 
    private String format;
    private String sku; 
    private double shippingWeight;
    private double listPrice;
    private double ourPrice;
    private boolean active = true; 
    
    @Column(columnDefinition="text")
    private String description;
    private int inStockNumber;
    
    @Transient
    private MultipartFile breadImage; 


	@OneToMany(mappedBy = "bread")
	@JsonIgnore
	private List<BreadToCartItem> breadToCartItemList;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

  

    public String getBaker() {
        return baker;
    }

    public void setBaker(String baker) {
        this.baker = baker;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public double getShippingWeight() {
        return shippingWeight;
    }

    public void setShippingWeight(double shippingWeight) {
        this.shippingWeight = shippingWeight;
    }

    public double getListPrice() {
        return listPrice;
    }

    public void setListPrice(double listPrice) {
        this.listPrice = listPrice;
    }

    public double getOurPrice() {
        return ourPrice;
    }

    public void setOurPrice(double ourPrice) {
        this.ourPrice = ourPrice;
    }

  

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getInStockNumber() {
        return inStockNumber;
    }

    public void setInStockNumber(int inStockNumber) {
        this.inStockNumber = inStockNumber;
    }

    public MultipartFile getImage() {
        return breadImage;
    }

    public void setImage(MultipartFile image) {
        this.breadImage = image;
    }

    public MultipartFile getBreadImage() {
        return breadImage;
    }

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}


	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getBakeDate() {
		return bakeDate;
	}

	public void setBakeDate(String bakeDate) {
		this.bakeDate = bakeDate;
	}

	public List<BreadToCartItem> getBreadToCartItemList() {
		return breadToCartItemList;
	}

	public void setBreadToCartItemList(List<BreadToCartItem> breadToCartItemList) {
		this.breadToCartItemList = breadToCartItemList;
	}

	public void setBreadImage(MultipartFile breadImage) {
		this.breadImage = breadImage;
	}
	
	
}

