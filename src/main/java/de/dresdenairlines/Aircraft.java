package de.dresdenairlines;
public class Aircraft {
    public final String id,type,model,registration;
    public final int seats,range;
    public double condition=1.0;
    public long nextAvailable=0;
    public Aircraft(String id,String type,String model,String registration,int seats,int range){
        this.id=id;this.type=type;this.model=model;this.registration=registration;this.seats=seats;this.range=range;
    }
}
